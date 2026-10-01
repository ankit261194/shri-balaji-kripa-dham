package com.example.shribalajikripadham.notification

import android.content.Context
import android.util.Log
import com.onesignal.OneSignal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Helper to manage OneSignal devotee identity and tags
 * Enables targeted token call push notifications by devotee phone number
 */
object OneSignalHelper {

    private const val TAG = "OneSignalHelper"

    /**
     * Bind devotee phone number as OneSignal external user ID and tag
     */
    fun registerDevoteeUser(phoneNumber: String) {
        val cleanPhone = phoneNumber.filter { it.isDigit() }.let {
            if (it.length > 10) it.takeLast(10) else it
        }

        if (cleanPhone.isBlank()) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // OneSignal v5: Login binds external_id to this device
                OneSignal.login(cleanPhone)

                // Also add tag for flexible query filtering
                OneSignal.User.addTag("phone", cleanPhone)
                OneSignal.User.addTag("role", "devotee")

                Log.d(TAG, "Devotee phone registered with OneSignal: $cleanPhone")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to register devotee with OneSignal: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Unbind devotee user upon sign out
     */
    fun unregisterDevoteeUser() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                OneSignal.logout()
                Log.d(TAG, "Devotee logged out from OneSignal")
            } catch (e: Exception) {
                Log.e(TAG, "OneSignal logout error: ${e.localizedMessage}")
            }
        }
    }
}
