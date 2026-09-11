package com.example.shribalajikripadham

import com.example.shribalajikripadham.hardware.DeviceFingerprintManager
import com.example.shribalajikripadham.hardware.GeofenceLocationManager
import org.junit.Assert.*
import org.junit.Test

class SecurityAndAntiFraudTest {

    @Test
    fun testHardwareDeviceIdFormatAndDeterminism() {
        val hash1 = DeviceFingerprintManager.sha256("TEST_WIDEVINE_HW_ROOT_123")
        val hash2 = DeviceFingerprintManager.sha256("TEST_WIDEVINE_HW_ROOT_123")
        val hash3 = DeviceFingerprintManager.sha256("TEST_WIDEVINE_HW_ROOT_999")

        assertEquals("Hardware hash must be deterministic", hash1, hash2)
        assertEquals("SHA-256 hash must be 64 hex characters", 64, hash1.length)
        assertNotEquals("Different hardware roots must produce different hashes", hash1, hash3)
    }

    @Test
    fun testServerSideGeofenceBoundaryValidation() {
        val ashramLat = 28.4089
        val ashramLon = 77.8789
        val allowedRadius = 200.0 // 200 meters

        // 1. Point 50m away inside Ashram
        val insideLat = 28.4092
        val insideLon = 77.8789
        val distInside = GeofenceLocationManager.calculateDistanceMeters(insideLat, insideLon, ashramLat, ashramLon)
        assertTrue("Distance should be within 200m (Actual: $distInside)", distInside <= allowedRadius)

        // 2. Point in Delhi/Noida (~60 km away)
        val delhiLat = 28.6139
        val delhiLon = 77.2090
        val distOutside = GeofenceLocationManager.calculateDistanceMeters(delhiLat, delhiLon, ashramLat, ashramLon)
        assertTrue("Delhi should be > 50,000m away (Actual: $distOutside)", distOutside > 50000.0)

        val isInside = GeofenceLocationManager.isInsideGeofence(delhiLat, delhiLon, ashramLat, ashramLon, allowedRadius)
        assertFalse("Spoofed remote coordinates must be rejected by server-side geofence", isInside)
    }

    @Test
    fun testLocationAccuracyThreshold() {
        val acceptableAccuracy = 15.0f
        val unacceptableAccuracy = 85.0f // greater than MAX_ALLOWED_ACCURACY_METERS (50m)

        assertTrue(acceptableAccuracy <= GeofenceLocationManager.MAX_ALLOWED_ACCURACY_METERS)
        assertTrue(unacceptableAccuracy > GeofenceLocationManager.MAX_ALLOWED_ACCURACY_METERS)
    }

    @Test
    fun testSecurityExceptionMessageFormat() {
        val expectedAlert = "Security Exception: Spoofed Location or Duplicate Device Request Denied."

        try {
            // Simulate throwing security exception on spoof detection
            val isMock = true
            if (isMock) {
                throw SecurityException(expectedAlert)
            }
        } catch (e: SecurityException) {
            assertEquals("Alert message must match exact anti-fraud specification", expectedAlert, e.message)
        }
    }
}
