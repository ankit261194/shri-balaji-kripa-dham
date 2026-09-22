package com.example.shribalajikripadham.data.model

data class RegisterEntry(
    val serialNumber: Int,
    val patientName: String,
    val phoneNumber: String = "",
    val city: String = "",
    val confidence: Int = 100
)
