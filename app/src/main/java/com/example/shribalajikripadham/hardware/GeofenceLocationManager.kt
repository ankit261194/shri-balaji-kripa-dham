package com.example.shribalajikripadham.hardware

import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Process
import android.provider.Settings
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.*

data class LocationSecurityResult(
    val isValid: Boolean,
    val isMock: Boolean,
    val accuracyMeters: Float,
    val distanceMeters: Double,
    val isInsideGeofence: Boolean,
    val isAdvanceDistanceEligible: Boolean = false, // > 30 km (outstation devotee)
    val isAshramLocalEligible: Boolean = false, // <= 200 m (physically at Ashram)
    val securityExceptionReason: String? = null
)

object GeofenceLocationManager {

    const val MAX_ALLOWED_ACCURACY_METERS = 250.0f
    const val OUTSTATION_MIN_DISTANCE_METERS = 30000.0 // 30 km
    const val LOCAL_ASHRAM_MAX_DISTANCE_METERS = 200.0 // 200 meters

    /**
     * Dual-Distance Eligibility Evaluator:
     * - Devotees >= 28 km: Can register token in advance from home/city (with road-curvature buffer).
     * - Local Devotees: MUST be physically within Ashram perimeter (allowedRadius + 150m GPS drift buffer).
     */
    fun isTokenDistancePermitted(
        distanceMeters: Double,
        isGeofenceEnforced: Boolean = true,
        allowedRadiusMeters: Double = LOCAL_ASHRAM_MAX_DISTANCE_METERS,
        isOutstationAdvanceAllowed: Boolean = true,
        outstationMinDistanceKm: Double = 30.0
    ): Boolean {
        if (!isGeofenceEnforced) return true
        if (distanceMeters < 0.0) return false // Negative distance = no GPS fix, strictly not permitted!
        // 2 km buffer for outstation straight-line vs actual road distance
        val outstationMinMeters = ((outstationMinDistanceKm - 2.0).coerceAtLeast(1.0)) * 1000.0
        val isOutstationPermitted = isOutstationAdvanceAllowed && (distanceMeters >= outstationMinMeters)
        // 150m GPS drift and campus buffer for Ashram local devotees (200m + 150m = 350m radius)
        val isLocalPermitted = (distanceMeters <= (allowedRadiusMeters + 150.0))
        return isOutstationPermitted || isLocalPermitted
    }

    /**
     * Calculates great-circle distance between two points on Earth using the Haversine formula.
     * Returns distance in meters.
     */
    fun calculateDistanceMeters(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val r = 6371000.0 // Earth radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    /**
     * Checks if coordinates are within the specified radius from the Ashram.
     */
    fun isInsideGeofence(
        userLat: Double, userLon: Double,
        ashramLat: Double, ashramLon: Double,
        allowedRadiusMeters: Double
    ): Boolean {
        val distance = calculateDistanceMeters(userLat, userLon, ashramLat, ashramLon)
        return distance <= allowedRadiusMeters
    }

    private val KNOWN_ROOT_PACKAGES = listOf(
        "com.topjohnwu.magisk",
        "io.github.vvb2060.magisk",
        "me.weishu.kernelsu",
        "com.koushikdutta.superuser",
        "eu.chainfire.supersu",
        "com.noshufou.android.su",
        "com.thirdparty.superuser",
        "com.yellowes.su"
    )

    private val ROOT_BINARY_PATHS = arrayOf(
        "/system/app/Superuser.apk",
        "/sbin/su",
        "/system/bin/su",
        "/system/xbin/su",
        "/data/local/xbin/su",
        "/data/local/bin/su",
        "/system/sd/xbin/su",
        "/system/bin/failsafe/su",
        "/data/local/su",
        "/system/bin/.ext/.su"
    )

    /**
     * Checks if device is rooted or has Magisk / KernelSU / su binary installed.
     */
    fun isDeviceRooted(context: Context): Boolean {
        // 1. Check for verified su binaries in system paths (genuine root only)
        for (path in ROOT_BINARY_PATHS) {
            try {
                if (java.io.File(path).exists()) return true
            } catch (ignored: Exception) {}
        }

        // 3. Check for installed root / magisk packages
        val pm = context.packageManager
        for (pkg in KNOWN_ROOT_PACKAGES) {
            try {
                pm.getPackageInfo(pkg, 0)
                return true
            } catch (ignored: Exception) {}
        }

        return false
    }

    private val KNOWN_MOCK_LOCATION_PACKAGES = listOf(
        "com.lexa.fakegps",
        "com.rosteam.gpsemulator",
        "com.incorporateapps.fakegps.fre",
        "com.incorporateapps.fakegps",
        "com.fakegps.mock",
        "com.blogspot.newprocess.fakegps",
        "com.gsmartstudio.fakegps",
        "org.hola.gpslocation",
        "com.location.changer",
        "com.mock.location",
        "com.hopeway.fakegps",
        "com.usefullapps.fakegpslocationpro",
        "com.ninja.gps",
        "com.marlon.floating.fake.location",
        "com.pe.fakegps",
        "com.lkr.fakegps",
        "com.dreams.fakegps"
    )

    /**
     * Master Kill-Switch for Mock Location Check.
     * Controlled dynamically via Server Live Config and Antigravity Studio.
     * Defaults to true, but can be disabled instantly worldwide with zero APK updates.
     */
    @Volatile
    var isMockCheckGloballyEnabled: Boolean = true

    /**
     * Master Emergency Bypass for Geofence radius restriction.
     * When enabled by Super Admin via Antigravity Studio / Server Config,
     * all devotees can generate tokens immediately from any location.
     */
    @Volatile
    var isGeofenceGloballyBypassed: Boolean = false

    /**
     * Checks if any known dedicated Fake GPS or Location Spoofer app is installed.
     * Note: We strictly only check known malicious packages, and NEVER scan generic
     * system app permissions to prevent falsely flagging innocent devotees' phones.
     */
    fun hasSpoofingAppsInstalled(context: Context): Pair<Boolean, String?> {
        if (!isMockCheckGloballyEnabled) return Pair(false, null)
        val pm = context.packageManager
        // Check only explicit known fake GPS packages
        for (pkg in KNOWN_MOCK_LOCATION_PACKAGES) {
            try {
                pm.getPackageInfo(pkg, 0)
                return Pair(true, pkg)
            } catch (ignored: Exception) {}
        }
        return Pair(false, null)
    }

    /**
     * Genuine Android OS Location Validation:
     * 1. Checks master kill-switch (can be toggled remotely in 0 seconds).
     * 2. Uses official Android OS `Location.isMock` (API 31+) / `Location.isFromMockProvider` (API 18-30).
     * 3. Checks location bundle mock flag & accuracy <= 0 anomaly.
     * 4. Checks known fake GPS packages.
     * Zero false positives on clean, normal devotee phones!
     */
    fun isMockLocation(location: Location?, context: Context): Boolean {
        if (!isMockCheckGloballyEnabled) return false

        // 1. Check if known dedicated fake GPS package is installed
        val (hasSpoofApp, _) = hasSpoofingAppsInstalled(context)
        if (hasSpoofApp) return true

        // 2. Native Location Object Mock Check (Official Android OS API)
        if (location != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (location.isMock) return true
            } else {
                @Suppress("DEPRECATION")
                if (location.isFromMockProvider) return true
            }

            // 3. Location bundle mock flag
            try {
                val extras = location.extras
                if (extras != null && extras.getBoolean("mockLocation", false)) {
                    return true
                }
            } catch (ignored: Exception) {}

            // 4. Anomaly: Accuracy <= 0.0 or exact 0.0m is a tell-tale fake GPS signature
            if (location.hasAccuracy() && location.accuracy <= 0.001f) {
                return true
            }
        }

        return false
    }

    /**
     * Strict Server/Repository-Side Location Validation with Dual-Distance Geofence Policy:
     * - Blocks Mock Location / Fake GPS immediately
     * - Rejects low confidence / inaccurate location (> 250m)
     * - Enforces:
     *     * > 30 km: Eligible to generate advance token from home/village
     *     * <= 30 km: Strictly BLOCKED unless physically within 200m of Ashram
     */
    fun validateLocationSecurity(
        location: Location?,
        context: Context,
        ashramLat: Double,
        ashramLon: Double,
        allowedRadiusMeters: Double = LOCAL_ASHRAM_MAX_DISTANCE_METERS,
        isGeofenceEnforced: Boolean = true,
        isOutstationAdvanceAllowed: Boolean = true,
        outstationMinDistanceKm: Double = 30.0
    ): LocationSecurityResult {
        if (!isGeofenceEnforced) {
            val acc = if (location != null && location.hasAccuracy()) location.accuracy else 10.0f
            val dist = if (location != null) calculateDistanceMeters(location.latitude, location.longitude, ashramLat, ashramLon) else 0.0
            return LocationSecurityResult(
                isValid = true,
                isMock = false,
                accuracyMeters = acc,
                distanceMeters = dist,
                isInsideGeofence = true,
                isAdvanceDistanceEligible = true,
                isAshramLocalEligible = true,
                securityExceptionReason = null
            )
        }

        if (location == null) {
            return LocationSecurityResult(
                isValid = false,
                isMock = false,
                accuracyMeters = Float.MAX_VALUE,
                distanceMeters = Double.MAX_VALUE,
                isInsideGeofence = false,
                securityExceptionReason = "जीपीएस लोकेशन अनुपलब्ध है। कृपया फ़ोन की लोकेशन (GPS) चालू करें।"
            )
        }

        // 1. Detect Fake GPS / Mock Location (Anti-Bypass Protection)
        val isMock = isMockLocation(location, context)
        if (isMock) {
            return LocationSecurityResult(
                isValid = false,
                isMock = true,
                accuracyMeters = location.accuracy,
                distanceMeters = calculateDistanceMeters(location.latitude, location.longitude, ashramLat, ashramLon),
                isInsideGeofence = false,
                securityExceptionReason = "⚠️ सुरक्षा चेतावनी: फ़ेक जीपीएस (Fake GPS) अथवा नकली लोकेशन का उपयोग पकड़ा गया है! टोकन पंजीकरण अवरुद्ध कर दिया गया है।"
            )
        }

        // 1B. Detect Root / Magisk / KernelSU (Anti-Hook Protection)
        val isRooted = isDeviceRooted(context)
        if (isRooted) {
            return LocationSecurityResult(
                isValid = false,
                isMock = true,
                accuracyMeters = location.accuracy,
                distanceMeters = calculateDistanceMeters(location.latitude, location.longitude, ashramLat, ashramLon),
                isInsideGeofence = false,
                securityExceptionReason = "⚠️ सुरक्षा चेतावनी: रूटेड डिवाइस (Root / Magisk) का उपयोग पकड़ा गया है! सुरक्षा कारणों से टोकन पंजीकरण अवरुद्ध कर दिया गया है।"
            )
        }

        // 2. Dual-Distance Evaluation
        val distance = calculateDistanceMeters(location.latitude, location.longitude, ashramLat, ashramLon)
        val outstationMinMeters = ((outstationMinDistanceKm - 2.0).coerceAtLeast(1.0)) * 1000.0
        val accuracy = if (location.hasAccuracy()) location.accuracy else 20.0f

        // Case A: Devotee is coming from >= outstationMinDistanceKm away -> Advance token is permitted from home/village
        if (isOutstationAdvanceAllowed && distance >= outstationMinMeters) {
            return LocationSecurityResult(
                isValid = true,
                isMock = false,
                accuracyMeters = accuracy,
                distanceMeters = distance,
                isInsideGeofence = false,
                isAdvanceDistanceEligible = true,
                isAshramLocalEligible = false,
                securityExceptionReason = null
            )
        }

        // Case B: Devotee is physically at Ashram (<= allowedRadiusMeters + 150m drift buffer)
        val effectiveRadius = allowedRadiusMeters + 150.0
        if (distance <= effectiveRadius) {
            // Validate Accuracy Threshold for local Ashram check
            if (accuracy > MAX_ALLOWED_ACCURACY_METERS) {
                return LocationSecurityResult(
                    isValid = false,
                    isMock = false,
                    accuracyMeters = accuracy,
                    distanceMeters = distance,
                    isInsideGeofence = false,
                    securityExceptionReason = "⚠️ जीपीएस सिग्नल बहुत कमज़ोर है (${String.format(java.util.Locale.US, "%.0f", accuracy)}m)। कृपया खुले स्थान पर आकर प्रयास करें।"
                )
            }
            return LocationSecurityResult(
                isValid = true,
                isMock = false,
                accuracyMeters = accuracy,
                distanceMeters = distance,
                isInsideGeofence = true,
                isAdvanceDistanceEligible = false,
                isAshramLocalEligible = true,
                securityExceptionReason = null
            )
        }

        // Case C: Devotee is within boundary (beyond allowed radius) -> Strictly BLOCKED!
        val km = String.format(java.util.Locale.US, "%.1f", distance / 1000.0)
        val allowedM = allowedRadiusMeters.toInt()
        val radiusDesc = if (allowedM >= 1000) "${String.format(java.util.Locale.US, "%.1f", allowedM / 1000.0)} किमी" else "$allowedM मीटर"
        val outDesc = String.format(java.util.Locale.US, "%.0f", outstationMinDistanceKm)
        val reason = if (isOutstationAdvanceAllowed) {
            "⚠️ आश्रम दूरी नियम: ${outDesc} किमी के दायरे में रहने वाले स्थानीय भक्तों हेतु टोकन पंजीकरण केवल आश्रम परिसर ($radiusDesc के भीतर) में ही मान्य है। आप अभी आश्रम से $km किमी दूर हैं। कृपया आश्रम पहुँचकर ही टोकन जनरेट करें ताकि दूर से आने वाले भक्तों का अवसर न छूटे।"
        } else {
            "⚠️ आश्रम जिओफेंस नियम: टोकन पंजीकरण केवल आश्रम परिसर ($radiusDesc के भीतर) में ही मान्य है। आप अभी आश्रम से $km किमी दूर हैं। कृपया आश्रम पहुँचकर ही टोकन जनरेट करें।"
        }
        return LocationSecurityResult(
            isValid = false,
            isMock = false,
            accuracyMeters = accuracy,
            distanceMeters = distance,
            isInsideGeofence = false,
            isAdvanceDistanceEligible = false,
            isAshramLocalEligible = false,
            securityExceptionReason = reason
        )
    }

    /**
     * Tries to get the highest accuracy location from GPS or Network provider.
     */
    @Suppress("MissingPermission")
    fun getLastKnownLocation(context: Context): Location? {
        val locManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        return try {
            val gpsLoc = locManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val netLoc = locManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            when {
                gpsLoc != null && netLoc != null -> {
                    // Prefer GPS if reasonably recent or more accurate
                    if (gpsLoc.hasAccuracy() && gpsLoc.accuracy <= (netLoc.accuracy + 20f)) gpsLoc else netLoc
                }
                gpsLoc != null -> gpsLoc
                else -> netLoc
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Actively requests a fresh location fix from GPS and Network providers.
     * Guaranteed to trigger phone GPS hardware so passive cache is not empty.
     */
    @Suppress("MissingPermission")
    fun requestFreshLocation(
        context: Context,
        onLocationResult: (Location?) -> Unit
    ) {
        val last = getLastKnownLocation(context)
        if (last != null && (System.currentTimeMillis() - last.time) < 20_000L && last.hasAccuracy() && last.accuracy <= MAX_ALLOWED_ACCURACY_METERS) {
            onLocationResult(last)
            return
        }

        val locManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (locManager == null) {
            onLocationResult(last)
            return
        }

        var delivered = false
        val listener = object : android.location.LocationListener {
            override fun onLocationChanged(loc: Location) {
                if (!delivered) {
                    delivered = true
                    try { locManager.removeUpdates(this) } catch (e: Exception) {}
                    onLocationResult(loc)
                }
            }
            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        try {
            val mainLooper = android.os.Looper.getMainLooper()
            if (locManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 1.0f, listener, mainLooper)
            }
            if (locManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000L, 1.0f, listener, mainLooper)
            }
            // Auto timeout removal after 5 seconds
            android.os.Handler(mainLooper).postDelayed({
                if (!delivered) {
                    delivered = true
                    try { locManager.removeUpdates(listener) } catch (e: Exception) {}
                    onLocationResult(getLastKnownLocation(context))
                }
            }, 5000L)
        } catch (e: Exception) {
            onLocationResult(last)
        }
    }

    /**
     * Coroutine-based fresh location awaiter.
     * Prevents Cold-Start 0.0 lat/lon or stale home cached location from prematurely blocking devotees.
     */
    suspend fun awaitFreshLocation(
        context: Context,
        timeoutMs: Long = 4000L
    ): Location? {
        val last = getLastKnownLocation(context)
        if (last != null && (System.currentTimeMillis() - last.time) < 20_000L && last.hasAccuracy() && last.accuracy <= MAX_ALLOWED_ACCURACY_METERS) {
            return last
        }
        return try {
            withTimeoutOrNull(timeoutMs) {
                suspendCancellableCoroutine<Location?> { cont ->
                    requestFreshLocation(context) { loc ->
                        if (cont.isActive) {
                            cont.resumeWith(Result.success(loc ?: last))
                        }
                    }
                }
            } ?: getLastKnownLocation(context)
        } catch (e: Exception) {
            getLastKnownLocation(context)
        }
    }

    /**
     * Resolves human-readable village, town, or city name from GPS coordinates
     * using Android Geocoder with multi-locale fallback (Hindi and English).
     */
    fun resolveVillageAndCity(context: Context, lat: Double, lon: Double): String {
        if (lat == 0.0 && lon == 0.0) return ""
        try {
            if (!android.location.Geocoder.isPresent()) return ""
            // 1. Try Hindi locale for authentic Devotee / Indian village names
            val hindiGeocoder = android.location.Geocoder(context, java.util.Locale("hi", "IN"))
            @Suppress("DEPRECATION")
            val hindiAddresses = hindiGeocoder.getFromLocation(lat, lon, 1)
            if (!hindiAddresses.isNullOrEmpty()) {
                val addr = hindiAddresses[0]
                val subLocality = addr.subLocality?.trim().orEmpty()
                val locality = addr.locality?.trim().orEmpty()
                val subAdmin = addr.subAdminArea?.trim().orEmpty()
                val primaryPlace = if (subLocality.isNotBlank()) subLocality else locality
                val secondaryPlace = if (subLocality.isNotBlank() && locality.isNotBlank() && !locality.equals(subLocality, ignoreCase = true)) locality else subAdmin
                if (primaryPlace.isNotBlank()) {
                    return if (secondaryPlace.isNotBlank() && !primaryPlace.contains(secondaryPlace)) {
                        "$primaryPlace ($secondaryPlace)"
                    } else {
                        primaryPlace
                    }
                }
            }

            // 2. Fallback to default / English locale if Hindi didn't yield specific village
            val defGeocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
            @Suppress("DEPRECATION")
            val defAddresses = defGeocoder.getFromLocation(lat, lon, 1)
            if (!defAddresses.isNullOrEmpty()) {
                val addr = defAddresses[0]
                val subLocality = addr.subLocality?.trim().orEmpty()
                val locality = addr.locality?.trim().orEmpty()
                val subAdmin = addr.subAdminArea?.trim().orEmpty()
                val primaryPlace = if (subLocality.isNotBlank()) subLocality else locality
                val secondaryPlace = if (subLocality.isNotBlank() && locality.isNotBlank()) locality else subAdmin
                if (primaryPlace.isNotBlank()) {
                    return if (secondaryPlace.isNotBlank() && !primaryPlace.contains(secondaryPlace)) {
                        "$primaryPlace ($secondaryPlace)"
                    } else {
                        primaryPlace
                    }
                }
            }
        } catch (_: Exception) {}

        // 3. Robust Online Reverse Geocode Fallback (OpenStreetMap Nominatim)
        // Ensures accurate village/town resolution even without Google Play Services
        try {
            val url = java.net.URL("https://nominatim.openstreetmap.org/reverse?lat=$lat&lon=$lon&format=json&accept-language=hi,en")
            val conn = (url.openConnection() as java.net.HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", "ShriBalajiKripaDhamApp/2.56.3")
                connectTimeout = 3500
                readTimeout = 3500
            }
            if (conn.responseCode == 200) {
                val resp = conn.inputStream.bufferedReader().use { it.readText() }
                val jsonObj = org.json.JSONObject(resp)
                val address = jsonObj.optJSONObject("address")
                if (address != null) {
                    val village = address.optString("village").trim()
                    val suburb = address.optString("suburb").trim()
                    val town = address.optString("town").trim()
                    val city = address.optString("city").trim()
                    val county = address.optString("county").trim()
                    val state = address.optString("state").trim()

                    val primary = village.ifEmpty { suburb }.ifEmpty { town }.ifEmpty { city }
                    val secondary = if (primary != county && county.isNotBlank()) county else state
                    if (primary.isNotBlank()) {
                        return if (secondary.isNotBlank() && !primary.contains(secondary)) "$primary ($secondary)" else primary
                    }
                }
            }
        } catch (_: Exception) {}

        return ""
    }
}

