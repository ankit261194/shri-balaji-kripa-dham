package com.example.shribalajikripadham.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.shribalajikripadham.data.model.AshramSevadarContact
import com.example.shribalajikripadham.data.model.SevadarChatMessage
import com.example.shribalajikripadham.data.network.HostingerCentralSyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object SevadarChatRepository {
    private const val TAG = "SevadarChatRepo"
    private const val BASE_URL = "https://shribalajikripadham.online/api/sevadar_chat.php"

    /**
     * Fetches official Sevadar directory from live cloud server with instant fallback to local cache.
     */
    suspend fun getSevadars(context: Context): List<AshramSevadarContact> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL?action=get_sevadars")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                HostingerCentralSyncManager.applyAuthHeaders(this, "GET", context)
            }

            if (conn.responseCode == 200) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)
                if (json.optBoolean("success", false)) {
                    val array = json.optJSONArray("sevadars") ?: JSONArray()
                    val list = mutableListOf<AshramSevadarContact>()
                    for (i in 0 until array.length()) {
                        list.add(AshramSevadarContact.fromJson(array.getJSONObject(i)))
                    }
                    if (list.isNotEmpty()) {
                        SevadarDirectoryManager.saveSevadars(context, list)
                        return@withContext list
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Cloud sevadars fetch failed, using local cache: ${e.message}")
        }
        return@withContext SevadarDirectoryManager.getAllSevadars(context)
    }

    /**
     * Loads live conversation messages from Hostinger MySQL with bidirectional sync.
     */
    suspend fun getMessages(
        context: Context,
        conversationId: String,
        sevadarId: String,
        devoteePhone: String,
        sinceTimestamp: Long = 0
    ): List<SevadarChatMessage> = withContext(Dispatchers.IO) {
        val localMsgs = SevadarDirectoryManager.getChatMessages(context, sevadarId).toMutableList()

        try {
            val qConv = URLEncoder.encode(conversationId, "UTF-8")
            val qSev = URLEncoder.encode(sevadarId, "UTF-8")
            val qPhone = URLEncoder.encode(devoteePhone, "UTF-8")
            val urlStr = "$BASE_URL?action=get_messages&conversation_id=$qConv&sevadar_id=$qSev&devotee_phone=$qPhone&since_timestamp=$sinceTimestamp"
            val url = URL(urlStr)

            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 7000
                readTimeout = 7000
                HostingerCentralSyncManager.applyAuthHeaders(this, "GET", context)
            }

            if (conn.responseCode == 200) {
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(text)
                if (json.optBoolean("success", false)) {
                    val arr = json.optJSONArray("messages") ?: JSONArray()
                    val remoteList = mutableListOf<SevadarChatMessage>()
                    for (i in 0 until arr.length()) {
                        remoteList.add(SevadarChatMessage.fromJson(arr.getJSONObject(i)))
                    }

                    // Merge remote messages into local cache
                    val map = localMsgs.associateBy { it.id }.toMutableMap()
                    remoteList.forEach { map[it.id] = it }
                    val merged = map.values.sortedBy { it.timestamp }
                    SevadarDirectoryManager.saveChatMessagesDirect(context, sevadarId, merged)
                    return@withContext merged
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Cloud messages fetch failed, serving local cache: ${e.message}")
        }

        return@withContext localMsgs
    }

    /**
     * Sends message to Hostinger MySQL cloud server and local storage immediately.
     */
    suspend fun sendMessage(
        context: Context,
        message: SevadarChatMessage
    ): Result<SevadarChatMessage> = withContext(Dispatchers.IO) {
        // Save immediately locally so UI reflects it with single tick
        SevadarDirectoryManager.sendChatMessage(context, message)

        try {
            val url = URL("$BASE_URL?action=send_message")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                HostingerCentralSyncManager.applyAuthHeaders(this, "POST", context)
            }

            val payload = message.toJson().toString()
            conn.outputStream.use { os ->
                os.write(payload.toByteArray(Charsets.UTF_8))
                os.flush()
            }

            if (conn.responseCode == 200) {
                val respText = conn.inputStream.bufferedReader().use { it.readText() }
                val respJson = JSONObject(respText)
                if (respJson.optBoolean("success", false)) {
                    val dataObj = respJson.optJSONObject("data")
                    val updatedMsg = if (dataObj != null) {
                        SevadarChatMessage.fromJson(dataObj)
                    } else {
                        message.copy(status = "DELIVERED")
                    }
                    SevadarDirectoryManager.updateChatMessageStatus(context, updatedMsg.sevadarId, updatedMsg.id, updatedMsg.status)
                    return@withContext Result.success(updatedMsg)
                }
            }
            return@withContext Result.success(message)
        } catch (e: Exception) {
            Log.e(TAG, "Error posting chat message to server", e)
            return@withContext Result.success(message) // Return optimistic message
        }
    }

    /**
     * Uploads media file (Photo / Voice Note / Document) to cloud server.
     */
    suspend fun uploadChatMedia(
        context: Context,
        source: Any, // File or Uri
        isVoiceNote: Boolean = false
    ): Result<String> = withContext(Dispatchers.IO) {
        val boundary = "==SBKD_CHAT_UPLOAD_" + System.currentTimeMillis() + "=="
        val lineEnd = "\r\n"
        val twoHyphens = "--"

        try {
            val url = URL("$BASE_URL?action=upload_chat_media")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                doInput = true
                useCaches = false
                connectTimeout = 15000
                readTimeout = 15000
                setRequestProperty("Connection", "Keep-Alive")
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                HostingerCentralSyncManager.applyAuthHeaders(this, "POST", context)
            }

            val (fileName, fileBytes) = when (source) {
                is File -> {
                    Pair(source.name, source.readBytes())
                }
                is Uri -> {
                    val cr = context.contentResolver
                    val bytes = cr.openInputStream(source)?.use { it.readBytes() }
                        ?: return@withContext Result.failure(Exception("Unable to read Uri"))
                    val ext = if (isVoiceNote) "m4a" else "jpg"
                    Pair("upload_${System.currentTimeMillis()}.$ext", bytes)
                }
                else -> return@withContext Result.failure(IllegalArgumentException("Unsupported media source"))
            }

            val mimeType = if (isVoiceNote) "audio/mp4" else "image/jpeg"

            val dos = DataOutputStream(conn.outputStream)
            dos.writeBytes("$twoHyphens$boundary$lineEnd")
            dos.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"$fileName\"$lineEnd")
            dos.writeBytes("Content-Type: $mimeType$lineEnd")
            dos.writeBytes(lineEnd)
            dos.write(fileBytes)
            dos.writeBytes(lineEnd)
            dos.writeBytes("$twoHyphens$boundary$twoHyphens$lineEnd")
            dos.flush()
            dos.close()

            if (conn.responseCode == 200) {
                val respText = conn.inputStream.bufferedReader().use { it.readText() }
                val respJson = JSONObject(respText)
                if (respJson.optBoolean("success", false)) {
                    val publicUrl = respJson.optString("url", respJson.optString("media_url", ""))
                    if (publicUrl.isNotBlank()) {
                        return@withContext Result.success(publicUrl)
                    }
                }
            }
            return@withContext Result.failure(Exception("Media upload failed (HTTP ${conn.responseCode})"))
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading chat media", e)
            return@withContext Result.failure(e)
        }
    }

    /**
     * Marks messages in a conversation as READ on server.
     */
    suspend fun markRead(
        context: Context,
        conversationId: String,
        readerRole: String = "DEVOTEE"
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL?action=mark_read")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                HostingerCentralSyncManager.applyAuthHeaders(this, "POST", context)
            }

            val body = JSONObject().apply {
                put("conversation_id", conversationId)
                put("reader_role", readerRole)
            }

            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            return@withContext (conn.responseCode == 200)
        } catch (e: Exception) {
            return@withContext false
        }
    }

    /**
     * Lists active conversations for Sevadar or Admin Dashboard.
     */
    suspend fun listConversations(
        context: Context,
        sevadarId: String = "",
        isSuperAdmin: Boolean = true
    ): List<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val qSev = URLEncoder.encode(sevadarId, "UTF-8")
            val url = URL("$BASE_URL?action=list_conversations&sevadar_id=$qSev&is_super_admin=$isSuperAdmin")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                HostingerCentralSyncManager.applyAuthHeaders(this, "GET", context)
            }

            if (conn.responseCode == 200) {
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(text)
                if (json.optBoolean("success", false)) {
                    val arr = json.optJSONArray("conversations") ?: JSONArray()
                    val list = mutableListOf<JSONObject>()
                    for (i in 0 until arr.length()) {
                        list.add(arr.getJSONObject(i))
                    }
                    return@withContext list
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error listing conversations from server", e)
        }
        return@withContext emptyList()
    }
}
