package com.example.shribalajikripadham.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.shribalajikripadham.data.model.AshramSevadarContact
import com.example.shribalajikripadham.data.model.SevadarChatMessage
import org.json.JSONArray
import org.json.JSONObject

object SevadarDirectoryManager {
    private const val PREFS_NAME = "sbkd_sevadar_directory_prefs"
    private const val KEY_SEVADARS = "sevadars_list_json"
    private const val PREFS_CHAT = "sbkd_sevadar_chat_prefs"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun getChatPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_CHAT, Context.MODE_PRIVATE)
    }

    fun getDefaultSevadars(): List<AshramSevadarContact> {
        return listOf(
            AshramSevadarContact(
                id = "ashram_helpline_main",
                name = "श्री बालाजी कृपा धाम (आधिकारिक हेल्पलाइन)",
                department = "सामान्य आश्रम सहायता",
                roleTitleHindi = "मुख्य आश्रम सेवादार",
                phoneNumber = "9100100251",
                whatsappNumber = "9100100251",
                isAvailable = true,
                description = "धाम पता, नियम, मंगलवार/रविवार दरबार समय व संपूर्ण आधिकारिक जानकारी"
            )
        )
    }

    fun getAllSevadars(context: Context): List<AshramSevadarContact> {
        val prefs = getPrefs(context)
        val rawJson = prefs.getString(KEY_SEVADARS, null)
        if (rawJson.isNullOrBlank()) {
            val defaults = getDefaultSevadars()
            saveSevadars(context, defaults)
            return defaults
        }
        return try {
            val array = JSONArray(rawJson)
            val list = mutableListOf<AshramSevadarContact>()
            for (i in 0 until array.length()) {
                val item = AshramSevadarContact.fromJson(array.getJSONObject(i))
                // CRITICAL PRIVACY & REAL-DATA FILTER:
                val isMock = item.id.startsWith("mock_")

                if (!isMock && item.name.isNotBlank()) {
                    list.add(item)
                }
            }
            if (list.isEmpty()) {
                val defaults = getDefaultSevadars()
                saveSevadars(context, defaults)
                defaults
            } else {
                saveSevadars(context, list)
                list
            }
        } catch (e: Exception) {
            val defaults = getDefaultSevadars()
            saveSevadars(context, defaults)
            defaults
        }
    }

    fun saveSevadars(context: Context, sevadars: List<AshramSevadarContact>) {
        val array = JSONArray()
        sevadars.forEach { array.put(it.toJson()) }
        getPrefs(context).edit().putString(KEY_SEVADARS, array.toString()).apply()
    }

    fun addSevadar(context: Context, sevadar: AshramSevadarContact) {
        val current = getAllSevadars(context).toMutableList()
        current.add(sevadar)
        saveSevadars(context, current)
    }

    fun updateSevadar(context: Context, updated: AshramSevadarContact) {
        val current = getAllSevadars(context).toMutableList()
        val index = current.indexOfFirst { it.id == updated.id }
        if (index >= 0) {
            current[index] = updated
            saveSevadars(context, current)
        }
    }

    fun deleteSevadar(context: Context, id: String) {
        val current = getAllSevadars(context).filter { it.id != id }
        saveSevadars(context, current)
    }

    // --- IN-APP CHAT MANAGEMENT (PERMANENT RETENTION & ROLE-BASED PRIVACY) ---
    /**
     * Retrieves all chat messages for a specific sevadar. Messages persist permanently like WhatsApp.
     */
    fun getChatMessages(context: Context, sevadarId: String): List<SevadarChatMessage> {
        val prefs = getChatPrefs(context)
        val raw = prefs.getString("chat_$sevadarId", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<SevadarChatMessage>()
            for (i in 0 until arr.length()) {
                list.add(SevadarChatMessage.fromJson(arr.getJSONObject(i)))
            }
            list.sortedBy { it.timestamp }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Role-based privacy:
     * - SUPER_ADMIN: Can view ANY sevadar's chat messages.
     * - SEVADAR / ADMIN: Can ONLY view messages where sevadarId matches their adminId or sevadarPhone matches their phone.
     *   They are STRICTLY BLOCKED from viewing other sevadars' messages!
     */
    fun getChatMessagesForRole(
        context: Context,
        currentUserRole: String,
        currentAdminId: String,
        currentAdminPhone: String,
        targetSevadarId: String,
        targetSevadarPhone: String
    ): List<SevadarChatMessage> {
        if (currentUserRole.equals("SUPER_ADMIN", ignoreCase = true)) {
            return getChatMessages(context, targetSevadarId)
        }

        val isOwnSevadar = (currentAdminId.isNotBlank() && targetSevadarId.equals(currentAdminId, ignoreCase = true)) ||
                (currentAdminPhone.isNotBlank() && targetSevadarPhone.isNotBlank() &&
                        currentAdminPhone.replace(Regex("[^0-9]"), "").endsWith(targetSevadarPhone.replace(Regex("[^0-9]"), "").takeLast(10)))

        return if (isOwnSevadar) {
            getChatMessages(context, targetSevadarId)
        } else {
            // Strictly forbidden to see another sevadar's chat messages!
            emptyList()
        }
    }

    /**
     * Super Admin helper to inspect all active conversations across all registered sevadars.
     */
    fun getAllChatsForSuperAdmin(context: Context): Map<String, List<SevadarChatMessage>> {
        val sevadars = getAllSevadars(context)
        val result = mutableMapOf<String, List<SevadarChatMessage>>()
        sevadars.forEach { sev ->
            val msgs = getChatMessages(context, sev.id)
            if (msgs.isNotEmpty()) {
                result[sev.id] = msgs
            }
        }
        return result
    }

    /**
     * Sends and permanently saves a chat message like WhatsApp. Never overwritten or deleted.
     */
    fun sendChatMessage(context: Context, message: SevadarChatMessage) {
        val current = getChatMessages(context, message.sevadarId).toMutableList()
        current.add(message)
        val arr = JSONArray()
        current.forEach { arr.put(it.toJson()) }
        getChatPrefs(context).edit().putString("chat_${message.sevadarId}", arr.toString()).apply()
    }
}
