package com.example.shribalajikripadham.data.sacred

/**
 * श्री बालाजी कृपा धाम (डूँगरा जाट) - पावन आरती एवं भजन डेटा मॉडल
 *
 * All authentic tracks and lyrics are now 100% dynamically managed by Ashram Admin.
 * Hardcoded faulty texts and external links have been permanently removed.
 */
data class SacredTrack(
    val id: Long = 0,
    val trackKey: String = "",
    val titleHindi: String = "",
    val titleEnglish: String = "",
    val subtitleHindi: String = "",
    val durationText: String = "",
    val audioUrl: String = "",
    val lyricsHindi: String = "",
    val isPublished: Boolean = true,
    val displayOrder: Int = 0,
    val youtubeSearchQuery: String = ""
)

/**
 * Dynamic fallback list: Empty by default.
 * Tracks are populated dynamically from the Ashram's secure database and Admin uploads.
 */
val SACRED_TRACKS: List<SacredTrack> = emptyList()
