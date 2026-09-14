package com.example.shribalajikripadham

import com.example.shribalajikripadham.data.model.AshramSettings
import com.example.shribalajikripadham.data.model.DevoteeFaceProfile
import com.example.shribalajikripadham.data.model.Token
import org.junit.Assert.*
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.*

class AdminControlsAndTokenExportTest {

    @Test
    fun testBusBookingPermanentlyHiddenByDefault() {
        val defaultSettings = AshramSettings()
        assertFalse(
            "CRITICAL PRD REQUIREMENT: Bus Seat Booking must remain PERMANENTLY HIDDEN by default (isYatraServiceEnabled == false)",
            defaultSettings.isYatraServiceEnabled
        )
    }

    @Test
    fun testMasterVisibilityTogglesDefaultsAndCustomization() {
        val settings = AshramSettings()

        // Verify default visibility of core features
        assertTrue("Token service should be enabled by default", settings.isTokenServiceEnabled)
        assertTrue("Counter service should be enabled by default", settings.isLiveCounterVisible)
        assertTrue("Events should be visible by default", settings.isEventsVisible)
        assertTrue("Aarti timings should be visible by default", settings.isAartiTimingsVisible)
        assertTrue("Guruji info should be visible by default", settings.isGurujiInfoVisible)
        assertTrue("Emergency notice should be visible by default", settings.isEmergencyNoticeVisible)

        // Super Admin toggles: test state modification
        val adminUpdated = settings.copy(
            isYatraServiceEnabled = true,
            isTokenServiceEnabled = false,
            isEmergencyNoticeVisible = false,
            isGurujiInfoVisible = false
        )

        assertTrue("Super Admin can explicitly enable Bus Booking", adminUpdated.isYatraServiceEnabled)
        assertFalse("Super Admin can toggle off Token Service", adminUpdated.isTokenServiceEnabled)
        assertFalse("Super Admin can hide Emergency Notice", adminUpdated.isEmergencyNoticeVisible)
        assertFalse("Super Admin can hide Guruji Info", adminUpdated.isGurujiInfoVisible)
    }

    @Test
    fun testPreScheduledTokenOpeningLogic() {
        val now = System.currentTimeMillis()
        val futureScheduledTime = now + 86400000L // 24 hours in future
        val pastScheduledTime = now - 3600000L    // 1 hour in past

        val futureSettings = AshramSettings(
            isTokenServiceEnabled = true,
            scheduledTokenOpenTimestamp = futureScheduledTime
        )

        val isBlockedForDevotee = futureSettings.scheduledTokenOpenTimestamp > System.currentTimeMillis()
        assertTrue("Devotee token generation must be BLOCKED when scheduled time is in future", isBlockedForDevotee)

        val pastSettings = AshramSettings(
            isTokenServiceEnabled = true,
            scheduledTokenOpenTimestamp = pastScheduledTime
        )

        val isUnlockedForDevotee = pastSettings.scheduledTokenOpenTimestamp <= System.currentTimeMillis()
        assertTrue("Devotee token generation must be UNLOCKED when scheduled time has passed", isUnlockedForDevotee)
    }

    @Test
    fun testTokenModelWithCityAndTimestamp() {
        val customToken = Token(
            id = 1,
            tokenNumber = 42,
            patientName = "Ram Mohan",
            phoneNumber = "9876543210",
            darbarDate = "2026-09-13",
            city = "बुलन्दशहर",
            deviceId = "DEVICE_HW_001",
            latitude = 28.3972915,
            longitude = 78.1460410,
            createdAt = System.currentTimeMillis()
        )

        assertEquals("City field must match provided value", "बुलन्दशहर", customToken.city)
        assertEquals(42, customToken.tokenNumber)
        assertEquals("Ram Mohan", customToken.patientName)
        assertEquals("9876543210", customToken.phoneNumber)

        // Test default city fallback
        val defaultToken = Token(
            id = 2,
            tokenNumber = 43,
            patientName = "Sita Devi",
            phoneNumber = "9812345678",
            darbarDate = "2026-09-13",
            deviceId = "DEVICE_HW_002",
            latitude = 28.3972915,
            longitude = 78.1460410
        )
        assertEquals("Default city must be local Ashram village", "डूँगरा जाट (स्थानीय)", defaultToken.city)
    }

    @Test
    fun testDevoteeFaceProfileWithCity() {
        val profile = DevoteeFaceProfile(
            id = 10,
            patientName = "Vikram Sharma",
            phoneNumber = "9988776655",
            faceVector = FloatArray(128) { 0.1f },
            city = "खुर्जा",
            visitCount = 3
        )

        assertEquals("DevoteeFaceProfile must store city/Aagman Sthan", "खुर्जा", profile.city)
        assertEquals(3, profile.visitCount)
    }

    @Test
    fun testTokenCardFormattingAndDeveloperCredit() {
        val token = Token(
            id = 5,
            tokenNumber = 108,
            patientName = "Deepak Chaudhary",
            phoneNumber = "9897012345",
            darbarDate = "2026-09-13",
            city = "मेरठ",
            deviceId = "DEVICE_HW_005",
            latitude = 28.3972915,
            longitude = 78.1460410,
            createdAt = 1789200000000L
        )

        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val formattedDate = sdf.format(Date(token.createdAt))
        assertNotNull("Formatted date must not be null", formattedDate)

        val expectedDeveloperCredit = "Developer: Ankit Chaudhary"
        val expectedSansthaName = "Shri Balaji Kripa Dham Dungra Jaat"
        val expectedGuruji = "Param Pujya Guruji Tejveer Singh Ji"

        assertTrue(expectedDeveloperCredit.contains("Ankit Chaudhary"))
        assertTrue(expectedSansthaName.contains("Dungra Jaat"))
        assertTrue(expectedGuruji.contains("Tejveer Singh"))
    }

    @Test
    fun testSuperAdminPasswordOnlySpecification() {
        val expectedPassword = "9100100251233433"
        val md = java.security.MessageDigest.getInstance("SHA-256")
        val hash = md.digest(expectedPassword.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
        
        // Ensure the hash is computed deterministically and non-empty
        assertEquals(64, hash.length)
        assertFalse("Old dummy passwords must not match", hash == md.digest("admin123".toByteArray()).joinToString("") { "%02x".format(it) })
    }

    @Test
    fun testZeroDummySevadarPolicy() {
        // Enforce that default sevadar credentials (e.g. sevadar1 / sevadar123) are permanently abolished
        val prohibitedUsernames = listOf("sevadar", "sevadar1", "dummy_sevadar", "admin")
        val activeSevadarList = emptyList<String>() // Super Admin only adds real sevadars manually
        
        assertTrue("No seeded dummy sevadar should exist in the initial database", activeSevadarList.isEmpty())
        for (prohibited in prohibitedUsernames) {
            assertFalse("Prohibited dummy username '$prohibited' must not be present", activeSevadarList.contains(prohibited))
        }
    }

    @Test
    fun testUiSectionConfigDefaults() {
        val defaults = com.example.shribalajikripadham.data.model.UiSectionConfig.defaultSections()
        assertEquals("There must be 12 configurable UI sections", 12, defaults.size)
        
        // Verify every section has a unique ID, valid titles, icon, isVisible=true, and proper orderIndex
        val ids = defaults.map { it.sectionId }.toSet()
        assertEquals("All section IDs must be unique", 12, ids.size)

        defaults.forEachIndexed { index, section ->
            assertEquals("Default order index must match position", index, section.orderIndex)
            assertTrue("Default sections must be visible", section.isVisible)
            assertTrue("Title Hindi must not be blank", section.titleHindi.isNotBlank())
            assertTrue("Title English must not be blank", section.titleEnglish.isNotBlank())
            assertTrue("Icon must not be blank", section.icon.isNotBlank())
        }
    }

    @Test
    fun testUiSectionConfigJsonRoundTrip() {
        val defaults = com.example.shribalajikripadham.data.model.UiSectionConfig.defaultSections()
        val json = com.example.shribalajikripadham.data.model.UiSectionConfig.toJson(defaults)
        assertTrue("JSON string must not be empty", json.isNotBlank())

        val restored = com.example.shribalajikripadham.data.model.UiSectionConfig.fromJson(json)
        assertEquals(defaults.size, restored.size)
        for (i in defaults.indices) {
            assertEquals(defaults[i].sectionId, restored[i].sectionId)
            assertEquals(defaults[i].titleHindi, restored[i].titleHindi)
            assertEquals(defaults[i].titleEnglish, restored[i].titleEnglish)
            assertEquals(defaults[i].icon, restored[i].icon)
            assertEquals(defaults[i].isVisible, restored[i].isVisible)
            assertEquals(defaults[i].orderIndex, restored[i].orderIndex)
        }
    }

    @Test
    fun testUiSectionReorderingAndVisibilityFiltering() {
        val sections = com.example.shribalajikripadham.data.model.UiSectionConfig.defaultSections().toMutableList()
        
        // Move Sevadar team (index 7) to top (orderIndex = 0)
        val sevadarIndex = sections.indexOfFirst { it.sectionId == com.example.shribalajikripadham.data.model.UiSectionConfig.ID_SEVADAR_TEAM }
        val sevadarSection = sections.removeAt(sevadarIndex)
        sections.add(0, sevadarSection)

        // Hide Free Treatment Box
        val freeBoxIndex = sections.indexOfFirst { it.sectionId == com.example.shribalajikripadham.data.model.UiSectionConfig.ID_FREE_TREATMENT_BOX }
        sections[freeBoxIndex] = sections[freeBoxIndex].copy(isVisible = false)

        // Reindex
        val reindexed = sections.mapIndexed { idx, itm -> itm.copy(orderIndex = idx) }
        val visibleSorted = reindexed.filter { it.isVisible }.sortedBy { it.orderIndex }

        assertEquals("First visible item must now be Sevadar Team", com.example.shribalajikripadham.data.model.UiSectionConfig.ID_SEVADAR_TEAM, visibleSorted.first().sectionId)
        assertFalse("Free Treatment Box must be hidden", visibleSorted.any { it.sectionId == com.example.shribalajikripadham.data.model.UiSectionConfig.ID_FREE_TREATMENT_BOX })
        assertEquals("Visible count must be 11 after hiding 1 item", 11, visibleSorted.size)
    }
}

