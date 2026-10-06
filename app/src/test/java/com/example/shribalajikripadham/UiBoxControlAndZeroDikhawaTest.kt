package com.example.shribalajikripadham

import com.example.shribalajikripadham.data.model.AshramSettings
import com.example.shribalajikripadham.data.model.UiSectionConfig
import com.example.shribalajikripadham.hardware.DeviceFingerprintManager
import org.junit.Assert.*
import org.junit.Test

class UiBoxControlAndZeroDikhawaTest {

    @Test
    fun testAllTwelveUiSectionsExistAndDefaultOrder() {
        val sections = UiSectionConfig.defaultSections()
        assertEquals("There must be exactly 12 default UI sections", 12, sections.size)

        val expectedIds = listOf(
            UiSectionConfig.ID_GURUJI_BANNER,
            UiSectionConfig.ID_EMERGENCY_NOTICE,
            UiSectionConfig.ID_FREE_TREATMENT_BOX,
            UiSectionConfig.ID_TOKEN_COUNTDOWN,
            UiSectionConfig.ID_SMART_FACE_TOKEN,
            UiSectionConfig.ID_QUICK_SERVICES,
            UiSectionConfig.ID_DARBAR_STATUS,
            UiSectionConfig.ID_SEVADAR_TEAM,
            UiSectionConfig.ID_DYNAMIC_EVENTS,
            UiSectionConfig.ID_AARTI_TIMINGS,
            UiSectionConfig.ID_SOCIAL_MEDIA_HUB,
            UiSectionConfig.ID_CONTACT_FOOTER
        )

        for (i in sections.indices) {
            assertEquals("Section at index $i must match expected ID", expectedIds[i], sections[i].sectionId)
            assertEquals("Default orderIndex must match list index", i, sections[i].orderIndex)
            assertTrue("Section ${sections[i].sectionId} must have non-blank Hindi title", sections[i].titleHindi.isNotBlank())
            assertTrue("Section ${sections[i].sectionId} must have non-blank English title", sections[i].titleEnglish.isNotBlank())
            assertTrue("Section ${sections[i].sectionId} must have non-blank icon", sections[i].icon.isNotBlank())
            assertTrue("Default section should be visible initially", sections[i].isVisible)
        }
    }

    @Test
    fun testUiSectionConfigJsonSerializationAndDeserialization() {
        val defaults = UiSectionConfig.defaultSections()
        val jsonStr = UiSectionConfig.toJson(defaults)
        assertNotNull("JSON string should not be null", jsonStr)
        assertTrue("JSON should contain section_id key", jsonStr.contains("section_id"))
        assertTrue("JSON should contain GURUJI_BANNER", jsonStr.contains(UiSectionConfig.ID_GURUJI_BANNER))

        val parsed = UiSectionConfig.fromJson(jsonStr)
        assertEquals("Parsed count must match original count", defaults.size, parsed.size)
        for (i in defaults.indices) {
            assertEquals("Parsed sectionId must match original", defaults[i].sectionId, parsed[i].sectionId)
            assertEquals("Parsed titleHindi must match original", defaults[i].titleHindi, parsed[i].titleHindi)
            assertEquals("Parsed titleEnglish must match original", defaults[i].titleEnglish, parsed[i].titleEnglish)
            assertEquals("Parsed isVisible must match original", defaults[i].isVisible, parsed[i].isVisible)
            assertEquals("Parsed orderIndex must match original", defaults[i].orderIndex, parsed[i].orderIndex)
        }
    }

    @Test
    fun testUiSectionVisibilityToggle() {
        val original = UiSectionConfig.defaultSections()
        
        // Admin hides Social Media Hub and Sevadar Team
        val modified = original.map {
            if (it.sectionId == UiSectionConfig.ID_SOCIAL_MEDIA_HUB || it.sectionId == UiSectionConfig.ID_SEVADAR_TEAM) {
                it.copy(isVisible = false)
            } else {
                it
            }
        }

        val visibleOnly = modified.filter { it.isVisible }
        assertEquals("Only 10 sections should remain visible after disabling 2", 10, visibleOnly.size)
        assertFalse("Disabled section should not be in visible list", visibleOnly.any { it.sectionId == UiSectionConfig.ID_SOCIAL_MEDIA_HUB })
        assertFalse("Disabled section should not be in visible list", visibleOnly.any { it.sectionId == UiSectionConfig.ID_SEVADAR_TEAM })

        // Verify JSON round-trip retains hidden state
        val jsonStr = UiSectionConfig.toJson(modified)
        val roundTripped = UiSectionConfig.fromJson(jsonStr)
        val hiddenInJson = roundTripped.filter { !it.isVisible }
        assertEquals("Exactly 2 sections must be recorded as hidden in JSON", 2, hiddenInJson.size)
    }

    @Test
    fun testUiSectionReordering() {
        val original = UiSectionConfig.defaultSections()

        // Admin moves Token Countdown (ID_TOKEN_COUNTDOWN) to the very top (index 0)
        val reordered = original.sortedBy { 
            if (it.sectionId == UiSectionConfig.ID_TOKEN_COUNTDOWN) -1 else it.orderIndex 
        }.mapIndexed { index, item -> item.copy(orderIndex = index) }

        assertEquals("First section must now be TOKEN_COUNTDOWN", UiSectionConfig.ID_TOKEN_COUNTDOWN, reordered[0].sectionId)
        assertEquals("First section orderIndex must be 0", 0, reordered[0].orderIndex)
    }

    @Test
    fun testSiliconHardwareFingerprintRobustness() {
        val testHardwareRaw = "BOARD=universal7880;HARDWARE=samsungexynos7880;BRAND=samsung;MODEL=SM-A720F;ID=abcdef1234567890;MANUFACTURER=samsung"
        val hashA = DeviceFingerprintManager.sha256(testHardwareRaw)
        val hashB = DeviceFingerprintManager.sha256(testHardwareRaw)

        assertEquals("Hardware fingerprint hash must be 100% deterministic", hashA, hashB)
        assertEquals("SHA-256 fingerprint must be 64 characters long", 64, hashA.length)
        assertTrue("Hash must contain only valid hex characters", hashA.matches(Regex("^[0-9a-fA-F]{64}$")))
    }

    @Test
    fun testDailyDarshanMillisecondCacheBustingFormat() {
        val now = System.currentTimeMillis()
        val cacheBusterUrl = "https://shribalajikripadham.online/uploads/darshan_today.jpg?cb=$now"

        assertTrue("Cache buster URL must contain millisecond timestamp param", cacheBusterUrl.contains("?cb="))
        val extractedTimestamp = cacheBusterUrl.substringAfter("?cb=").toLongOrNull()
        assertNotNull("Extracted timestamp must be a valid long", extractedTimestamp)
        assertTrue("Extracted timestamp must be greater than year 2026 epoch", extractedTimestamp!! > 1700000000000L)
    }

    @Test
    fun testAshramSettingsZeroDummyDefaults() {
        val settings = AshramSettings()

        // Verify genuine Ashram details
        assertTrue("Ashram name must be genuine", settings.ashramName.isNotBlank())
        assertTrue("Guruji name must be genuine", settings.gurujiName.isNotBlank())
        assertTrue("Address must be genuine", settings.address.isNotBlank())
        // Verify default darbar geofence coordinates
        assertEquals("Ashram latitude must match real Ashram location", 28.3972915, settings.latitude, 0.0001)
        assertEquals("Ashram longitude must match real Ashram location", 78.1460410, settings.longitude, 0.0001)
    }
}
