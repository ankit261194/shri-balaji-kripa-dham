package com.example.shribalajikripadham

import android.app.Application
import android.util.Log
import com.onesignal.OneSignal
import com.onesignal.debug.LogLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ShriBalajiApp : Application() {

    companion object {
        private const val TAG = "ShriBalajiApp"
        const val ONESIGNAL_APP_ID = "db065153-88a1-4a01-badc-ae33c4b38cbe"
    }

    override fun onCreate() {
        super.onCreate()

        try {
            // OneSignal Debugging
            OneSignal.Debug.logLevel = LogLevel.WARN

            // Initialize OneSignal with Temple App ID
            OneSignal.initWithContext(this, ONESIGNAL_APP_ID)

            // Request Notification Permission prompt for Android 13+
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    OneSignal.Notifications.requestPermission(true)
                } catch (e: Exception) {
                    Log.e(TAG, "OneSignal permission request failed: ${e.localizedMessage}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "OneSignal initialization failed: ${e.localizedMessage}")
        }
    }
}
