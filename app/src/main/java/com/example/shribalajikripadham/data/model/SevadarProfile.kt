package com.example.shribalajikripadham.data.model

data class SevadarProfile(
    val id: Long = 0,
    val name: String,
    val roleTitleHindi: String = "सेवादार",
    val roleTitleEnglish: String = "Sevadar",
    val phoneNumber: String,
    val dutyHindi: String = "",
    val dutyEnglish: String = "",
    val initials: String = "",
    val photoUri: String = "",
    val displayOrder: Int = 0,
    val isActive: Boolean = true
) {
    companion object {
        fun defaultProfiles(): List<SevadarProfile> = emptyList()
    }
}

object AshramDataDefaults {
    val sevadars: List<SevadarProfile> = emptyList()
}

