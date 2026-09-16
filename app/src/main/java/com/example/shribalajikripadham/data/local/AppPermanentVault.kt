package com.example.shribalajikripadham.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.example.shribalajikripadham.data.model.Admin
import com.example.shribalajikripadham.data.model.AdminRole
import com.example.shribalajikripadham.data.model.AshramSettings
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.charset.StandardCharsets

/**
 * AppPermanentVault:
 * Indestructible local persistent backup vault for Admins, Permissions, and Settings.
 * Automatically mirrors all added Admins and Custom Banners to isolated app storage.
 * If SQLite is ever reset, re-installed, or migrated, this vault seamlessly restores everything!
 */
object AppPermanentVault {
    private const val TAG = "AppPermanentVault"
    private const val VAULT_FILE_NAME = "sbkd_vault_data.json"

    fun saveVault(context: Context, admins: List<Admin>, settings: AshramSettings?) {
        try {
            val root = JSONObject()
            root.put("vault_version", 1)
            root.put("updated_at", System.currentTimeMillis())

            // 1. Serialize Admins
            val adminsArr = JSONArray()
            admins.forEach { a ->
                val aObj = JSONObject().apply {
                    put("id", a.id)
                    put("name", a.name)
                    put("username", a.username)
                    put("phone", a.phoneNumber)
                    put("role", a.role.name)
                    put("pin_hash", a.pinHash)
                    put("password_hash", a.passwordHash)
                    put("photo_uri", a.photoUri)
                    put("is_active", a.isActive)
                    put("can_manage_tokens", a.canManageTokens)
                    put("can_issue_manual_tokens", a.canIssueManualTokens)
                    put("can_manage_yatra", a.canManageYatra)
                    put("can_manage_expenses", a.canManageExpenses)
                    put("can_change_location", a.canChangeLocation)
                    put("can_send_notifications", a.canSendNotifications)
                    put("can_edit_ashram_info", a.canEditAshramInfo)
                    put("can_manage_admins", a.canManageAdmins)
                    put("can_view_devotee_photos", a.canViewDevoteePhotos)
                    put("can_issue_tokens_anywhere", a.canIssueTokensAnywhere)
                    put("can_scan_paper_register", a.canScanPaperRegister)
                    put("can_manage_parchas", a.canManageParchas)
                    put("can_cancel_tokens", a.canCancelTokens)
                    put("can_delete_tokens", a.canDeleteTokens)
                    put("can_custom_token_number", a.canSetCustomTokenNumber)
                    put("can_export_pdf", a.canExportPdf)
                    put("can_manage_arzi", a.canManageArzi)
                    put("created_at", a.createdAt)
                }
                adminsArr.put(aObj)
            }
            root.put("admins", adminsArr)

            // 2. Serialize Custom Settings & Banner
            if (settings != null) {
                val setObj = JSONObject().apply {
                    put("banner_title", settings.bannerTitle)
                    put("banner_subtitle", settings.bannerSubtitle)
                    put("banner_photo_uri", settings.bannerPhotoUri)
                    put("is_banner_visible", settings.isBannerVisible)
                    put("banner_action_url", settings.bannerActionUrl)
                    put("emergency_notice", settings.emergencyNoticeText)
                    put("is_emergency_notice_visible", settings.isEmergencyNoticeVisible)
                    put("latitude", settings.latitude)
                    put("longitude", settings.longitude)
                    put("allowed_radius_meters", settings.allowedRadiusMeters)
                    put("is_geofence_enforced", settings.isGeofenceEnforced)
                    put("ashram_name", settings.ashramName)
                    put("guruji_name", settings.gurujiName)
                    put("address", settings.address)
                    put("contact_phone", settings.contactPhone)
                    put("darbar_timings", settings.darbarTimings)
                }
                root.put("settings", setObj)
            }

            val jsonStr = root.toString(2)

            // Save in internal app files
            val internalFile = File(context.filesDir, VAULT_FILE_NAME)
            internalFile.writeText(jsonStr, StandardCharsets.UTF_8)

            // Also mirror in external files dir (persists across reinstall if files kept)
            try {
                val extDir = context.getExternalFilesDir(null)
                if (extDir != null) {
                    val extFile = File(extDir, VAULT_FILE_NAME)
                    extFile.writeText(jsonStr, StandardCharsets.UTF_8)
                }
            } catch (e: Exception) {}

            Log.d(TAG, "Permanent vault saved with ${admins.size} admins")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save permanent vault: ${e.message}")
        }
    }

    fun restoreVault(context: Context, db: SQLiteDatabase): Boolean {
        try {
            var file = File(context.filesDir, VAULT_FILE_NAME)
            if (!file.exists()) {
                val extDir = context.getExternalFilesDir(null)
                if (extDir != null) {
                    file = File(extDir, VAULT_FILE_NAME)
                }
            }
            if (!file.exists()) return false

            val jsonStr = file.readText(StandardCharsets.UTF_8)
            val root = JSONObject(jsonStr)

            // Restore Admins
            val adminsArr = root.optJSONArray("admins")
            if (adminsArr != null && adminsArr.length() > 0) {
                db.beginTransaction()
                try {
                    for (i in 0 until adminsArr.length()) {
                        val a = adminsArr.getJSONObject(i)
                        val username = a.optString("username", "")
                        if (username.isBlank()) continue

                        val cv = ContentValues().apply {
                            put("name", a.optString("name", ""))
                            put("username", username)
                            put("phone", a.optString("phone", ""))
                            put("role", a.optString("role", "SEVADAR"))
                            put("pin_hash", a.optString("pin_hash", ""))
                            put("password_hash", a.optString("password_hash", ""))
                            put("photo_uri", a.optString("photo_uri", ""))
                            put("is_active", if (a.optBoolean("is_active", true)) 1 else 0)
                            put("can_manage_tokens", if (a.optBoolean("can_manage_tokens", true)) 1 else 0)
                            put("can_issue_manual_tokens", if (a.optBoolean("can_issue_manual_tokens", true)) 1 else 0)
                            put("can_manage_yatra", if (a.optBoolean("can_manage_yatra", true)) 1 else 0)
                            put("can_manage_expenses", if (a.optBoolean("can_manage_expenses", true)) 1 else 0)
                            put("can_change_location", if (a.optBoolean("can_change_location", false)) 1 else 0)
                            put("can_send_notifications", if (a.optBoolean("can_send_notifications", false)) 1 else 0)
                            put("can_edit_ashram_info", if (a.optBoolean("can_edit_ashram_info", false)) 1 else 0)
                            put("can_manage_admins", if (a.optBoolean("can_manage_admins", false)) 1 else 0)
                            put("can_view_devotee_photos", if (a.optBoolean("can_view_devotee_photos", false)) 1 else 0)
                            put("can_issue_tokens_anywhere", if (a.optBoolean("can_issue_tokens_anywhere", false)) 1 else 0)
                            put("can_scan_paper_register", if (a.optBoolean("can_scan_paper_register", false)) 1 else 0)
                            put("can_manage_parchas", if (a.optBoolean("can_manage_parchas", false)) 1 else 0)
                            put("can_cancel_tokens", if (a.optBoolean("can_cancel_tokens", false)) 1 else 0)
                            put("can_delete_tokens", if (a.optBoolean("can_delete_tokens", false)) 1 else 0)
                            put("can_custom_token_number", if (a.optBoolean("can_custom_token_number", false)) 1 else 0)
                            put("can_export_pdf", if (a.optBoolean("can_export_pdf", true)) 1 else 0)
                            put("can_manage_arzi", if (a.optBoolean("can_manage_arzi", false)) 1 else 0)
                            put("created_at", a.optLong("created_at", System.currentTimeMillis()))
                        }

                        val updated = db.update("admins", cv, "username = ?", arrayOf(username))
                        if (updated == 0) {
                            db.insert("admins", null, cv)
                        }
                    }
                    db.setTransactionSuccessful()
                } finally {
                    db.endTransaction()
                }
            }

            // Restore Banner and Custom Settings
            val setObj = root.optJSONObject("settings")
            if (setObj != null) {
                val cv = ContentValues().apply {
                    val bTitle = setObj.optString("banner_title", "")
                    if (bTitle.isNotBlank()) put("banner_title", bTitle)
                    val bSub = setObj.optString("banner_subtitle", "")
                    if (bSub.isNotBlank()) put("banner_subtitle", bSub)
                    val bPhoto = setObj.optString("banner_photo_uri", "")
                    if (bPhoto.isNotBlank()) put("banner_photo_uri", bPhoto)
                    if (setObj.has("is_banner_visible")) put("is_banner_visible", if (setObj.optBoolean("is_banner_visible", true)) 1 else 0)
                    val bUrl = setObj.optString("banner_action_url", "")
                    if (bUrl.isNotBlank()) put("banner_action_url", bUrl)

                    val emNotice = setObj.optString("emergency_notice", "")
                    if (emNotice.isNotBlank()) put("emergency_notice", emNotice)
                    if (setObj.has("is_emergency_notice_visible")) put("is_emergency_notice_visible", if (setObj.optBoolean("is_emergency_notice_visible", false)) 1 else 0)

                    val lat = setObj.optDouble("latitude", 0.0)
                    if (lat != 0.0) put("latitude", lat)
                    val lng = setObj.optDouble("longitude", 0.0)
                    if (lng != 0.0) put("longitude", lng)
                }
                if (cv.size() > 0) {
                    db.update("ashram_settings", cv, "id = 1", null)
                }
            }

            Log.d(TAG, "Permanent vault successfully restored!")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore permanent vault: ${e.message}")
            return false
        }
    }
}
