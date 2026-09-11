package com.example.shribalajikripadham.data.model

enum class TokenStatus {
    WAITING,
    PENDING,
    CALLED,
    COMPLETED,
    CANCELLED
}

data class Token(
    val id: Long = 0,
    val tokenNumber: Int,
    val darbarDate: String,
    val patientName: String,
    val phoneNumber: String,
    val city: String = "डूँगरा जाट (स्थानीय)",
    val deviceId: String,
    val latitude: Double,
    val longitude: Double,
    val status: TokenStatus = TokenStatus.WAITING,
    val registeredBy: String = "USER_APP",
    val photoUri: String = "",
    val isDarshanCompleted: Boolean = false,
    val darshanCompletedAt: Long = 0L,
    val originAddress: String = city,
    val destinationAddress: String = "श्री बालाजी कृपा धाम, डुंगरा जाट",
    val distanceKm: Float = -1f,
    val createdAt: Long = System.currentTimeMillis()
)
