package com.example.shribalajikripadham.hardware

import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Process
import android.provider.Settings
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

    const val MAX_ALLOWED_ACCURACY_METERS = 60.0f
    const val OUTSTATION_MIN_DISTANCE_METERS = 30000.0 // 30 km
    const val LOCAL_ASHRAM_MAX_DISTANCE_METERS = 200.0 // 200 meters

    /**
     * Dual-Distance Eligibility Evaluator:
     * - Devotees > 30 km: Can register token in advance from home/city.
     * - Devotees <= 30 km: MUST be physically within 200m of Ashram (Gram Dungra Jaat).
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
        val outstationMinMeters = outstationMinDistanceKm * 1000.0
        val isOutstationPermitted = isOutstationAdvanceAllowed && (distanceMeters > outstationMinMeters)
        val isLocalPermitted = (distanceMeters <= allowedRadiusMeters)
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
        // 1. Check build tags for test-keys
        val buildTags = Build.TAGS
        if (buildTags != null && buildTags.contains("test-keys")) {
            return true
        }

        // 2. Check for su binaries in system paths
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
     * Checks if any known Fake GPS or Location Spoofer app is installed,
     * or if ANY app on the device holds ACCESS_MOCK_LOCATION permission.
     */
    fun hasSpoofingAppsInstalled(context: Context): Pair<Boolean, String?> {
        val pm = context.packageManager
        // 1. Check known mock location packages
        for (pkg in KNOWN_MOCK_LOCATION_PACKAGES) {
            try {
                pm.getPackageInfo(pkg, 0)
                return Pair(true, pkg)
            } catch (ignored: Exception) {}
        }

        // 2. Deep scan for any package holding ACCESS_MOCK_LOCATION or selected in Developer Options
        try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
            val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
            }
            for (pkg in packages) {
                if (pkg.packageName == context.packageName) continue
                
                // Check requestedPermissions
                val perms = pkg.requestedPermissions
                if (perms != null) {
                    for (p in perms) {
                        if (p == "android.permission.ACCESS_MOCK_LOCATION") {
                            return Pair(true, pkg.packageName)
                        }
                    }
                }

                // Check if selected in Developer Options as active mock provider
                val appInfo = pkg.applicationInfo
                if (appOps != null && appInfo != null) {
                    val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        appOps.unsafeCheckOpNoThrow(
                            AppOpsManager.OPSTR_MOCK_LOCATION,
                            appInfo.uid,
                            pkg.packageName
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        appOps.checkOpNoThrow(
                            AppOpsManager.OPSTR_MOCK_LOCATION,
                            appInfo.uid,
                            pkg.packageName
                        )
                    }
                    if (mode == AppOpsManager.MODE_ALLOWED) {
                        return Pair(true, pkg.packageName)
                    }
                }
            }
        } catch (ignored: Exception) {}

        return Pair(false, null)
    }

    /**
     * Comprehensive Fake GPS & Mock Location Detection:
     * 1. Known mock location / spoofing app signature check & ACCESS_MOCK_LOCATION permission scanner
     * 2. Developer options mock location provider status
     * 3. Location.isMock (API 31+) / Location.isFromMockProvider (API 18+)
     * 4. Location bundle "mockLocation" extras check & artificial accuracy check
     * 5. AppOpsManager OPSTR_MOCK_LOCATION permission check
     * 6. Settings.Secure.ALLOW_MOCK_LOCATION (Legacy check)
     */
    fun isMockLocation(location: Location?, context: Context): Boolean {
        // 1. Check for installed fake GPS applications or apps with ACCESS_MOCK_LOCATION
        val (hasSpoofApp, _) = hasSpoofingAppsInstalled(context)
        if (hasSpoofApp) return true

        // 2. Native Location Object Mock Check (if location object is available)
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

        // 5. System AppOps Mock Location Check
        try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
            if (appOps != null) {
                val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    appOps.unsafeCheckOpNoThrow(
                        AppOpsManager.OPSTR_MOCK_LOCATION,
                        Process.myUid(),
                        context.packageName
                    )
                } else {
                    @Suppress("DEPRECATION")
                    appOps.checkOpNoThrow(
                        AppOpsManager.OPSTR_MOCK_LOCATION,
                        Process.myUid(),
                        context.packageName
                    )
                }
                if (mode == AppOpsManager.MODE_ALLOWED) return true
            }
        } catch (ignored: Exception) {}

        // 6. Settings Mock Location Check for legacy Android versions
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            try {
                @Suppress("DEPRECATION")
                if (Settings.Secure.getString(context.contentResolver, Settings.Secure.ALLOW_MOCK_LOCATION) != "0") {
                    return true
                }
            } catch (ignored: Exception) {}
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

        // 2. Validate Accuracy Threshold (Must be <= MAX_ALLOWED_ACCURACY_METERS)
        val accuracy = if (location.hasAccuracy()) location.accuracy else Float.MAX_VALUE
        if (accuracy > MAX_ALLOWED_ACCURACY_METERS) {
            return LocationSecurityResult(
                isValid = false,
                isMock = false,
                accuracyMeters = accuracy,
                distanceMeters = calculateDistanceMeters(location.latitude, location.longitude, ashramLat, ashramLon),
                isInsideGeofence = false,
                securityExceptionReason = "⚠️ जीपीएस सिग्नल बहुत कमज़ोर है (${String.format(java.util.Locale.US, "%.1f", accuracy)}m > ${MAX_ALLOWED_ACCURACY_METERS}m)। कृपया खुले स्थान पर आकर प्रयास करें।"
            )
        }

        // 3. Dual-Distance Evaluation
        val distance = calculateDistanceMeters(location.latitude, location.longitude, ashramLat, ashramLon)
        val outstationMinMeters = outstationMinDistanceKm * 1000.0

        // Case A: Devotee is coming from > outstationMinDistanceKm away -> Advance token is permitted
        if (isOutstationAdvanceAllowed && distance > outstationMinMeters) {
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

        // Case B: Devotee is physically at Ashram (<= allowedRadiusMeters) -> Local token permitted
        if (distance <= allowedRadiusMeters) {
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
        if (last != null && (System.currentTimeMillis() - last.time) < 60_000L) {
            onLocationResult(last)
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
            // Auto timeout removal after 8 seconds
            android.os.Handler(mainLooper).postDelayed({
                if (!delivered) {
                    delivered = true
                    try { locManager.removeUpdates(listener) } catch (e: Exception) {}
                    onLocationResult(getLastKnownLocation(context))
                }
            }, 8000L)
        } catch (e: Exception) {
            onLocationResult(last)
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

