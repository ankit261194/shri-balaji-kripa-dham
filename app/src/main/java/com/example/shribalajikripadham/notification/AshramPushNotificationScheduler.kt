package com.example.shribalajikripadham.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

object AshramPushNotificationScheduler {

    const val JOB_ID = 78601
    private const val TAG = "PushScheduler"

    /**
     * Schedules periodic background push check using Android JobScheduler.
     * Guaranteed to persist across reboots and wake up the device when network is available.
     */
    fun schedulePeriodicJob(context: Context) {
        try {
            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            val componentName = ComponentName(context, AshramBackgroundPushJobService::class.java)

            // Minimum periodic interval on Android is 15 minutes (15 * 60 * 1000)
            val builder = JobInfo.Builder(JOB_ID, componentName)
                .setPeriodic(15 * 60 * 1000L)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPersisted(true) // Persists across phone reboots

            val result = jobScheduler.schedule(builder.build())
            if (result == JobScheduler.RESULT_SUCCESS) {
                Log.d(TAG, "JobScheduler periodic push job scheduled successfully")
            } else {
                Log.w(TAG, "JobScheduler scheduling failed, attempting fallback AlarmManager")
                scheduleAlarmFallback(context)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling job: ${e.message}")
            scheduleAlarmFallback(context)
        }
    }

    /**
     * Fallback for devices with aggressive battery optimizers (MIUI, ColorOS, FunTouchOS).
     */
    fun scheduleAlarmFallback(context: Context) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, AshramBootReceiver::class.java).apply {
                action = "com.example.shribalajikripadham.CHECK_PUSH_NOW"
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                999,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            // 30 minute inexact repeating alarm
            val interval = 30 * 60 * 1000L
            val triggerAt = System.currentTimeMillis() + interval
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                interval,
                pendingIntent
            )
        } catch (e: Exception) {
            Log.e(TAG, "AlarmManager fallback error: ${e.message}")
        }
    }
}
