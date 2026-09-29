package com.example.shribalajikripadham.data.model

import org.json.JSONObject

/**
 * Data Model for Devotee & Official Ashram 24-Hour Bhakti Statuses
 */
data class BhaktiStatusItem(
    val id: Long,
    val deviceId: String,
    val userName: String,
    val phoneNumber: String,
    val city: String,
    val caption: String,
    val mediaUrl: String,
    val isOfficial: Boolean,
    val audioSnippetUrl: String,
    val createdAt: Long,
    val expiresAt: Long,
    val viewsCount: Int
) {
    companion object {
        fun fromJson(json: JSONObject): BhaktiStatusItem {
            return BhaktiStatusItem(
                id = json.optLong("id", 0L),
                deviceId = json.optString("device_id", ""),
                userName = json.optString("user_name", "भक्त"),
                phoneNumber = json.optString("phone_number", ""),
                city = json.optString("city", ""),
                caption = json.optString("caption", ""),
                mediaUrl = json.optString("media_url", ""),
                isOfficial = json.optInt("is_official", 0) == 1,
                audioSnippetUrl = json.optString("audio_snippet_url", ""),
                createdAt = json.optLong("created_at", System.currentTimeMillis() / 1000),
                expiresAt = json.optLong("expires_at", (System.currentTimeMillis() / 1000) + 86400),
                viewsCount = json.optInt("views_count", 0)
            )
        }
    }
}

/**
 * Data Model for Devotees who viewed a status (for Admin tracking)
 */
data class StatusViewer(
    val viewerName: String,
    val viewerPhone: String,
    val viewedAt: Long
) {
    companion object {
        fun fromJson(json: JSONObject): StatusViewer {
            return StatusViewer(
                viewerName = json.optString("viewer_name", "भक्त"),
                viewerPhone = json.optString("viewer_phone", ""),
                viewedAt = json.optLong("viewed_at", 0L)
            )
        }
    }
}

/**
 * Data Model for Daily Divine Suvichar
 */
data class DailySuvichar(
    val quote: String,
    val meaning: String,
    val source: String
) {
    companion object {
        fun default(): DailySuvichar {
            return DailySuvichar(
                quote = "कवन सो काज कठिन जग माहीं । जो नहिं होत तात तुम्ह पाहीं ॥",
                meaning = "संसार में ऐसा कोई कठिन कार्य नहीं जो श्री बालाजी महाराज की कृपा से सुगम न हो सके।",
                source = "श्री रामचरितमानस (सुंदरकांड)"
            )
        }

        fun fromJson(json: JSONObject?): DailySuvichar {
            if (json == null) return default()
            return DailySuvichar(
                quote = json.optString("quote", default().quote),
                meaning = json.optString("meaning", default().meaning),
                source = json.optString("source", default().source)
            )
        }
    }
}
