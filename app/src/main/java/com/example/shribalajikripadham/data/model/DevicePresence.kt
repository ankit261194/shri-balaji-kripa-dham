package com.example.shribalajikripadham.data.model

data class DevicePresence(
    val deviceId: String,
    val deviceModel: String,
    val userName: String = "",
    val phoneNumber: String = "",
    val city: String = "",
    val appVersion: String = "2.56.14",
    val lastSeenAt: Long = System.currentTimeMillis(),
    val openCount: Int = 1,
    val role: String = "USER",
    val androidVersion: String = "",
    val isOnline: Boolean = false,
    val ipAddress: String = ""
)

