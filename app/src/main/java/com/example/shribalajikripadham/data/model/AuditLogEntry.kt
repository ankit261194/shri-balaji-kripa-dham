package com.example.shribalajikripadham.data.model

data class AuditLogEntry(
    val id: Long = 0,
    val action: String,             // e.g. "TOKEN_CANCELLED", "TOKEN_DELETED", "VIP_TOKEN_ISSUED", "DB_BACKUP"
    val tokenNumber: Int = 0,
    val performedBy: String = "SYSTEM", // e.g. "Ankit Chaudhary (Super Admin)", "Sevadar Ramesh"
    val role: String = "SEVADAR",   // "SUPER_ADMIN" or "SEVADAR"
    val reason: String = "",        // e.g. "भक्त नहीं आए", "गलत प्रविष्टि"
    val darbarDate: String = "",
    val details: String = "",       // JSON or text description
    val timestamp: Long = System.currentTimeMillis()
)
