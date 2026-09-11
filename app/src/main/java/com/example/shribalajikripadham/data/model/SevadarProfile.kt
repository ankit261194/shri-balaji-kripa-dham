package com.example.shribalajikripadham.data.model

data class SevadarProfile(
    val id: Int,
    val name: String,
    val roleTitleHindi: String,
    val roleTitleEnglish: String,
    val phoneNumber: String,
    val dutyHindi: String,
    val dutyEnglish: String,
    val initials: String
)

object AshramDataDefaults {
    // Zero dummy sevadars policy: list is empty until added by Super Admin
    val sevadars: List<SevadarProfile> = emptyList()
}
