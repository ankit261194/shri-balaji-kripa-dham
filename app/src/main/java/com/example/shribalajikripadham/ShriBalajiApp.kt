package com.example.shribalajikripadham

import android.app.Application
import android.util.Log
import androidx.annotation.Keep
import com.onesignal.OneSignal
import com.onesignal.debug.LogLevel

@Keep
class ShriBalajiApp : Application() {

    companion object {
        private const val TAG = "ShriBalajiApp"
        const val ONESIGNAL_APP_ID = "db065153-88a1-4a01-badc-ae33c4b38cbe"
    }

    override fun onCreate() {
        super.onCreate()

        try {
            // OneSignal Debugging set to WARN for production
            OneSignal.Debug.logLevel = LogLevel.WARN

            // Safe async initialization of OneSignal with Temple App ID
            OneSignal.initWithContext(this, ONESIGNAL_APP_ID)
        } catch (t: Throwable) {
            // Gracefully catch any linkage or initialization issues so the app ALWAYS launches smoothly
            Log.e(TAG, "OneSignal initialization gracefully bypassed: ${t.localizedMessage}", t)
        }
    }
}
