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
                id = "sev_token_1",
                name = "श्री सुखवीर सिंह जी",
                department = "टोकन व दर्शन",
                roleTitleHindi = "मुख्य टोकन सेवादार",
                phoneNumber = "9720691090",
                whatsappNumber = "9720691090",
                isAvailable = true,
                description = "रविवार टोकन वितरण, कतार नियंत्रण व दर्शन सहायता"
            ),
            AshramSevadarContact(
                id = "sev_arzi_2",
                name = "श्री रामकुमार जी",
                department = "अर्जी व डाक विभाग",
                roleTitleHindi = "अर्जी व्यवस्थापक",
                phoneNumber = "9720691090",
                whatsappNumber = "9720691090",
                isAvailable = true,
                description = "नारियल व ध्वजा अर्जी, डाक प्रेषण व ट्रैकिंग सहायता"
            ),
            AshramSevadarContact(
                id = "sev_bus_3",
                name = "श्री धर्मेन्द्र शर्मा जी",
                department = "बस व यात्रा व्यवस्था",
                roleTitleHindi = "यात्रा संयोजक",
                phoneNumber = "9720691090",
                whatsappNumber = "9720691090",
                isAvailable = true,
                description = "आश्रम यात्रा बस सीट बुकिंग, समय सारणी व मार्ग निर्देश"
            ),
            AshramSevadarContact(
                id = "sev_havan_4",
                name = "श्री महेश शास्त्री जी",
                department = "हवन व पूजा सेवा",
                roleTitleHindi = "हवन सेवा प्रभारी",
                phoneNumber = "9720691090",
                whatsappNumber = "9720691090",
                isAvailable = true,
                description = "रविवार महा-हवन आहुति, यजमान संकल्प व पूजन सामग्री"
            ),
            AshramSevadarContact(
                id = "sev_bhandara_5",
                name = "श्री विजयपाल जी",
                department = "भंडारा व आवास",
                roleTitleHindi = "भंडारा सेवादार",
                phoneNumber = "9720691090",
                whatsappNumber = "9720691090",
                isAvailable = true,
                description = "प्रसाद भंडारा, दूर-दराज भक्तों हेतु विश्राम व आवास सेवा"
            ),
            AshramSevadarContact(
                id = "sev_general_6",
                name = "धाम मुख्य हेल्पलाइन",
                department = "सामान्य आश्रम सहायता",
                roleTitleHindi = "आश्रम कार्यालय",
                phoneNumber = "9720691090",
                whatsappNumber = "9720691090",
                isAvailable = true,
                description = "धाम पता, नियम, मंगलवार/रविवार दरबार समय व संपूर्ण जानकारी"
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
                list.add(AshramSevadarContact.fromJson(array.getJSONObject(i)))
            }
            if (list.isEmpty()) {
                val defaults = getDefaultSevadars()
                saveSevadars(context, defaults)
                defaults
            } else {
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

    // --- IN-APP CHAT MANAGEMENT ---
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

    fun sendChatMessage(context: Context, message: SevadarChatMessage) {
        val current = getChatMessages(context, message.sevadarId).toMutableList()
        current.add(message)
        val arr = JSONArray()
        current.forEach { arr.put(it.toJson()) }
        getChatPrefs(context).edit().putString("chat_${message.sevadarId}", arr.toString()).apply()
    }

    fun clearChat(context: Context, sevadarId: String) {
        getChatPrefs(context).edit().remove("chat_$sevadarId").apply()
    }
}
