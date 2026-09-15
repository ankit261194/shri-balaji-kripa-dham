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


        // Create notification channel on app launch
        try {
            NotificationHelper.createNotificationChannel(this)
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


                // Request Notification Permission on Android 13+ (TIRAMISU)
                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { _ -> }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }

                    // Background telemetry heartbeat (installed devices tracking for Super Admin)
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                        com.example.shribalajikripadham.data.network.AppTelemetryManager.recordAppHeartbeat(context)
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
}
