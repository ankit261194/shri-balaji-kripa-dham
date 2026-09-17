package com.example.shribalajikripadham.notification

import android.content.Context
import android.util.Log
import com.example.shribalajikripadham.util.NotificationHelper
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AshramFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "AshramFCMService"
        const val PREFS_FCM = "sbkd_fcm_prefs"
        const val KEY_FCM_TOKEN = "fcm_token"
        const val KEY_LAST_PHONE = "last_registered_phone"

        fun getStoredFcmToken(context: Context): String? {
            val prefs = context.getSharedPreferences(PREFS_FCM, Context.MODE_PRIVATE)
            return prefs.getString(KEY_FCM_TOKEN, null)
        }

        fun registerDevoteePhone(context: Context, phoneNumber: String) {
            if (phoneNumber.isBlank()) return
            val prefs = context.getSharedPreferences(PREFS_FCM, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_LAST_PHONE, phoneNumber.trim()).apply()

            val token = getStoredFcmToken(context)
            if (token.isNullOrBlank()) return

            val deviceId = com.example.shribalajikripadham.hardware.DeviceFingerprintManager.getDeviceId(context)
            val deviceName = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"

            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                try {
                    com.example.shribalajikripadham.data.network.HostingerCentralSyncManager.registerFcmDeviceToken(
                        phoneNumber = phoneNumber.trim(),
                        fcmToken = token,
                        deviceId = deviceId,
                        deviceName = deviceName
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error registering FCM token: ${e.localizedMessage}")
                }
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM Token generated: $token")
        val prefs = getSharedPreferences(PREFS_FCM, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_FCM_TOKEN, token).apply()
        val lastPhone = prefs.getString(KEY_LAST_PHONE, null)
        if (!lastPhone.isNullOrBlank()) {
            registerDevoteePhone(applicationContext, lastPhone)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "FCM Message received from: ${remoteMessage.from}")

        var title = remoteMessage.notification?.title
        var body = remoteMessage.notification?.body

        if (remoteMessage.data.isNotEmpty()) {
            val dataTitle = remoteMessage.data["title"]
            val dataBody = remoteMessage.data["body"] ?: remoteMessage.data["message"]
            if (!dataTitle.isNullOrBlank()) title = dataTitle
            if (!dataBody.isNullOrBlank()) body = dataBody

            val type = remoteMessage.data["type"]
            if (type == "TOKEN_CALL") {
                val tokenNumber = remoteMessage.data["token_number"] ?: ""
                val devoteeName = remoteMessage.data["patient_name"] ?: ""
                if (tokenNumber.isNotBlank()) {
                    title = "🔔 टोकन बुलावा: टोकन #$tokenNumber"
                    body = if (devoteeName.isNotBlank()) {
                        "श्री $devoteeName जी, टोकन #$tokenNumber का नंबर आ गया है। कृपया गुरुजी के समीप पधारें।"
                    } else {
                        "टोकन #$tokenNumber का नंबर आ गया है। कृपया गुरुजी के समीप पधारें।"
                    }
                }
            }
        }

        if (!title.isNullOrBlank() || !body.isNullOrBlank()) {
            NotificationHelper.showSystemNotification(
                context = applicationContext,
                title = title ?: "श्री बालाजी कृपा धाम",
                message = body ?: "आश्रम से नया संदेश प्राप्त हुआ है।"
            )
        }
    }

    private fun syncTokenWithBackend(token: String) {
        Log.d(TAG, "FCM Token stored locally: $token")
    }
}
