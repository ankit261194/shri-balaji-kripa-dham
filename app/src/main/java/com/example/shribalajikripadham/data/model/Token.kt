package com.example.shribalajikripadham.data.model

enum class TokenStatus {
    WAITING,
    PENDING,
    CALLED,
    SERVING,
    COMPLETED,
    CANCELLED,
    ABSENT;

    companion object {
        fun fromRaw(raw: String?): TokenStatus {
            if (raw.isNullOrBlank()) return WAITING
            val clean = raw.trim().uppercase()
            return try {
                valueOf(clean)
            } catch (_: Exception) {
                when (clean) {
                    "SERVING", "CALLING" -> CALLED
                    "DONE", "FINISHED" -> COMPLETED
                    "CANCEL", "CANCELLED", "CANCELED" -> CANCELLED
                    "ABSENT", "NOT_PRESENT" -> ABSENT
                    else -> WAITING
                }
            }
        }
    }
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
    val darbarVenue: String = "DUNGRA_JAAT",
    val createdAt: Long = System.currentTimeMillis()
)
