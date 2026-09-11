package com.example.shribalajikripadham.data.model

import java.util.Arrays

data class DevoteeFaceProfile(
    val id: Long = 0,
    val patientName: String,
    val phoneNumber: String,
    val city: String = "डूँगरा जाट (स्थानीय)",
    val faceVector: FloatArray,
    val photoUri: String = "",
    val visitCount: Int = 1,
    val lastConfidence: Float = 1.0f,
    val lastVerifiedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as DevoteeFaceProfile
        if (id != other.id) return false
        if (patientName != other.patientName) return false
        if (phoneNumber != other.phoneNumber) return false
        if (city != other.city) return false
        if (!Arrays.equals(faceVector, other.faceVector)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + patientName.hashCode()
        result = 31 * result + phoneNumber.hashCode()
        result = 31 * result + Arrays.hashCode(faceVector)
        return result
    }
}

data class FaceMatchResult(
    val profile: DevoteeFaceProfile,
    val confidence: Float,
    val isHighConfidence: Boolean,
    val matchStatusDescription: String
)
