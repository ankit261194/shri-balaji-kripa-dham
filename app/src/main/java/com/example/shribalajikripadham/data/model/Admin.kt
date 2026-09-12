package com.example.shribalajikripadham.data.model

enum class AdminRole {
    SUPER_ADMIN,
    SEVADAR
}

data class Admin(
    val id: Long = 0,
    val name: String,
    val username: String = "",
    val phoneNumber: String,
    val role: AdminRole = AdminRole.SEVADAR,
    val pinHash: String = "",
    val passwordHash: String = "",
    // Granular permissions matrix:
    val canManageTokens: Boolean = true,
    val canIssueManualTokens: Boolean = true,
    val canManageYatra: Boolean = true,
    val canManageExpenses: Boolean = true,
    val canChangeLocation: Boolean = false,
    val canSendNotifications: Boolean = false,
    val canEditAshramInfo: Boolean = false,
    val canManageAdmins: Boolean = false,
    val canViewDevoteePhotos: Boolean = false,
    val canIssueTokensAnywhere: Boolean = false,
    val canScanPaperRegister: Boolean = false,
    val canManageParchas: Boolean = false,
    val photoUri: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
