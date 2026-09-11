package com.example.shribalajikripadham.data.model

data class AppNotification(
    val id: Long = 0,
    val title: String,
    val message: String,
    val priority: String = "HIGH",
    val sentBy: String = "Super Admin",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)
