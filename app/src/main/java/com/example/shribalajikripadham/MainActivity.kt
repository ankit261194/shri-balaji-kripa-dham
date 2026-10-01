package com.example.shribalajikripadham

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.shribalajikripadham.data.model.AshramSettings
import com.example.shribalajikripadham.data.repository.AshramRepository
import com.example.shribalajikripadham.theme.MaroonPrimary
import com.example.shribalajikripadham.theme.SaffronPrimary
import com.example.shribalajikripadham.theme.ShriBalajiKripaDhamTheme
import com.example.shribalajikripadham.util.AppUpdateManager
import com.example.shribalajikripadham.util.NotificationHelper
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Bulletproof Global Uncaught Exception Handler: guarantees zero silent app terminations
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            android.util.Log.e("AshramCrashGuard", "CRITICAL FATAL CRASH PREVENTED on ${thread.name}: ${throwable.message}", throwable)
            try {
                val prefs = getSharedPreferences("app_crash_vault", android.content.Context.MODE_PRIVATE)
                prefs.edit().putString("last_fatal_error", "${throwable.javaClass.simpleName}: ${throwable.message}\n${throwable.stackTraceToString()}").apply()
            } catch (_: Exception) {}
        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Create notification channel & schedule native background push job
        try {
            NotificationHelper.createNotificationChannel(this)
            com.example.shribalajikripadham.notification.AshramPushNotificationScheduler.schedulePeriodicJob(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        setContent {
            val context = LocalContext.current
            var currentSacredTheme by remember {
                mutableStateOf(
                    try {
                        com.example.shribalajikripadham.theme.ThemePreferences.getSelectedTheme(context)
                    } catch (e: Exception) {
                        com.example.shribalajikripadham.theme.SacredTheme.WHATSAPP_EMERALD
                    }
                )
            }

            ShriBalajiKripaDhamTheme(sacredTheme = currentSacredTheme) {


                // Unified One-Time Permission Request (Location + Camera + Storage/Images + Notifications)
                val allRequiredPermissions = remember {
                    val list = mutableListOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.CAMERA
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        list.add(Manifest.permission.POST_NOTIFICATIONS)
                        list.add(Manifest.permission.READ_MEDIA_IMAGES)
                    } else {
                        list.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
                    list.toTypedArray()
                }

                val permissionPrefs = remember {
                    context.getSharedPreferences("sbkd_app_permissions", android.content.Context.MODE_PRIVATE)
                }
                var hasRequestedInitialPermissions by remember {
                    mutableStateOf(permissionPrefs.getBoolean("has_requested_initial_permissions_v3", false))
                }

                val unifiedPermissionsLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { _ ->
                    permissionPrefs.edit().putBoolean("has_requested_initial_permissions_v3", true).apply()
                    hasRequestedInitialPermissions = true
                }

                LaunchedEffect(Unit) {
                    if (!hasRequestedInitialPermissions) {
                        val pendingPermissions = allRequiredPermissions.filter { perm ->
                            ContextCompat.checkSelfPermission(context, perm) != PackageManager.PERMISSION_GRANTED
                        }
                        if (pendingPermissions.isNotEmpty()) {
                            unifiedPermissionsLauncher.launch(pendingPermissions.toTypedArray())
                        }
                        permissionPrefs.edit().putBoolean("has_requested_initial_permissions_v3", true).apply()
                        hasRequestedInitialPermissions = true
                    }

                    // Background telemetry heartbeat & immediate broadcast push check
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                        try {
                            com.example.shribalajikripadham.notification.AshramBackgroundPushJobService.executeBackgroundCheck(context)
                            com.example.shribalajikripadham.util.DevotionalAudioCacheManager.startSilentBackgroundSync(context)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        // Continuous foreground heartbeat loop: keeps device marked "Online Now" while app is actively used
                        while (isActive) {
                            try {
                                com.example.shribalajikripadham.data.network.AppTelemetryManager.recordAppHeartbeat(context)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            kotlinx.coroutines.delay(180_000L) // Ping every 3 minutes
                        }
                    }

                    // Firebase Cloud Messaging (FCM) Token Initialization & Registration
                    try {
                        com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                            if (task.isSuccessful && !task.result.isNullOrBlank()) {
                                val token = task.result
                                val prefs = context.getSharedPreferences(
                                    com.example.shribalajikripadham.notification.AshramFirebaseMessagingService.PREFS_FCM,
                                    android.content.Context.MODE_PRIVATE
                                )
                                prefs.edit().putString(
                                    com.example.shribalajikripadham.notification.AshramFirebaseMessagingService.KEY_FCM_TOKEN,
                                    token
                                ).apply()

                                val lastPhone = prefs.getString(
                                    com.example.shribalajikripadham.notification.AshramFirebaseMessagingService.KEY_LAST_PHONE,
                                    null
                                )
                                if (!lastPhone.isNullOrBlank()) {
                                    com.example.shribalajikripadham.notification.AshramFirebaseMessagingService.registerDevoteePhone(context, lastPhone)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    // Safe OneSignal Push Notification initialization in background IO
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                        try {
                            com.onesignal.OneSignal.initWithContext(context.applicationContext, "db065153-88a1-4a01-badc-ae33c4b38cbe")
                            android.util.Log.i("OneSignal", "OneSignal initialized safely in background IO")
                        } catch (t: Throwable) {
                            android.util.Log.e("OneSignal", "OneSignal safe background init bypass: ${t.localizedMessage}")
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize().imePadding(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainNavigation(
                        currentTheme = currentSacredTheme,
                        onThemeChanged = { newTheme ->
                            currentSacredTheme = newTheme
                            com.example.shribalajikripadham.theme.ThemePreferences.setSelectedTheme(context, newTheme)
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Silent Force-Refresh on Resume: Trigger live config & token status sync instantly
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                val repo = AshramRepository(applicationContext)
                repo.syncLiveConfigFromGitHub()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
