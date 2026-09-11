package com.example.shribalajikripadham.data.model

data class AshramEvent(
    val id: Long = 0,
    val titleHindi: String,
    val titleEnglish: String,
    val dateDescriptionHindi: String,
    val dateDescriptionEnglish: String,
    val detailsHindi: String,
    val detailsEnglish: String,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
