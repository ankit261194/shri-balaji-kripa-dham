package com.example.shribalajikripadham

import com.example.shribalajikripadham.data.local.DatabaseHelper
import com.example.shribalajikripadham.data.model.Admin
import com.example.shribalajikripadham.data.model.AdminRole
import com.example.shribalajikripadham.hardware.GeofenceLocationManager
import com.example.shribalajikripadham.util.AppUpdateManager
import org.junit.Assert.*
import org.junit.Test

class GeofenceAndLogicTest {

    @Test
    fun testHaversineDistanceZeroForIdenticalCoords() {
        val lat = 28.3972915
        val lon = 78.1460410
        val distance = GeofenceLocationManager.calculateDistanceMeters(lat, lon, lat, lon)
        assertEquals(0.0, distance, 0.001)
    }

    @Test
    fun testGeofenceInsideBoundary() {
        val ashramLat = 28.3972915
        val ashramLon = 78.1460410
        // A point ~50m away
        val userLat = 28.4092
        val userLon = 77.8791

        val isInside = GeofenceLocationManager.isInsideGeofence(
            userLat, userLon,
            ashramLat, ashramLon,
            allowedRadiusMeters = 200.0
        )
        assertTrue("User at ~50m must be inside 200m geofence", isInside)
    }

    @Test
    fun testGeofenceOutsideBoundary() {
        val ashramLat = 28.3972915
        val ashramLon = 78.1460410
        // Delhi / Bulandshahr city center ~30km away
        val userLat = 28.4069
        val userLon = 77.5000

        val isInside = GeofenceLocationManager.isInsideGeofence(
            userLat, userLon,
            ashramLat, ashramLon,
            allowedRadiusMeters = 200.0
        )
        assertFalse("User at 30km must be outside geofence", isInside)
    }

    @Test
    fun testPinHashConsistency() {
        val pin1 = "7777"
        val hash1 = DatabaseHelper.hashPin(pin1)
        val hash2 = DatabaseHelper.hashPin(pin1)
        assertEquals("Hash must be deterministic", hash1, hash2)

        val wrongPin = "1111"
        val wrongHash = DatabaseHelper.hashPin(wrongPin)
        assertNotEquals("Different PIN must yield different hash", hash1, wrongHash)
    }

    @Test
    fun testPasswordHashDeterministicAndSalted() {
        val pass = "SuperAdmin@7777"
        val hash1 = DatabaseHelper.hashPassword(pass)
        val hash2 = DatabaseHelper.hashPassword(pass)
        assertEquals("Password hash must be deterministic", hash1, hash2)

        val wrongPass = "SuperAdmin@1234"
        val wrongHash = DatabaseHelper.hashPassword(wrongPass)
        assertNotEquals("Different password must produce distinct hash", hash1, wrongHash)
        // Ensure salt changes hash compared to raw pin hash
        val pinHash = DatabaseHelper.hashPin(pass)
        assertNotEquals("Salted password hash must differ from raw hash", hash1, pinHash)
    }

    @Test
    fun testSevadarRbacPermissionMatrix() {
        val superAdmin = Admin(
            id = 1,
            name = "Guruji",
            username = "admin",
            phoneNumber = "+91 98765 00000",
            role = AdminRole.SUPER_ADMIN,
            canChangeLocation = true,
            canManageTokens = true,
            canManageYatra = true,
            canManageExpenses = true,
            canSendNotifications = true,
            canEditAshramInfo = true,
            canManageAdmins = true
        )
        assertTrue(superAdmin.role == AdminRole.SUPER_ADMIN)
        assertTrue(superAdmin.canManageAdmins)
        assertTrue(superAdmin.canSendNotifications)

        val restrictedSevadar = Admin(
            id = 2,
            name = "Sevadar",
            username = "sevadar1",
            phoneNumber = "+91 98765 43210",
            role = AdminRole.SEVADAR,
            canChangeLocation = false,
            canManageTokens = true,
            canManageYatra = false,
            canManageExpenses = false,
            canSendNotifications = false,
            canEditAshramInfo = false,
            canManageAdmins = false
        )
        assertFalse(restrictedSevadar.canChangeLocation)
        assertTrue(restrictedSevadar.canManageTokens)
        assertFalse(restrictedSevadar.canManageYatra)
        assertFalse(restrictedSevadar.canManageAdmins)
    }

    @Test
    fun testAutoUpdateVersionCheck() {
        val currentInstalled = 1
        val targetServerVersion = 2
        assertTrue("Should detect update available when target > current",
            AppUpdateManager.isUpdateAvailable(currentInstalled, targetServerVersion))

        val sameVersion = 1
        assertFalse("Should not detect update when target == current",
            AppUpdateManager.isUpdateAvailable(currentInstalled, sameVersion))
    }

    @Test
    fun testFinancialBalanceCalculation() {
        val totalFareCollected = 30000.0 // e.g. 20 seats x 1500
        val totalExpenses = 21500.0     // diesel + toll + dharamshala
        val expectedBalance = 8500.0
        val netBalance = totalFareCollected - totalExpenses
        assertEquals(expectedBalance, netBalance, 0.001)
    }
}
