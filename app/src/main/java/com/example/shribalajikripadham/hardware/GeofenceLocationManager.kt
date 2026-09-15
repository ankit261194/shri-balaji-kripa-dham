package com.example.shribalajikripadham.hardware

import android.app.AppOpsManager
import android.content.Context
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

    const val MAX_ALLOWED_ACCURACY_METERS = 250.0f
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
        outstationMinDistanceKm: Double = 30.0
    ): Boolean {
        if (!isGeofenceEnforced) return true
        val outstationMinMeters = outstationMinDistanceKm * 1000.0
        return (distanceMeters > outstationMinMeters) || (distanceMeters <= allowedRadiusMeters)
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

    /**
     * Comprehensive Fake GPS & Mock Location Detection:
     * 1. Location.isMock (API 31+) / Location.isFromMockProvider (API 18+)
     * 2. Settings.Secure.ALLOW_MOCK_LOCATION (Legacy check)
     * 3. AppOpsManager OPSTR_MOCK_LOCATION check
     */
    fun isMockLocation(location: Location?, context: Context): Boolean {
        if (location == null) return false

        // 1. Native Location Object Mock Check
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (location.isMock) return true
        } else {
            @Suppress("DEPRECATION")
            if (location.isFromMockProvider) return true
        }

        // 2. System AppOps Mock Location Check
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

        // 3. Settings Mock Location Check for legacy Android versions
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
        isGeofenceEnforced: Boolean = true
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

        // Case A: Devotee is coming from > 30 km away -> Advance token is permitted
        if (distance > OUTSTATION_MIN_DISTANCE_METERS) {
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

        // Case C: Devotee is within 30 km (beyond allowed radius) -> Strictly BLOCKED!
        val km = String.format(java.util.Locale.US, "%.1f", distance / 1000.0)
        val allowedM = allowedRadiusMeters.toInt()
        val radiusDesc = if (allowedM >= 1000) "${String.format(java.util.Locale.US, "%.1f", allowedM / 1000.0)} किमी" else "$allowedM मीटर"
        return LocationSecurityResult(
            isValid = false,
            isMock = false,
            accuracyMeters = accuracy,
            distanceMeters = distance,
            isInsideGeofence = false,
            isAdvanceDistanceEligible = false,
            isAshramLocalEligible = false,
            securityExceptionReason = "⚠️ आश्रम दूरी नियम: 30 किमी के दायरे में रहने वाले स्थानीय भक्तों हेतु टोकन पंजीकरण केवल आश्रम परिसर ($radiusDesc के भीतर) में ही मान्य है। आप अभी आश्रम से $km किमी दूर हैं। कृपया आश्रम पहुँचकर ही टोकन जनरेट करें ताकि दूर से आने वाले भक्तों का अवसर न छूटे।"
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
}
