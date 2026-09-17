package com.example.shribalajikripadham.data.model

/**
 * Prominent Donors & Patrons Profile Model.
 *
 * PRIVACY MANDATE:
 * Devotee/Donor phone numbers are STRICTLY for internal admin audit.
 * They are NEVER displayed or transmitted to the public website or public APIs.
 */
data class DonorProfile(
    val id: Long = 0,
    val name: String,
    val cityAddress: String = "ग्राम डूँगरा जाट",
    val title: String = "मंदिर निर्माण सहयोगी",
    val photoUri: String = "",
    val phone: String = "",
    val notes: String = "",
    val displayOrder: Int = 0,
    val isActive: Boolean = true
)
