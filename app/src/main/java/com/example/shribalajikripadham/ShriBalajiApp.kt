package com.example.shribalajikripadham

import android.app.Application
import android.util.Log
import androidx.annotation.Keep
import com.onesignal.OneSignal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Keep
class ShriBalajiApp : Application() {

    companion object {
        const val ONESIGNAL_APP_ID = "db065153-88a1-4a01-badc-ae33c4b38cbe"
        private const val TAG = "ShriBalajiApp"
    }

    override fun onCreate() {
        super.onCreate()

        // 1. Initialize OneSignal v5 Push Notification Engine on Application Startup
        try {
            OneSignal.initWithContext(this, ONESIGNAL_APP_ID)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    OneSignal.Notifications.requestPermission(false)
                } catch (t: Throwable) {
                    Log.w(TAG, "OneSignal permission prompt warning: ${t.localizedMessage}")
                }
            }
            Log.i(TAG, "OneSignal v5 initialized successfully for Shri Balaji Kripa Dham")
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to initialize OneSignal: ${t.localizedMessage}", t)
        }
    }
}
