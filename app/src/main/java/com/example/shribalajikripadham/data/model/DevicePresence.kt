package com.example.shribalajikripadham.data.model

data class DevicePresence(
    val deviceId: String,
    val deviceModel: String,
    val userName: String = "",
    val phoneNumber: String = "",
    val city: String = "",
    val appVersion: String = "2.9.0",
    val lastSeenAt: Long = System.currentTimeMillis(),
    val openCount: Int = 1,
    val role: String = "USER"
)
