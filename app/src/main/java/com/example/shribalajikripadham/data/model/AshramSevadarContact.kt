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
            val phone = json.optString("phone", json.optString("phoneNumber", "9100100251"))
            val wa = json.optString("whatsapp", json.optString("whatsappNumber", phone))
            val role = json.optString("role", json.optString("roleTitleHindi", "सेवादार"))
            val dept = json.optString("department", "सामान्य आश्रम सहायता")
            val photo = json.optString("photo_url", json.optString("photoUri", ""))
            val bio = json.optString("bio", json.optString("description", ""))
            val isAvail = if (json.has("is_available")) {
                val v = json.opt("is_available")
                if (v is Boolean) v else (json.optInt("is_available", 1) == 1)
            } else {
                json.optBoolean("isAvailable", true)
            }

            return AshramSevadarContact(
                id = json.optString("id", System.currentTimeMillis().toString()),
                name = json.optString("name", "आश्रम सेवादार"),
                department = dept,
                roleTitleHindi = role,
                phoneNumber = phone,
                whatsappNumber = wa,
                isAvailable = isAvail,
                photoUri = photo,
                description = bio
            )
        }
    }
}

/**
 * WhatsApp-Level Sevadar Chat Message Model:
 * Fully supports real-time text, photos, audio voice notes, documents,
 * sender roles, delivery status ticks (SENT, DELIVERED, READ), and timestamps.
 */
data class SevadarChatMessage(
    val id: String,
    val conversationId: String = "",
    val sevadarId: String = "",
    val sevadarName: String = "आश्रम सेवादार",
    val devoteeId: String = "",
    val devoteeName: String = "भक्त",
    val devoteePhone: String = "",
    val senderRole: String = "DEVOTEE", // DEVOTEE, SEVADAR, ADMIN
    val isFromDevotee: Boolean = (senderRole == "DEVOTEE"),
    val senderName: String = if (isFromDevotee) devoteeName else sevadarName,
    val messageType: String = "TEXT", // TEXT, PHOTO, AUDIO_VOICE, DOCUMENT
    val message: String = "",
    val attachmentUri: String? = null,
    val attachmentType: String = "NONE", // NONE, PHOTO, AUDIO_VOICE, FILE
    val mediaDurationSec: Int = 0,
    val status: String = "SENT", // SENT, DELIVERED, READ
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("msg_id", id)
            put("conversationId", conversationId)
            put("conversation_id", conversationId)
            put("sevadarId", sevadarId)
            put("sevadar_id", sevadarId)
            put("sevadarName", sevadarName)
            put("sevadar_name", sevadarName)
            put("devoteeId", devoteeId)
            put("devotee_id", devoteeId)
            put("devoteeName", devoteeName)
            put("devotee_name", devoteeName)
            put("devoteePhone", devoteePhone)
            put("devotee_phone", devoteePhone)
            put("senderRole", senderRole)
            put("sender_role", senderRole)
            put("senderName", senderName)
            put("isFromDevotee", isFromDevotee)
            put("messageType", messageType)
            put("message_type", messageType)
            put("message", message)
            put("message_text", message)
            put("attachmentUri", attachmentUri ?: "")
            put("attachment_url", attachmentUri ?: "")
            put("attachmentType", attachmentType)
            put("attachment_type", attachmentType)
            put("mediaDurationSec", mediaDurationSec)
            put("media_duration", mediaDurationSec)
            put("status", status)
            put("timestamp", timestamp)
            put("created_at", timestamp)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): SevadarChatMessage {
            val att = json.optString("attachment_url", json.optString("attachmentUri", ""))
            val attType = json.optString("attachment_type", json.optString("attachmentType", "NONE"))
            val msgText = json.optString("message_text", json.optString("message", ""))
            val sRole = json.optString("sender_role", json.optString("senderRole", ""))
            val isFromDev = if (sRole.isNotBlank()) {
                sRole.equals("DEVOTEE", ignoreCase = true)
            } else {
                json.optBoolean("isFromDevotee", true)
            }
            val resolvedRole = if (sRole.isNotBlank()) sRole else (if (isFromDev) "DEVOTEE" else "SEVADAR")

            val msgId = json.optString("msg_id", json.optString("id", System.currentTimeMillis().toString()))
            val time = json.optLong("created_at", json.optLong("timestamp", System.currentTimeMillis()))
            val convId = json.optString("conversation_id", json.optString("conversationId", ""))
            val sevId = json.optString("sevadar_id", json.optString("sevadarId", ""))
            val sevName = json.optString("sevadar_name", json.optString("sevadarName", "आश्रम सेवादार"))
            val devId = json.optString("devotee_id", json.optString("devoteeId", ""))
            val devName = json.optString("devotee_name", json.optString("devoteeName", "भक्त"))
            val devPhone = json.optString("devotee_phone", json.optString("devoteePhone", ""))
            val mType = json.optString("message_type", json.optString("messageType", "TEXT"))
            val duration = json.optInt("media_duration", json.optInt("mediaDurationSec", 0))
            val sName = json.optString("senderName", if (isFromDev) devName else sevName)
            val stat = json.optString("status", "SENT")

            return SevadarChatMessage(
                id = msgId,
                conversationId = convId,
                sevadarId = sevId,
                sevadarName = sevName,
                devoteeId = devId,
                devoteeName = devName,
                devoteePhone = devPhone,
                senderRole = resolvedRole,
                isFromDevotee = isFromDev,
                senderName = sName,
                messageType = mType,
                message = msgText,
                attachmentUri = if (att.isBlank()) null else att,
                attachmentType = attType,
                mediaDurationSec = duration,
                status = stat,
                timestamp = time
            )
        }
    }
}
