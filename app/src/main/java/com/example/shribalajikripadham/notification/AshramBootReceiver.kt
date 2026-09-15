package com.example.shribalajikripadham.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AshramBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) return

        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON" -> {
                // Ensure periodic job is active after phone turns on or app updates
                AshramPushNotificationScheduler.schedulePeriodicJob(context)

                // Run an immediate check on boot in background
                CoroutineScope(Dispatchers.IO).launch {
                    AshramBackgroundPushJobService.executeBackgroundCheck(context)
                }
            }
            "com.example.shribalajikripadham.CHECK_PUSH_NOW" -> {
                CoroutineScope(Dispatchers.IO).launch {
                    AshramBackgroundPushJobService.executeBackgroundCheck(context)
                }
            }
        }
    }
}
