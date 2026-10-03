package com.example.shribalajikripadham.data.model

import org.json.JSONObject

data class AshramSevadarContact(
    val id: String,
    val name: String,
    val department: String,
    val roleTitleHindi: String,
    val phoneNumber: String,
    val whatsappNumber: String,
    val isAvailable: Boolean = true,
    val photoUri: String = "",
    val description: String = ""
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("name", name)
            put("department", department)
            put("roleTitleHindi", roleTitleHindi)
            put("phoneNumber", phoneNumber)
            put("whatsappNumber", whatsappNumber)
            put("isAvailable", isAvailable)
            put("photoUri", photoUri)
            put("description", description)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): AshramSevadarContact {
            return AshramSevadarContact(
                id = json.optString("id", System.currentTimeMillis().toString()),
                name = json.optString("name", ""),
                department = json.optString("department", "सामान्य सहायता"),
                roleTitleHindi = json.optString("roleTitleHindi", "सेवादार"),
                phoneNumber = json.optString("phoneNumber", "9720691090"),
                whatsappNumber = json.optString("whatsappNumber", json.optString("phoneNumber", "9720691090")),
                isAvailable = json.optBoolean("isAvailable", true),
                photoUri = json.optString("photoUri", ""),
                description = json.optString("description", "")
            )
        }
    }
}

data class SevadarChatMessage(
    val id: String,
    val sevadarId: String,
    val senderName: String,
    val isFromDevotee: Boolean,
    val message: String,
    val attachmentUri: String? = null,
    val attachmentType: String = "NONE", // NONE, PHOTO, FILE
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("sevadarId", sevadarId)
            put("senderName", senderName)
            put("isFromDevotee", isFromDevotee)
            put("message", message)
            put("attachmentUri", attachmentUri ?: "")
            put("attachmentType", attachmentType)
            put("timestamp", timestamp)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): SevadarChatMessage {
            val att = json.optString("attachmentUri", "")
            return SevadarChatMessage(
                id = json.optString("id", System.currentTimeMillis().toString()),
                sevadarId = json.optString("sevadarId", ""),
                senderName = json.optString("senderName", "भक्त"),
                isFromDevotee = json.optBoolean("isFromDevotee", true),
                message = json.optString("message", ""),
                attachmentUri = if (att.isBlank()) null else att,
                attachmentType = json.optString("attachmentType", "NONE"),
                timestamp = json.optLong("timestamp", System.currentTimeMillis())
            )
        }
    }
}
