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
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
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
                        com.example.shribalajikripadham.theme.SacredTheme.ROYAL_MAROON
                    }
                )
            }

            ShriBalajiKripaDhamTheme(sacredTheme = currentSacredTheme) {


                // Unified All-In-One Permission Request (Location + Camera + Notifications)
                val allRequiredPermissions = remember {
                    val list = mutableListOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.CAMERA
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        list.add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    list.toTypedArray()
                }

                val unifiedPermissionsLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { _ -> }

                LaunchedEffect(Unit) {
                    val pendingPermissions = allRequiredPermissions.filter { perm ->
                        ContextCompat.checkSelfPermission(context, perm) != PackageManager.PERMISSION_GRANTED
                    }
                    if (pendingPermissions.isNotEmpty()) {
                        unifiedPermissionsLauncher.launch(pendingPermissions.toTypedArray())
                    }

                    // Background telemetry heartbeat & immediate broadcast push check
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                        try {
                            com.example.shribalajikripadham.data.network.AppTelemetryManager.recordAppHeartbeat(context)
                            com.example.shribalajikripadham.notification.AshramBackgroundPushJobService.executeBackgroundCheck(context)
                            // 📿 Silent, hidden background devotional audio pre-cache (0ms buffer + self-healing)
                            com.example.shribalajikripadham.util.DevotionalAudioCacheManager.startSilentBackgroundSync(context)
                        } catch (e: Exception) {
                            e.printStackTrace()
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
