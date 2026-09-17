package com.example.shribalajikripadham.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.shribalajikripadham.data.local.DatabaseHelper
import com.example.shribalajikripadham.ai.FaceEmbeddingEngine
import com.example.shribalajikripadham.data.model.*
import com.example.shribalajikripadham.data.network.GitHubLiveSyncManager
import com.example.shribalajikripadham.data.repository.AshramRepository
import com.example.shribalajikripadham.data.repository.AdminPermissionsUpdate
import com.example.shribalajikripadham.hardware.GeofenceLocationManager
import com.example.shribalajikripadham.theme.*
import com.example.shribalajikripadham.ui.common.SacredAvatar
import com.example.shribalajikripadham.ui.home.AppUiLayout
import com.example.shribalajikripadham.util.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.example.shribalajikripadham.util.DevoteePhotoHelper
import com.example.shribalajikripadham.util.TakeAnyPicturePreview
import com.example.shribalajikripadham.util.TakeRearPicturePreview
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction

import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class CreatedSevadarShareData(
    val name: String,
    val username: String,
    val phone: String,
    val password: String,
    val pin: String,
    val permissions: List<String>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    isHindi: Boolean,
    onBack: () -> Unit,
    onNavigateToHallDisplay: () -> Unit = {}
) {
    val context = LocalContext.current
    val repository = remember { AshramRepository(context) }
    val scope = rememberCoroutineScope()

    var loggedInAdmin by remember { mutableStateOf<Admin?>(null) }
    var currentSessionId by remember { mutableStateOf("") }
    var myLoginTimestamp by remember { mutableLongStateOf(0L) }
    var forceLogoutMessage by remember { mutableStateOf<String?>(null) }
    var showLogoutExitDialog by remember { mutableStateOf(false) }

    // Intercept back button when admin is logged in: must confirm logout before exiting
    BackHandler(enabled = loggedInAdmin != null) {
        showLogoutExitDialog = true
    }

    // Sevadar photo states (must be declared before activity result launchers)
    var editingAdmin by remember { mutableStateOf<Admin?>(null) }
    var newSevPhotoUri by remember { mutableStateOf("") }
    var editSevPhotoUri by remember { mutableStateOf("") }

    val newSevCameraLauncher = rememberLauncherForActivityResult(
        contract = TakeAnyPicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            val savedPath = DevoteePhotoHelper.saveDevoteePhoto(context, bitmap, "sevadar")
            if (savedPath.isNotBlank()) {
                newSevPhotoUri = savedPath
                Toast.makeText(context, if (isHindi) "📸 सेवादार फोटो सेट, क्लाउड सिंक जारी..." else "Photo set, syncing to cloud...", Toast.LENGTH_SHORT).show()
                scope.launch(Dispatchers.IO) {
                    val safeName = "sevadar_" + System.currentTimeMillis() + ".jpg"
                    val cloudUrl = com.example.shribalajikripadham.data.network.GitHubLiveSyncManager.uploadPhotoToGitHub(context, savedPath, safeName)
                    if (!cloudUrl.isNullOrBlank()) {
                        withContext(Dispatchers.Main) {
                            newSevPhotoUri = cloudUrl
                        }
                    }
                }
            }
        }
    }

    val newSevGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val bmp = DevoteePhotoHelper.loadBitmap(context, uri.toString())
            if (bmp != null) {
                val savedPath = DevoteePhotoHelper.saveDevoteePhoto(context, bmp, "sevadar")
                if (savedPath.isNotBlank()) {
                    newSevPhotoUri = savedPath
                    Toast.makeText(context, if (isHindi) "📁 गैलरी से फोटो चुनी गई, क्लाउड सिंक जारी..." else "Photo selected, syncing to cloud...", Toast.LENGTH_SHORT).show()
                    scope.launch(Dispatchers.IO) {
                        val safeName = "sevadar_" + System.currentTimeMillis() + ".jpg"
                        val cloudUrl = com.example.shribalajikripadham.data.network.GitHubLiveSyncManager.uploadPhotoToGitHub(context, savedPath, safeName)
                        if (!cloudUrl.isNullOrBlank()) {
                            withContext(Dispatchers.Main) {
                                newSevPhotoUri = cloudUrl
                            }
                        }
                    }
                }
            }
        }
    }

    val editSevCameraLauncher = rememberLauncherForActivityResult(
        contract = TakeAnyPicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            val savedPath = DevoteePhotoHelper.saveDevoteePhoto(context, bitmap, "sevadar")
            if (savedPath.isNotBlank()) {
                editSevPhotoUri = savedPath
                Toast.makeText(context, if (isHindi) "📸 सेवादार फोटो सेट, क्लाउड सिंक जारी..." else "Photo set, syncing to cloud...", Toast.LENGTH_SHORT).show()
                scope.launch(Dispatchers.IO) {
                    val safeName = "sevadar_" + (editingAdmin?.username ?: System.currentTimeMillis().toString()) + ".jpg"
                    val cloudUrl = com.example.shribalajikripadham.data.network.GitHubLiveSyncManager.uploadPhotoToGitHub(context, savedPath, safeName)
                    if (!cloudUrl.isNullOrBlank()) {
                        withContext(Dispatchers.Main) {
                            editSevPhotoUri = cloudUrl
                        }
                    }
                }
            }
        }
    }

    val editSevGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val bmp = DevoteePhotoHelper.loadBitmap(context, uri.toString())
            if (bmp != null) {
                val savedPath = DevoteePhotoHelper.saveDevoteePhoto(context, bmp, "sevadar")
                if (savedPath.isNotBlank()) {
                    editSevPhotoUri = savedPath
                    Toast.makeText(context, if (isHindi) "📁 गैलरी से फोटो चुनी गई, क्लाउड सिंक जारी..." else "Photo selected, syncing to cloud...", Toast.LENGTH_SHORT).show()
                    scope.launch(Dispatchers.IO) {
                        val safeName = "sevadar_" + (editingAdmin?.username ?: System.currentTimeMillis().toString()) + ".jpg"
                        val cloudUrl = com.example.shribalajikripadham.data.network.GitHubLiveSyncManager.uploadPhotoToGitHub(context, savedPath, safeName)
                        if (!cloudUrl.isNullOrBlank()) {
                            withContext(Dispatchers.Main) {
                                editSevPhotoUri = cloudUrl
                            }
                        }
                    }
                }
            }
        }
    }

    var loginWithCreds by remember { mutableStateOf(true) }
    var usernameInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var pinInput by remember { mutableStateOf("") }
    var loginError by remember { mutableStateOf<String?>(null) }

    var settings by remember { mutableStateOf(AshramSettings()) }
    var todayTokens by remember { mutableStateOf<List<Token>>(emptyList()) }
    var adminsList by remember { mutableStateOf<List<Admin>>(emptyList()) }
    var eventsList by remember { mutableStateOf<List<AshramEvent>>(emptyList()) }
    var notificationsList by remember { mutableStateOf<List<AppNotification>>(emptyList()) }

    var selectedTab by remember { mutableIntStateOf(0) }

    // Forms
    var manualName by remember { mutableStateOf("") }
    var manualPhone by remember { mutableStateOf("") }
    var manualSuccessMsg by remember { mutableStateOf<String?>(null) }

    var latInput by remember { mutableStateOf("") }
    var longInput by remember { mutableStateOf("") }
    var radiusInput by remember { mutableStateOf("") }
    var geofenceEnforced by remember { mutableStateOf(true) }
    var isOutstationAllowed by remember { mutableStateOf(true) }
    var outstationKmInput by remember { mutableStateOf("30") }
    var locationSuccessMsg by remember { mutableStateOf<String?>(null) }
    var locationErrorMsg by remember { mutableStateOf<String?>(null) }

    // Sevadar Creation Dialog
    var showCreateSevadarDialog by remember { mutableStateOf(false) }
    var newSevName by remember { mutableStateOf("") }
    var newSevUsername by remember { mutableStateOf("") }
    var newSevPhone by remember { mutableStateOf("") }
    var newSevPassword by remember { mutableStateOf("") }
    var newSevPin by remember { mutableStateOf("") }
    var newSevCanTokens by remember { mutableStateOf(true) }
    var newSevCanManualTokens by remember { mutableStateOf(true) }
    var newSevCanYatra by remember { mutableStateOf(true) }
    var newSevCanExpenses by remember { mutableStateOf(true) }
    var newSevCanLocation by remember { mutableStateOf(false) }
    var newSevCanNotif by remember { mutableStateOf(false) }
    var newSevCanContent by remember { mutableStateOf(false) }
    var newSevCanPhotos by remember { mutableStateOf(false) }
    var newSevCanArzi by remember { mutableStateOf(false) }
    var createSevErrorMsg by remember { mutableStateOf<String?>(null) }

    // App Customizer
    var customAshramName by remember { mutableStateOf("") }
    var customGurujiName by remember { mutableStateOf("") }
    var customGurujiPhotoUri by remember { mutableStateOf("") }
    var customAddress by remember { mutableStateOf("") }
    var customPhone by remember { mutableStateOf("") }
    var customTimings by remember { mutableStateOf("") }
    var customDisclaimer by remember { mutableStateOf("") }
    var customEmergencyNotice by remember { mutableStateOf("") }
    var customWhatsappGroup by remember { mutableStateOf("") }
    var customYoutubeChannel by remember { mutableStateOf("") }
    var customFacebookPage by remember { mutableStateOf("") }
    var customInstagramPage by remember { mutableStateOf("") }
    var customAppShareUrl by remember { mutableStateOf("") }
    var customSundayTokenBannerTitle by remember { mutableStateOf("") }
    var customSundayTokenBannerText by remember { mutableStateOf("") }
    var customSundayTokenCustomNotice by remember { mutableStateOf("") }
    var telemetryTotalDevices by remember { mutableIntStateOf(0) }
    var telemetryActiveToday by remember { mutableIntStateOf(0) }
    var customizerSuccessMsg by remember { mutableStateOf<String?>(null) }

    // Service Toggles & Master Visibility
    var svcTokenEnabled by remember { mutableStateOf(true) }
    var svcYatraEnabled by remember { mutableStateOf(false) }
    var svcLiveCounterVisible by remember { mutableStateOf(true) }
    var svcEventsVisible by remember { mutableStateOf(true) }
    var svcAartiTimingsVisible by remember { mutableStateOf(true) }
    var svcGurujiInfoVisible by remember { mutableStateOf(true) }
    var svcEmergencyNoticeVisible by remember { mutableStateOf(true) }
    var svcScheduledTimestamp by remember { mutableLongStateOf(0L) }
    var customScheduledDateStr by remember { mutableStateOf("") }
    var svcBusBookingLive by remember { mutableStateOf(false) }
    var svcBusFareAmount by remember { mutableStateOf("1500") }
    var svcPaymentFeatureLive by remember { mutableStateOf(false) }
    var svcCanAdminViewPayments by remember { mutableStateOf(false) }
    var svcCanDevoteeViewPayments by remember { mutableStateOf(false) }
    var svcUpiId by remember { mutableStateOf("shribalajikripadham@upi") }
    var svcUpiName by remember { mutableStateOf("Shri Balaji Kripa Dham") }
    var svcUpiQrUri by remember { mutableStateOf("") }
    var svcArziLedgerLive by remember { mutableStateOf(false) }
    var svcBadiArziRate by remember { mutableStateOf("100") }
    var svcChhotiArziRate by remember { mutableStateOf("50") }
    var svcCanAdminViewArzi by remember { mutableStateOf(false) }
    var svcCanDevoteeViewArzi by remember { mutableStateOf(false) }
    var svcCanDevoteeViewYatraDiary by remember { mutableStateOf(false) }
    var customParichayHindi by remember { mutableStateOf("") }
    var customParichayEnglish by remember { mutableStateOf("") }
    var customHistoryHindi by remember { mutableStateOf("") }
    var customRulesHindi by remember { mutableStateOf("") }
    var svcSuccessMsg by remember { mutableStateOf<String?>(null) }

    // Notification broadcast
    var notifTitle by remember { mutableStateOf("") }
    var notifMsg by remember { mutableStateOf("") }
    var notifPriority by remember { mutableStateOf("HIGH") }
    var notifSuccessMsg by remember { mutableStateOf<String?>(null) }

    // Auto update manager
    var updVersionCode by remember { mutableStateOf("1") }
    var updVersionName by remember { mutableStateOf("1.0") }
    var updNotes by remember { mutableStateOf("") }
    var updApkUrl by remember { mutableStateOf("") }
    var updIsForce by remember { mutableStateOf(false) }
    var updSuccessMsg by remember { mutableStateOf<String?>(null) }

    // Add Event Dialog
    var showAddEventDialog by remember { mutableStateOf(false) }
    var evTitleHi by remember { mutableStateOf("") }
    var evTitleEn by remember { mutableStateOf("") }
    var evDateHi by remember { mutableStateOf("") }
    var evDateEn by remember { mutableStateOf("") }
    var evDescHi by remember { mutableStateOf("") }
    var evDescEn by remember { mutableStateOf("") }

    // Edit Sevadar Dialog (Super Admin Control)
    var editSevName by remember { mutableStateOf("") }
    var editSevUsername by remember { mutableStateOf("") }
    var editSevPhone by remember { mutableStateOf("") }
    var editSevPassword by remember { mutableStateOf("") }
    var editSevPin by remember { mutableStateOf("") }
    var editSevErrorMsg by remember { mutableStateOf<String?>(null) }
    var superAdminAccount by remember { mutableStateOf<Admin?>(null) }
    var editSevCanTokens by remember { mutableStateOf(false) }
    var editSevCanManualTokens by remember { mutableStateOf(false) }
    var editSevCanYatra by remember { mutableStateOf(false) }
    var editSevCanExpenses by remember { mutableStateOf(false) }
    var editSevCanLocation by remember { mutableStateOf(false) }
    var editSevCanNotif by remember { mutableStateOf(false) }
    var editSevCanContent by remember { mutableStateOf(false) }
    var editSevCanPhotos by remember { mutableStateOf(false) }
    var editSevCanAnywhere by remember { mutableStateOf(false) }
    var newSevCanAnywhere by remember { mutableStateOf(false) }
    var newSevCanScanRegister by remember { mutableStateOf(false) }
    var editSevCanScanRegister by remember { mutableStateOf(false) }
    var newSevCanParchas by remember { mutableStateOf(false) }
    var createdSevadarShareData by remember { mutableStateOf<CreatedSevadarShareData?>(null) }
    var sevadarToShareViaWhatsApp by remember { mutableStateOf<Admin?>(null) }
    var customSharePassword by remember { mutableStateOf("") }
    var customSharePin by remember { mutableStateOf("") }
    var isSharingBanner by remember { mutableStateOf(false) }
    var editSevCanParchas by remember { mutableStateOf(false) }
    var newSevCanCancelTokens by remember { mutableStateOf(false) }
    var newSevCanDeleteTokens by remember { mutableStateOf(false) }
    var newSevCanCustomTokenNumber by remember { mutableStateOf(false) }
    var newSevCanExportPdf by remember { mutableStateOf(true) }
    var editSevCanCancelTokens by remember { mutableStateOf(false) }
    var editSevCanDeleteTokens by remember { mutableStateOf(false) }
    var editSevCanCustomTokenNumber by remember { mutableStateOf(false) }
    var editSevCanExportPdf by remember { mutableStateOf(true) }
    var editSevCanArzi by remember { mutableStateOf(false) }
    var customDistancesList by remember { mutableStateOf<List<CustomCityDistance>>(emptyList()) }
    var uiSectionsList by remember { mutableStateOf<List<UiSectionConfig>>(emptyList()) }

    fun refreshData() {
        scope.launch {
            val s = repository.getSettings()
            settings = s
            latInput = s.latitude.toString()
            longInput = s.longitude.toString()
            radiusInput = if (s.allowedRadiusMeters % 1.0 == 0.0) s.allowedRadiusMeters.toInt().toString() else s.allowedRadiusMeters.toString()
            geofenceEnforced = s.isGeofenceEnforced
            isOutstationAllowed = s.isOutstationAdvanceAllowed
            outstationKmInput = if (s.outstationMinDistanceKm % 1.0 == 0.0) s.outstationMinDistanceKm.toInt().toString() else s.outstationMinDistanceKm.toString()

            customAshramName = s.ashramName
            customGurujiName = s.gurujiName
            customGurujiPhotoUri = s.gurujiPhotoUri
            customAddress = s.address
            customPhone = s.contactPhone
            customTimings = s.darbarTimings
            customDisclaimer = s.freeDisclaimer
            customEmergencyNotice = s.emergencyNoticeText
            customWhatsappGroup = s.whatsappGroupUrl
            customYoutubeChannel = s.youtubeChannelUrl
            customFacebookPage = s.facebookPageUrl
            customInstagramPage = s.instagramUrl
            customAppShareUrl = s.appShareUrl
            customSundayTokenBannerTitle = s.sundayTokenBannerTitle
            customSundayTokenBannerText = s.sundayTokenBannerText
            customSundayTokenCustomNotice = s.sundayTokenCustomNotice

            try {
                val telemetry = repository.getActiveDevicesTelemetry()
                telemetryTotalDevices = telemetry.first
                telemetryActiveToday = telemetry.second
            } catch (e: Exception) {
                e.printStackTrace()
            }

            svcTokenEnabled = s.isTokenServiceEnabled
            svcYatraEnabled = s.isYatraServiceEnabled
            svcLiveCounterVisible = s.isLiveCounterVisible
            svcEventsVisible = s.isEventsVisible
            svcAartiTimingsVisible = s.isAartiTimingsVisible
            svcGurujiInfoVisible = s.isGurujiInfoVisible
            svcEmergencyNoticeVisible = s.isEmergencyNoticeVisible
            svcScheduledTimestamp = s.scheduledTokenOpenTimestamp
            if (s.scheduledTokenOpenTimestamp > 0L) {
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                customScheduledDateStr = sdf.format(java.util.Date(s.scheduledTokenOpenTimestamp))
            } else {
                customScheduledDateStr = ""
            }

            svcBusBookingLive = s.isBusBookingLive
            svcBusFareAmount = s.busSeatFareAmount.toString()
            svcPaymentFeatureLive = s.isPaymentFeatureLive
            svcCanAdminViewPayments = s.canAdminViewPaymentHistory
            svcCanDevoteeViewPayments = s.canDevoteeViewPaymentHistory
            svcUpiId = s.ashramUpiId
            svcUpiName = s.ashramUpiName
            svcUpiQrUri = s.customUpiQrUri
            svcArziLedgerLive = s.isArziLedgerLive
            svcBadiArziRate = if (s.badiArziRate % 1.0 == 0.0) s.badiArziRate.toInt().toString() else s.badiArziRate.toString()
            svcChhotiArziRate = if (s.chhotiArziRate % 1.0 == 0.0) s.chhotiArziRate.toInt().toString() else s.chhotiArziRate.toString()
            svcCanAdminViewArzi = s.canAdminViewArziLedger
            svcCanDevoteeViewArzi = s.canDevoteeViewArziLedger
            svcCanDevoteeViewYatraDiary = s.canDevoteeViewYatraDiary
            customParichayHindi = s.ashramParichayHindi
            customParichayEnglish = s.ashramParichayEnglish
            customHistoryHindi = s.ashramHistoryHindi
            customRulesHindi = s.ashramRulesHindi

            updVersionCode = s.latestVersionCode.toString()
            updVersionName = s.latestVersionName
            updNotes = s.updateNotes
            updApkUrl = s.apkDownloadUrl
            updIsForce = s.isForceUpdate

            todayTokens = repository.getAllTokensToday()
            adminsList = repository.getAllAdmins()
            superAdminAccount = repository.getSuperAdmin()
            eventsList = repository.getAllEvents()
            notificationsList = repository.getAllNotifications()
            customDistancesList = repository.getAllCustomCityDistances()
            uiSectionsList = repository.getUiSectionConfigs()
        }
    }

    LaunchedEffect(Unit) {
        refreshData()
        scope.launch {
            try {
                repository.syncAdminsFromGitHub()
                repository.syncLiveConfigFromGitHub()
                refreshData()
            } catch (e: Exception) {}
        }
    }

    LaunchedEffect(loggedInAdmin) {
        if (loggedInAdmin != null) {
            refreshData()
            try {
                repository.syncFullHostingerToLocal()
                repository.syncLiveConfigFromGitHub()
                repository.syncLiveTokensFromCloud()
                repository.syncAdminsFromGitHub()
                refreshData()
            } catch (e: Exception) {}

            // ⚡ REAL-TIME ADMIN AUTO-SYNC (Every 4 seconds):
            // All tokens booked by devotees, bills/expenses, and payments update LIVE without clicking refresh!
            scope.launch {
                while (true) {
                    delay(4000)
                    try {
                        val (tokCount, expCount, payCount) = repository.syncFullHostingerToLocal()
                        if (tokCount > 0 || expCount > 0 || payCount > 0) {
                            refreshData()
                        }
                    } catch (e: Exception) {}
                }
            }

            // Record admin device telemetry heartbeat
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                try {
                    com.example.shribalajikripadham.data.network.AppTelemetryManager.recordAppHeartbeat(
                        context = context,
                        devoteeName = loggedInAdmin!!.name,
                        devoteePhone = loggedInAdmin!!.phoneNumber,
                        role = "ADMIN"
                    )
                } catch (e: Exception) {}
            }

            val currentDevId = com.example.shribalajikripadham.hardware.DeviceFingerprintManager.getDeviceId(context)
            val adminId = loggedInAdmin!!.id.toString()

            // Auto-refresh token queue and monitor single-device session from cloud
            while (isActive) {
                delay(12000)
                try {
                    // Check if another phone logged in with this admin account
                    if (currentSessionId.isNotBlank() && myLoginTimestamp > 0L) {
                        val (isSessValid, errDetail) = repository.checkAdminSessionActive(
                            adminId = adminId,
                            currentSessionId = currentSessionId,
                            currentDeviceId = currentDevId,
                            myLoginTimestamp = myLoginTimestamp
                        )
                        if (!isSessValid) {
                            forceLogoutMessage = if (isHindi)
                                "⚠️ आपका एडमिन खाता किसी अन्य फोन पर लॉगिन किया गया है!\n\nसुरक्षा नियमों के अनुसार एक समय पर केवल एक ही फोन में एडमिन लॉगिन की अनुमति है। यह पुराना सत्र स्वतः समाप्त कर दिया गया है।"
                            else
                                "⚠️ Your admin account was logged into from another device!\n\nOnly one device can be logged in at a time. This older session has been terminated."
                            loggedInAdmin = null
                            currentSessionId = ""
                            myLoginTimestamp = 0L
                            break
                        }
                    }

                    val (hasNew, count) = repository.syncLiveTokensFromCloud()
                    if (hasNew && count > 0) {
                        todayTokens = repository.getAllTokensToday()
                    }
                } catch (e: Exception) {}
            }
        }
    }

    // Logout Confirmation Dialog on Back Press or Logout Button
    if (showLogoutExitDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutExitDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🔒", fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isHindi) "लॉगआउट व बाहर निकलें" else "Confirm Logout & Exit",
                        fontWeight = FontWeight.Bold,
                        color = MaroonPrimary
                    )
                }
            },
            text = {
                Text(
                    text = if (isHindi)
                        "सुरक्षा चेतावनी: क्या आप एडमिन पैनल से लॉगआउट करके बाहर निकलना चाहते हैं?\n\nएडमिन पैनल छोड़ने के लिए लॉगआउट होना अनिवार्य है ताकि आपका सत्र सुरक्षित रहे।"
                    else
                        "Security Warning: Do you want to log out and exit the Admin Panel?\n\nLogging out is required to protect your session.",
                    fontSize = 14.sp,
                    color = Color.DarkGray
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutExitDialog = false
                        val adminToLogout = loggedInAdmin
                        val sessId = currentSessionId
                        loggedInAdmin = null
                        currentSessionId = ""
                        myLoginTimestamp = 0L
                        usernameInput = ""
                        passwordInput = ""
                        pinInput = ""
                        scope.launch {
                            if (adminToLogout != null) {
                                repository.clearAdminSession(adminToLogout.id.toString(), sessId)
                            }
                        }
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary)
                ) {
                    Text(if (isHindi) "लॉगआउट करें व निकलें" else "Logout & Exit", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showLogoutExitDialog = false }) {
                    Text(if (isHindi) "रहें (रद्द करें)" else "Stay / Cancel")
                }
            }
        )
    }

    // Forced Logout Notification Dialog
    if (forceLogoutMessage != null) {
        AlertDialog(
            onDismissRequest = { forceLogoutMessage = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⚠️", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isHindi) "सुरक्षा सूचना: सत्र समाप्त" else "Session Terminated",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD32F2F)
                    )
                }
            },
            text = {
                Text(
                    text = forceLogoutMessage!!,
                    fontSize = 14.sp,
                    color = Color.Black
                )
            },
            confirmButton = {
                Button(
                    onClick = { forceLogoutMessage = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary)
                ) {
                    Text(if (isHindi) "समझ गया (लॉगिन करें)" else "OK (Login Again)", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (loggedInAdmin == null)
                            if (isHindi) "व्यवस्थापक प्रवेश" else "Admin Authentication"
                        else
                            if (isHindi) "नियंत्रण कक्ष: ${loggedInAdmin?.name}" else "Control Panel: ${loggedInAdmin?.name}",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 17.sp
                    )
                },
                navigationIcon = {
                    TextButton(onClick = {
                        if (loggedInAdmin != null) {
                            showLogoutExitDialog = true
                        } else {
                            onBack()
                        }
                    }) {
                        Text(if (isHindi) "← वापस" else "← Back", color = SaffronLight, fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    if (loggedInAdmin != null) {
                        IconButton(onClick = {
                            val file = com.example.shribalajikripadham.util.AshramManualPdfGenerator.generateAdminGuidePdf(context)
                            if (file != null) {
                                com.example.shribalajikripadham.util.AshramManualPdfGenerator.openOrSharePdf(
                                    context,
                                    file,
                                    if (isHindi) "श्री बालाजी कृपा धाम - व्यवस्थापक मार्गदर्शिका" else "Shri Balaji Kripa Dham - Admin Manual"
                                )
                            } else {
                                Toast.makeText(context, if (isHindi) "PDF तैयार करने में असमर्थ" else "Failed to generate PDF", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Text("📖", fontSize = 18.sp)
                        }
                        TextButton(onClick = {
                            showLogoutExitDialog = true
                        }) {
                            Text(if (isHindi) "लॉगआउट" else "Logout", color = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaroonPrimary)
            )
        }
    ) { padding ->
        if (loggedInAdmin == null) {
            // --- AUTHENTICATION SCREEN ---
            var selectedLoginPortal by remember { mutableStateOf(AdminRole.SUPER_ADMIN) }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaroonSurface)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Portal Selector: Super Admin Box vs Sevadar Box
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E293B))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = {
                            selectedLoginPortal = AdminRole.SUPER_ADMIN
                            loginError = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedLoginPortal == AdminRole.SUPER_ADMIN) AmberGold else Color.Transparent,
                            contentColor = if (selectedLoginPortal == AdminRole.SUPER_ADMIN) MaroonAccent else Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (isHindi) "👑 मुख्य व्यवस्थापक (अंकित चौधरी)" else "👑 Super Admin (Ankit Chaudhary)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    Button(
                        onClick = {
                            selectedLoginPortal = AdminRole.SEVADAR
                            loginError = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedLoginPortal == AdminRole.SEVADAR) SaffronPrimary else Color.Transparent,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (isHindi) "🙏 सेवादार स्टाफ" else "🙏 Sevadar Staff",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // DEDICATED BOX BASED ON SELECTION
                if (selectedLoginPortal == AdminRole.SUPER_ADMIN) {
                    // ==========================================
                    // 1. DEDICATED SUPER ADMIN BOX (Royal Gold/Maroon)
                    // ==========================================
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(2.dp, AmberGold),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .background(Brush.linearGradient(listOf(MaroonAccent, MaroonPrimary)))
                                    .border(2.dp, AmberGold, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("👑", fontSize = 34.sp)
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (isHindi) "मुख्य व्यवस्थापक (अंकित चौधरी) प्रवेश" else "Super Admin (Ankit Chaudhary) Portal",
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.sp,
                                color = MaroonAccent
                            )
                            Text(
                                text = if (isHindi) "परम पूज्य गुरुजी / मुख्य ट्रस्टी अधिकार क्षेत्र" else "Supreme Authority & System Control",
                                fontSize = 12.sp,
                                color = TextSecondaryDark
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Password-only field for Super Admin
                            OutlinedTextField(
                                value = passwordInput,
                                onValueChange = { passwordInput = it },
                                label = { Text(if (isHindi) "सुपर एडमिन पासवर्ड दर्ज करें" else "Enter Super Admin Password", color = Color(0xFF333333)) },
                                leadingIcon = { Text("🔑") },
                                textStyle = androidx.compose.ui.text.TextStyle(color = Color(0xFF111111), fontSize = 15.sp),
                                colors = sacredOutlinedTextFieldColors(),
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )

                            if (loginError != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    color = Color(0xFFFFEBEE),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = loginError!!,
                                        color = Color.Red,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = {
                                    scope.launch {
                                        loginError = null
                                        val pass = passwordInput.trim()
                                        if (pass.isBlank()) {
                                            loginError = if (isHindi) "कृपया सुपर एडमिन पासवर्ड दर्ज करें" else "Please enter Super Admin password"
                                            return@launch
                                        }
                                        var admin = repository.authenticateSuperAdminByPasswordOnly(pass)
                                        if (admin == null) {
                                            try {
                                                repository.syncAdminsFromGitHub()
                                            } catch (e: Exception) {}
                                            admin = repository.authenticateSuperAdminByPasswordOnly(pass)
                                        }
                                        if (admin != null) {
                                            val loginTime = System.currentTimeMillis()
                                            myLoginTimestamp = loginTime
                                            val devId = com.example.shribalajikripadham.hardware.DeviceFingerprintManager.getDeviceId(context)
                                            val devModel = com.example.shribalajikripadham.data.network.AppTelemetryManager.getDeviceModelName()
                                            val sessResult = repository.registerAdminSession(
                                                adminId = admin.id.toString(),
                                                role = admin.role.name,
                                                deviceId = devId,
                                                deviceModel = devModel
                                            )
                                            currentSessionId = sessResult.second
                                            loggedInAdmin = admin
                                        } else {
                                            loginError = if (isHindi) "गलत पासवर्ड! कृपया सही सुपर एडमिन पासवर्ड दर्ज करें।" else "Incorrect password! Please enter the valid Super Admin password."
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaroonAccent)
                            ) {
                                Text(
                                    text = if (isHindi) "👑 सुपर एडमिन (अंकित चौधरी) लॉगिन" else "👑 Login as Super Admin (Ankit Chaudhary)",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AmberGold
                                )
                            }
                        }
                    }
                } else {
                    // ==========================================
                    // 2. DEDICATED SEVADAR BOX (Saffron/Green)
                    // ==========================================
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(2.dp, SaffronPrimary),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .background(SaffronPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🙏", fontSize = 34.sp)
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (isHindi) "सेवादार एवं काउंटर स्टाफ प्रवेश" else "Sevadar & Staff Portal",
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.sp,
                                color = MaroonPrimary
                            )
                            Text(
                                text = if (isHindi) "टोकन काउंटर, दर्शन व्यवस्था एवं बस यात्री प्रबंधन" else "Counter Operations & Devotee Management",
                                fontSize = 12.sp,
                                color = TextSecondaryDark
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Toggle Mode (Username/Password vs Quick PIN)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFF0F0F0))
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Button(
                                    onClick = { loginWithCreds = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (loginWithCreds) SaffronPrimary else Color.Transparent,
                                        contentColor = if (loginWithCreds) Color.White else Color.DarkGray
                                    ),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(if (isHindi) "यूजरनेम / पासवर्ड" else "Username & Pass", fontSize = 12.sp)
                                }
                                Button(
                                    onClick = { loginWithCreds = false },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (!loginWithCreds) SaffronPrimary else Color.Transparent,
                                        contentColor = if (!loginWithCreds) Color.White else Color.DarkGray
                                    ),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(if (isHindi) "सेवादार पिन (PIN)" else "Quick PIN", fontSize = 12.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            if (loginWithCreds) {
                                OutlinedTextField(
                                    value = usernameInput,
                                    onValueChange = { usernameInput = it },
                                    label = { Text(if (isHindi) "सेवादार यूजरनेम" else "Sevadar Username", color = Color(0xFF333333)) },
                                    leadingIcon = { Text("👤") },
                                    textStyle = androidx.compose.ui.text.TextStyle(color = Color(0xFF111111), fontSize = 15.sp),
                                    colors = sacredOutlinedTextFieldColors(),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = passwordInput,
                                    onValueChange = { passwordInput = it },
                                    label = { Text(if (isHindi) "पासवर्ड" else "Password", color = Color(0xFF333333)) },
                                    leadingIcon = { Text("🔑") },
                                    textStyle = androidx.compose.ui.text.TextStyle(color = Color(0xFF111111), fontSize = 15.sp),
                                    colors = sacredOutlinedTextFieldColors(),
                                    visualTransformation = PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                            } else {
                                OutlinedTextField(
                                    value = pinInput,
                                    onValueChange = { if (it.length <= 6) pinInput = it },
                                    label = { Text(if (isHindi) "4-अंकीय सेवादार पिन दर्ज करें" else "Enter 4-Digit Sevadar PIN", color = Color(0xFF333333)) },
                                    leadingIcon = { Text("🔢") },
                                    textStyle = androidx.compose.ui.text.TextStyle(color = Color(0xFF111111), fontSize = 15.sp),
                                    colors = sacredOutlinedTextFieldColors(),
                                    visualTransformation = PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }

                            if (loginError != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    color = Color(0xFFFFEBEE),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = loginError!!,
                                        color = Color.Red,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = {
                                    scope.launch {
                                        loginError = null
                                        var admin = if (loginWithCreds) {
                                            if (usernameInput.isBlank() || passwordInput.isBlank()) {
                                                loginError = if (isHindi) "कृपया यूजरनेम व पासवर्ड दोनों भरें" else "Please enter both username and password"
                                                return@launch
                                            }
                                            repository.authenticateAdminByCredentials(usernameInput, passwordInput)
                                        } else {
                                            if (pinInput.isBlank()) {
                                                loginError = if (isHindi) "कृपया पिन दर्ज करें" else "Please enter PIN"
                                                return@launch
                                            }
                                            repository.authenticateAdmin(pinInput)
                                        }

                                        if (admin == null) {
                                            try {
                                                repository.syncAdminsFromGitHub()
                                            } catch (e: Exception) {}
                                            admin = if (loginWithCreds) {
                                                repository.authenticateAdminByCredentials(usernameInput, passwordInput)
                                            } else {
                                                repository.authenticateAdmin(pinInput)
                                            }
                                        }

                                        if (admin != null) {
                                            val loginTime = System.currentTimeMillis()
                                            myLoginTimestamp = loginTime
                                            val devId = com.example.shribalajikripadham.hardware.DeviceFingerprintManager.getDeviceId(context)
                                            val devModel = com.example.shribalajikripadham.data.network.AppTelemetryManager.getDeviceModelName()
                                            val sessResult = repository.registerAdminSession(
                                                adminId = admin.id.toString(),
                                                role = admin.role.name,
                                                deviceId = devId,
                                                deviceModel = devModel
                                            )
                                            currentSessionId = sessResult.second
                                            loggedInAdmin = admin
                                        } else {
                                            loginError = if (isHindi) "गलत क्रेडेंशियल्स अथवा सेवादार खाता निष्क्रिय है!" else "Invalid credentials or account is inactive!"
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary)
                            ) {
                                Text(
                                    text = if (isHindi) "🙏 सेवादार लॉगिन" else "🙏 Login as Sevadar",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // --- LOGGED IN DASHBOARD ---
            val admin = loggedInAdmin!!
            val isSuper = admin.role == AdminRole.SUPER_ADMIN

            // Build allowed tabs based on permissions
            val allowedTabs = mutableListOf<String>()
            if (admin.canManageTokens) allowedTabs.add(if (isHindi) "टोकन कतार" else "Tokens")
            if (admin.canIssueManualTokens) allowedTabs.add(if (isHindi) "मैनुअल टोकन" else "Manual")
            if (admin.canScanPaperRegister || isSuper) allowedTabs.add(if (isHindi) "रजिस्टर स्कैन" else "Register Scan")
            if (isSuper || admin.canManageParchas) {
                allowedTabs.add(if (isHindi) "आश्रम पर्चे" else "Sacred Parchas")
            }
            if (isSuper) allowedTabs.add(if (isHindi) "सक्रिय फोन" else "Active Devices")
            if (admin.canChangeLocation || isSuper) allowedTabs.add(if (isHindi) "GPS लोकेशन" else "Location")
            if (admin.canSendNotifications || isSuper) allowedTabs.add(if (isHindi) "सूचना भेजें" else "Broadcast")
            if (isSuper || admin.canEditAshramInfo) {
                allowedTabs.add(if (isHindi) "UI बॉक्स कंट्रोल" else "UI Control")
            }
            if (isSuper || (admin.canManageYatra && settings.isBusBookingLive)) {
                allowedTabs.add(if (isHindi) "बस बुकिंग लेजर" else "Bus Ledger")
            }
            if (isSuper || settings.canAdminViewPaymentHistory) {
                allowedTabs.add(if (isHindi) "पेमेंट लेजर" else "Payment Ledger")
            }
            if (isSuper || (admin.canManageArzi && settings.canAdminViewArziLedger)) {
                allowedTabs.add(if (isHindi) "अर्जी लेजर 📦" else "Arzi Ledger 📦")
            }
            if (isSuper || (settings.canAdminViewPaymentHistory && admin.canManageExpenses)) {
                allowedTabs.add(if (isHindi) "महा-लेजर 📊" else "Master Ledger 📊")
            }
            if (isSuper) {
                allowedTabs.add(if (isHindi) "त्रिमूर्ति क्लाउड सिंक ☁️" else "Triple Cloud Sync ☁️")
            }
            if (isSuper) {
                allowedTabs.add(if (isHindi) "सेवादार खाते" else "Sevadars")
                allowedTabs.add(if (isHindi) "सेवाएं ऑन/ऑफ" else "Services")
                allowedTabs.add(if (isHindi) "🌐 वेबसाइट व CMS" else "Website & CMS")
                allowedTabs.add(if (isHindi) "ऐप कस्टमाइजर" else "Customizer")
                allowedTabs.add(if (isHindi) "कस्टम दूरियाँ" else "Distances")
                allowedTabs.add(if (isHindi) "सुपर कंट्रोल" else "Super Control")
                allowedTabs.add(if (isHindi) "🪪 ID कार्ड स्टूडियो" else "🪪 ID Card Studio")
                allowedTabs.add(if (isHindi) "ऑटो-अपडेट" else "Updates")
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color(0xFFF9F9F9))
            ) {
                // NEW REDESIGNED ADMIN PORTAL HEADER WITH ASSIGNED PERMISSIONS BADGES
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSuper) Color(0xFFFFF9EE) else Color(0xFFF1F8E9)
                    ),
                    border = BorderStroke(1.2.dp, if (isSuper) SaffronPrimary else Color(0xFF388E3C))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SacredAvatar(
                                photoUri = admin.photoUri,
                                fallbackText = admin.name,
                                size = 50.dp,
                                primaryColor = if (isSuper) SaffronPrimary else Color(0xFF2E7D32),
                                borderColor = if (isSuper) AmberGold else Color(0xFF81C784)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isSuper) (if (isHindi) "अंकित चौधरी (Super Admin) 👑" else "Ankit Chaudhary (Super Admin) 👑") else admin.name,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    color = MaroonPrimary
                                )
                                Text(
                                    text = "👤 ${admin.username.ifEmpty { "superadmin" }} | 📞 ${admin.phoneNumber}",
                                    fontSize = 12.sp,
                                    color = Color.DarkGray
                                )
                                if (isSuper) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Surface(
                                        color = Color(0xFFE8F5E9),
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, Color(0xFF81C784))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("📱 ", fontSize = 11.sp)
                                            Text(
                                                text = if (isHindi) "सक्रिय फोन: $telemetryTotalDevices (आज सक्रिय: $telemetryActiveToday)" else "Active Handsets: $telemetryTotalDevices (Today: $telemetryActiveToday)",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1B5E20)
                                            )
                                        }
                                    }
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSuper) SaffronPrimary else Color(0xFF2E7D32),
                                shadowElevation = 2.dp
                            ) {
                                Text(
                                    text = if (isSuper) "👑 SUPER ADMIN" else "🙏 SEVADAR",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = (if (isSuper) SaffronPrimary else Color(0xFF2E7D32)).copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (isHindi) "आपकी स्वीकृत सेवाएं व अनुमतियां (Assigned Access):" else "Your Assigned Permissions:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaroonAccent
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Permission Badges Matrix
                        val rbacBadges = listOf(
                            Triple("टोकन कतार", admin.canManageTokens || isSuper, "🎟️"),
                            Triple("मैनुअल टोकन", admin.canIssueManualTokens || isSuper, "✍️"),
                            Triple("VIP कोटा (2..20)", isSuper || (admin.canSetCustomTokenNumber && settings.allowAdminReservedTokens), "👑"),
                            Triple("रजिस्टर स्कैन", admin.canScanPaperRegister || isSuper, "📷"),
                            Triple("आश्रम पर्चे", admin.canManageParchas || isSuper, "📜"),
                            Triple("बालाजी यात्रा", admin.canManageYatra || isSuper, "🚌"),
                            Triple("आय-व्यय", admin.canManageExpenses || isSuper, "💰"),
                            Triple("GPS दायरा", admin.canChangeLocation || isSuper, "📍"),
                            Triple("सूचना प्रसारण", admin.canSendNotifications || isSuper, "📢"),
                            Triple("आश्रम विवरण", admin.canEditAshramInfo || isSuper, "⚙️"),
                            Triple("सेवादार नियंत्रण", isSuper, "👥"),
                            Triple("भक्त फोटो", admin.canViewDevoteePhotos || isSuper, "📸"),
                            Triple("अर्जी लेजर", isSuper || admin.canManageArzi, "📦"),
                            Triple("महा-लेजर", isSuper || (settings.canAdminViewPaymentHistory && admin.canManageExpenses), "📊"),
                            Triple("ID कार्ड स्टूडियो", isSuper, "🪪")
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(rbacBadges) { (label, isGranted, icon) ->
                                Surface(
                                    color = if (isGranted) Color(0xFFE8F5E9) else Color(0xFFEEEEEE),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isGranted) Color(0xFF4CAF50) else Color(0xFFBDBDBD)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(icon, fontSize = 11.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "$label: " + (if (isGranted) "✓" else "🔒"),
                                            fontSize = 11.sp,
                                            fontWeight = if (isGranted) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isGranted) Color(0xFF1B5E20) else Color(0xFF757575)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 📘 व्यवस्थापक एवं सेवादार कार्यप्रणाली मार्गदर्शिका (PDF) - EXCLUSIVE TO ADMIN DASHBOARD
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 3.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEDE7F6)),
                    border = BorderStroke(1.dp, Color(0xFFB39DDB))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("📘", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (isHindi) "व्यवस्थापक एवं सेवादार कार्यप्रणाली मार्गदर्शिका (PDF)" else "Admin & Sevadar Manual (PDF)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                    color = Color(0xFF4A148C)
                                )
                                Text(
                                    text = if (isHindi) "समस्त कार्यप्रणाली, 2-VIP टोकन कोटा, टीवी बोर्ड व नियम - यहाँ पढ़ें व डाउनलोड करें" else "Complete operations guide, VIP 2-token quota, TV board & rules",
                                    fontSize = 10.sp,
                                    color = Color.DarkGray
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val file = com.example.shribalajikripadham.util.AshramManualPdfGenerator.generateAdminGuidePdf(context)
                                if (file != null) {
                                    com.example.shribalajikripadham.util.AshramManualPdfGenerator.openOrSharePdf(
                                        context,
                                        file,
                                        if (isHindi) "श्री बालाजी कृपा धाम - व्यवस्थापक मार्गदर्शिका" else "Shri Balaji Kripa Dham - Admin Manual"
                                    )
                                } else {
                                    Toast.makeText(context, if (isHindi) "PDF तैयार करने में असमर्थ" else "Failed to generate PDF", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A1B9A)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isHindi) "PDF पढ़ें" else "Open PDF",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                // Scrollable Tabs
                PrimaryScrollableTabRow(
                    selectedTabIndex = selectedTab.coerceIn(0, (allowedTabs.size - 1).coerceAtLeast(0)),
                    containerColor = MaroonPrimary,
                    contentColor = Color.White,
                    edgePadding = 8.dp
                ) {
                    allowedTabs.forEachIndexed { index, tabTitle ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(tabTitle, fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
                        )
                    }
                }

                val currentTabTitle = allowedTabs.getOrNull(selectedTab) ?: ""

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                    when {
                        currentTabTitle == "टोकन कतार" || currentTabTitle == "Tokens" -> {
                            TokenQueueTab(
                                isHindi = isHindi,
                                settings = settings,
                                todayTokens = todayTokens,
                                canViewPhotos = admin.canViewDevoteePhotos || isSuper,
                                canCancelTokens = admin.canCancelTokens || isSuper,
                                canDeleteTokens = admin.canDeleteTokens || isSuper,
                                canExportPdf = admin.canExportPdf || isSuper,
                                onUpdateRunningToken = { newNum ->
                                    scope.launch {
                                        repository.updateRunningTokenNumber(newNum)
                                        refreshData()
                                    }
                                },
                                onUpdateStatus = { id, status ->
                                    scope.launch {
                                        repository.updateTokenStatus(id, status)
                                        refreshData()
                                    }
                                },
                                onToggleDarshan = { tokenId, completed ->
                                    scope.launch {
                                        repository.toggleDarshanCompleted(tokenId, completed)
                                        refreshData()
                                    }
                                },
                                onCancelToken = { tokenId ->
                                    scope.launch {
                                        repository.cancelToken(tokenId)
                                        refreshData()
                                    }
                                },
                                onDeleteToken = { tokenId ->
                                    scope.launch {
                                        repository.deleteToken(tokenId)
                                        refreshData()
                                    }
                                },
                                onSyncFromCloud = {
                                    scope.launch {
                                        val res = repository.syncLiveTokensFromCloud()
                                        refreshData()
                                        if (res.first) {
                                            Toast.makeText(context, if (isHindi) "✅ क्लाउड से ${res.second} नए भक्त टोकन सिंक हुए!" else "✅ Synced ${res.second} new tokens from cloud!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, if (isHindi) "क्लाउड सिंक: सभी टोकन पहले से अपडेट हैं" else "Cloud sync: All tokens are up to date", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                onSyncFromGoogleSheet = {
                                    scope.launch {
                                        val res = repository.syncLiveTokensFromCloud()
                                        refreshData()
                                        if (res.first) {
                                            Toast.makeText(context, if (isHindi) "✅ क्लाउड से ${res.second} नए टोकन सिंक हुए!" else "✅ Synced ${res.second} new tokens from cloud!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, if (isHindi) "क्लाउड सिंक: सभी टोकन पहले से अपडेट हैं" else "Cloud sync: All tokens are up to date", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                onUpdateVoicePreset = { newPreset ->
                                    scope.launch {
                                        repository.updateTokenVoicePreset(newPreset)
                                        refreshData()
                                    }
                                },
                                onFillReservedToken = { tokenNum, name, phone, city ->
                                    scope.launch {
                                        val isSuper = admin.role == AdminRole.SUPER_ADMIN
                                        val hasReservedPermission = isSuper || (admin.canSetCustomTokenNumber && settings.allowAdminReservedTokens)
                                        val adminAttribution = if (isSuper) "SUPER_ADMIN (अंकित चौधरी)" else "ADMIN (${admin.name})"
                                        val vipNumbers = listOf(2, 4, 6, 8, 10, 12, 14, 16, 18, 20)
                                        if (!isSuper && tokenNum in vipNumbers) {
                                            if (!hasReservedPermission) {
                                                Toast.makeText(
                                                    context,
                                                    if (isHindi) "❌ अनुमति अस्वीकृत: आपके पास विशेष आरक्षित VIP टोकन भरने का अधिकार नहीं है। केवल सुपर एडमिन द्वारा अधिकृत एडमिन ही यह टोकन भर सकते हैं।" else "❌ Permission denied: You do not have permission to fill reserved VIP tokens.",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                                return@launch
                                            }
                                            val myCount = repository.getAdminReservedTokensCountToday(adminAttribution)
                                            if (myCount >= 2) {
                                                Toast.makeText(
                                                    context,
                                                    if (isHindi) "❌ कोटा समाप्त: आप आज केवल अधिकतम 2 विशेष आरक्षित टोकन भर सकते हैं।" else "❌ Quota exceeded: Maximum 2 reserved tokens per day allowed.",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                                return@launch
                                            }
                                        }
                                        val today = DatabaseHelper.getTodayDateString()
                                        repository.insertOrUpdateCentralToken(
                                            tokenNumber = tokenNum,
                                            darbarDate = today,
                                            patientName = name,
                                            phoneNumber = phone,
                                            city = city.ifEmpty { "डूँगरा जाट (स्थानीय)" },
                                            registeredBy = adminAttribution,
                                            status = "WAITING",
                                            isDarshanCompleted = false
                                        )
                                        try {
                                            com.example.shribalajikripadham.data.network.HostingerCentralSyncManager.issueCentralToken(
                                                patientName = name,
                                                phoneNumber = phone,
                                                city = city.ifEmpty { "डूँगरा जाट (स्थानीय)" },
                                                registeredBy = if (isSuper) "SUPER_ADMIN" else "ADMIN",
                                                darbarDate = today,
                                                customTokenNumber = tokenNum
                                            )
                                        } catch (e: Exception) {}
                                        refreshData()
                                        Toast.makeText(context, if (isHindi) "✅ आरक्षित टोकन #$tokenNum भक्त $name को आवंटित किया गया!" else "✅ Reserved token #$tokenNum assigned to $name!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onRejectReservedToken = { tokenNum ->
                                    scope.launch {
                                        val today = DatabaseHelper.getTodayDateString()
                                        repository.insertOrUpdateCentralToken(
                                            tokenNumber = tokenNum,
                                            darbarDate = today,
                                            patientName = "व्यवस्थापक द्वारा निरस्त / छोड़ा गया",
                                            phoneNumber = "",
                                            city = "निरस्त",
                                            registeredBy = "ADMIN",
                                            status = TokenStatus.CANCELLED.name,
                                            isDarshanCompleted = false
                                        )
                                        try {
                                            com.example.shribalajikripadham.data.network.HostingerCentralSyncManager.updateCentralTokenStatus(
                                                tokenNum, today, TokenStatus.CANCELLED.name, false
                                            )
                                        } catch (e: Exception) {}
                                        refreshData()
                                        Toast.makeText(context, if (isHindi) "टोकन #$tokenNum निरस्त कर दिया गया" else "Token #$tokenNum rejected", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onPushAllTokensToGitHub = {
                                    scope.launch {
                                        Toast.makeText(context, if (isHindi) "GitHub पर सभी टोकन बैकअप भेजा जा रहा है..." else "Pushing all tokens to GitHub...", Toast.LENGTH_SHORT).show()
                                        val (ok, msg) = repository.pushAllTokensToGitHub()
                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    }
                                },
                                onNavigateToHallDisplay = onNavigateToHallDisplay
                            )
                        }
                        currentTabTitle == "मैनुअल टोकन" || currentTabTitle == "Manual" -> {
                            val canBypass = isSuper || admin.canIssueTokensAnywhere
                            val attribution = if (isSuper) "SUPER_ADMIN (अंकित चौधरी)" else "ADMIN (${admin.name})"

                            ManualTokenTab(
                                isHindi = isHindi,
                                admin = admin,
                                settings = settings,
                                repository = repository,
                                canBypassGeofence = canBypass,
                                attribution = attribution,
                                onTokenIssued = { refreshData() },
                                onNavigateToScanRegister = {
                                    val idx = allowedTabs.indexOfFirst { it == "रजिस्टर स्कैन" || it == "Register Scan" }
                                    if (idx >= 0) selectedTab = idx
                                }
                            )
                        }
                        currentTabTitle == "रजिस्टर स्कैन" || currentTabTitle == "Register Scan" -> {
                            PaperRegisterScanTab(
                                isHindi = isHindi,
                                admin = admin,
                                repository = repository,
                                onTokensGenerated = { refreshData() }
                            )
                        }
                        currentTabTitle == "आश्रम पर्चे" || currentTabTitle == "Sacred Parchas" -> {
                            com.example.shribalajikripadham.ui.parcha.SacredParchasScreen(
                                isHindi = isHindi,
                                currentAdmin = admin,
                                isEmbedded = true,
                                onBack = {
                                    val idx = allowedTabs.indexOfFirst { it == "टोकन कतार" || it == "Tokens" }
                                    selectedTab = if (idx >= 0) idx else 0
                                }
                            )
                        }
                        currentTabTitle == "सक्रिय फोन" || currentTabTitle == "Active Devices" -> {
                            ActiveDevicesTab(
                                isHindi = isHindi,
                                repository = repository
                            )
                        }
                        currentTabTitle == "GPS लोकेशन" || currentTabTitle == "Location" -> {
                            LocationConfigTab(
                                isHindi = isHindi,
                                canChangeLocation = admin.canChangeLocation || isSuper,
                                lat = latInput,
                                onLatChange = { latInput = it },
                                long = longInput,
                                onLongChange = { longInput = it },
                                radius = radiusInput,
                                onRadiusChange = { radiusInput = it },
                                isEnforced = geofenceEnforced,
                                onEnforcedChange = { geofenceEnforced = it },
                                isOutstationAllowed = isOutstationAllowed,
                                onOutstationAllowedChange = { isOutstationAllowed = it },
                                outstationMinKm = outstationKmInput,
                                onOutstationMinKmChange = { outstationKmInput = it },
                                successMsg = locationSuccessMsg,
                                errorMsg = locationErrorMsg,
                                onSave = {
                                    scope.launch {
                                        try {
                                            val latVal = latInput.toDouble()
                                            val longVal = longInput.toDouble()
                                            val radVal = radiusInput.toDouble().coerceIn(10.0, 50000.0)
                                            val outKmVal = outstationKmInput.toDoubleOrNull()?.coerceIn(1.0, 500.0) ?: 30.0
                                            repository.updateAshramLocation(
                                                requestingAdmin = admin,
                                                newLat = latVal,
                                                newLong = longVal,
                                                newRadius = radVal,
                                                isGeofenceEnforced = geofenceEnforced,
                                                isOutstationAdvanceAllowed = isOutstationAllowed,
                                                outstationMinDistanceKm = outKmVal
                                            )
                                            try { repository.publishCurrentSettingsToGitHub(admin.name) } catch (e: Exception) {}
                                            val radText = if (radVal >= 1000.0) "${String.format(java.util.Locale.US, "%.1f", radVal/1000.0)}km" else "${radVal.toInt()}m"
                                            val outText = "${outKmVal.toInt()}km"
                                            locationSuccessMsg = if (isHindi) "✓ GPS लोकेशन, परिधि ($radText) व बाहरी भक्त नियम ($outText) सुरक्षित व क्लाउड द्वारा सभी भक्तों के फोन पर लाइव अपडेट हो गई!" else "GPS coordinates, radius ($radText) & outstation rule ($outText) updated & broadcast to all users live!"
                                            locationErrorMsg = null
                                            Toast.makeText(context, locationSuccessMsg, Toast.LENGTH_LONG).show()
                                            refreshData()
                                        } catch (e: Exception) {
                                            locationErrorMsg = e.localizedMessage
                                            Toast.makeText(context, "त्रुटि: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            )
                        }
                        currentTabTitle == "सूचना भेजें" || currentTabTitle == "Broadcast" -> {
                            BroadcastNotificationTab(
                                isHindi = isHindi,
                                title = notifTitle,
                                onTitleChange = { notifTitle = it },
                                message = notifMsg,
                                onMsgChange = { notifMsg = it },
                                priority = notifPriority,
                                onPriorityChange = { notifPriority = it },
                                successMsg = notifSuccessMsg,
                                pastNotifications = notificationsList,
                                onSend = {
                                    scope.launch {
                                        if (notifTitle.isBlank() || notifMsg.isBlank()) {
                                            notifSuccessMsg = if (isHindi) "कृपया शीर्षक और संदेश दोनों लिखें" else "Please enter title and message"
                                            return@launch
                                        }
                                        repository.saveBroadcastNotification(notifTitle, notifMsg, notifPriority, admin.name)
                                        repository.updateEmergencyNotice("$notifTitle: $notifMsg")
                                        try { repository.publishCurrentSettingsToGitHub(admin.name) } catch (e: Exception) {}
                                        try {
                                            com.example.shribalajikripadham.data.network.GitHubLiveSyncManager.publishBroadcastNoticeToCloud(
                                                context = context,
                                                title = notifTitle,
                                                message = notifMsg,
                                                priority = notifPriority,
                                                sentBy = admin.name
                                            )
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                        // Trigger heads-up system alert
                                        NotificationHelper.showSystemNotification(context, notifTitle, notifMsg)
                                        notifSuccessMsg = if (isHindi) "सूचना प्रसारित व सभी भक्तों के फोन पर लाइव अपडेट कर दी गई!" else "Notification successfully broadcasted live to all devotees!"
                                        notifTitle = ""
                                        notifMsg = ""
                                        refreshData()
                                    }
                                }
                            )
                        }
                        currentTabTitle == "सेवादार खाते" || currentTabTitle == "Sevadars" -> {
                            SevadarManagementTab(
                                isHindi = isHindi,
                                admins = adminsList,
                                onOpenCreate = { showCreateSevadarDialog = true },
                                onToggleAnywhere = { targetAdmin, isEnabled ->
                                    scope.launch {
                                        repository.updateAdminAnywhereTokenPermission(targetAdmin.id, isEnabled)
                                        refreshData()
                                    }
                                },
                                onToggleScanRegister = { targetAdmin, isEnabled ->
                                    scope.launch {
                                        repository.updateAdminScanRegisterPermission(targetAdmin.id, isEnabled)
                                        refreshData()
                                    }
                                },
                                onToggleParchas = { targetAdmin, isEnabled ->
                                    scope.launch {
                                        repository.updateAdminParchaPermission(targetAdmin.id, isEnabled)
                                        refreshData()
                                        val msg = if (isEnabled)
                                            (if (isHindi) "✓ ${targetAdmin.name} को पर्चा प्रबंधन अधिकार दे दिया गया।" else "Parcha permission granted.")
                                        else
                                            (if (isHindi) "🔒 ${targetAdmin.name} से पर्चा प्रबंधन अधिकार वापस लिया गया।" else "Parcha permission revoked.")
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onSendWhatsApp = { targetAdmin ->
                                    sevadarToShareViaWhatsApp = targetAdmin
                                    customSharePassword = ""
                                    customSharePin = ""
                                },
                                onOpenEdit = { targetAdmin ->
                                    editingAdmin = targetAdmin
                                    editSevName = targetAdmin.name
                                    editSevUsername = targetAdmin.username
                                    editSevPhone = targetAdmin.phoneNumber
                                    editSevPassword = ""
                                    editSevPin = ""
                                    editSevErrorMsg = null
                                    editSevPhotoUri = targetAdmin.photoUri
                                    editSevCanTokens = targetAdmin.canManageTokens
                                    editSevCanManualTokens = targetAdmin.canIssueManualTokens
                                    editSevCanYatra = targetAdmin.canManageYatra
                                    editSevCanExpenses = targetAdmin.canManageExpenses
                                    editSevCanLocation = targetAdmin.canChangeLocation
                                    editSevCanNotif = targetAdmin.canSendNotifications
                                    editSevCanContent = targetAdmin.canEditAshramInfo
                                    editSevCanPhotos = targetAdmin.canViewDevoteePhotos
                                    editSevCanAnywhere = targetAdmin.canIssueTokensAnywhere
                                    editSevCanScanRegister = targetAdmin.canScanPaperRegister
                                    editSevCanParchas = targetAdmin.canManageParchas
                                    editSevCanCancelTokens = targetAdmin.canCancelTokens
                                    editSevCanDeleteTokens = targetAdmin.canDeleteTokens
                                    editSevCanCustomTokenNumber = targetAdmin.canSetCustomTokenNumber
                                    editSevCanExportPdf = targetAdmin.canExportPdf
                                    editSevCanArzi = targetAdmin.canManageArzi
                                },
                                onToggleActive = { targetAdmin ->
                                    scope.launch {
                                        repository.updateAdminPermissions(
                                            targetAdmin.id,
                                            targetAdmin.canManageTokens,
                                            targetAdmin.canIssueManualTokens,
                                            targetAdmin.canManageYatra,
                                            targetAdmin.canManageExpenses,
                                            targetAdmin.canChangeLocation,
                                            targetAdmin.canSendNotifications,
                                            canEditAshramInfo = targetAdmin.canEditAshramInfo,
                                            canViewDevoteePhotos = targetAdmin.canViewDevoteePhotos,
                                            canIssueTokensAnywhere = targetAdmin.canIssueTokensAnywhere,
                                            canScanPaperRegister = targetAdmin.canScanPaperRegister,
                                            canManageParchas = targetAdmin.canManageParchas,
                                            canManageArzi = targetAdmin.canManageArzi,
                                            isActive = !targetAdmin.isActive
                                        )
                                        try { repository.publishAdminsToGitHub() } catch (e: Exception) {}
                                        refreshData()
                                    }
                                },
                                onDelete = { targetAdmin ->
                                    scope.launch {
                                        repository.deleteAdmin(targetAdmin.id)
                                        try { repository.publishAdminsToGitHub() } catch (e: Exception) {}
                                        refreshData()
                                    }
                                }
                            )
                        }
                        currentTabTitle == "बस बुकिंग लेजर" || currentTabTitle == "Bus Ledger" -> {
                            BusLedgerTab(
                                isHindi = isHindi,
                                repository = repository,
                                settings = settings,
                                isSuperAdmin = isSuper,
                                scope = scope,
                                context = context
                            )
                        }
                        currentTabTitle == "पेमेंट लेजर" || currentTabTitle == "Payment Ledger" -> {
                            PaymentLedgerTab(
                                isHindi = isHindi,
                                repository = repository,
                                settings = settings,
                                isSuperAdmin = isSuper,
                                scope = scope,
                                context = context
                            )
                        }
                        currentTabTitle == "अर्जी लेजर 📦" || currentTabTitle == "Arzi Ledger 📦" -> {
                            ArziLedgerTab(
                                isHindi = isHindi,
                                repository = repository,
                                settings = settings,
                                isSuperAdmin = isSuper,
                                scope = scope,
                                context = context
                            )
                        }
                        currentTabTitle == "महा-लेजर 📊" || currentTabTitle == "Master Ledger 📊" -> {
                            UnifiedMasterLedgerTab(
                                isHindi = isHindi,
                                repository = repository,
                                settings = settings,
                                isSuperAdmin = isSuper,
                                scope = scope,
                                context = context
                            )
                        }
                        currentTabTitle == "त्रिमूर्ति क्लाउड सिंक ☁️" || currentTabTitle == "Triple Cloud Sync ☁️" || currentTabTitle == "Google Sheet 📊" || currentTabTitle == "Google Sheet बही-खाता 📊" -> {
                            GoogleSheetLedgerTab(
                                isHindi = isHindi,
                                repository = repository
                            )
                        }
                        currentTabTitle == "सेवाएं ऑन/ऑफ" || currentTabTitle == "Services" -> {
                            PublicServiceMatrixTab(
                                isHindi = isHindi,
                                isToken = svcTokenEnabled,
                                onTokenChange = { svcTokenEnabled = it },
                                isYatra = svcYatraEnabled,
                                onYatraChange = { svcYatraEnabled = it },
                                isLiveCounter = svcLiveCounterVisible,
                                onLiveCounterChange = { svcLiveCounterVisible = it },
                                isEvents = svcEventsVisible,
                                onEventsChange = { svcEventsVisible = it },
                                isAartiTimings = svcAartiTimingsVisible,
                                onAartiTimingsChange = { svcAartiTimingsVisible = it },
                                isGurujiInfo = svcGurujiInfoVisible,
                                onGurujiInfoChange = { svcGurujiInfoVisible = it },
                                isEmergencyNotice = svcEmergencyNoticeVisible,
                                onEmergencyNoticeChange = { svcEmergencyNoticeVisible = it },
                                scheduledTimestamp = svcScheduledTimestamp,
                                onScheduledTimestampChange = { svcScheduledTimestamp = it },
                                customDateStr = customScheduledDateStr,
                                onCustomDateStrChange = { customScheduledDateStr = it },
                                isBusBookingLive = svcBusBookingLive,
                                onBusBookingLiveChange = { svcBusBookingLive = it },
                                busFareAmount = svcBusFareAmount,
                                onBusFareAmountChange = { svcBusFareAmount = it },
                                isPaymentFeatureLive = svcPaymentFeatureLive,
                                onPaymentFeatureLiveChange = { svcPaymentFeatureLive = it },
                                canAdminViewPayments = svcCanAdminViewPayments,
                                onCanAdminViewPaymentsChange = { svcCanAdminViewPayments = it },
                                canDevoteeViewPayments = svcCanDevoteeViewPayments,
                                onCanDevoteeViewPaymentsChange = { svcCanDevoteeViewPayments = it },
                                upiId = svcUpiId,
                                onUpiIdChange = { svcUpiId = it },
                                upiName = svcUpiName,
                                onUpiNameChange = { svcUpiName = it },
                                customUpiQrUri = svcUpiQrUri,
                                onCustomUpiQrUriChange = { svcUpiQrUri = it },
                                isArziLedgerLive = svcArziLedgerLive,
                                onArziLedgerLiveChange = { svcArziLedgerLive = it },
                                badiArziRate = svcBadiArziRate,
                                onBadiArziRateChange = { svcBadiArziRate = it },
                                chhotiArziRate = svcChhotiArziRate,
                                onChhotiArziRateChange = { svcChhotiArziRate = it },
                                canAdminViewArzi = svcCanAdminViewArzi,
                                onCanAdminViewArziChange = { svcCanAdminViewArzi = it },
                                canDevoteeViewArzi = svcCanDevoteeViewArzi,
                                onCanDevoteeViewArziChange = { svcCanDevoteeViewArzi = it },
                                canDevoteeViewYatraDiary = svcCanDevoteeViewYatraDiary,
                                onCanDevoteeViewYatraDiaryChange = { svcCanDevoteeViewYatraDiary = it },
                                successMsg = svcSuccessMsg,
                                onSave = {
                                    scope.launch {
                                        // Parse customDateStr if set
                                        var finalTimestamp = svcScheduledTimestamp
                                        if (customScheduledDateStr.isNotBlank()) {
                                            try {
                                                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                                                val parsed = sdf.parse(customScheduledDateStr.trim())
                                                if (parsed != null) {
                                                    finalTimestamp = parsed.time
                                                    svcScheduledTimestamp = finalTimestamp
                                                }
                                            } catch (e: Exception) {
                                                // ignore parse error if timestamp already selected
                                            }
                                        }

                                        repository.updateScheduledTokenOpenTime(finalTimestamp)
                                        repository.updateMasterVisibilityToggles(
                                            isTokenEnabled = svcTokenEnabled,
                                            isYatraEnabled = svcYatraEnabled,
                                            isLiveCounterVisible = svcLiveCounterVisible,
                                            isEventsVisible = svcEventsVisible,
                                            isAartiTimingsVisible = svcAartiTimingsVisible,
                                            isGurujiInfoVisible = svcGurujiInfoVisible,
                                            isEmergencyNoticeVisible = svcEmergencyNoticeVisible
                                        )
                                        repository.updateBusAndPaymentSettings(
                                            isBusBookingLive = svcBusBookingLive,
                                            isPaymentFeatureLive = svcPaymentFeatureLive,
                                            canAdminViewPaymentHistory = svcCanAdminViewPayments,
                                            canDevoteeViewPaymentHistory = svcCanDevoteeViewPayments,
                                            ashramUpiId = svcUpiId.trim(),
                                            ashramUpiName = svcUpiName.trim(),
                                            busSeatFareAmount = svcBusFareAmount.toIntOrNull() ?: 1500,
                                            customUpiQrUri = svcUpiQrUri.trim()
                                        )
                                        repository.updateArziSettings(
                                            isArziLedgerLive = svcArziLedgerLive,
                                            badiArziRate = svcBadiArziRate.toDoubleOrNull() ?: 100.0,
                                            chhotiArziRate = svcChhotiArziRate.toDoubleOrNull() ?: 50.0,
                                            canAdminViewArziLedger = svcCanAdminViewArzi,
                                            canDevoteeViewArziLedger = svcCanDevoteeViewArzi
                                        )
                                        repository.updateCanDevoteeViewYatraDiary(svcCanDevoteeViewYatraDiary)
                                        try { repository.publishCurrentSettingsToGitHub(admin.name) } catch (e: Exception) {}
                                        try {
                                            com.example.shribalajikripadham.data.network.GitHubLiveSyncManager.publishBroadcastNoticeToCloud(
                                                context = context,
                                                title = notifTitle,
                                                message = notifMsg,
                                                priority = notifPriority,
                                                sentBy = admin.name
                                            )
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                        svcSuccessMsg = if (isHindi)
                                            "सेवाएं, अर्जी दर, बस व्यवस्था, Yatra डायरी व UPI पेमेंट सेटिंग्स सुरक्षित और सभी भक्तों के फोन पर लाइव अपडेट हुई!"
                                        else
                                            "Service visibility, Arzi rates, Yatra diary & payment settings saved & broadcast to all devotees!"
                                        Toast.makeText(context, if (isHindi) "✓ सेटिंग्स सुरक्षित व लाइव अपडेट!" else "Settings saved & live updated!", Toast.LENGTH_SHORT).show()
                                        refreshData()
                                    }
                                }
                            )
                        }
                        currentTabTitle == "🌐 वेबसाइट व CMS" || currentTabTitle == "Website & CMS" -> {
                            WebsiteAndCmsManagerTab(
                                isHindi = isHindi,
                                admin = admin,
                                settings = settings,
                                repository = repository,
                                onRefresh = { refreshData() }
                            )
                        }
                        currentTabTitle == "ऐप कस्टमाइजर" || currentTabTitle == "Customizer" -> {
                            AppCustomizerTab(
                                isHindi = isHindi,
                                ashramName = customAshramName,
                                onAshramNameChange = { customAshramName = it },
                                gurujiName = customGurujiName,
                                onGurujiNameChange = { customGurujiName = it },
                                gurujiPhotoUri = customGurujiPhotoUri,
                                onGurujiPhotoUriChange = { customGurujiPhotoUri = it },
                                address = customAddress,
                                onAddressChange = { customAddress = it },
                                phone = customPhone,
                                onPhoneChange = { customPhone = it },
                                timings = customTimings,
                                onTimingsChange = { customTimings = it },
                                disclaimer = customDisclaimer,
                                onDisclaimerChange = { customDisclaimer = it },
                                emergencyNotice = customEmergencyNotice,
                                onEmergencyNoticeChange = { customEmergencyNotice = it },
                                ashramParichayHindi = customParichayHindi,
                                onAshramParichayHindiChange = { customParichayHindi = it },
                                ashramParichayEnglish = customParichayEnglish,
                                onAshramParichayEnglishChange = { customParichayEnglish = it },
                                ashramHistoryHindi = customHistoryHindi,
                                onAshramHistoryHindiChange = { customHistoryHindi = it },
                                ashramRulesHindi = customRulesHindi,
                                onAshramRulesHindiChange = { customRulesHindi = it },
                                whatsappGroup = customWhatsappGroup,
                                onWhatsappGroupChange = { customWhatsappGroup = it },
                                youtubeChannel = customYoutubeChannel,
                                onYoutubeChannelChange = { customYoutubeChannel = it },
                                facebookPage = customFacebookPage,
                                onFacebookPageChange = { customFacebookPage = it },
                                instagramPage = customInstagramPage,
                                onInstagramPageChange = { customInstagramPage = it },
                                appShareUrl = customAppShareUrl,
                                onAppShareUrlChange = { customAppShareUrl = it },
                                sundayTokenBannerTitle = customSundayTokenBannerTitle,
                                onSundayTokenBannerTitleChange = { customSundayTokenBannerTitle = it },
                                sundayTokenBannerText = customSundayTokenBannerText,
                                onSundayTokenBannerTextChange = { customSundayTokenBannerText = it },
                                sundayTokenCustomNotice = customSundayTokenCustomNotice,
                                onSundayTokenCustomNoticeChange = { customSundayTokenCustomNotice = it },
                                successMsg = customizerSuccessMsg,
                                events = eventsList,
                                onOpenAddEvent = { showAddEventDialog = true },
                                onDeleteEvent = { evId ->
                                    scope.launch {
                                        repository.deleteEvent(evId)
                                        try { repository.publishCurrentSettingsToGitHub(admin.name) } catch (e: Exception) {}
                                        try {
                                            com.example.shribalajikripadham.data.network.GitHubLiveSyncManager.publishBroadcastNoticeToCloud(
                                                context = context,
                                                title = notifTitle,
                                                message = notifMsg,
                                                priority = notifPriority,
                                                sentBy = admin.name
                                            )
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                        refreshData()
                                    }
                                },
                                onSave = {
                                    scope.launch {
                                        repository.updateAshramDetails(
                                            customAshramName,
                                            customGurujiName,
                                            customAddress,
                                            customPhone,
                                            customTimings,
                                            customDisclaimer,
                                            customEmergencyNotice
                                        )
                                        repository.updateGurujiPhoto(customGurujiPhotoUri)
                                        repository.updateSocialLinks(
                                            customWhatsappGroup,
                                            customPhone,
                                            customYoutubeChannel,
                                            customFacebookPage,
                                            customInstagramPage,
                                            customAppShareUrl
                                        )
                                        repository.updateSundayTokenBanner(
                                            customSundayTokenBannerTitle,
                                            customSundayTokenBannerText,
                                            customSundayTokenCustomNotice
                                        )
                                        repository.updateAshramParichayAndRules(
                                            customParichayHindi.trim(),
                                            customParichayEnglish.trim(),
                                            customHistoryHindi.trim(),
                                            customRulesHindi.trim()
                                        )
                                        try { repository.publishCurrentSettingsToGitHub(admin.name) } catch (e: Exception) {}
                                        try {
                                            com.example.shribalajikripadham.data.network.GitHubLiveSyncManager.publishBroadcastNoticeToCloud(
                                                context = context,
                                                title = notifTitle,
                                                message = notifMsg,
                                                priority = notifPriority,
                                                sentBy = admin.name
                                            )
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                        customizerSuccessMsg = if (isHindi) "✓ आश्रम विवरण, फोटो व सोशल लिंक्स सुरक्षित व सभी भक्तों के फोन पर लाइव अपडेट हो गए!" else "Ashram details saved & published live to all users!"
                                        Toast.makeText(context, if (isHindi) "✓ आश्रम विवरण व परिचय सुरक्षित!" else "Details saved!", Toast.LENGTH_SHORT).show()
                                        refreshData()
                                    }
                                }
                            )
                        }
                        currentTabTitle == "कस्टम दूरियाँ" || currentTabTitle == "Distances" -> {
                            CustomDistancesTab(
                                isHindi = isHindi,
                                customDistances = customDistancesList,
                                onAddDistance = { name, dist ->
                                    scope.launch {
                                        repository.addCustomCityDistance(name, dist)
                                        refreshData()
                                    }
                                },
                                onDeleteDistance = { id ->
                                    scope.launch {
                                        repository.deleteCustomCityDistance(id)
                                        refreshData()
                                    }
                                }
                            )
                        }
                        currentTabTitle == "सुपर कंट्रोल" || currentTabTitle == "Super Control" -> {
                            SuperControlTab(
                                isHindi = isHindi,
                                settings = settings,
                                superAdmin = superAdminAccount ?: (if (admin.role == AdminRole.SUPER_ADMIN) admin else null),
                                repository = repository,
                                onRefreshData = { refreshData() },
                                onUpdateSuperAdminProfile = { name, phone, username, password, pin, photoUri, onResult ->
                                    scope.launch {
                                        val (ok, msg) = repository.updateSuperAdminProfile(
                                            name = name,
                                            phone = phone,
                                            username = username,
                                            password = password,
                                            pin = pin,
                                            photoUri = photoUri
                                        )
                                        if (ok) {
                                            repository.updateContactPhone(phone)
                                            try { repository.publishCurrentSettingsToGitHub(name) } catch (e: Exception) {}
                                            refreshData()
                                        }
                                        onResult(ok, msg)
                                    }
                                },
                                onUpdateMasterPassword = { currentPass, newPass, onResult ->
                                    scope.launch {
                                        val isValid = repository.verifySuperAdminPassword(currentPass)
                                        if (!isValid) {
                                            onResult(false, if (isHindi) "वर्तमान पासवर्ड गलत है!" else "Current password incorrect!")
                                            return@launch
                                        }
                                        val updated = repository.updateSuperAdminPassword(newPass)
                                        if (updated) {
                                            onResult(true, if (isHindi) "मास्टर पासवर्ड सफलतापूर्वक बदल दिया गया!" else "Master password updated successfully!")
                                        } else {
                                            onResult(false, if (isHindi) "पासवर्ड बदलने में त्रुटि!" else "Failed to update password!")
                                        }
                                    }
                                },
                                onUpdateMaxDailyTokens = { maxTokens ->
                                    scope.launch {
                                        repository.updateMaxDailyTokens(maxTokens)
                                        try { repository.publishCurrentSettingsToGitHub(admin.name) } catch (e: Exception) {}
                                        try {
                                            com.example.shribalajikripadham.data.network.GitHubLiveSyncManager.publishBroadcastNoticeToCloud(
                                                context = context,
                                                title = notifTitle,
                                                message = notifMsg,
                                                priority = notifPriority,
                                                sentBy = admin.name
                                            )
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                        refreshData()
                                    }
                                },
                                onUpdateEnforcedLayout = { layoutKey, isEnforced ->
                                    scope.launch {
                                        repository.updateActiveUiLayoutEnforced(layoutKey, isEnforced)
                                        try { repository.publishCurrentSettingsToGitHub(admin.name) } catch (e: Exception) {}
                                        try {
                                            com.example.shribalajikripadham.data.network.GitHubLiveSyncManager.publishBroadcastNoticeToCloud(
                                                context = context,
                                                title = notifTitle,
                                                message = notifMsg,
                                                priority = notifPriority,
                                                sentBy = admin.name
                                            )
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                        refreshData()
                                    }
                                },
                                onUpdateCloudSync = { url, isEnabled ->
                                    scope.launch {
                                        repository.updateCloudSyncSettings(url, isEnabled)
                                        refreshData()
                                    }
                                },
                                onTriggerCloudSync = { onResult ->
                                    scope.launch {
                                        val s = repository.getSettings()
                                        val (success, msg) = repository.syncWithCloudEndpoint(s.cloudSyncUrl)
                                        onResult(success, msg)
                                        if (success) refreshData()
                                    }
                                },
                                onExportDatabaseBackup = { onResult ->
                                    scope.launch {
                                        try {
                                            val json = repository.exportFullDatabaseBackupJson()
                                            onResult(true, json)
                                        } catch (e: Exception) {
                                            onResult(false, e.localizedMessage ?: "Export error")
                                        }
                                    }
                                },
                                onRestoreDatabaseBackup = { json, onResult ->
                                    scope.launch {
                                        try {
                                            val ok = repository.restoreFullDatabaseFromJson(json)
                                            if (ok) {
                                                refreshData()
                                                onResult(true, if (isHindi) "डेटाबेस सफलतापूर्वक रीस्टोर हो गया!" else "Database restored successfully!")
                                            } else {
                                                onResult(false, if (isHindi) "अमान्य बैकअप डेटा!" else "Invalid backup data!")
                                            }
                                        } catch (e: Exception) {
                                            onResult(false, "त्रुटि: ${e.localizedMessage}")
                                        }
                                    }
                                }
                            )
                        }
                        currentTabTitle == "UI बॉक्स कंट्रोल" || currentTabTitle == "UI Control" -> {
                            UiBoxControlTab(
                                isHindi = isHindi,
                                sections = uiSectionsList,
                                onSaveSections = { updatedList ->
                                    scope.launch {
                                        repository.saveUiSectionConfigs(updatedList)
                                        try { repository.publishLiveConfigToGitHub(updatedList, admin.name) } catch (e: Exception) {}
                                        refreshData()
                                        Toast.makeText(context, if (isHindi) "✓ UI लेआउट क्रम सुरक्षित व सभी भक्तों के फोन पर लाइव अपडेट हो गया!" else "UI layout updated & published live to all devotees!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onResetToDefault = {
                                    scope.launch {
                                        repository.resetUiSectionConfigsToDefault()
                                        refreshData()
                                        Toast.makeText(context, if (isHindi) "डिफ़ॉल्ट UI क्रम रीसेट कर दिया गया!" else "Reset to default UI layout!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onPublishToGitHub = { updatedList ->
                                    val res = repository.publishLiveConfigToGitHub(updatedList, admin.name)
                                    refreshData()
                                    res
                                }
                            )
                        }
                        currentTabTitle == "🪪 ID कार्ड स्टूडियो" || currentTabTitle == "🪪 ID Card Studio" -> {
                            SuperIdCardStudioTab(
                                isHindi = isHindi,
                                repository = repository,
                                adminsList = adminsList
                            )
                        }
                        currentTabTitle == "ऑटो-अपडेट" || currentTabTitle == "Updates" -> {
                            AutoUpdateManagerTab(
                                isHindi = isHindi,
                                currentCode = AppUpdateManager.getCurrentVersionCode(context),
                                currentName = AppUpdateManager.getCurrentVersionName(context),
                                targetCode = updVersionCode,
                                onTargetCodeChange = { updVersionCode = it },
                                targetName = updVersionName,
                                onTargetNameChange = { updVersionName = it },
                                notes = updNotes,
                                onNotesChange = { updNotes = it },
                                apkUrl = updApkUrl,
                                onApkUrlChange = { updApkUrl = it },
                                isForce = updIsForce,
                                onForceChange = { updIsForce = it },
                                successMsg = updSuccessMsg,
                                onSave = {
                                    scope.launch {
                                        val currentVer = AppUpdateManager.getCurrentVersionCode(context)
                                        val code = updVersionCode.toIntOrNull() ?: (currentVer + 1)
                                        val finalApkUrl = if (updApkUrl.isNotBlank()) updApkUrl.trim() else AppUpdateManager.DEFAULT_APK_URL
                                        val finalName = if (updVersionName.isNotBlank()) updVersionName.trim() else "2.2.0"
                                        val finalNotes = if (updNotes.isNotBlank()) updNotes.trim() else (if (isHindi) "नया अपडेट: 8 नए सुपर एडमिन नियंत्रण फीचर्स, पासवर्ड चेंज, CSV एक्सपोर्ट, टोकन कोटा एवं तीव्र गति।" else "New update with 8 Super Admin features, CSV export and performance improvements.")
                                        repository.updateAppUpdateConfig(code, finalName, finalNotes, finalApkUrl, updIsForce)
                                        updSuccessMsg = if (isHindi) "नया वर्जन विन्यास (Build #$code) सफलतापूर्वक जारी किया गया! अब होम स्क्रीन पर अपडेट पॉपअप दिखेगा।" else "Version update config (Build #$code) published successfully!"
                                        refreshData()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // --- CREATE SEVADAR DIALOG ---
    if (showCreateSevadarDialog) {
        AlertDialog(
            onDismissRequest = { showCreateSevadarDialog = false },
            title = { Text(if (isHindi) "नया सेवादार खाता बनाएं" else "Create Sevadar Account", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    createSevErrorMsg?.let { err ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                            border = BorderStroke(1.dp, Color(0xFFE57373))
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("⚠️", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(err, color = Color(0xFFC62828), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = newSevName,
                        onValueChange = { newSevName = it; createSevErrorMsg = null },
                        label = { Text(if (isHindi) "सेवादार का नाम" else "Full Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newSevUsername,
                        onValueChange = { newSevUsername = it; createSevErrorMsg = null },
                        label = { Text(if (isHindi) "यूजरनेम (Unique Username)" else "Unique Username") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newSevPassword,
                        onValueChange = { newSevPassword = it; createSevErrorMsg = null },
                        label = { Text(if (isHindi) "पासवर्ड (Password)" else "Password") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newSevPhone,
                        onValueChange = { newSevPhone = it; createSevErrorMsg = null },
                        label = { Text(if (isHindi) "मोबाइल नंबर" else "Mobile Number") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newSevPin,
                        onValueChange = { newSevPin = it; createSevErrorMsg = null },
                        label = { Text(if (isHindi) "त्वरित 4-अंकीय पिन" else "Quick 4-Digit PIN") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9)),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFC5E1A5))
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (isHindi) "📷 सेवादार प्रोफाइल फोटो (गैलरी / कैमरा)" else "📷 Sevadar Photo (Gallery / Camera)",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = Color(0xFF2E7D32)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            SacredAvatar(photoUri = newSevPhotoUri, name = newSevName.ifEmpty { "सेवादार" }, size = 60.dp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { newSevGalleryLauncher.launch("image/*") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1565C0))
                                ) {
                                    Text(if (isHindi) "🖼️ गैलरी" else "🖼️ Gallery", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = { newSevCameraLauncher.launch(null) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2E7D32))
                                ) {
                                    Text(if (isHindi) "📷 कैमरा" else "📷 Camera", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                if (newSevPhotoUri.isNotBlank()) {
                                    OutlinedButton(
                                        onClick = {
                                            val rotated = DevoteePhotoHelper.rotateSavedPhoto(context, newSevPhotoUri, 90f)
                                            if (rotated.isNotBlank()) {
                                                newSevPhotoUri = rotated
                                                Toast.makeText(context, if (isHindi) "🔄 फोटो 90° सीधी हो गई!" else "🔄 Photo rotated 90°!", Toast.LENGTH_SHORT).show()
                                                scope.launch(Dispatchers.IO) {
                                                    val safeName = "sevadar_" + System.currentTimeMillis() + ".jpg"
                                                    val cloudUrl = com.example.shribalajikripadham.data.network.GitHubLiveSyncManager.uploadPhotoToGitHub(context, rotated, safeName)
                                                    if (!cloudUrl.isNullOrBlank()) {
                                                        withContext(Dispatchers.Main) {
                                                            newSevPhotoUri = cloudUrl
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SaffronPrimary)
                                    ) {
                                        Text("🔄 90°", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    OutlinedButton(
                                        onClick = { newSevPhotoUri = "" },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                                    ) {
                                        Text("❌", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                    OutlinedTextField(
                        value = newSevPhotoUri,
                        onValueChange = { newSevPhotoUri = it },
                        label = { Text(if (isHindi) "सेवादार फोटो (URL या फ़ाइल पाथ)" else "Photo URL / File Path") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(if (isHindi) "अनुमतियाँ (Permissions):" else "Granted Permissions:", fontWeight = FontWeight.Bold)

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = newSevCanTokens, onCheckedChange = { newSevCanTokens = it })
                        Text(if (isHindi) "टोकन कतार प्रबंधन" else "Manage Token Queue", fontSize = 13.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = newSevCanManualTokens, onCheckedChange = { newSevCanManualTokens = it })
                        Text(if (isHindi) "मैनुअल टोकन जारी करना" else "Issue Manual Tokens", fontSize = 13.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = newSevCanYatra, onCheckedChange = { newSevCanYatra = it })
                        Text(if (isHindi) "बालाजी यात्रा सीट बुकिंग" else "Manage Yatra Seats", fontSize = 13.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = newSevCanExpenses, onCheckedChange = { newSevCanExpenses = it })
                        Text(if (isHindi) "यात्रा खर्च जोड़ना" else "Add Yatra Expenses", fontSize = 13.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = newSevCanLocation, onCheckedChange = { newSevCanLocation = it })
                        Text(if (isHindi) "आश्रम GPS लोकेशन बदलना" else "Change Ashram GPS", fontSize = 13.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = newSevCanNotif, onCheckedChange = { newSevCanNotif = it })
                        Text(if (isHindi) "सूचना प्रसारित करना" else "Send Broadcast Notifications", fontSize = 13.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = newSevCanContent, onCheckedChange = { newSevCanContent = it })
                        Text(if (isHindi) "आश्रम विवरण व उत्सव बदलना" else "Edit Content & Events", fontSize = 13.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = newSevCanPhotos, onCheckedChange = { newSevCanPhotos = it })
                        Text(if (isHindi) "भक्तों की फोटो देखने की अनुमति" else "Allow Viewing Devotee Photos", fontSize = 13.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = newSevCanAnywhere, onCheckedChange = { newSevCanAnywhere = it })
                        Text(
                            text = if (isHindi) "कहीं से भी टोकन जारी करने की अनुमति (Anywhere)" else "Allow Issuing Tokens Anywhere",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaroonAccent
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = newSevCanScanRegister, onCheckedChange = { newSevCanScanRegister = it })
                        Text(
                            text = if (isHindi) "📝 रजिस्टर कॉपी स्कैन व टोकन जारी अधिकार" else "Scan Paper Register Permission",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0D47A1)
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = newSevCanParchas, onCheckedChange = { newSevCanParchas = it })
                        Text(
                            text = if (isHindi) "📜 आश्रम पर्चे प्रबंधन (Super Admin Delegation)" else "Manage Sacred Parchas (Delegation)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE65100)
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = newSevCanCancelTokens, onCheckedChange = { newSevCanCancelTokens = it })
                        Text(if (isHindi) "🚫 टोकन रद्द करने की अनुमति (Cancel Token)" else "Allow Cancel Token", fontSize = 13.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = newSevCanDeleteTokens, onCheckedChange = { newSevCanDeleteTokens = it })
                        Text(if (isHindi) "🗑️ टोकन स्थायी हटाने की अनुमति (Delete Token)" else "Allow Delete Token", fontSize = 13.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = newSevCanCustomTokenNumber, onCheckedChange = { newSevCanCustomTokenNumber = it })
                        Text(
                            text = if (isHindi) "👑 विशेष आरक्षित VIP टोकन (2..20, कोटा: 2) व कस्टम टोकन अधिकार" else "Allow Reserved VIP Slots (2..20, Max 2) & Custom Token #",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaroonPrimary
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = newSevCanExportPdf, onCheckedChange = { newSevCanExportPdf = it })
                        Text(if (isHindi) "📄 आज की टोकन सूची PDF डाउनलोड (Export PDF)" else "Allow Export PDF", fontSize = 13.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = newSevCanArzi, onCheckedChange = { newSevCanArzi = it })
                        Text(if (isHindi) "📦 अर्जी डिब्बा वितरण व लेजर प्रबंधन" else "Manage Arzi Distribution & Ledger", fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            if (newSevName.isNotBlank() && newSevUsername.isNotBlank() && newSevPassword.isNotBlank()) {
                                val perms = mutableListOf<String>()
                                if (newSevCanTokens) perms.add("रविवार टोकन कतार")
                                if (newSevCanManualTokens) perms.add("मैनुअल टोकन जारी करना")
                                if (newSevCanYatra) perms.add("श्री बालाजी यात्रा सेवा")
                                if (newSevCanExpenses) perms.add("धाम व्यय (खर्च) प्रबंधन")
                                if (newSevCanLocation) perms.add("GPS व लोकेशन सेटिंग")
                                if (newSevCanNotif) perms.add("सूचना व घोषणाएं")
                                if (newSevCanContent) perms.add("आश्रम जानकारी संपादन")
                                if (newSevCanPhotos) perms.add("भक्त फोटो व बायोमेट्रिक")
                                if (newSevCanAnywhere) perms.add("कहीं से भी टोकन जारी करना")
                                if (newSevCanScanRegister) perms.add("रजिस्टर कॉपी स्कैन")
                                if (newSevCanParchas) perms.add("आश्रम पावन पर्चे")
                                if (newSevCanArzi) perms.add("अर्जी डिब्बा व लेजर")

                                val (createdOk, createMsg) = repository.createSevadarAdmin(
                                    name = newSevName,
                                    username = newSevUsername,
                                    phone = newSevPhone,
                                    role = AdminRole.SEVADAR,
                                    password = newSevPassword,
                                    pin = newSevPin,
                                    canManageTokens = newSevCanTokens,
                                    canIssueManualTokens = newSevCanManualTokens,
                                    canManageYatra = newSevCanYatra,
                                    canManageExpenses = newSevCanExpenses,
                                    canChangeLocation = newSevCanLocation,
                                    canSendNotifications = newSevCanNotif,
                                    canEditAshramInfo = newSevCanContent,
                                    canViewDevoteePhotos = newSevCanPhotos,
                                    canIssueTokensAnywhere = newSevCanAnywhere,
                                    canScanPaperRegister = newSevCanScanRegister,
                                    canManageParchas = newSevCanParchas,
                                    canCancelTokens = newSevCanCancelTokens,
                                    canDeleteTokens = newSevCanDeleteTokens,
                                    canSetCustomTokenNumber = newSevCanCustomTokenNumber,
                                    canExportPdf = newSevCanExportPdf,
                                    canManageArzi = newSevCanArzi,
                                    photoUri = newSevPhotoUri
                                )

                                if (createdOk) {
                                    // Capture credentials for instant WhatsApp banner sharing
                                    createdSevadarShareData = CreatedSevadarShareData(
                                        name = newSevName,
                                        username = newSevUsername,
                                        phone = newSevPhone,
                                        password = newSevPassword,
                                        pin = newSevPin,
                                        permissions = perms
                                    )

                                    showCreateSevadarDialog = false
                                    createSevErrorMsg = null
                                    newSevName = ""
                                    newSevUsername = ""
                                    newSevPassword = ""
                                    newSevPhone = ""
                                    newSevPin = ""
                                    newSevPhotoUri = ""
                                    newSevCanPhotos = false
                                    newSevCanAnywhere = false
                                    newSevCanScanRegister = false
                                    newSevCanParchas = false
                                    try { repository.publishAdminsToGitHub() } catch (e: Exception) {}
                                    refreshData()
                                } else {
                                    createSevErrorMsg = createMsg
                                }
                            } else {
                                createSevErrorMsg = if (isHindi) "कृपया सेवादार का नाम, यूजरनेम और पासवर्ड अवश्य भरें!" else "Please fill Name, Username and Password!"
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary)
                ) {
                    Text(if (isHindi) "खाता बनाएं" else "Create Account")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateSevadarDialog = false }) {
                    Text(if (isHindi) "रद्द करें" else "Cancel")
                }
            }
        )
    }

    // --- SHARE CREATED SEVADAR CREDENTIALS TO WHATSAPP DIALOG ---
    if (createdSevadarShareData != null) {
        val shareData = createdSevadarShareData!!
        AlertDialog(
            onDismissRequest = { createdSevadarShareData = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🎉", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isHindi) "नया सेवादार खाता तैयार!" else "Sevadar Account Created!",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = MaroonPrimary
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (isHindi)
                            "नया खाता बन गया है। आप सीधे सेवादार के व्हाट्सएप पर यूजरनेम, पासवर्ड व पिन का आकर्षक बैनर कार्ड भेज सकते हैं:"
                        else
                            "Account created! Send username, password & PIN banner card directly to WhatsApp:",
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDE7)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFFFD54F)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("👤 नाम: ${shareData.name}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaroonPrimary)
                            Text("📱 मोबाइल: ${shareData.phone}", fontSize = 12.sp, color = Color.Black)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("🆔 यूजरनेम: ${shareData.username}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("🔑 पासवर्ड: ${shareData.password}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFC62828))
                            Text("🔢 सुरक्षा पिन: ${shareData.pin}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFE65100))
                        }
                    }

                    // 1. Send Banner Image + Text to WhatsApp
                    Button(
                        onClick = {
                            scope.launch {
                                isSharingBanner = true
                                try {
                                    val msgText = AdminCredentialBannerHelper.formatWhatsAppMessage(
                                        name = shareData.name,
                                        username = shareData.username,
                                        password = shareData.password,
                                        pin = shareData.pin,
                                        phone = shareData.phone,
                                        permissions = shareData.permissions
                                    )
                                    val bmp = AdminCredentialBannerHelper.renderCredentialBannerBitmap(
                                        name = shareData.name,
                                        username = shareData.username,
                                        password = shareData.password,
                                        pin = shareData.pin,
                                        phone = shareData.phone,
                                        permissions = shareData.permissions
                                    )
                                    val file = AdminCredentialBannerHelper.saveBannerBitmapToFile(
                                        context = context,
                                        bitmap = bmp,
                                        username = shareData.username
                                    )
                                    AdminCredentialBannerHelper.sendToWhatsApp(
                                        context = context,
                                        phone = shareData.phone,
                                        messageText = msgText,
                                        bannerFile = file
                                    )
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    Toast.makeText(context, "त्रुटि: ${e.message}", Toast.LENGTH_SHORT).show()
                                } finally {
                                    isSharingBanner = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = if (isSharingBanner) "बैनर तैयार हो रहा है..." else "📲 व्हाट्सएप पर बैनर व विवरण भेजें",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    // 2. Direct text on WhatsApp
                    OutlinedButton(
                        onClick = {
                            val msgText = AdminCredentialBannerHelper.formatWhatsAppMessage(
                                name = shareData.name,
                                username = shareData.username,
                                password = shareData.password,
                                pin = shareData.pin,
                                phone = shareData.phone,
                                permissions = shareData.permissions
                            )
                            AdminCredentialBannerHelper.sendToWhatsApp(
                                context = context,
                                phone = shareData.phone,
                                messageText = msgText,
                                bannerFile = null
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("💬 केवल टेक्स्ट मैसेज भेजें (Text Only)", fontSize = 12.sp, color = Color(0xFF1B5E20), fontWeight = FontWeight.SemiBold)
                    }

                    // 3. Copy Details
                    OutlinedButton(
                        onClick = {
                            val msgText = AdminCredentialBannerHelper.formatWhatsAppMessage(
                                name = shareData.name,
                                username = shareData.username,
                                password = shareData.password,
                                pin = shareData.pin,
                                phone = shareData.phone,
                                permissions = shareData.permissions
                            )
                            AdminCredentialBannerHelper.copyToClipboard(context, msgText)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("📋 विवरण कॉपी करें (Copy Details)", fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { createdSevadarShareData = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary)
                ) {
                    Text(if (isHindi) "पूर्ण (Done)" else "Done")
                }
            }
        )
    }

    // --- RESEND / SHARE WHATSAPP DIALOG FOR EXISTING SEVADAR ---
    if (sevadarToShareViaWhatsApp != null) {
        val target = sevadarToShareViaWhatsApp!!
        AlertDialog(
            onDismissRequest = { sevadarToShareViaWhatsApp = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📲", fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isHindi) "व्हाट्सएप पर क्रेडेंशियल्स भेजें" else "Send Credentials to WhatsApp",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaroonPrimary
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "सेवादार: ${target.name} (${target.phoneNumber})",
                        fontWeight = FontWeight.Bold,
                        color = MaroonPrimary
                    )
                    Text(
                        text = "यूजरनेम: ${target.username}",
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )

                    OutlinedTextField(
                        value = customSharePassword,
                        onValueChange = { customSharePassword = it },
                        label = { Text(if (isHindi) "पासवर्ड (यदि नया/ज्ञात हो)" else "Password (if known)") },
                        placeholder = { Text("उदा. 123456") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = customSharePin,
                        onValueChange = { if (it.length <= 6) customSharePin = it },
                        label = { Text(if (isHindi) "सुरक्षा पिन (PIN)" else "Security PIN") },
                        placeholder = { Text("उदा. 1234") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Button(
                        onClick = {
                            scope.launch {
                                isSharingBanner = true
                                try {
                                    val perms = mutableListOf<String>()
                                    if (target.canManageTokens) perms.add("रविवार टोकन कतार")
                                    if (target.canIssueManualTokens) perms.add("मैनुअल टोकन जारी करना")
                                    if (target.canManageYatra) perms.add("श्री बालाजी यात्रा सेवा")
                                    if (target.canManageExpenses) perms.add("धाम व्यय (खर्च) प्रबंधन")
                                    if (target.canChangeLocation) perms.add("GPS व लोकेशन सेटिंग")
                                    if (target.canSendNotifications) perms.add("सूचना व घोषणाएं")
                                    if (target.canEditAshramInfo) perms.add("आश्रम जानकारी संपादन")
                                    if (target.canViewDevoteePhotos) perms.add("भक्त फोटो व बायोमेट्रिक")
                                    if (target.canIssueTokensAnywhere) perms.add("कहीं से भी टोकन जारी करना")
                                    if (target.canScanPaperRegister) perms.add("रजिस्टर कॉपी स्कैन")
                                    if (target.canManageParchas) perms.add("आश्रम पावन पर्चे")

                                    val pwd = customSharePassword.ifBlank { "(सुरक्षा कारणों से गोपनीय / पूर्व निर्धारित)" }
                                    val pn = customSharePin.ifBlank { "(पूर्व निर्धारित पिन)" }

                                    val msgText = AdminCredentialBannerHelper.formatWhatsAppMessage(
                                        name = target.name,
                                        username = target.username,
                                        password = pwd,
                                        pin = pn,
                                        phone = target.phoneNumber,
                                        permissions = perms
                                    )
                                    val bmp = AdminCredentialBannerHelper.renderCredentialBannerBitmap(
                                        name = target.name,
                                        username = target.username,
                                        password = pwd,
                                        pin = pn,
                                        phone = target.phoneNumber,
                                        permissions = perms
                                    )
                                    val file = AdminCredentialBannerHelper.saveBannerBitmapToFile(
                                        context = context,
                                        bitmap = bmp,
                                        username = target.username
                                    )
                                    AdminCredentialBannerHelper.sendToWhatsApp(
                                        context = context,
                                        phone = target.phoneNumber,
                                        messageText = msgText,
                                        bannerFile = file
                                    )
                                    sevadarToShareViaWhatsApp = null
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    Toast.makeText(context, "त्रुटि: ${e.message}", Toast.LENGTH_SHORT).show()
                                } finally {
                                    isSharingBanner = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = if (isSharingBanner) "बैनर तैयार हो रहा है..." else "📲 व्हाट्सएप पर बैनर व विवरण भेजें",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { sevadarToShareViaWhatsApp = null }) {
                    Text(if (isHindi) "रद्द करें" else "Cancel")
                }
            }
        )
    }

    // --- EDIT SEVADAR DIALOG (Super Admin Control) ---
    if (editingAdmin != null) {
        val target = editingAdmin!!
        AlertDialog(
            onDismissRequest = { editingAdmin = null },
            title = { Text(if (isHindi) "सेवादार पूर्ण विवरण व अनुमतियाँ संपादित करें" else "Edit Sevadar Details & Permissions", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "व्यवस्थापक / सेवादार आईडी: #${target.id}",
                        fontWeight = FontWeight.Bold,
                        color = MaroonPrimary
                    )

                    editSevErrorMsg?.let { err ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                            border = BorderStroke(1.dp, Color(0xFFE57373))
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("⚠️", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(err, color = Color(0xFFC62828), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = editSevName,
                        onValueChange = { editSevName = it; editSevErrorMsg = null },
                        label = { Text(if (isHindi) "सेवादार का पूरा नाम" else "Full Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editSevUsername,
                        onValueChange = { editSevUsername = it; editSevErrorMsg = null },
                        label = { Text(if (isHindi) "यूजर आईडी (Unique Username)" else "Unique Username") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editSevPhone,
                        onValueChange = { editSevPhone = it; editSevErrorMsg = null },
                        label = { Text(if (isHindi) "मोबाइल नंबर" else "Mobile Number") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editSevPassword,
                        onValueChange = { editSevPassword = it; editSevErrorMsg = null },
                        label = { Text(if (isHindi) "नया पासवर्ड (छोड़ने पर पुराना रहेगा)" else "New Password (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editSevPin,
                        onValueChange = { editSevPin = it; editSevErrorMsg = null },
                        label = { Text(if (isHindi) "नया 4-अंकीय पिन (छोड़ने पर पुराना रहेगा)" else "New 4-digit PIN (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9)),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFC5E1A5))
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (isHindi) "📷 सेवादार प्रोफाइल फोटो (गैलरी / कैमरा)" else "📷 Sevadar Photo (Gallery / Camera)",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = Color(0xFF2E7D32)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            SacredAvatar(photoUri = editSevPhotoUri, name = editSevName.ifEmpty { target.name }, size = 60.dp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { editSevGalleryLauncher.launch("image/*") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1565C0))
                                ) {
                                    Text(if (isHindi) "🖼️ गैलरी" else "🖼️ Gallery", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = { editSevCameraLauncher.launch(null) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2E7D32))
                                ) {
                                    Text(if (isHindi) "📷 कैमरा" else "📷 Camera", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                if (editSevPhotoUri.isNotBlank()) {
                                    OutlinedButton(
                                        onClick = {
                                            val rotated = DevoteePhotoHelper.rotateSavedPhoto(context, editSevPhotoUri, 90f)
                                            if (rotated.isNotBlank()) {
                                                editSevPhotoUri = rotated
                                                Toast.makeText(context, if (isHindi) "🔄 फोटो 90° सीधी हो गई!" else "🔄 Photo rotated 90°!", Toast.LENGTH_SHORT).show()
                                                scope.launch(Dispatchers.IO) {
                                                    val safeName = "sevadar_" + (editingAdmin?.username ?: System.currentTimeMillis().toString()) + ".jpg"
                                                    val cloudUrl = com.example.shribalajikripadham.data.network.GitHubLiveSyncManager.uploadPhotoToGitHub(context, rotated, safeName)
                                                    if (!cloudUrl.isNullOrBlank()) {
                                                        withContext(Dispatchers.Main) {
                                                            editSevPhotoUri = cloudUrl
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SaffronPrimary)
                                    ) {
                                        Text("🔄 90°", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    OutlinedButton(
                                        onClick = { editSevPhotoUri = "" },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                                    ) {
                                        Text("❌", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(if (isHindi) "अनुमतियाँ प्रबंधित करें:" else "Manage Permissions:", fontWeight = FontWeight.Bold)

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = editSevCanTokens, onCheckedChange = { editSevCanTokens = it })
                        Text(if (isHindi) "टोकन कतार प्रबंधन" else "Manage Token Queue", fontSize = 13.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = editSevCanManualTokens, onCheckedChange = { editSevCanManualTokens = it })
                        Text(if (isHindi) "मैनुअल टोकन जारी करना" else "Issue Manual Tokens", fontSize = 13.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = editSevCanYatra, onCheckedChange = { editSevCanYatra = it })
                        Text(if (isHindi) "बालाजी यात्रा सीट बुकिंग" else "Manage Yatra Seats", fontSize = 13.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = editSevCanExpenses, onCheckedChange = { editSevCanExpenses = it })
                        Text(if (isHindi) "यात्रा खर्च जोड़ना" else "Add Yatra Expenses", fontSize = 13.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = editSevCanLocation, onCheckedChange = { editSevCanLocation = it })
                        Text(if (isHindi) "आश्रम GPS लोकेशन बदलना" else "Change Ashram GPS", fontSize = 13.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = editSevCanNotif, onCheckedChange = { editSevCanNotif = it })
                        Text(if (isHindi) "सूचना प्रसारित करना" else "Send Broadcast Notifications", fontSize = 13.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = editSevCanContent, onCheckedChange = { editSevCanContent = it })
                        Text(if (isHindi) "आश्रम विवरण व उत्सव बदलना" else "Edit Content & Events", fontSize = 13.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = editSevCanPhotos, onCheckedChange = { editSevCanPhotos = it })
                        Text(if (isHindi) "भक्तों की फोटो देखने की अनुमति" else "Allow Viewing Devotee Photos", fontSize = 13.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = editSevCanAnywhere, onCheckedChange = { editSevCanAnywhere = it })
                        Text(
                            text = if (isHindi) "कहीं से भी टोकन जारी करने की अनुमति (Anywhere)" else "Allow Issuing Tokens Anywhere",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaroonAccent
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = editSevCanScanRegister, onCheckedChange = { editSevCanScanRegister = it })
                        Text(
                            text = if (isHindi) "📝 रजिस्टर कॉपी स्कैन व टोकन जारी अधिकार" else "Scan Paper Register Permission",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0D47A1)
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = editSevCanParchas, onCheckedChange = { editSevCanParchas = it })
                        Text(
                            text = if (isHindi) "📜 आश्रम पर्चे प्रबंधन (Super Admin Delegation)" else "Manage Sacred Parchas (Delegation)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE65100)
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = editSevCanCancelTokens, onCheckedChange = { editSevCanCancelTokens = it })
                        Text(if (isHindi) "🚫 टोकन रद्द करने की अनुमति (Cancel Token)" else "Allow Cancel Token", fontSize = 13.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = editSevCanDeleteTokens, onCheckedChange = { editSevCanDeleteTokens = it })
                        Text(if (isHindi) "🗑️ टोकन स्थायी हटाने की अनुमति (Delete Token)" else "Allow Delete Token", fontSize = 13.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = editSevCanCustomTokenNumber, onCheckedChange = { editSevCanCustomTokenNumber = it })
                        Text(
                            text = if (isHindi) "👑 विशेष आरक्षित VIP टोकन (2..20, कोटा: 2) व कस्टम टोकन अधिकार" else "Allow Reserved VIP Slots (2..20, Max 2) & Custom Token #",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaroonPrimary
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = editSevCanExportPdf, onCheckedChange = { editSevCanExportPdf = it })
                        Text(if (isHindi) "📄 आज की टोकन सूची PDF डाउनलोड (Export PDF)" else "Allow Export PDF", fontSize = 13.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = editSevCanArzi, onCheckedChange = { editSevCanArzi = it })
                        Text(if (isHindi) "📦 अर्जी डिब्बा वितरण व लेजर प्रबंधन" else "Manage Arzi Distribution & Ledger", fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val (ok, msg) = repository.updateAdminFullDetails(
                                adminId = target.id,
                                name = editSevName,
                                username = editSevUsername,
                                phone = editSevPhone,
                                password = editSevPassword.ifBlank { null },
                                pin = editSevPin.ifBlank { null },
                                photoUri = editSevPhotoUri,
                                permissions = AdminPermissionsUpdate(
                                    canManageTokens = editSevCanTokens,
                                    canIssueManualTokens = editSevCanManualTokens,
                                    canManageYatra = editSevCanYatra,
                                    canManageExpenses = editSevCanExpenses,
                                    canChangeLocation = editSevCanLocation,
                                    canSendNotifications = editSevCanNotif,
                                    canEditAshramInfo = editSevCanContent,
                                    canViewDevoteePhotos = editSevCanPhotos,
                                    canIssueTokensAnywhere = editSevCanAnywhere,
                                    canScanPaperRegister = editSevCanScanRegister,
                                    canManageParchas = editSevCanParchas,
                                    canCancelTokens = editSevCanCancelTokens,
                                    canDeleteTokens = editSevCanDeleteTokens,
                                    canSetCustomTokenNumber = editSevCanCustomTokenNumber,
                                    canExportPdf = editSevCanExportPdf,
                                    canManageArzi = editSevCanArzi,
                                    isActive = target.isActive
                                )
                            )
                            if (ok) {
                                editingAdmin = null
                                editSevErrorMsg = null
                                refreshData()
                            } else {
                                editSevErrorMsg = msg
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary)
                ) {
                    Text(if (isHindi) "परिवर्तन सुरक्षित करें" else "Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingAdmin = null }) {
                    Text(if (isHindi) "रद्द करें" else "Cancel")
                }
            }
        )
    }

    // --- ADD EVENT DIALOG ---
    if (showAddEventDialog) {
        AlertDialog(
            onDismissRequest = { showAddEventDialog = false },
            title = { Text(if (isHindi) "नया उत्सव / कार्यक्रम जोड़ें" else "Add New Ashram Event", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = evTitleHi,
                        onValueChange = { evTitleHi = it },
                        label = { Text("उत्सव नाम (हिंदी)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = evTitleEn,
                        onValueChange = { evTitleEn = it },
                        label = { Text("Event Title (English)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = evDateHi,
                        onValueChange = { evDateHi = it },
                        label = { Text("तारीख व समय (हिंदी)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = evDateEn,
                        onValueChange = { evDateEn = it },
                        label = { Text("Date & Time (English)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = evDescHi,
                        onValueChange = { evDescHi = it },
                        label = { Text("विस्तृत विवरण (हिंदी)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                    OutlinedTextField(
                        value = evDescEn,
                        onValueChange = { evDescEn = it },
                        label = { Text("Details (English)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            if (evTitleHi.isNotBlank()) {
                                repository.addEvent(
                                    titleHindi = evTitleHi,
                                    titleEnglish = evTitleEn.ifEmpty { evTitleHi },
                                    dateDescHindi = evDateHi,
                                    dateDescEnglish = evDateEn.ifEmpty { evDateHi },
                                    detailsHindi = evDescHi,
                                    detailsEnglish = evDescEn.ifEmpty { evDescHi }
                                )
                                try { repository.publishCurrentSettingsToGitHub(loggedInAdmin?.name ?: "SuperAdmin") } catch (e: Exception) {}
                                showAddEventDialog = false
                                evTitleHi = ""
                                evTitleEn = ""
                                evDateHi = ""
                                evDateEn = ""
                                evDescHi = ""
                                evDescEn = ""
                                refreshData()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary)
                ) {
                    Text(if (isHindi) "जोड़ें" else "Add Event")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddEventDialog = false }) {
                    Text(if (isHindi) "रद्द करें" else "Cancel")
                }
            }
        )
    }
}

// ==========================================
// SUB-TAB COMPOSABLES
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TokenQueueTab(
    isHindi: Boolean,
    settings: AshramSettings,
    todayTokens: List<Token>,
    canViewPhotos: Boolean = false,
    canCancelTokens: Boolean = true,
    canDeleteTokens: Boolean = true,
    canExportPdf: Boolean = true,
    onUpdateRunningToken: (Int) -> Unit,
    onUpdateStatus: (Long, TokenStatus) -> Unit,
    onToggleDarshan: (Long, Boolean) -> Unit,
    onCancelToken: ((Long) -> Unit)? = null,
    onDeleteToken: ((Long) -> Unit)? = null,
    onSyncFromCloud: (() -> Unit)? = null,
    onSyncFromGoogleSheet: (() -> Unit)? = null,
    onUpdateVoicePreset: ((String) -> Unit)? = null,
    onFillReservedToken: ((Int, String, String, String) -> Unit)? = null,
    onRejectReservedToken: ((Int) -> Unit)? = null,
    onPushAllTokensToGitHub: (() -> Unit)? = null,
    onNavigateToHallDisplay: () -> Unit = {}
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val syncManager = com.example.shribalajikripadham.data.network.GoogleSheetTokenSyncManager
    var showSheetConfigDialog by remember { mutableStateOf(false) }
    var sheetWebhookUrlInput by remember { mutableStateOf(syncManager.getWebhookUrl(context)) }
    var isSyncingSheet by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedDistanceFilter by remember { mutableStateOf(DistanceFilter.ALL) }
    var selectedSortOrder by remember { mutableStateOf(TokenSortOrder.TOKEN_NUMBER) }
    var isExportingPdf by remember { mutableStateOf(false) }
    var isVoiceMuted by remember { mutableStateOf(AshramVoiceAnnouncementManager.isMuted(context)) }
    var tokenToCancel by remember { mutableStateOf<Token?>(null) }
    var tokenToDelete by remember { mutableStateOf<Token?>(null) }
    var zoomedPhotoToken by remember { mutableStateOf<Token?>(null) }
    var reservedTokenToFill by remember { mutableStateOf<Token?>(null) }
    var fillName by remember { mutableStateOf("") }
    var fillPhone by remember { mutableStateOf("") }
    var fillCity by remember { mutableStateOf("डूँगरा जाट (स्थानीय)") }

    val filteredTokens = remember(todayTokens, searchQuery, selectedDistanceFilter, selectedSortOrder, settings) {
        TokenDistanceHelper.filterAndSortTokens(
            tokens = todayTokens,
            ashramLat = settings.latitude,
            ashramLong = settings.longitude,
            searchQuery = searchQuery,
            distanceFilter = selectedDistanceFilter,
            sortOrder = selectedSortOrder
        )
    }

    val totalCount = todayTokens.size
    val completedCount = todayTokens.count { it.isDarshanCompleted }
    val pendingCount = totalCount - completedCount

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Current Calling Token
        item {
            val currentCalledDevotee = todayTokens.find { it.tokenNumber == settings.runningTokenNumber }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(if (isHindi) "वर्तमान बुलाया गया नंबर" else "Currently Active Token", fontSize = 14.sp, color = Color.Gray)
                    Text("#${settings.runningTokenNumber}", fontSize = 48.sp, fontWeight = FontWeight.Bold, color = SaffronPrimary)

                    if (currentCalledDevotee != null) {
                        Surface(
                            color = Color(0xFFFFF3E0),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, SaffronPrimary.copy(alpha = 0.5f)),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("👤 ", fontSize = 14.sp)
                                Text(
                                    text = "${currentCalledDevotee.patientName} (${currentCalledDevotee.city})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaroonPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val prevNum = (settings.runningTokenNumber - 1).coerceAtLeast(1)
                                onUpdateRunningToken(prevNum)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("-1 पिछला", color = Color.Black)
                        }
                        Button(
                            onClick = {
                                val nextNum = settings.runningTokenNumber + 1
                                onUpdateRunningToken(nextNum)
                                val nextDev = todayTokens.find { it.tokenNumber == nextNum }
                                AshramVoiceAnnouncementManager.announceNextToken(
                                    context = context,
                                    tokenNumber = nextNum,
                                    devoteeName = nextDev?.patientName ?: "",
                                    city = nextDev?.city ?: ""
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                            modifier = Modifier.weight(1.3f)
                        ) {
                            Text("🔊 +1 अगला टोकन", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                val dev = todayTokens.find { it.tokenNumber == settings.runningTokenNumber }
                                AshramVoiceAnnouncementManager.announceNextToken(
                                    context = context,
                                    tokenNumber = settings.runningTokenNumber,
                                    devoteeName = dev?.patientName ?: "",
                                    city = dev?.city ?: ""
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaroonPrimary)
                        ) {
                            Text(if (isHindi) "📢 पुनः बोलें" else "📢 Repeat Call", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                val newMuted = !isVoiceMuted
                                AshramVoiceAnnouncementManager.setMuted(context, newMuted)
                                isVoiceMuted = newMuted
                                val msg = if (newMuted)
                                    (if (isHindi) "🔇 आवाज म्यूट कर दी गई" else "Voice muted")
                                else
                                    (if (isHindi) "🔊 आवाज चालू कर दी गई" else "Voice enabled")
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (isVoiceMuted) Color.Red else Color(0xFF2E7D32)
                            )
                        ) {
                            Text(
                                text = if (isVoiceMuted) (if (isHindi) "🔇 आवाज बंद" else "🔇 Muted") else (if (isHindi) "🔊 आवाज चालू" else "🔊 Voice ON"),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onNavigateToHallDisplay,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("📺 स्मार्ट टीवी व आश्रम हॉल डिस्प्ले मोड (Open TV Board)", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        // 1.1 Inline Token Queue Voice Selection & Audio Preview Studio (Genuine Human & Live Recorded Voices)
        item {
            var selectedVoiceId by remember(settings.tokenVoicePreset) {
                mutableStateOf(settings.tokenVoicePreset.ifBlank { AshramVoiceAnnouncementManager.getSelectedVoicePreset(context) })
            }
            var isRecordingAudio by remember { mutableStateOf(false) }
            var hasRecording by remember { mutableStateOf(AshramVoiceAnnouncementManager.hasCustomRecording(context)) }

            val recordAudioLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                if (isGranted) {
                    val started = AshramVoiceAnnouncementManager.startCustomRecording(context)
                    if (started) {
                        isRecordingAudio = true
                        Toast.makeText(context, "🎙️ रिकॉर्डिंग शुरू... स्पष्ट बोलें", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "रिकॉर्डिंग शुरू करने में विफल", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "ऑडियो रिकॉर्डिंग हेतु माइक्रोफ़ोन अनुमति आवश्यक है", Toast.LENGTH_SHORT).show()
                }
            }

            val allVoices = remember { AshramVoiceAnnouncementManager.AVAILABLE_VOICE_PRESETS }
            val activeVoiceInfo = remember(selectedVoiceId, allVoices) {
                allVoices.find { it.id == selectedVoiceId } ?: allVoices[0]
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFDFBF7)),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, SaffronPrimary.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🎙️", fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (isHindi) "टोकन घोषणा आवाज़ (प्राकृतिक HD एवं लाइव स्टूडियो)" else "Token Voice (Natural HD & Live Studio)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaroonPrimary
                                )
                                Text(
                                    text = if (isHindi) "100% वास्तविक इंसानी स्वर एवं लाइव माइक रिकॉर्डिंग" else "100% Real human audio & live mic recording",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                        Surface(
                            color = SaffronPrimary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "${activeVoiceInfo.icon} ${activeVoiceInfo.category}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaroonPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Live In-App Voice Recording Studio Box
                    Surface(
                        color = if (isRecordingAudio) Color(0xFFFFEBEE) else Color(0xFFF1F8E9),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, if (isRecordingAudio) Color(0xFFD32F2F) else Color(0xFF81C784)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(if (isRecordingAudio) "🔴" else "🎙️", fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isRecordingAudio) "रिकॉर्डिंग चल रही है..." else "लाइव माइक रिकॉर्डिंग स्टूडियो",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isRecordingAudio) Color(0xFFC62828) else Color(0xFF2E7D32)
                                    )
                                }
                                if (hasRecording && !isRecordingAudio) {
                                    Surface(color = Color(0xFFE8F5E9), shape = RoundedCornerShape(4.dp)) {
                                        Text("✓ रिकॉर्डेड उपलब्ध", fontSize = 10.sp, color = Color(0xFF2E7D32), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (isRecordingAudio) {
                                    Button(
                                        onClick = {
                                            AshramVoiceAnnouncementManager.stopCustomRecording(context)
                                            isRecordingAudio = false
                                            hasRecording = true
                                            selectedVoiceId = AshramVoiceAnnouncementManager.PRESET_CUSTOM_RECORDED
                                            AshramVoiceAnnouncementManager.setVoicePreset(context, selectedVoiceId)
                                            onUpdateVoicePreset?.invoke(selectedVoiceId)
                                            Toast.makeText(context, "✓ लाइव आवाज़ सेव व लागू हो गई!", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("⏹️ रिकॉर्डिंग रोकें व सेव करें", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = {
                                            recordAudioLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                        },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2E7D32)),
                                        border = BorderStroke(1.dp, Color(0xFF4CAF50)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("🎙️ अपनी आवाज़ रिकॉर्ड करें", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    if (hasRecording) {
                                        OutlinedButton(
                                            onClick = {
                                                AshramVoiceAnnouncementManager.playCustomRecording(context) { }
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp)
                                        ) {
                                            Text("▶️ सुनें", fontSize = 11.sp)
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                AshramVoiceAnnouncementManager.deleteCustomRecording(context)
                                                hasRecording = false
                                                if (selectedVoiceId == AshramVoiceAnnouncementManager.PRESET_CUSTOM_RECORDED) {
                                                    selectedVoiceId = AshramVoiceAnnouncementManager.PRESET_NATURAL_MALE
                                                    AshramVoiceAnnouncementManager.setVoicePreset(context, selectedVoiceId)
                                                    onUpdateVoicePreset?.invoke(selectedVoiceId)
                                                }
                                                Toast.makeText(context, "रिकॉर्डिंग हटा दी गई", Toast.LENGTH_SHORT).show()
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                                            border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f)),
                                            contentPadding = PaddingValues(horizontal = 8.dp)
                                        ) {
                                            Text("🗑️", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Preset Selection Cards
                    Text("सक्रिय उद्घोषणा स्वर चुनें:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaroonPrimary)
                    Spacer(modifier = Modifier.height(6.dp))

                    allVoices.forEach { voice ->
                        val isSelected = selectedVoiceId == voice.id
                        Surface(
                            onClick = {
                                selectedVoiceId = voice.id
                                AshramVoiceAnnouncementManager.setVoicePreset(context, voice.id)
                                onUpdateVoicePreset?.invoke(voice.id)
                                Toast.makeText(context, "✓ आवाज़ चुनी गई: ${voice.nameHindi}", Toast.LENGTH_SHORT).show()
                            },
                            color = if (isSelected) Color(0xFFFFF3E0) else Color.White,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (isSelected) SaffronPrimary else Color(0xFFE0E0E0)),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(voice.icon, fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isHindi) voice.nameHindi else voice.nameEnglish,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 12.sp,
                                        color = if (isSelected) MaroonPrimary else Color.Black
                                    )
                                    Text(
                                        text = voice.description,
                                        fontSize = 10.sp,
                                        color = Color.Gray,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                OutlinedButton(
                                    onClick = {
                                        AshramVoiceAnnouncementManager.testVoice(context, voice.id)
                                    },
                                    shape = RoundedCornerShape(6.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SaffronPrimary),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("▶️ टेस्ट", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            AshramVoiceAnnouncementManager.setVoicePreset(context, selectedVoiceId)
                            onUpdateVoicePreset?.invoke(selectedVoiceId)
                            Toast.makeText(
                                context,
                                if (isHindi) "✓ घोषणा आवाज़ सुरक्षित व लागू हो गई!" else "✓ Announcement voice applied!",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary)
                    ) {
                        Text(if (isHindi) "✅ यह आवाज़ लागू करें" else "✅ Apply Voice", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        // 2. Darshan Statistics & PDF Export Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9FB)),
                border = BorderStroke(1.dp, Color(0xFFE0E0E0))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isHindi) "📊 आज के दर्शन आंकड़े" else "📊 Today's Darshan Stats",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaroonPrimary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                color = Color(0xFFE8F5E9),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFF4CAF50))
                            ) {
                                Text(
                                    "✓ संपन्न: $completedCount",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1B5E20),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Surface(
                                color = Color(0xFFFFF3E0),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFFFFB74D))
                            ) {
                                Text(
                                    "⏳ शेष: $pendingCount",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE65100),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (canExportPdf) {
                        Button(
                            onClick = {
                                if (todayTokens.isEmpty()) {
                                    Toast.makeText(context, if (isHindi) "आज कोई टोकन नहीं है" else "No tokens today", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                isExportingPdf = true
                                try {
                                    val pdfFile = TokenPdfExporter.exportTokensToPdf(context, todayTokens, settings)
                                    TokenPdfExporter.openOrSharePdf(context, pdfFile)
                                    Toast.makeText(context, if (isHindi) "PDF रिपोर्ट तैयार है!" else "PDF report ready!", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "PDF Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                } finally {
                                    isExportingPdf = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (isExportingPdf)
                                    (if (isHindi) "PDF बनाई जा रही है..." else "Generating PDF...")
                                else
                                    ("📄 " + (if (isHindi) "आज की टोकन सूची PDF डाउनलोड / शेयर करें" else "Export Today's Token List to PDF")),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Button(
                        onClick = {
                            if (todayTokens.isEmpty()) {
                                Toast.makeText(context, if (isHindi) "आज कोई टोकन नहीं है" else "No tokens today", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            try {
                                TokenCsvExporter.shareTokensCsv(context, filteredTokens, settings.darbarDate)
                                Toast.makeText(context, if (isHindi) "एक्सेल / CSV रिपोर्ट तैयार है!" else "Excel / CSV report ready!", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "CSV Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "📊 " + (if (isHindi) "आज की टोकन सूची एक्सेल / CSV डाउनलोड करें" else "Export Today's Token List to Excel / CSV"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // ☁️ UNIVERSAL 100% ONLINE CLOUD TOKEN SYNC CARD (GitHub + Google Sheets)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF81C784)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("☁️", fontSize = 22.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = if (isHindi) "लाइव क्लाउड टोकन सिंक (100% ऑनलाइन)" else "Live Cloud Token Sync (100% Online)",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = Color(0xFF1B5E20)
                                        )
                                        Text(
                                            text = if (isHindi) "🟢 सेंट्रल क्लाउड सिंक सक्रिय (भक्तों के फोन से स्वतः जुड़ेगा)" else "🟢 Central Cloud Active (Auto Devotee Sync)",
                                            fontSize = 11.sp,
                                            color = Color(0xFF2E7D32),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                IconButton(onClick = { showSheetConfigDialog = true }) {
                                    Text("⚙️", fontSize = 18.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val syncAction = onSyncFromCloud ?: onSyncFromGoogleSheet
                                        if (syncAction != null) {
                                            isSyncingSheet = true
                                            syncAction()
                                            isSyncingSheet = false
                                        }
                                    },
                                    enabled = !isSyncingSheet,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = if (isSyncingSheet)
                                            (if (isHindi) "सिंक हो रहा है..." else "Syncing...")
                                        else
                                            ("🔄 " + (if (isHindi) "क्लाउड से सिंक करें" else "Sync Cloud Tokens")),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }

                                OutlinedButton(
                                    onClick = { showSheetConfigDialog = true },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1B5E20)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "⚙️ " + (if (isHindi) "शीट सेटिंग्स" else "Sheet Settings"),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            if (onPushAllTokensToGitHub != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { onPushAllTokensToGitHub() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "☁️ " + (if (isHindi) "GitHub पर सभी टोकन बैकअप पुश करें" else "Push All Tokens Backup to GitHub"),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Search & Distance Filter Bar
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(if (isHindi) "नाम, फोन नंबर या शहर से खोजें..." else "Search devotee by name, phone or city...", color = Color(0xFF757575)) },
                    leadingIcon = { Text("🔍") },
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color(0xFF111111), fontSize = 15.sp, fontWeight = FontWeight.Medium),
                    colors = sacredOutlinedTextFieldColors(),
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = {
                                    searchQuery = ""
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                }) {
                                    Text("✕", color = Color(0xFF111111), fontWeight = FontWeight.Bold)
                                }
                            }
                            IconButton(onClick = {
                                keyboardController?.hide()
                                focusManager.clearFocus()
                            }) {
                                Text("⌨️⬇️", fontSize = 13.sp)
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        }
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                if (searchQuery.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isHindi) "खोज परिणाम: ${filteredTokens.size} भक्त मिले" else "Found: ${filteredTokens.size} devotees",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaroonPrimary
                        )
                        OutlinedButton(
                            onClick = {
                                keyboardController?.hide()
                                focusManager.clearFocus()
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(26.dp)
                        ) {
                            Text(if (isHindi) "कीपैड बन्द करें ⬇️" else "Hide Keypad ⬇️", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Distance Filter Chips Row
                Text(
                    text = if (isHindi) "दूरी के आधार पर फ़िल्टर (आश्रम से दूरी):" else "Filter by Distance from Ashram:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.DarkGray
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DistanceFilter.values().forEach { df ->
                        FilterChip(
                            selected = selectedDistanceFilter == df,
                            onClick = { selectedDistanceFilter = df },
                            label = {
                                Text(
                                    when (df) {
                                        DistanceFilter.ALL -> if (isHindi) "सभी (${todayTokens.size})" else "All"
                                        DistanceFilter.WITHIN_10_KM -> if (isHindi) "📍 <10 km" else "<10 km"
                                        DistanceFilter.BETWEEN_10_AND_50_KM -> if (isHindi) "🚗 10-50 km" else "10-50 km"
                                        DistanceFilter.BEYOND_50_KM -> if (isHindi) "🛣️ >50 km" else ">50 km"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = if (selectedDistanceFilter == df) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SaffronPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                // Sort Order Chips Row
                Text(
                    text = if (isHindi) "क्रमबद्ध करें (Sort):" else "Sort by:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.DarkGray
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TokenSortOrder.values().forEach { so ->
                        FilterChip(
                            selected = selectedSortOrder == so,
                            onClick = { selectedSortOrder = so },
                            label = {
                                Text(
                                    when (so) {
                                        TokenSortOrder.TOKEN_NUMBER -> if (isHindi) "टोकन #" else "Token #"
                                        TokenSortOrder.DISTANCE_DESC -> if (isHindi) "🛣️ दूर पहले" else "Far First"
                                        TokenSortOrder.DISTANCE_ASC -> if (isHindi) "📍 पास पहले" else "Near First"
                                        TokenSortOrder.DARSHAN_PENDING_FIRST -> if (isHindi) "⏳ शेष पहले" else "Pending"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = if (selectedSortOrder == so) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaroonPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
        }

        // 4. Header with token count
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isHindi) "पंजीकृत भक्त (${filteredTokens.size}/${todayTokens.size})" else "Devotees (${filteredTokens.size}/${todayTokens.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaroonPrimary
                )
                if (searchQuery.isNotEmpty() || selectedDistanceFilter != DistanceFilter.ALL) {
                    TextButton(onClick = {
                        searchQuery = ""
                        selectedDistanceFilter = DistanceFilter.ALL
                    }) {
                        Text(if (isHindi) "फ़िल्टर हटाएं" else "Reset", fontSize = 12.sp, color = SaffronPrimary)
                    }
                }
            }
        }

        // 5. Line-by-line Devotee Token Cards
        if (filteredTokens.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            if (isHindi) "कोई टोकन नहीं मिला" else "No tokens found",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        items(filteredTokens) { token ->
            val distBadge = TokenDistanceHelper.formatDistance(token, settings.latitude, settings.longitude)
            val isReservedSlot = token.id < 0L || token.patientName.contains("व्यवस्थापक आरक्षित") || (token.patientName.contains("प्रतीक्षारत") && token.phoneNumber.isEmpty()) || token.patientName.contains("व्यवस्थापक द्वारा निरस्त")
            val isCancelledReserved = isReservedSlot && (token.status == TokenStatus.CANCELLED || token.patientName.contains("निरस्त"))

            if (isReservedSlot) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCancelledReserved) Color(0xFFFFEBEE) else Color(0xFFFFF8E1)
                    ),
                    border = BorderStroke(
                        1.5.dp,
                        if (isCancelledReserved) Color(0xFFEF9A9A) else Color(0xFFFFB300)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isCancelledReserved) Color(0xFFC62828) else Color(0xFFE65100)
                                ) {
                                    Text(
                                        text = "👑 #${token.tokenNumber}",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (isCancelledReserved) "व्यवस्थापक द्वारा निरस्त / छोड़ा गया" else "व्यवस्थापक आरक्षित (VIP कोटा)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = if (isCancelledReserved) Color(0xFFC62828) else Color(0xFFBF360C)
                                    )
                                    Text(
                                        text = if (isCancelledReserved) "यह आरक्षित टोकन आज निरस्त/खाली है" else "व्यवस्थापक आरक्षित - प्रतीक्षारत / मरीज अभी उपस्थित नहीं है",
                                        fontSize = 12.sp,
                                        color = Color.DarkGray
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!isCancelledReserved) {
                                Button(
                                    onClick = {
                                        fillName = ""
                                        fillPhone = ""
                                        fillCity = "डूँगरा जाट (स्थानीय)"
                                        reservedTokenToFill = token
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.weight(1.2f)
                                ) {
                                    Text("🟢 मरीज आया / भरें", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        onRejectReservedToken?.invoke(token.tokenNumber)
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828)),
                                    border = BorderStroke(1.dp, Color(0xFFEF9A9A)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("🔴 निरस्त / छोड़ें", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                OutlinedButton(
                                    onClick = {
                                        fillName = ""
                                        fillPhone = ""
                                        fillCity = "डूँगरा जाट (स्थानीय)"
                                        reservedTokenToFill = token
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2E7D32)),
                                    border = BorderStroke(1.dp, Color(0xFF81C784)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.weight(1.2f)
                                ) {
                                    Text("🔄 पुनः मरीज विवरण भरें", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    AshramVoiceAnnouncementManager.announceNextToken(
                                        context = context,
                                        tokenNumber = token.tokenNumber,
                                        devoteeName = "व्यवस्थापक आरक्षित",
                                        city = ""
                                    )
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE65100)),
                                border = BorderStroke(1.dp, Color(0xFFFFB74D))
                            ) {
                                Text("📢 माइक", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        token.isDarshanCompleted -> Color(0xFFF1F8E9)
                        token.tokenNumber == settings.runningTokenNumber -> Color(0xFFFFF8E1)
                        else -> Color.White
                    }
                ),
                border = BorderStroke(
                    1.dp,
                    when {
                        token.isDarshanCompleted -> Color(0xFF81C784)
                        token.tokenNumber == settings.runningTokenNumber -> SaffronPrimary
                        else -> Color(0xFFE0E0E0)
                    }
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Row 1: Token # + Name + Distance Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (token.isDarshanCompleted) Color(0xFF2E7D32) else SaffronPrimary
                            ) {
                                Text(
                                    text = "#${token.tokenNumber}",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = token.patientName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFF1E293B)
                            )
                        }

                        // Distance Badge
                        Surface(
                            color = Color(0xFFFFF3E0),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, Color(0xFFFFB74D))
                        ) {
                            Text(
                                text = distBadge,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFE65100),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Row 2: Phone + Resident Location + Total Distance + Registered by
                    Text(
                        text = "📞 ${token.phoneNumber}  |  🏠 निवासी: ${token.originAddress.ifEmpty { token.city }}  |  दूरी: ${if (token.distanceKm >= 0f) "%.1f km".format(token.distanceKm) else distBadge}",
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )

                    // Registration Attribution Badge
                    val regBadgeText = when {
                        token.registeredBy.startsWith("SUPER_ADMIN") -> "👑 सुपर एडमिन (अंकित चौधरी)"
                        token.registeredBy.startsWith("ADMIN") -> "🏢 एडमिन (${token.registeredBy.removePrefix("ADMIN").trim('(', ')', ' ')})"
                        token.registeredBy.startsWith("DESK") -> "🏢 एडमिन (${token.registeredBy.removePrefix("DESK_")})"
                        else -> "📱 स्वयं (Self)"
                    }
                    val regBadgeColor = when {
                        token.registeredBy.startsWith("SUPER_ADMIN") -> Color(0xFFE65100)
                        token.registeredBy.startsWith("ADMIN") || token.registeredBy.startsWith("DESK") -> Color(0xFF1565C0)
                        else -> Color(0xFF2E7D32)
                    }
                    Surface(
                        color = regBadgeColor.copy(alpha = 0.10f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, regBadgeColor.copy(alpha = 0.35f)),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = "पंजीकरण: $regBadgeText",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = regBadgeColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Devotee Photo with tap-to-zoom
                    if (token.photoUri.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = Color(0xFFE8F5E9),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFFA5D6A7)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { zoomedPhotoToken = token }
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(1.5.dp, Color(0xFF2E7D32), RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val bmp = remember(token.photoUri) { DevoteePhotoHelper.loadBitmap(context, token.photoUri) }
                                        if (bmp != null) {
                                            Image(
                                                bitmap = bmp.asImageBitmap(),
                                                contentDescription = token.patientName,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            SacredAvatar(photoUri = token.photoUri, name = token.patientName, size = 42.dp)
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "📸 " + (if (isHindi) "सत्यापित फोटो संलग्न" else "Verified Photo Attached"),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1B5E20)
                                        )
                                        Text(
                                            text = if (isHindi) "टैप करें: बड़ा फोटो देखें (Zoom)" else "Tap to Zoom Full Screen",
                                            fontSize = 10.sp,
                                            color = Color(0xFF2E7D32)
                                        )
                                    }
                                }
                                Surface(
                                    color = Color(0xFF2E7D32),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "🔍 ZOOM",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isHindi) "📷 बिना फोटो (मैनुअल काउंटर टोकन)" else "📷 No Photo (Desk Token)",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = Color(0xFFEEEEEE))
                    Spacer(modifier = Modifier.height(8.dp))

                    // Row 3: Status Chip + 1-Tap Single Receipt PDF + 1-Tap Darshan Completed Checkmark Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = when (token.status) {
                                    TokenStatus.WAITING, TokenStatus.PENDING -> AmberGold
                                    TokenStatus.CALLED -> SaffronPrimary
                                    TokenStatus.COMPLETED -> Color(0xFF2E7D32)
                                    TokenStatus.CANCELLED -> Color.Red
                                }
                            ) {
                                Text(
                                    text = when (token.status) {
                                        TokenStatus.WAITING, TokenStatus.PENDING -> if (isHindi) "प्रतीक्षारत" else "Waiting"
                                        TokenStatus.CALLED -> if (isHindi) "बुलाया गया" else "Called"
                                        TokenStatus.COMPLETED -> if (isHindi) "दर्शन पूर्ण" else "Completed"
                                        TokenStatus.CANCELLED -> if (isHindi) "रद्द" else "Cancelled"
                                    },
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            OutlinedButton(
                                onClick = {
                                    TokenPdfExporter.shareSingleTokenReceipt(context, token, settings)
                                },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("📄 रसीद PDF", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            val printScope = rememberCoroutineScope()
                            OutlinedButton(
                                onClick = {
                                    printScope.launch {
                                        val res = com.example.shribalajikripadham.hardware.BluetoothThermalPrinterHelper.printTokenSlip(
                                            context = context,
                                            token = token,
                                            ashramName = settings.ashramName
                                        )
                                        Toast.makeText(context, res.second, Toast.LENGTH_SHORT).show()
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("🖨️ POS प्रिंट", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            OutlinedButton(
                                onClick = {
                                    AshramVoiceAnnouncementManager.announceNextToken(
                                        context = context,
                                        tokenNumber = token.tokenNumber,
                                        devoteeName = token.patientName,
                                        city = token.city
                                    )
                                },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(28.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE65100))
                            ) {
                                Text("📢 बोलें", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // 1-Tap Checkmark Tick Button ("दिखा लिया")
                        FilterChip(
                            selected = token.isDarshanCompleted,
                            onClick = { onToggleDarshan(token.id, !token.isDarshanCompleted) },
                            label = {
                                Text(
                                    text = if (token.isDarshanCompleted)
                                        (if (isHindi) "✓ दिखा लिया (दर्शन संपन्न)" else "✓ Darshan Done")
                                    else
                                        (if (isHindi) "☐ दर्शन शेष (दिखाना बाकी)" else "☐ Darshan Pending"),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (token.isDarshanCompleted) Color(0xFF1B5E20) else Color.DarkGray
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFC8E6C9),
                                containerColor = Color(0xFFEEEEEE)
                            ),
                            border = BorderStroke(
                                1.5.dp,
                                if (token.isDarshanCompleted) Color(0xFF2E7D32) else Color.LightGray
                            )
                        )
                    }

                    // Row 4: Super Admin & Admin Token Cancellation & Removal Action Buttons (Permission Controlled)
                    if ((canCancelTokens && onCancelToken != null) || (canDeleteTokens && onDeleteToken != null)) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (canCancelTokens && onCancelToken != null && token.status != TokenStatus.CANCELLED) {
                                OutlinedButton(
                                    onClick = { tokenToCancel = token },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828)),
                                    border = BorderStroke(1.dp, Color(0xFFFFCDD2)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("🚫 " + (if (isHindi) "रद्द करें" else "Cancel"), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            if (canDeleteTokens && onDeleteToken != null) {
                                Button(
                                    onClick = { tokenToDelete = token },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("🗑️ " + (if (isHindi) "हटाएं" else "Delete"), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
            }
        }
    }

    // Dialog to fill devotee details in reserved VIP slot
    if (reservedTokenToFill != null) {
        val rToken = reservedTokenToFill!!
        AlertDialog(
            onDismissRequest = { reservedTokenToFill = null },
            title = {
                Text(
                    text = "👑 आरक्षित टोकन #${rToken.tokenNumber} - विवरण भरें",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaroonPrimary
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                    Text(
                        text = "व्यवस्थापक आरक्षित स्लॉट में उपस्थित मरीज/भक्त का विवरण दर्ज करें:",
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = fillName,
                        onValueChange = { fillName = it },
                        label = { Text("भक्त/मरीज का नाम *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = fillPhone,
                        onValueChange = { fillPhone = it },
                        label = { Text("मोबाइल नंबर") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = fillCity,
                        onValueChange = { fillCity = it },
                        label = { Text("शहर / ग्राम / पता") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (fillName.isBlank()) {
                            Toast.makeText(context, "कृपया मरीज का नाम दर्ज करें", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        onFillReservedToken?.invoke(rToken.tokenNumber, fillName.trim(), fillPhone.trim(), fillCity.trim())
                        reservedTokenToFill = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Text("✅ सुरक्षित करें व जारी करें", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { reservedTokenToFill = null }) {
                    Text("रद्द करें", color = Color.Gray)
                }
            }
        )
    }

    // Full-Screen Devotee Photo Zoom Dialog for Gate Verification
    if (zoomedPhotoToken != null) {
        val zToken = zoomedPhotoToken!!
        Dialog(onDismissRequest = { zoomedPhotoToken = null }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "टोकन #${zToken.tokenNumber} फोटो सत्यापन",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaroonPrimary
                            )
                            Text(
                                text = if (isHindi) "दरबार प्रवेश द्वार सत्यापन" else "Temple Entry Gate Verification",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                        IconButton(onClick = { zoomedPhotoToken = null }) {
                            Text("✕", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val bigBmp = remember(zToken.photoUri) { DevoteePhotoHelper.loadBitmap(context, zToken.photoUri) }
                    if (bigBmp != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(280.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .border(2.dp, SaffronPrimary, RoundedCornerShape(14.dp))
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = bigBmp.asImageBitmap(),
                                contentDescription = zToken.patientName,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFF5F5F5)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("फोटो लोड नहीं हो सकी", color = Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = zToken.patientName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = "📞 ${zToken.phoneNumber}  |  🏠 ${zToken.originAddress.ifEmpty { zToken.city }}",
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )
                    Text(
                        text = "पंजीकरण: ${zToken.registeredBy}  |  दूरी: ${if (zToken.distanceKm >= 0f) "%.1f km".format(zToken.distanceKm) else "आश्रम परिसर"}",
                        fontSize = 11.sp,
                        color = SaffronDark,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { zoomedPhotoToken = null },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("✓ सत्यापन पूर्ण (Close)", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }

    if (showSheetConfigDialog) {
        AlertDialog(
            onDismissRequest = { showSheetConfigDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🔗 ", fontSize = 20.sp)
                    Text(
                        text = if (isHindi) "Google Sheets वेबहुक सेटिंग्स" else "Google Sheets Webhook Config",
                        fontWeight = FontWeight.Bold,
                        color = MaroonPrimary
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isHindi)
                            "गूगल शीट्स वेबहुक URL दर्ज करें ताकि सभी टोकन, चेहरे का डाटा और डिवाइस टेलीमेट्री लाइव गूगल शीट पर सिंक हो सकें।"
                        else
                            "Enter Google Apps Script Webhook URL to enable live sync of tokens, face profiles, and device telemetry to Google Sheets.",
                        fontSize = 13.sp,
                        color = Color.DarkGray
                    )
                    OutlinedTextField(
                        value = sheetWebhookUrlInput,
                        onValueChange = { sheetWebhookUrlInput = it },
                        label = { Text("Webhook URL") },
                        placeholder = { Text("https://script.google.com/macros/s/...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (syncManager.isConfigured(context)) {
                        Text(
                            text = if (isHindi) "✅ वर्तमान स्थिति: सक्रिय एवं कनेक्टेड" else "✅ Current Status: Active & Connected",
                            fontSize = 12.sp,
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Text(
                            text = if (isHindi) "⚠️ वर्तमान स्थिति: कनेक्ट नहीं है" else "⚠️ Current Status: Not Connected",
                            fontSize = 12.sp,
                            color = Color(0xFFD32F2F),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        syncManager.saveWebhookUrl(context, sheetWebhookUrlInput)
                        showSheetConfigDialog = false
                        Toast.makeText(
                            context,
                            if (isHindi) "वेबहुक URL सफलतापूर्वक सेव किया गया!" else "Webhook URL saved successfully!",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary)
                ) {
                    Text(if (isHindi) "सुरक्षित करें (Save)" else "Save", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSheetConfigDialog = false }) {
                    Text(if (isHindi) "रद्द करें" else "Cancel", color = Color.Gray)
                }
            }
        )
    }

    // Confirmation Dialog for Token Cancellation
    if (tokenToCancel != null) {
        val t = tokenToCancel!!
        AlertDialog(
            onDismissRequest = { tokenToCancel = null },
            title = {
                Text(
                    text = if (isHindi) "टोकन #${t.tokenNumber} रद्द (Cancel) करें?" else "Cancel Token #${t.tokenNumber}?",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFC62828)
                )
            },
            text = {
                Text(
                    text = if (isHindi)
                        "क्या आप भक्त ${t.patientName} (फोन: ${t.phoneNumber}) का टोकन #${t.tokenNumber} रद्द करना चाहते हैं? इसकी स्थिति 'रद्द' (CANCELLED) में बदल दी जाएगी।"
                    else
                        "Cancel Token #${t.tokenNumber} for ${t.patientName}? The status will be marked as CANCELLED."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCancelToken?.invoke(t.id)
                        tokenToCancel = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                ) {
                    Text(if (isHindi) "हाँ, रद्द करें" else "Yes, Cancel", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { tokenToCancel = null }) {
                    Text(if (isHindi) "वापस जाएं" else "Back")
                }
            }
        )
    }

    // Confirmation Dialog for Permanent Token Deletion
    if (tokenToDelete != null) {
        val t = tokenToDelete!!
        AlertDialog(
            onDismissRequest = { tokenToDelete = null },
            title = {
                Text(
                    text = if (isHindi) "टोकन #${t.tokenNumber} को हमेशा के लिए हटाएं?" else "Delete Token #${t.tokenNumber}?",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD32F2F)
                )
            },
            text = {
                Text(
                    text = if (isHindi)
                        "क्या आप भक्त ${t.patientName} (फोन: ${t.phoneNumber}) का टोकन #${t.tokenNumber} डेटाबेस से पूर्णतः हटाना (Delete) चाहते हैं? यह क्रिया वापस नहीं ली जा सकती।"
                    else
                        "Permanently delete Token #${t.tokenNumber} for ${t.patientName}? This action cannot be reversed."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteToken?.invoke(t.id)
                        tokenToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text(if (isHindi) "हाँ, हमेशा के लिए हटाएं" else "Yes, Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { tokenToDelete = null }) {
                    Text(if (isHindi) "रद्द करें" else "Cancel")
                }
            }
        )
    }
}

@Composable
fun ManualTokenTab(
    isHindi: Boolean,
    admin: Admin,
    settings: AshramSettings,
    repository: AshramRepository,
    canBypassGeofence: Boolean,
    attribution: String,
    onTokenIssued: () -> Unit,
    onNavigateToScanRegister: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    var searchInput by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<DevoteeFaceProfile>>(emptyList()) }

    var formCustomTokenNumber by remember { mutableStateOf("") }
    var formName by remember { mutableStateOf("") }
    var formPhone by remember { mutableStateOf("") }
    var formCity by remember { mutableStateOf("डूँगरा जाट (स्थानीय)") }
    var formPhotoUri by remember { mutableStateOf("") }
    var formCapturedBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var nameSuggestions by remember { mutableStateOf<List<DevoteeFaceProfile>>(emptyList()) }

    var successMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isIssuing by remember { mutableStateOf(false) }
    var myReservedTokensCountToday by remember { mutableIntStateOf(0) }

    // Live geofence check for location-restricted admins
    var userLat by remember { mutableDoubleStateOf(settings.latitude) }
    var userLng by remember { mutableDoubleStateOf(settings.longitude) }

    LaunchedEffect(Unit) {
        val loc = GeofenceLocationManager.getLastKnownLocation(context)
        if (loc != null) {
            userLat = loc.latitude
            userLng = loc.longitude
        }
        repository.syncDevoteesFromCloud()
        if (admin.role != AdminRole.SUPER_ADMIN) {
            try {
                myReservedTokensCountToday = repository.getAdminReservedTokensCountToday(attribution)
            } catch (e: Exception) {}
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = TakeRearPicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            val safeBmp = DevoteePhotoHelper.toSoftwareBitmap(bitmap)
            formCapturedBitmap = safeBmp
            formPhotoUri = DevoteePhotoHelper.saveDevoteePhoto(context, safeBmp, "desk_manual")
        }
    }

    // Top Search Bar query
    LaunchedEffect(searchInput) {
        val q = searchInput.trim()
        if (q.length >= 2) {
            val byName = repository.searchDevoteesByName(q, limit = 6)
            val byPhone = if (q.all { it.isDigit() } && q.length >= 4) {
                val p = repository.searchDevoteeByPhone(q)
                if (p != null) listOf(p) else emptyList()
            } else emptyList()
            searchResults = (byName + byPhone).distinctBy { it.phoneNumber }
        } else {
            searchResults = emptyList()
        }
    }

    // Phone 10-digit auto-fill
    LaunchedEffect(formPhone) {
        val clean = formPhone.trim().replace("+91", "").replace(" ", "").replace("-", "")
        if (clean.length == 10) {
            val found = repository.searchDevoteeByPhone(clean)
            if (found != null) {
                if (formName.isBlank()) formName = found.patientName
                if (formCity == "डूँगरा जाट (स्थानीय)" || formCity.isBlank()) formCity = found.city
                if (formPhotoUri.isBlank() && found.photoUri.isNotBlank()) formPhotoUri = found.photoUri
            }
        }
    }

    // Name suggestions while typing
    LaunchedEffect(formName) {
        val q = formName.trim()
        if (q.length >= 2) {
            nameSuggestions = repository.searchDevoteesByName(q, limit = 5)
        } else {
            nameSuggestions = emptyList()
        }
    }

    fun issueTokenForDevotee(pName: String, pPhone: String, pCity: String, pPhotoUri: String, bitmap: android.graphics.Bitmap?) {
        scope.launch {
            isIssuing = true
            errorMessage = null
            successMessage = null
            try {
                // Check geofence if not bypass-permitted
                if (!canBypassGeofence) {
                    val dist = GeofenceLocationManager.calculateDistanceMeters(
                        userLat, userLng,
                        settings.latitude, settings.longitude
                    )
                    if (dist > settings.allowedRadiusMeters) {
                        errorMessage = if (isHindi)
                            "⚠️ आप आश्रम GPS सीमा से बाहर हैं (${String.format("%.1f", dist / 1000.0)} km)। टोकन केवल आश्रम में उपस्थित होकर या सुपर एडमिन की अनुमति से जारी हो सकता है।"
                        else
                            "⚠️ Outside Ashram GPS boundary. Token can only be issued inside Ashram premises or with Super Admin permission."
                        isIssuing = false
                        return@launch
                    }
                }

                val customNum = formCustomTokenNumber.trim().toIntOrNull()
                val vipNumbers = listOf(2, 4, 6, 8, 10, 12, 14, 16, 18, 20)
                val isSuperAdmin = admin.role == AdminRole.SUPER_ADMIN
                val hasReservedPermission = isSuperAdmin || (admin.canSetCustomTokenNumber && settings.allowAdminReservedTokens)
                if (customNum != null && customNum in vipNumbers && !isSuperAdmin) {
                    if (!hasReservedPermission) {
                        errorMessage = if (isHindi)
                            "❌ अनुमति अस्वीकृत: आपके पास विशेष आरक्षित VIP टोकन जारी करने की अनुमति नहीं है। यह अधिकार केवल सुपर एडमिन द्वारा विशेष अनुमति प्राप्त एडमिन के पास है।"
                        else
                            "❌ Access Denied: You do not have permission to issue reserved VIP tokens. Only Super Admin authorized admins can issue these."
                        isIssuing = false
                        return@launch
                    }
                    val myCount = repository.getAdminReservedTokensCountToday(attribution)
                    if (myCount >= 2) {
                        errorMessage = if (isHindi)
                            "❌ कोटा समाप्त: आप आज केवल अधिकतम 2 विशेष आरक्षित टोकन (2, 4, 6, 8, 10, 12, 14, 16, 18, 20) जारी कर सकते हैं। सुपर एडमिन द्वारा प्रति एडमिन 2 टोकन की सीमा निर्धारित है।"
                        else
                            "❌ Quota exceeded: Maximum 2 reserved VIP tokens per day allowed."
                        isIssuing = false
                        return@launch
                    }
                }

                val token = repository.registerToken(
                    patientName = pName.trim(),
                    phoneNumber = pPhone.trim(),
                    deviceId = "ADMIN_${admin.id}_${System.currentTimeMillis()}",
                    latitude = if (canBypassGeofence) settings.latitude else userLat,
                    longitude = if (canBypassGeofence) settings.longitude else userLng,
                    city = pCity.trim().ifEmpty { "डूँगरा जाट (स्थानीय)" },
                    registeredBy = attribution,
                    photoUri = pPhotoUri,
                    bypassGeofence = canBypassGeofence,
                    customTokenNumber = customNum
                )

                // If photo was captured, enroll face vector in universal registry
                if (bitmap != null) {
                    try {
                        val v = FaceEmbeddingEngine.extractVectorFromBitmap(bitmap)
                        repository.upsertDevoteeProfile(
                            name = pName.trim(),
                            phone = pPhone.trim(),
                            city = pCity.trim().ifEmpty { "डूँगरा जाट (स्थानीय)" },
                            faceVector = v,
                            photoUri = pPhotoUri,
                            registeredBy = attribution
                        )
                    } catch (e: Exception) {}
                }

                successMessage = if (isHindi)
                    "✅ टोकन #${token.tokenNumber} सफलतापूर्वक जारी हुआ! (${token.patientName})"
                else
                    "✅ Token #${token.tokenNumber} successfully issued for ${token.patientName}!"

                formCustomTokenNumber = ""
                formName = ""
                formPhone = ""
                formCity = "डूँगरा जाट (स्थानीय)"
                formPhotoUri = ""
                formCapturedBitmap = null
                searchInput = ""
                searchResults = emptyList()
                keyboardController?.hide()
                focusManager.clearFocus()
                onTokenIssued()
                if (!isSuperAdmin) {
                    try {
                        myReservedTokensCountToday = repository.getAdminReservedTokensCountToday(attribution)
                    } catch (e: Exception) {}
                }
            } catch (e: Exception) {
                errorMessage = "त्रुटि: ${e.localizedMessage ?: "अज्ञात समस्या"}"
            } finally {
                isIssuing = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Register Scan Shortcut Banner
        if (onNavigateToScanRegister != null && (admin.canScanPaperRegister || admin.role == AdminRole.SUPER_ADMIN)) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToScanRegister() },
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                border = BorderStroke(1.5.dp, AmberGold),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Text("📷", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isHindi) "रजिस्टर / कॉपी का फोटो खींचकर टोकन बनाएं" else "Scan Paper Register for Batch Tokens",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaroonPrimary
                            )
                            Text(
                                text = if (isHindi) "कॉपी पर लिखे क्रम में स्वतः क्रमबद्ध टोकन बनेंगे ➔" else "Auto-issue sequential tokens from notebook page ➔",
                                fontSize = 11.sp,
                                color = Color.DarkGray
                            )
                        }
                    }
                    Text("➔", fontWeight = FontWeight.Bold, color = MaroonAccent, fontSize = 16.sp)
                }
            }
        }

        // Issuance Authority Badge
        Surface(
            color = if (canBypassGeofence) Color(0xFFE8F5E9) else Color(0xFFFFF8E1),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, if (canBypassGeofence) Color(0xFF81C784) else Color(0xFFFFB74D)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(if (canBypassGeofence) "🌐" else "📍", fontSize = 22.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (canBypassGeofence)
                            (if (isHindi) "टोकन अधिकार: कहीं से भी जारी करने की अनुमति (Anywhere Authorized)" else "Authority: Can Issue Tokens Anywhere")
                        else
                            (if (isHindi) "टोकन अधिकार: आश्रम GPS सीमा में जारी करने की अनुमति" else "Authority: Ashram GPS Enforced"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (canBypassGeofence) Color(0xFF1B5E20) else Color(0xFFE65100)
                    )
                    Text(
                        text = "पंजीकरणकर्ता: $attribution",
                        fontSize = 11.sp,
                        color = Color.DarkGray,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // 1. FAST SEARCH BAR (Search by Name or Phone)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(14.dp),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🔍", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isHindi) "भक्त का नाम या मोबाइल नंबर खोजें" else "Search Devotee by Name or Mobile",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaroonPrimary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = searchInput,
                    onValueChange = { searchInput = it },
                    placeholder = { Text(if (isHindi) "उदा. राजेश, 9876543210..." else "e.g. Ramesh, 9876543210...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        }
                    ),
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (searchInput.isNotEmpty()) {
                                IconButton(onClick = {
                                    searchInput = ""
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                }) {
                                    Text("✕", color = Color.Gray)
                                }
                            }
                            IconButton(onClick = {
                                keyboardController?.hide()
                                focusManager.clearFocus()
                            }) {
                                Text("⌨️⬇️", fontSize = 13.sp)
                            }
                        }
                    }
                )

                // Search Results Dropdown Cards
                if (searchResults.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isHindi) "मिले भक्त (${searchResults.size}):" else "Matching Devotees (${searchResults.size}):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaroonAccent
                        )
                        OutlinedButton(
                            onClick = {
                                keyboardController?.hide()
                                focusManager.clearFocus()
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(26.dp)
                        ) {
                            Text(if (isHindi) "कीपैड बन्द करें ⬇️" else "Hide Keypad ⬇️", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        searchResults.forEach { devotee ->
                            Surface(
                                color = Color(0xFFF8F9FA),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Text("👤", fontSize = 20.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(devotee.patientName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text("📞 ${devotee.phoneNumber} | 🏠 ${devotee.city}", fontSize = 11.sp, color = Color.DarkGray)
                                        }
                                    }
                                    Button(
                                        onClick = {
                                            keyboardController?.hide()
                                            focusManager.clearFocus()
                                            issueTokenForDevotee(
                                                devotee.patientName,
                                                devotee.phoneNumber,
                                                devotee.city,
                                                devotee.photoUri,
                                                null
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(if (isHindi) "⚡ टोकन दें" else "⚡ Issue", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. REGISTRATION / WALK-IN FORM
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (isHindi) "नया टोकन विवरण दर्ज करें" else "Enter New Token Details",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaroonPrimary
                )
                Text(
                    text = if (isHindi)
                        "मोबाइल नंबर डालते ही पुराना विवरण स्वतः आ जाएगा। फोटो पूर्णतः वैकल्पिक है।"
                    else
                        "Entering phone auto-fills details. Photo is completely optional.",
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Patient Name with suggestions
                OutlinedTextField(
                    value = formName,
                    onValueChange = { formName = it },
                    label = { Text(if (isHindi) "भक्त / मरीज का नाम *" else "Devotee / Patient Name *") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                if (nameSuggestions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        nameSuggestions.forEach { s ->
                            SuggestionChip(
                                onClick = {
                                    formName = s.patientName
                                    formPhone = s.phoneNumber
                                    formCity = s.city
                                    if (s.photoUri.isNotBlank()) formPhotoUri = s.photoUri
                                    nameSuggestions = emptyList()
                                },
                                label = { Text("${s.patientName} (${s.city})", fontSize = 11.sp) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = formPhone,
                    onValueChange = { if (it.length <= 10) formPhone = it },
                    label = { Text(if (isHindi) "संपर्क फोन नंबर *" else "Mobile Number *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = formCity,
                    onValueChange = { formCity = it },
                    label = { Text(if (isHindi) "कहाँ के निवासी हैं / शहर / गाँव *" else "City / Village *") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                // City Chips
                val quickCities = listOf("डूँगरा जाट (स्थानीय)", "बुलन्दशहर", "खुर्जा", "नोएडा", "दिल्ली", "मेरठ", "अलीगढ़")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickCities.forEach { c ->
                        SuggestionChip(
                            onClick = { formCity = c },
                            label = { Text(c, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // VIP Reserved Tokens 2, 4, 6, 8, 10, 12, 14, 16, 18, 20 (Superadmin or Permitted Admin)
                val isSuperAdmin = admin.role == AdminRole.SUPER_ADMIN
                val canIssueReserved = isSuperAdmin || (admin.canSetCustomTokenNumber && settings.allowAdminReservedTokens)
                if (canIssueReserved) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.2.dp, AmberGold)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("👑", fontSize = 18.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isHindi) "व्यवस्थापक आरक्षित टोकन (VIP 2..20)" else "Admin Reserved VIP Slots",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaroonPrimary
                                    )
                                }
                                if (isSuperAdmin) {
                                    TextButton(
                                        onClick = {
                                            scope.launch {
                                                repository.updateAllowAdminReservedTokens(!settings.allowAdminReservedTokens)
                                                Toast.makeText(context, if (!settings.allowAdminReservedTokens) "✓ अन्य एडमिन्स को आरक्षित टोकन अनुमति दी गई" else "✓ अनुमति हटाई गई", Toast.LENGTH_SHORT).show()
                                                onTokenIssued()
                                            }
                                        },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (settings.allowAdminReservedTokens) "🔓 स्टाफ अनुमति: चालू" else "🔒 स्टाफ अनुमति: बंद",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (settings.allowAdminReservedTokens) Color(0xFF2E7D32) else Color(0xFFC62828)
                                        )
                                    }
                                }
                            }
                            if (!isSuperAdmin) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isHindi)
                                            "👑 आपका दैनिक कोटा: $myReservedTokensCountToday / 2 टोकन प्रयुक्त (शेष: ${(2 - myReservedTokensCountToday).coerceAtLeast(0)})"
                                        else
                                            "👑 Your Daily Quota: $myReservedTokensCountToday / 2 used (Remaining: ${(2 - myReservedTokensCountToday).coerceAtLeast(0)})",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (myReservedTokensCountToday >= 2) Color(0xFFC62828) else Color(0xFF2E7D32)
                                    )
                                    if (myReservedTokensCountToday >= 2) {
                                        Surface(
                                            color = Color(0xFFFFEBEE),
                                            shape = RoundedCornerShape(4.dp),
                                            border = BorderStroke(1.dp, Color(0xFFEF9A9A))
                                        ) {
                                            Text(
                                                text = if (isHindi) "कोटा पूर्ण 🔒" else "Quota Full 🔒",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFC62828),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                            Text(
                                text = if (isHindi) "अपने परिचितों / विशेष मरीजों हेतु तुरंत टोकन चुनें (टैप करते ही नंबर सेट हो जाएगा):" else "Select reserved token number:",
                                fontSize = 11.sp,
                                color = Color.DarkGray
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            val vipNumbers = listOf(2, 4, 6, 8, 10, 12, 14, 16, 18, 20)
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                vipNumbers.forEach { num ->
                                    val isSelected = formCustomTokenNumber == num.toString()
                                    val isQuotaExceeded = !isSuperAdmin && myReservedTokensCountToday >= 2
                                    Surface(
                                        onClick = {
                                            if (isQuotaExceeded) {
                                                Toast.makeText(
                                                    context,
                                                    if (isHindi) "❌ कोटा समाप्त: आप आज के 2 विशेष आरक्षित टोकन (2..20) पहले ही जारी कर चुके हैं।" else "❌ Quota exceeded: Maximum 2 reserved VIP tokens per day allowed.",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            } else {
                                                formCustomTokenNumber = num.toString()
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        color = when {
                                            isSelected -> MaroonPrimary
                                            isQuotaExceeded -> Color(0xFFEEEEEE)
                                            else -> Color.White
                                        },
                                        border = BorderStroke(1.dp, when {
                                            isSelected -> AmberGold
                                            isQuotaExceeded -> Color(0xFFBDBDBD)
                                            else -> Color(0xFFFFB74D)
                                        })
                                    ) {
                                        Text(
                                            text = "#$num",
                                            color = when {
                                                isSelected -> Color.White
                                                isQuotaExceeded -> Color.Gray
                                                else -> MaroonPrimary
                                            },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                } else if (!isSuperAdmin) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFE0E0E0))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🔒", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (!admin.canSetCustomTokenNumber) {
                                    if (isHindi) "विशेष आरक्षित टोकन (2..20) केवल सुपर एडमिन द्वारा अधिकृत सेवादारों के लिए उपलब्ध हैं।"
                                    else "Reserved VIP tokens (2..20) are restricted to authorized sevadars only."
                                } else {
                                    if (isHindi) "सुपर एडमिन ने वर्तमान में स्टाफ हेतु आरक्षित VIP टोकन जारी करना बंद किया हुआ है।"
                                    else "Staff reserved VIP token issuance is currently disabled by Super Admin."
                                },
                                fontSize = 11.sp,
                                color = Color.Gray,
                                lineHeight = 16.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                OutlinedTextField(
                    value = formCustomTokenNumber,
                    onValueChange = { if (it.length <= 6 && it.all { ch -> ch.isDigit() }) formCustomTokenNumber = it },
                    label = { Text(if (isHindi) "टोकन नंबर (वैकल्पिक / अपनी पसंद का टोकन #)" else "Custom Token Number (Optional)") },
                    placeholder = { Text(if (isHindi) "खाली छोड़ें (स्वतः अगला # मिलेगा) या नंबर लिखें (उदा. 51)" else "Leave blank for auto or enter custom number (e.g. 51)") },
                    leadingIcon = { Text("🔢") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Optional Photo Capture Row
                Surface(
                    color = if (formCapturedBitmap != null) Color(0xFFF1F8E9) else Color(0xFFF8F9FA),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (formCapturedBitmap != null) Color(0xFF2E7D32) else Color(0xFFE0E0E0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            if (formCapturedBitmap != null) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(1.dp, Color(0xFF2E7D32), RoundedCornerShape(8.dp))
                                ) {
                                    Image(
                                        bitmap = formCapturedBitmap!!.asImageBitmap(),
                                        contentDescription = "Devotee Photo",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (isHindi) "✓ फोटो संलग्न" else "✓ Photo Attached",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color(0xFF2E7D32)
                                    )
                                    Text(
                                        text = if (isHindi) "सभी फोन से चेहरा पहचान सक्रिय होगी" else "Enables cross-phone face recognition",
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )
                                }
                            } else {
                                Text("📸", fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = if (isHindi) "भक्त का फोटो (वैकल्पिक)" else "Devotee Photo (Optional)",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF333333)
                                    )
                                    Text(
                                        text = if (isHindi) "बिना फोटो के भी टोकन तुरंत जारी हो जाएगा" else "Token issues immediately without photo",
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }

                        if (formCapturedBitmap != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = {
                                    val currentBmp = formCapturedBitmap
                                    if (currentBmp != null) {
                                        val rotated = DevoteePhotoHelper.rotateBitmap(currentBmp, 90f)
                                        formCapturedBitmap = rotated
                                        formPhotoUri = DevoteePhotoHelper.saveDevoteePhoto(context, rotated, "desk_manual")
                                        Toast.makeText(context, if (isHindi) "🔄 फोटो 90° सीधी हो गई!" else "Photo rotated 90°!", Toast.LENGTH_SHORT).show()
                                    }
                                }) {
                                    Text("🔄", fontSize = 16.sp)
                                }
                                IconButton(onClick = {
                                    formCapturedBitmap = null
                                    formPhotoUri = ""
                                }) {
                                    Text("✕", color = Color.Red, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            OutlinedButton(
                                onClick = { cameraLauncher.launch(null) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(if (isHindi) "फोटो खींचें" else "Take Photo", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(errorMessage!!, color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                if (successMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(successMessage!!, color = Color(0xFF2E7D32), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                        if (formName.isBlank()) {
                            errorMessage = if (isHindi) "कृपया भक्त का नाम दर्ज करें" else "Please enter devotee name"
                            return@Button
                        }
                        if (formPhone.length < 10) {
                            errorMessage = if (isHindi) "कृपया 10 अंकों का फोन नंबर दर्ज करें" else "Please enter 10-digit phone number"
                            return@Button
                        }
                        issueTokenForDevotee(formName, formPhone, formCity, formPhotoUri, formCapturedBitmap)
                    },
                    enabled = !isIssuing,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isIssuing) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text(if (isHindi) "🎟️ टोकन पर्ची जारी करें" else "🎟️ Issue Token Pass", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun LocationConfigTab(
    isHindi: Boolean,
    canChangeLocation: Boolean,
    lat: String,
    onLatChange: (String) -> Unit,
    long: String,
    onLongChange: (String) -> Unit,
    radius: String,
    onRadiusChange: (String) -> Unit,
    isEnforced: Boolean,
    onEnforcedChange: (Boolean) -> Unit,
    isOutstationAllowed: Boolean,
    onOutstationAllowedChange: (Boolean) -> Unit,
    outstationMinKm: String,
    onOutstationMinKmChange: (String) -> Unit,
    successMsg: String?,
    errorMsg: String?,
    onSave: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Card 1: Ashram GPS Coordinates & Geofence Radius
        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (isHindi) "आश्रम GPS जिओफेंस विन्यास" else "Ashram GPS & Geofence Setup",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaroonPrimary
                )
                Text(
                    text = if (isHindi)
                        "रविवार टोकन पंजीकरण के लिए वैध परिधि (डूँगरा जाट आश्रम)"
                    else
                        "Allowed radius for Sunday token registration at Dungra Jaat ashram",
                    fontSize = 13.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Cloud Broadcast Info Card
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFE8F5E9),
                    border = BorderStroke(1.dp, Color(0xFF81C784)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("🌐", fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isHindi)
                                "सुपर एडमिन ग्लोबल कंट्रोल: यहाँ दर्ज किया गया अक्षांश (Lat) व देशांतर (Long) सीधे क्लाउड पर ब्रॉडकास्ट होगा और सभी भक्तों के फोन में बैकग्राउंड में तुरंत अपडेट हो जाएगा। भक्त इसी नई लोकेशन पर आकर ही टोकन जनरेट कर सकेंगे।"
                            else
                                "Super Admin Universal Control: Coordinates saved here will immediately broadcast to the cloud and sync across all devotees' devices.",
                            fontSize = 11.sp,
                            color = Color(0xFF1B5E20),
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = lat,
                    onValueChange = onLatChange,
                    label = { Text(if (isHindi) "अक्षांश (Latitude)" else "Latitude") },
                    enabled = canChangeLocation,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = long,
                    onValueChange = onLongChange,
                    label = { Text(if (isHindi) "देशांतर (Longitude)" else "Longitude") },
                    enabled = canChangeLocation,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))
                // Quick Location Helper Buttons
                if (canChangeLocation) {
                    val scope = rememberCoroutineScope()
                    val context = LocalContext.current
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    val loc = GeofenceLocationManager.getLastKnownLocation(context)
                                    if (loc != null) {
                                        onLatChange(loc.latitude.toString())
                                        onLongChange(loc.longitude.toString())
                                        Toast.makeText(context, if (isHindi) "✓ वर्तमान डिवाइस GPS लोकेशन भर दी गई!" else "Current GPS coordinates filled!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, if (isHindi) "GPS लोकेशन प्राप्त करने में असमर्थ" else "Failed to get GPS location", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (isHindi) "📍 मेरी GPS लोकेशन लें" else "📍 Use My GPS", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                onLatChange("28.3972915")
                                onLongChange("78.1460410")
                                Toast.makeText(context, if (isHindi) "डूँगरा जाट आश्रम कोऑर्डिनेट्स सेट" else "Default Dungra Jaat set", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (isHindi) "🚩 मूल धाम डूँगरा जाट" else "🚩 Dungra Jaat", fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = radius,
                    onValueChange = { input ->
                        val clean = input.filter { it.isDigit() || it == '.' }
                        val num = clean.toDoubleOrNull()
                        if (num != null && num > 50000.0) {
                            onRadiusChange("50000")
                        } else {
                            onRadiusChange(clean)
                        }
                    },
                    label = { Text(if (isHindi) "स्वीकृत आश्रम परिधि (मीटर में, उदा. 200m, 500m)" else "Allowed Radius (meters, e.g. 200m, 500m)") },
                    supportingText = {
                        Text(
                            text = if (isHindi)
                                "स्थानीय भक्तों के लिए वैध भौतिक परिधि (डिफ़ॉल्ट 200m)। यहाँ जो भी परिधि सेट करेंगे वह स्थायी रहेगी और कभी अपने आप 1500m नहीं बदलेगी।"
                            else
                                "Permitted local radius (default 200m). Will stay locked permanently and never revert to 1500m.",
                            fontSize = 11.sp,
                            color = MaroonAccent
                        )
                    },
                    enabled = canChangeLocation,
                    modifier = Modifier.fillMaxWidth()
                )

                if (canChangeLocation) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isHindi) "⚡ त्वरित परिधि चुनें (मीटर):" else "⚡ Quick Radius Presets (Meters):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onRadiusChange("100") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("100m", fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = { onRadiusChange("200") },
                            modifier = Modifier.weight(1f),
                            colors = if (radius == "200" || radius == "200.0") ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFFE8F5E9)) else ButtonDefaults.outlinedButtonColors()
                        ) {
                            Text("200m", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { onRadiusChange("500") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("500m", fontSize = 11.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onRadiusChange("1000") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("1 km", fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = { onRadiusChange("2000") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("2 km", fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = { onRadiusChange("5000") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("5 km", fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = isEnforced,
                        onCheckedChange = onEnforcedChange,
                        enabled = canChangeLocation
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        if (isHindi) "सख्त जिओफेंसिंग लागू रखें (तय परिधि के अंदर ही टोकन जारी होंगे)" else "Strict Geofencing Enforced (Inside radius only)",
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Card 2: 30 km Outstation Rule & Advance Booking Controls
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFFF9800)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🚗", fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (isHindi) "30 किमी बाहरी भक्त दूरी नियम (Outstation Rule)" else "Outstation Devotee Distance Rule",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaroonPrimary
                        )
                        Text(
                            text = if (isHindi) "दूरस्थ भक्तों हेतु घर बैठे ऑनलाइन टोकन की अनुमति" else "Remote token generation for distant devotees",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Switch for Outstation Advance Booking
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(
                        checked = isOutstationAllowed,
                        onCheckedChange = onOutstationAllowedChange,
                        enabled = canChangeLocation
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isHindi)
                            "30 किमी से दूर वाले भक्तों को घर से टोकन की अनुमति दें (चालू/बंद)"
                        else
                            "Allow remote tokens for outstation devotees (>30 km)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isOutstationAllowed) Color(0xFFE3F2FD) else Color(0xFFFFEBEE),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isHindi) {
                            if (isOutstationAllowed)
                                "✓ नियम सक्रिय: $outstationMinKm किमी से अधिक दूर रहने वाले भक्त घर/शहर से रविवार टोकन जनरेट कर सकते हैं। $outstationMinKm किमी के दायरे वाले स्थानीय भक्तों को आश्रम परिसर (${radius}m) में आकर ही टोकन मिलेगा।"
                            else
                                "⚠️ नियम बंद: सभी भक्तों (दूर व पास) को अनिवार्य रूप से आश्रम परिसर (${radius}m) में उपस्थित होकर ही टोकन लेना होगा।"
                        } else {
                            if (isOutstationAllowed)
                                "Active: Devotees >$outstationMinKm km can register from home. Local devotees must be within ${radius}m of Ashram."
                            else
                                "Inactive: All devotees must be physically present inside Ashram (${radius}m)."
                        },
                        fontSize = 11.sp,
                        color = if (isOutstationAllowed) Color(0xFF0D47A1) else Color(0xFFB71C1C),
                        modifier = Modifier.padding(10.dp)
                    )
                }

                if (isOutstationAllowed) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = outstationMinKm,
                        onValueChange = { input ->
                            val clean = input.filter { it.isDigit() || it == '.' }
                            val num = clean.toDoubleOrNull()
                            if (num != null && num > 500.0) {
                                onOutstationMinKmChange("500")
                            } else {
                                onOutstationMinKmChange(clean)
                            }
                        },
                        label = { Text(if (isHindi) "न्यूनतम बाहरी दूरी (किमी में, उदा. 30 km)" else "Min Outstation Distance (km)") },
                        supportingText = {
                            Text(
                                text = if (isHindi) "उदा. 30 लिखने पर 30 किमी से दूर वाले भक्तों का टोकन घर बैठे बनेगा" else "e.g. 30 allows devotees >30km away to register",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        },
                        enabled = canChangeLocation,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isHindi) "⚡ त्वरित दूरी बटन (क्लिक करके सेट करें):" else "⚡ Quick Distance Presets:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onOutstationMinKmChange("15") },
                            modifier = Modifier.weight(1f),
                            colors = if (outstationMinKm == "15" || outstationMinKm == "15.0") ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFFFFF3E0)) else ButtonDefaults.outlinedButtonColors()
                        ) {
                            Text("15 किमी", fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = { onOutstationMinKmChange("20") },
                            modifier = Modifier.weight(1f),
                            colors = if (outstationMinKm == "20" || outstationMinKm == "20.0") ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFFFFF3E0)) else ButtonDefaults.outlinedButtonColors()
                        ) {
                            Text("20 किमी", fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = { onOutstationMinKmChange("30") },
                            modifier = Modifier.weight(1f),
                            colors = if (outstationMinKm == "30" || outstationMinKm == "30.0") ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFFFFE0B2)) else ButtonDefaults.outlinedButtonColors()
                        ) {
                            Text("30 किमी", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { onOutstationMinKmChange("50") },
                            modifier = Modifier.weight(1f),
                            colors = if (outstationMinKm == "50" || outstationMinKm == "50.0") ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFFFFF3E0)) else ButtonDefaults.outlinedButtonColors()
                        ) {
                            Text("50 किमी", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Status Messages and Save Button
        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (successMsg != null) {
                    Text(successMsg, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                if (errorMsg != null) {
                    Text(errorMsg, color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Button(
                    onClick = onSave,
                    enabled = canChangeLocation,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary)
                ) {
                    Text(if (isHindi) "🌐 लोकेशन, परिधि व 30km नियम सुरक्षित करें" else "🌐 Save & Broadcast Location Rules Live", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun BroadcastNotificationTab(
    isHindi: Boolean,
    title: String,
    onTitleChange: (String) -> Unit,
    message: String,
    onMsgChange: (String) -> Unit,
    priority: String,
    onPriorityChange: (String) -> Unit,
    successMsg: String?,
    pastNotifications: List<AppNotification>,
    onSend: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isHindi) "भक्तों को तत्काल सूचना प्रसारित करें" else "Broadcast Announcement to Devotees",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaroonPrimary
                    )
                    Text(
                        text = if (isHindi)
                            "यह सूचना भक्तों के फोन पर ऐप बंद होने पर भी लॉकस्क्रीन पर दिखाई देगी।"
                        else
                            "This alert will appear on users' status bars and lockscreens even if the app is closed.",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = title,
                        onValueChange = onTitleChange,
                        label = { Text(if (isHindi) "सूचना का शीर्षक" else "Alert Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = message,
                        onValueChange = onMsgChange,
                        label = { Text(if (isHindi) "संदेश का विवरण" else "Message Content") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )

                    if (successMsg != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(successMsg, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onSend,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary)
                    ) {
                        Text(if (isHindi) "तुरंत प्रसारित करें (Send Broadcast)" else "Broadcast Now", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Text(
                if (isHindi) "हाल ही में प्रसारित सूचनाएं" else "Recent Broadcast History",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaroonPrimary
            )
        }

        items(pastNotifications) { notif ->
            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(notif.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaroonPrimary)
                        Text(notif.priority, color = if (notif.priority == "HIGH") Color.Red else Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(notif.message, fontSize = 13.sp, color = Color.DarkGray)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("द्वारा: ${notif.sentBy}", fontSize = 11.sp, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun SevadarManagementTab(
    isHindi: Boolean,
    admins: List<Admin>,
    onOpenCreate: () -> Unit,
    onOpenEdit: (Admin) -> Unit,
    onToggleActive: (Admin) -> Unit,
    onToggleAnywhere: (Admin, Boolean) -> Unit,
    onToggleScanRegister: (Admin, Boolean) -> Unit,
    onToggleParchas: (Admin, Boolean) -> Unit,
    onSendWhatsApp: (Admin) -> Unit,
    onDelete: (Admin) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isHindi) "सेवादार व व्यवस्थापक खाते" else "Sevadar & Admin Accounts",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaroonPrimary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = onOpenCreate,
                        colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary)
                    ) {
                        Text(if (isHindi) "+ नया सेवादार" else "+ Add Sevadar")
                    }
                }
            }
        }

        items(admins) { a ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (!a.isActive) Color(0xFFFAFAFA) else Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            SacredAvatar(photoUri = a.photoUri, name = a.name, size = 44.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(a.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Username: ${a.username.ifEmpty { "N/A" }} | Phone: ${a.phoneNumber}", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (a.role == AdminRole.SUPER_ADMIN) SaffronPrimary else Color(0xFF388E3C)
                        ) {
                            Text(
                                a.role.name,
                                color = Color.White,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "अनुमतियाँ: " + listOfNotNull(
                            if (a.canManageTokens) "टोकन" else null,
                            if (a.canIssueManualTokens) "मैनुअल टोकन" else null,
                            if (a.canManageYatra) "यात्रा" else null,
                            if (a.canManageExpenses) "खर्च" else null,
                            if (a.canChangeLocation) "GPS" else null,
                            if (a.canSendNotifications) "नोटिफिकेशन" else null,
                            if (a.canEditAshramInfo) "कंटेंट" else null,
                            if (a.canViewDevoteePhotos) "भक्त फोटो" else null,
                            if (a.canScanPaperRegister) "रजिस्टर स्कैन" else null,
                            if (a.canManageParchas) "आश्रम पर्चे" else null
                        ).joinToString(", "),
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )

                    if (a.role != AdminRole.SUPER_ADMIN) {
                        Spacer(modifier = Modifier.height(8.dp))
                        // Anywhere Token Issuance Quick Switch
                        Surface(
                            color = if (a.canIssueTokensAnywhere) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (a.canIssueTokensAnywhere) Color(0xFF81C784) else Color(0xFFFFB74D)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isHindi) "🌐 कहीं से भी टोकन जारी करने की अनुमति" else "🌐 Issue Tokens Anywhere",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (a.canIssueTokensAnywhere) Color(0xFF1B5E20) else MaroonAccent
                                    )
                                    Text(
                                        text = if (a.canIssueTokensAnywhere)
                                            (if (isHindi) "सक्रिय: बिना आश्रम GPS सीमा के टोकन बना सकते हैं।" else "Active: Can issue tokens outside Ashram GPS.")
                                        else
                                            (if (isHindi) "अक्रिय: केवल आश्रम GPS सीमा में ही टोकन जारी होंगे।" else "Inactive: Restricted to Ashram GPS boundary."),
                                        fontSize = 10.sp,
                                        color = Color.DarkGray
                                    )
                                }
                                Switch(
                                    checked = a.canIssueTokensAnywhere,
                                    onCheckedChange = { isChecked ->
                                        onToggleAnywhere(a, isChecked)
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        // Paper Register Scan Quick Switch
                        Surface(
                            color = if (a.canScanPaperRegister) Color(0xFFEDE7F6) else Color(0xFFFFF3E0),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (a.canScanPaperRegister) Color(0xFFB39DDB) else Color(0xFFFFB74D)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isHindi) "📝 रजिस्टर कॉपी स्कैन व क्रमबद्ध टोकन अधिकार" else "📝 Scan Paper Register Permission",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (a.canScanPaperRegister) Color(0xFF4A148C) else MaroonAccent
                                    )
                                    Text(
                                        text = if (a.canScanPaperRegister)
                                            (if (isHindi) "सक्रिय: डायरी/कॉपी फोटो खींचकर क्रमबद्ध टोकन जारी कर सकते हैं।" else "Active: Can scan paper register & issue sequential tokens.")
                                        else
                                            (if (isHindi) "अक्रिय: रजिस्टर स्कैन करने की अनुमति नहीं है।" else "Inactive: Not authorized to scan register."),
                                        fontSize = 10.sp,
                                        color = Color.DarkGray
                                    )
                                }
                                Switch(
                                    checked = a.canScanPaperRegister,
                                    onCheckedChange = { isChecked ->
                                        onToggleScanRegister(a, isChecked)
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        // Sacred Parchas Management Quick Switch (Super Admin Control)
                        Surface(
                            color = if (a.canManageParchas) Color(0xFFFFF8E1) else Color(0xFFFAFAFA),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (a.canManageParchas) Color(0xFFFFD54F) else Color(0xFFE0E0E0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isHindi) "📜 आश्रम पर्चे प्रबंधन अधिकार (Super Admin Control)" else "📜 Sacred Parchas Management Access",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (a.canManageParchas) Color(0xFFE65100) else Color.DarkGray
                                    )
                                    Text(
                                        text = if (a.canManageParchas)
                                            (if (isHindi) "सक्रिय: नया पर्चा जोड़ने, एडिट, डिलीट व हाइड/लाइव करने की अनुमति है।" else "Active: Can create, edit & hide parchas.")
                                        else
                                            (if (isHindi) "अक्रिय: केवल सुपर एडमिन ही पर्चों का संपादन कर सकते हैं।" else "Inactive: Restricted to Super Admin."),
                                        fontSize = 10.sp,
                                        color = Color.DarkGray
                                    )
                                }
                                Switch(
                                    checked = a.canManageParchas,
                                    onCheckedChange = { isChecked ->
                                        onToggleParchas(a, isChecked)
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                            Button(
                                onClick = { onSendWhatsApp(a) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                            ) {
                                Text(if (isHindi) "📲 व्हाट्सएप पर भेजें" else "📲 WhatsApp", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { onOpenEdit(a) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaroonAccent)
                            ) {
                                Text(if (isHindi) "✏️ अनुमतियाँ व फोटो" else "✏️ Permissions & Photo", fontSize = 12.sp, color = Color.White)
                            }
                            OutlinedButton(onClick = { onToggleActive(a) }) {
                                Text(if (a.isActive) (if (isHindi) "निष्क्रिय करें" else "Deactivate") else (if (isHindi) "सक्रिय करें" else "Activate"), fontSize = 12.sp)
                            }
                            OutlinedButton(onClick = { onDelete(a) }) {
                                Text(if (isHindi) "हटाएं" else "Delete", color = Color.Red, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PublicServiceMatrixTab(
    isHindi: Boolean,
    isToken: Boolean,
    onTokenChange: (Boolean) -> Unit,
    isYatra: Boolean,
    onYatraChange: (Boolean) -> Unit,
    isLiveCounter: Boolean,
    onLiveCounterChange: (Boolean) -> Unit,
    isEvents: Boolean,
    onEventsChange: (Boolean) -> Unit,
    isAartiTimings: Boolean,
    onAartiTimingsChange: (Boolean) -> Unit,
    isGurujiInfo: Boolean,
    onGurujiInfoChange: (Boolean) -> Unit,
    isEmergencyNotice: Boolean,
    onEmergencyNoticeChange: (Boolean) -> Unit,
    scheduledTimestamp: Long,
    onScheduledTimestampChange: (Long) -> Unit,
    customDateStr: String,
    onCustomDateStrChange: (String) -> Unit,
    isBusBookingLive: Boolean = false,
    onBusBookingLiveChange: (Boolean) -> Unit = {},
    busFareAmount: String = "1500",
    onBusFareAmountChange: (String) -> Unit = {},
    isPaymentFeatureLive: Boolean = false,
    onPaymentFeatureLiveChange: (Boolean) -> Unit = {},
    canAdminViewPayments: Boolean = false,
    onCanAdminViewPaymentsChange: (Boolean) -> Unit = {},
    canDevoteeViewPayments: Boolean = false,
    onCanDevoteeViewPaymentsChange: (Boolean) -> Unit = {},
    upiId: String = "shribalajikripadham@upi",
    onUpiIdChange: (String) -> Unit = {},
    upiName: String = "Shri Balaji Kripa Dham",
    onUpiNameChange: (String) -> Unit = {},
    customUpiQrUri: String = "",
    onCustomUpiQrUriChange: (String) -> Unit = {},
    isArziLedgerLive: Boolean = false,
    onArziLedgerLiveChange: (Boolean) -> Unit = {},
    badiArziRate: String = "100",
    onBadiArziRateChange: (String) -> Unit = {},
    chhotiArziRate: String = "50",
    onChhotiArziRateChange: (String) -> Unit = {},
    canAdminViewArzi: Boolean = false,
    onCanAdminViewArziChange: (Boolean) -> Unit = {},
    canDevoteeViewArzi: Boolean = false,
    onCanDevoteeViewArziChange: (Boolean) -> Unit = {},
    canDevoteeViewYatraDiary: Boolean = false,
    onCanDevoteeViewYatraDiaryChange: (Boolean) -> Unit = {},
    successMsg: String?,
    onSave: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // SECTION 1: PRE-SCHEDULED TOKEN OPENING (Timing Control)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color(0xFFFFD54F))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⏱️", fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isHindi) "टोकन खुलने का पूर्व-निर्धारित समय (Timing Control)" else "Pre-Scheduled Token Opening",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaroonPrimary
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (isHindi)
                        "सुपर एडमिन टोकन पंजीकरण शुरू होने की निश्चित तारीख व समय पहले से तय कर सकते हैं। उस समय से पहले भक्तों को स्क्रीन पर काउंटडाउन दिखेगा और टोकन बटन लॉक रहेगा।"
                    else
                        "Pre-schedule the exact Date & Time when Token Generation automatically starts. Devotees see a live countdown before this time.",
                    fontSize = 12.sp,
                    color = Color.DarkGray
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Current Status Card
                val now = System.currentTimeMillis()
                val statusText = when {
                    scheduledTimestamp <= 0L -> if (isHindi) "🟢 तत्काल खुला (Immediate / Open during Darbar)" else "🟢 Immediate / Open"
                    scheduledTimestamp > now -> {
                        val sdf = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault())
                        val dt = sdf.format(java.util.Date(scheduledTimestamp))
                        if (isHindi) "🟡 निर्धारित समय: $dt पर स्वतः खुलेगा (वर्तमान में बंद)" else "🟡 Scheduled for: $dt (Locked until then)"
                    }
                    else -> if (isHindi) "🟢 निर्धारित समय पूरा हो चुका है — स्वतः चालू है" else "🟢 Scheduled time reached — Active"
                }

                Surface(
                    color = if (scheduledTimestamp > now) Color(0xFFFFF3E0) else Color(0xFFE8F5E9),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = statusText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (scheduledTimestamp > now) Color(0xFFE65100) else Color(0xFF2E7D32),
                        modifier = Modifier.padding(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Preset Buttons
                Text(
                    text = if (isHindi) "त्वरित समय विकल्प (Quick Presets):" else "Quick Timing Presets:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val cal = java.util.Calendar.getInstance()
                            while (cal.get(java.util.Calendar.DAY_OF_WEEK) != java.util.Calendar.SUNDAY) {
                                cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
                            }
                            cal.set(java.util.Calendar.HOUR_OF_DAY, 6)
                            cal.set(java.util.Calendar.MINUTE, 0)
                            cal.set(java.util.Calendar.SECOND, 0)
                            cal.set(java.util.Calendar.MILLISECOND, 0)
                            if (cal.timeInMillis <= System.currentTimeMillis()) {
                                cal.add(java.util.Calendar.WEEK_OF_YEAR, 1)
                            }
                            val targetTime = cal.timeInMillis
                            onScheduledTimestampChange(targetTime)
                            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                            onCustomDateStrChange(sdf.format(java.util.Date(targetTime)))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isHindi) "आगामी रविवार 6 AM" else "Next Sun 6 AM", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            onScheduledTimestampChange(0L)
                            onCustomDateStrChange("")
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isHindi) "तत्काल खोलें (Open)" else "Open Now", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = customDateStr,
                    onValueChange = onCustomDateStrChange,
                    label = { Text(if (isHindi) "कस्टम तारीख व समय (YYYY-MM-DD HH:mm)" else "Custom Date & Time (YYYY-MM-DD HH:mm)") },
                    placeholder = { Text("उदा. 2026-09-13 06:00") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // SECTION 2: 60-SEATER LUXURY BUS CONTROL (SUPER ADMIN DIRECT CONTROL)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, if (isBusBookingLive) Color(0xFF4CAF50) else Color(0xFFFFB74D))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Text("🚌", fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (isHindi) "60-सीटर डीलक्स बस बुकिंग (सुपर एडमिन नियंत्रण)" else "60-Seater Deluxe Bus Booking",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaroonPrimary
                            )
                            Text(
                                text = if (isHindi) "12 पंक्तियाँ × 5 सीटें (3×2 कॉन्फ़िगरेशन)" else "12 Rows × 5 Seats (3x2 Configuration)",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }
                    Switch(checked = isBusBookingLive, onCheckedChange = onBusBookingLiveChange)
                }

                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = if (isBusBookingLive) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isBusBookingLive)
                            (if (isHindi) "🟢 लाइव: आम भक्तों के ऐप में बस सीट बुकिंग व A4 टिकट सुविधा खुली है।" else "🟢 LIVE: Devotees can view 60-seat layout and book seats.")
                        else
                            (if (isHindi) "🔒 गुप्त/छिपा हुआ (Hidden): यह विकल्प भक्तों से पूरी तरह छिपा हुआ है। सुपर एडमिन जब चाहें इसे लाइव कर सकते हैं।" else "🔒 HIDDEN: Bus booking is hidden from devotee screens."),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isBusBookingLive) Color(0xFF2E7D32) else Color(0xFFE65100),
                        modifier = Modifier.padding(8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = busFareAmount,
                    onValueChange = onBusFareAmountChange,
                    label = { Text(if (isHindi) "प्रति सीट किराया (₹)" else "Seat Fare Amount (₹)") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // SECTION 3: ASHRAM UPI QR CODE & PAYMENT GATEWAY CONTROL (SUPER ADMIN DIRECT CONTROL)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, if (isPaymentFeatureLive) Color(0xFF4CAF50) else Color(0xFF90CAF9))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Text("💳", fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (isHindi) "आश्रम UPI QR कोड व डिजिटल पेमेंट (सुपर एडमिन नियंत्रण)" else "Ashram UPI QR & Payment Gateway",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaroonPrimary
                            )
                            Text(
                                text = if (isHindi) "PhonePe, GPay, Paytm, BHIM ऑडिट लेजर" else "PhonePe, GPay, Paytm, BHIM Audit Ledger",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }
                    Switch(checked = isPaymentFeatureLive, onCheckedChange = onPaymentFeatureLiveChange)
                }

                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = if (isPaymentFeatureLive) Color(0xFFE8F5E9) else Color(0xFFE3F2FD),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isPaymentFeatureLive)
                            (if (isHindi) "🟢 लाइव: ऐप में आश्रम UPI QR कोड व पेमेंट सुविधा लाइव है।" else "🟢 LIVE: UPI QR code payment is active in app.")
                        else
                            (if (isHindi) "🔒 गुप्त/छिपा हुआ (Hidden): पेमेंट विकल्प भक्तों से छिपा हुआ है। केवल सुपर एडमिन ही इसे आवश्यकता पड़ने पर लाइव कर सकते हैं।" else "🔒 HIDDEN: Payment feature is hidden from public devotees."),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isPaymentFeatureLive) Color(0xFF2E7D32) else Color(0xFF1565C0),
                        modifier = Modifier.padding(8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = upiId,
                    onValueChange = onUpiIdChange,
                    label = { Text(if (isHindi) "आश्रम की आधिकारिक UPI ID" else "Ashram Official UPI ID") },
                    placeholder = { Text("उदा. shribalajikripadham@upi") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = upiName,
                    onValueChange = onUpiNameChange,
                    label = { Text(if (isHindi) "खाताधारक / ट्रस्ट का नाम" else "Payee / Trust Name") },
                    placeholder = { Text("उदा. Shri Balaji Kripa Dham") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = if (isHindi) "🖼️ आश्रम का कस्टम UPI QR कोड (वैकल्पिक)" else "Custom UPI QR Code Image (Optional)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaroonPrimary
                )
                Text(
                    text = if (isHindi) "अपना बैंक/PhonePe/GPay का QR कोड फोटो लगाएं, जो बस टिकट व दान स्क्रीन पर दिखेगा।" else "Upload your custom bank QR code image displayed on booking/donation screens.",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(6.dp))

                val customQrPicker = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent()
                ) { uri ->
                    if (uri != null) {
                        try {
                            val loaded = com.example.shribalajikripadham.util.DevoteePhotoHelper.loadBitmap(context, uri.toString())
                            if (loaded != null) {
                                val savedPath = com.example.shribalajikripadham.util.DevoteePhotoHelper.saveDevoteePhoto(context, loaded, "custom_upi_qr")
                                onCustomUpiQrUriChange(savedPath)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                if (customUpiQrUri.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(70.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color.LightGray)
                        ) {
                            val bmp = remember(customUpiQrUri) {
                                try { com.example.shribalajikripadham.util.DevoteePhotoHelper.loadBitmap(context, customUpiQrUri) } catch (e: Exception) { null }
                            }
                            if (bmp != null) {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "Custom QR",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("QR", fontWeight = FontWeight.Bold, color = Color.Gray)
                                }
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedButton(
                                onClick = { customQrPicker.launch("image/*") },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(if (isHindi) "🔄 QR बदलें" else "Change QR", fontSize = 11.sp)
                            }
                            TextButton(
                                onClick = { onCustomUpiQrUriChange("") },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(if (isHindi) "❌ QR हटाएं" else "Remove QR", fontSize = 11.sp, color = Color.Red)
                            }
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = { customQrPicker.launch("image/*") },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isHindi) "📁 गैलरी से अपना QR कोड फोटो चुनें" else "Pick Custom QR Image from Gallery", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Divider(color = Color(0xFFE0E0E0))
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isHindi) "पेमेंट लेजर व इतिहास अनुमतियाँ (Access Control):" else "Payment Ledger Permissions:",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = Color.DarkGray
                )

                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isHindi) "👥 एडमिन (पुजारी / सेवकों) को पेमेंट लेजर देखने दें" else "Allow Admins to view Payment Ledger",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (isHindi) "अक्रिय रहने पर केवल सुपर एडमिन ही पेमेंट इतिहास देख सकते हैं।" else "If off, only Super Admin can view history.",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }
                    Switch(checked = canAdminViewPayments, onCheckedChange = onCanAdminViewPaymentsChange)
                }

                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isHindi) "📱 आम भक्तों को उनके पेमेंट की हिस्ट्री देखने दें" else "Allow Devotees to view their Payment History",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (isHindi) "भक्त अपने फोन पर की गई लेन-देन की रसीद देख सकेंगे।" else "Devotees can see their receipt on their phone.",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }
                    Switch(checked = canDevoteeViewPayments, onCheckedChange = onCanDevoteeViewPaymentsChange)
                }
            }
        }

        // SECTION 3.5: ARZI BOX DISTRIBUTION & PRICING CONTROL (SUPER ADMIN DIRECT CONTROL)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, if (isArziLedgerLive) Color(0xFFE65100) else Color(0xFFFFCC80))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Text("📦", fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (isHindi) "पवित्र अर्जी डिब्बा लेजर व दर नियंत्रण" else "Arzi Box Distribution & Price Matrix",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaroonPrimary
                            )
                            Text(
                                text = if (isHindi) "बड़ी/छोटी अर्जी दर, सेवादारों को एक्सेस व लेजर दृश्यता" else "Badi/Chhoti rates, Sevadar access & ledger",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }
                    Switch(checked = isArziLedgerLive, onCheckedChange = onArziLedgerLiveChange)
                }

                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = if (isArziLedgerLive) Color(0xFFFFF3E0) else Color(0xFFEEEEEE),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isArziLedgerLive)
                            (if (isHindi) "🟢 सक्रिय: अर्जी वितरण व लेजर रिकॉर्डिंग की सुविधा चालू है।" else "🟢 LIVE: Arzi distribution & voice ledger are active.")
                        else
                            (if (isHindi) "🔒 गुप्त/छिपा हुआ (Hidden): अर्जी सेवा छिपी हुई है। केवल सुपर एडमिन ही आवश्यकता पड़ने पर चालू कर सकते हैं।" else "🔒 HIDDEN: Arzi service is hidden from non-super admins."),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isArziLedgerLive) Color(0xFFE65100) else Color.DarkGray,
                        modifier = Modifier.padding(8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = badiArziRate,
                        onValueChange = onBadiArziRateChange,
                        label = { Text(if (isHindi) "बड़ी अर्जी दर (₹)" else "Badi Arzi Rate (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = chhotiArziRate,
                        onValueChange = onChhotiArziRateChange,
                        label = { Text(if (isHindi) "छोटी अर्जी दर (₹)" else "Chhoti Arzi Rate (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = Color(0xFFE0E0E0))
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isHindi) "अर्जी लेजर अनुमतियाँ (Access Delegation):" else "Arzi Ledger Permissions:",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = Color.DarkGray
                )

                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isHindi) "👥 अधिकृत सेवादारों को अर्जी लेजर देखने दें" else "Allow Assigned Sevadars to view Arzi Ledger",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (isHindi) "अक्रिय होने पर केवल सुपर एडमिन ही अर्जी लेजर देख सकेंगे।" else "If off, only Super Admin can view Arzi ledger.",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }
                    Switch(checked = canAdminViewArzi, onCheckedChange = onCanAdminViewArziChange)
                }

                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isHindi) "📱 भक्तों को उनका अर्जी हिसाब देखने दें" else "Allow Devotees to view their Arzi Status",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (isHindi) "भक्त अपने फोन पर अर्जी रसीद देख सकेंगे।" else "Devotees can view Arzi receipt.",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }
                    Switch(checked = canDevoteeViewArzi, onCheckedChange = onCanDevoteeViewArziChange)
                }
            }
        }

        // SECTION 4: MASTER FEATURE VISIBILITY MATRIX
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🎛️", fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isHindi) "समस्त ऐप दृश्यता नियंत्रण (Master Feature Visibility)" else "Master Feature Visibility Matrix",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaroonPrimary
                    )
                }

                Text(
                    text = if (isHindi)
                        "सुपर एडमिन ऐप के किसी भी फीचर को आम भक्तों के लिए ऑन या ऑफ कर सकते हैं।"
                    else
                        "Super Admin can dynamically hide or show ANY feature across the entire devotee application.",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                ServiceSwitchRow("🏷️ रविवार दरबार टोकन पंजीकरण (Sunday Token Generation)", isToken, onTokenChange)
                ServiceSwitchRow("🔢 लाइव टोकन काउंटर ट्रैकर (Live Darbar Queue Counter)", isLiveCounter, onLiveCounterChange)
                ServiceSwitchRow("🎪 वार्षिक उत्सव व कार्यक्रम (Annual Festivals & Events)", isEvents, onEventsChange)
                ServiceSwitchRow("⏰ आरती व दरबार समय सारणी (Aarti & Darbar Timings Card)", isAartiTimings, onAartiTimingsChange)
                ServiceSwitchRow("👑 गुरुजी परिचय व आश्रम इतिहास (Guruji Bio & Ashram Info)", isGurujiInfo, onGurujiInfoChange)
                ServiceSwitchRow("📢 आपातकालीन सूचना पट्टी (Emergency Announcement Banner)", isEmergencyNotice, onEmergencyNoticeChange)

                if (successMsg != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = Color(0xFFE8F5E9),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = successMsg,
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                // SECTION 5: YATRA EXPENSE DIARY PRIVACY (SUPER ADMIN ONLY BY DEFAULT)
                Card(
                    colors = CardDefaults.cardColors(containerColor = if (canDevoteeViewYatraDiary) Color(0xFFE8F5E9) else Color(0xFFFFF8E1)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (canDevoteeViewYatraDiary) Color(0xFF81C784) else Color(0xFFFFD54F)),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Text("📔", fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = if (isHindi) "यात्रा खर्च डायरी (Yatra Khata Diary) गोपनीयता" else "Yatra Expense Diary Privacy",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaroonPrimary
                                    )
                                    Text(
                                        text = if (canDevoteeViewYatraDiary)
                                            (if (isHindi) "🔓 खुला: भक्त व सेवादार भी यात्रा खर्च डायरी देख सकते हैं।" else "🔓 PUBLIC: Devotees can view expenses.")
                                        else
                                            (if (isHindi) "🔒 केवल सुपर एडमिन (अनुशंसित): भक्तों से पूर्णतः छिपा हुआ।" else "🔒 PRIVATE: Super Admin exclusive privilege."),
                                        fontSize = 11.sp,
                                        color = Color.DarkGray
                                    )
                                }
                            }
                            Switch(checked = canDevoteeViewYatraDiary, onCheckedChange = onCanDevoteeViewYatraDiaryChange)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (isHindi) "💾 समस्त सेटिंग्स सुरक्षित करें (Save Changes)" else "💾 Save All Controls", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
private fun ServiceSwitchRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun AppCustomizerTab(
    isHindi: Boolean,
    ashramName: String,
    onAshramNameChange: (String) -> Unit,
    gurujiName: String,
    onGurujiNameChange: (String) -> Unit,
    gurujiPhotoUri: String,
    onGurujiPhotoUriChange: (String) -> Unit,
    address: String,
    onAddressChange: (String) -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit,
    timings: String,
    onTimingsChange: (String) -> Unit,
    disclaimer: String,
    onDisclaimerChange: (String) -> Unit,
    emergencyNotice: String,
    onEmergencyNoticeChange: (String) -> Unit,
    whatsappGroup: String,
    onWhatsappGroupChange: (String) -> Unit,
    youtubeChannel: String,
    onYoutubeChannelChange: (String) -> Unit,
    facebookPage: String,
    onFacebookPageChange: (String) -> Unit,
    instagramPage: String,
    onInstagramPageChange: (String) -> Unit,
    appShareUrl: String,
    onAppShareUrlChange: (String) -> Unit,
    sundayTokenBannerTitle: String = "",
    onSundayTokenBannerTitleChange: (String) -> Unit = {},
    sundayTokenBannerText: String = "",
    onSundayTokenBannerTextChange: (String) -> Unit = {},
    sundayTokenCustomNotice: String = "",
    onSundayTokenCustomNoticeChange: (String) -> Unit = {},
    ashramParichayHindi: String = "",
    onAshramParichayHindiChange: (String) -> Unit = {},
    ashramParichayEnglish: String = "",
    onAshramParichayEnglishChange: (String) -> Unit = {},
    ashramHistoryHindi: String = "",
    onAshramHistoryHindiChange: (String) -> Unit = {},
    ashramRulesHindi: String = "",
    onAshramRulesHindiChange: (String) -> Unit = {},
    successMsg: String?,
    events: List<AshramEvent>,
    onOpenAddEvent: () -> Unit,
    onDeleteEvent: (Long) -> Unit,
    onSave: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val gurujiCameraLauncher = rememberLauncherForActivityResult(
        contract = TakeAnyPicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            val savedPath = DevoteePhotoHelper.saveGurujiPhoto(context, bitmap)
            if (savedPath.isNotBlank()) {
                onGurujiPhotoUriChange(savedPath)
                Toast.makeText(context, if (isHindi) "📸 गुरुजी की फोटो सुरक्षित, वेबसाइट व ऐप पर लाइव सिंक जारी..." else "Guruji photo saved, syncing...", Toast.LENGTH_SHORT).show()
                scope.launch(Dispatchers.IO) {
                    try {
                        val repo = AshramRepository(context)
                        repo.updateGurujiPhoto(savedPath)
                        val cloudUrl = com.example.shribalajikripadham.data.network.GitHubLiveSyncManager.uploadPhotoToGitHub(context, savedPath, "guruji_profile.jpg")
                        val finalUrl = if (!cloudUrl.isNullOrBlank()) "$cloudUrl?t=${System.currentTimeMillis()}" else savedPath
                        repo.updateGurujiPhoto(finalUrl)
                        com.example.shribalajikripadham.data.network.HostingerCentralSyncManager.syncSettingsToHostinger(repo.getSettings())
                        withContext(Dispatchers.Main) {
                            onGurujiPhotoUriChange(finalUrl)
                            Toast.makeText(context, if (isHindi) "✅ गुरुजी की फोटो वेबसाइट व ऐप पर 100% लाइव हो गई!" else "Guruji photo live on app & website!", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    val gurujiGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val bmp = DevoteePhotoHelper.loadBitmap(context, uri.toString())
            if (bmp != null) {
                val savedPath = DevoteePhotoHelper.saveGurujiPhoto(context, bmp)
                if (savedPath.isNotBlank()) {
                    onGurujiPhotoUriChange(savedPath)
                    Toast.makeText(context, if (isHindi) "📁 गुरुजी की फोटो सुरक्षित, वेबसाइट व ऐप पर लाइव सिंक जारी..." else "Guruji photo saved, syncing...", Toast.LENGTH_SHORT).show()
                    scope.launch(Dispatchers.IO) {
                        try {
                            val repo = AshramRepository(context)
                            repo.updateGurujiPhoto(savedPath)
                            val cloudUrl = com.example.shribalajikripadham.data.network.GitHubLiveSyncManager.uploadPhotoToGitHub(context, savedPath, "guruji_profile.jpg")
                            val finalUrl = if (!cloudUrl.isNullOrBlank()) "$cloudUrl?t=${System.currentTimeMillis()}" else savedPath
                            repo.updateGurujiPhoto(finalUrl)
                            com.example.shribalajikripadham.data.network.HostingerCentralSyncManager.syncSettingsToHostinger(repo.getSettings())
                            withContext(Dispatchers.Main) {
                                onGurujiPhotoUriChange(finalUrl)
                                Toast.makeText(context, if (isHindi) "✅ गुरुजी की फोटो वेबसाइट व ऐप पर 100% लाइव हो गई!" else "Guruji photo live on app & website!", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isHindi) "आश्रम का विवरण एवं सामग्री कस्टमाइजर" else "Ashram Details & Content Customizer",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaroonPrimary
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = ashramName,
                        onValueChange = onAshramNameChange,
                        label = { Text("आश्रम का नाम") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = gurujiName,
                        onValueChange = onGurujiNameChange,
                        label = { Text("पूज्य गुरुजी का नाम") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFFFD54F))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (isHindi) "👑 पूज्य गुरुजी की पावन फोटो (गैलरी / कैमरा)" else "👑 Revered Guruji Photo (Gallery / Camera)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaroonPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            SacredAvatar(photoUri = gurujiPhotoUri, name = gurujiName, size = 80.dp)
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { gurujiGalleryLauncher.launch("image/*") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1565C0))
                                ) {
                                    Text(if (isHindi) "🖼️ गैलरी से चुनें" else "🖼️ From Gallery", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                                OutlinedButton(
                                    onClick = { gurujiCameraLauncher.launch(null) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2E7D32))
                                ) {
                                    Text(if (isHindi) "📷 कैमरे से लें" else "📷 Take Photo", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            if (gurujiPhotoUri.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            val rotated = DevoteePhotoHelper.rotateSavedPhoto(context, gurujiPhotoUri, 90f)
                                            if (rotated.isNotBlank()) {
                                                onGurujiPhotoUriChange(rotated)
                                                Toast.makeText(context, if (isHindi) "🔄 गुरुजी की फोटो 90° सीधी हो गई!" else "🔄 Photo rotated 90°!", Toast.LENGTH_SHORT).show()
                                                scope.launch(Dispatchers.IO) {
                                                    val cloudUrl = com.example.shribalajikripadham.data.network.GitHubLiveSyncManager.uploadPhotoToGitHub(context, rotated, "guruji_profile.jpg")
                                                    if (!cloudUrl.isNullOrBlank()) {
                                                        withContext(Dispatchers.Main) {
                                                            onGurujiPhotoUriChange(cloudUrl)
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SaffronPrimary)
                                    ) {
                                        Text(if (isHindi) "🔄 90° सीधा करें" else "🔄 Rotate 90°", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                    TextButton(
                                        onClick = { onGurujiPhotoUriChange("") },
                                        colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                                    ) {
                                        Text(if (isHindi) "❌ फोटो हटाएं" else "❌ Remove Photo", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = gurujiPhotoUri,
                        onValueChange = onGurujiPhotoUriChange,
                        label = { Text(if (isHindi) "पूज्य गुरुजी फोटो (URL या फ़ाइल पाथ)" else "Guruji Photo URL / File Path") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = address,
                        onValueChange = onAddressChange,
                        label = { Text("आश्रम का पता") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = phone,
                        onValueChange = onPhoneChange,
                        label = { Text("आश्रम संपर्क नंबर") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = timings,
                        onValueChange = onTimingsChange,
                        label = { Text("दरबार समय सारिणी") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = disclaimer,
                        onValueChange = onDisclaimerChange,
                        label = { Text("100% निःशुल्क इलाज घोषणा") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = emergencyNotice,
                        onValueChange = onEmergencyNoticeChange,
                        label = { Text("आपातकालीन सूचना पट्टी (Emergency Ticker)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color(0xFFE0E0E0))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isHindi) "📖 आश्रम परिचय, इतिहास व नियम (भक्तों की स्क्रीन हेतु)" else "📖 Ashram Info, History & Rules",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaroonPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = ashramParichayHindi,
                        onValueChange = onAshramParichayHindiChange,
                        label = { Text(if (isHindi) "आश्रम परिचय (हिंदी)" else "Ashram Introduction (Hindi)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = ashramParichayEnglish,
                        onValueChange = onAshramParichayEnglishChange,
                        label = { Text("Ashram Introduction (English)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = ashramHistoryHindi,
                        onValueChange = onAshramHistoryHindiChange,
                        label = { Text(if (isHindi) "आश्रम का पावन इतिहास (हिंदी)" else "Ashram History (Hindi)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = ashramRulesHindi,
                        onValueChange = onAshramRulesHindiChange,
                        label = { Text(if (isHindi) "आश्रम नियम व मर्यादा (हिंदी)" else "Ashram Rules & Guidelines") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )

                    if (successMsg != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(successMsg, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onSave,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary)
                    ) {
                        Text(if (isHindi) "विवरण सुरक्षित करें" else "Save Details", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isHindi) "सोशल मीडिया व शेयरिंग लिंक्स (Social Media & Share Links)" else "Social Media & Share Links",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaroonPrimary
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = whatsappGroup,
                        onValueChange = onWhatsappGroupChange,
                        label = { Text(if (isHindi) "💬 व्हाट्सएप्प ग्रुप लिंक" else "WhatsApp Group URL") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = youtubeChannel,
                        onValueChange = onYoutubeChannelChange,
                        label = { Text(if (isHindi) "▶️ यूट्यूब चैनल लिंक" else "YouTube Channel URL") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = facebookPage,
                        onValueChange = onFacebookPageChange,
                        label = { Text(if (isHindi) "📘 फेसबुक पेज लिंक" else "Facebook Page URL") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = instagramPage,
                        onValueChange = onInstagramPageChange,
                        label = { Text(if (isHindi) "📸 इंस्टाग्राम प्रोफाइल लिंक" else "Instagram Profile URL") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = appShareUrl,
                        onValueChange = onAppShareUrlChange,
                        label = { Text(if (isHindi) "📲 ऍप डाउनलोड / शेयर लिंक" else "App Download / Share URL") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onSave,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                    ) {
                        Text(if (isHindi) "सोशल लिंक्स सुरक्षित करें" else "Save Social Links", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(3.dp),
                border = BorderStroke(1.2.dp, SaffronPrimary.copy(alpha = 0.6f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🎟️", fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isHindi) "रविवार टोकन बैनर व दिशा-निर्देश संपादक" else "Sunday Token Banner & Guidelines",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaroonPrimary
                        )
                    }
                    Text(
                        text = if (isHindi)
                            "यहाँ से रविवार टोकन स्क्रीन पर प्रदर्शित होने वाला मुख्य बैनर, 1 फोन = 1 टोकन नियम व विशेष सूचना पट्टी बदलें। यह तुरंत सभी भक्तों के फोन में लाइव अपडेट होगा।"
                        else
                            "Customize the Sunday Token banner, hardware restriction rules and live announcement ticker displayed on all devotee phones.",
                        fontSize = 12.sp,
                        color = Color.DarkGray,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = sundayTokenBannerTitle,
                        onValueChange = onSundayTokenBannerTitleChange,
                        label = { Text(if (isHindi) "टोकन बैनर मुख्य शीर्षक" else "Sunday Token Banner Title") },
                        placeholder = { Text("हार्डवेयर फिंगरप्रिंट नियम: 1 फोन = 1 टोकन") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = sundayTokenBannerText,
                        onValueChange = onSundayTokenBannerTextChange,
                        label = { Text(if (isHindi) "टोकन बैनर नियम विवरण" else "Sunday Token Banner Details") },
                        placeholder = { Text("एक मोबाइल डिवाइस से प्रत्येक रविवार को केवल 1 टोकन लिया जा सकता है।") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = sundayTokenCustomNotice,
                        onValueChange = onSundayTokenCustomNoticeChange,
                        label = { Text(if (isHindi) "विशेष लाइव सूचना पट्टी (Notice Ticker)" else "Sunday Token Live Notice") },
                        placeholder = { Text("विशेष सूचना: कृपया आश्रम पहुंचकर ही टोकन प्राप्त करें।") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = onSave,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary)
                    ) {
                        Text(
                            if (isHindi) "💾 रविवार टोकन बैनर सुरक्षित व लाइव करें" else "Save & Publish Sunday Banner",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isHindi) "वार्षिक उत्सव व कार्यक्रम (${events.size})" else "Events & Festivals (${events.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaroonPrimary
                )
                Button(
                    onClick = onOpenAddEvent,
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary)
                ) {
                    Text("+ नया उत्सव")
                }
            }
        }

        items(events) { ev ->
            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(ev.titleHindi, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaroonPrimary)
                        IconButton(onClick = { onDeleteEvent(ev.id) }) {
                            Text("✕", color = Color.Red, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text(ev.dateDescriptionHindi, fontSize = 12.sp, color = SaffronPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(ev.detailsHindi, fontSize = 13.sp, color = Color.DarkGray)
                }
            }
        }
    }
}

@Composable
fun AutoUpdateManagerTab(
    isHindi: Boolean,
    currentCode: Int,
    currentName: String,
    targetCode: String,
    onTargetCodeChange: (String) -> Unit,
    targetName: String,
    onTargetNameChange: (String) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    apkUrl: String,
    onApkUrlChange: (String) -> Unit,
    isForce: Boolean,
    onForceChange: (Boolean) -> Unit,
    successMsg: String?,
    onSave: () -> Unit
) {
    val context = LocalContext.current
    var showTestPopup by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (isHindi) "ऐप ऑटो-अपडेट विन्यास" else "App Auto-Update Manager",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaroonPrimary
                )
                Text(
                    text = "वर्तमान स्थापित संस्करण (Installed Version): v$currentName (Build #$currentCode)",
                    fontSize = 13.sp,
                    color = Color.DarkGray,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = targetCode,
                    onValueChange = onTargetCodeChange,
                    label = { Text("नवीनतम वर्जन कोड (Latest Version Code e.g. 2)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = targetName,
                    onValueChange = onTargetNameChange,
                    label = { Text("नवीनतम वर्जन नाम (Latest Version Name e.g. 1.1)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = onNotesChange,
                    label = { Text("अपडेट विवरण व नए फीचर्स (Release Notes)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = apkUrl,
                    onValueChange = onApkUrlChange,
                    label = { Text("APK डाउनलोड लिंक या प्ले स्टोर लिंक") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = isForce, onCheckedChange = onForceChange)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        if (isHindi) "अनिवार्य अपडेट (Force Update - बिना अपडेट ऐप न चले)" else "Force Update (Devotees must update)",
                        fontSize = 13.sp
                    )
                }

                val tCode = targetCode.toIntOrNull() ?: 0
                if (tCode <= currentCode) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = Color(0xFFFFF8E1),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("💡 ", fontSize = 16.sp)
                            Text(
                                text = if (isHindi) "महत्वपूर्ण: भक्तों के फोन पर अपडेट पॉपअप तभी दिखेगा जब 'नवीनतम वर्जन कोड' वर्तमान कोड ($currentCode) से बड़ा होगा (जैसे ${currentCode + 1})।"
                                else "Important: The update popup triggers on devotees' devices only when 'Latest Version Code' is greater than installed code ($currentCode), e.g. ${currentCode + 1}.",
                                fontSize = 11.sp,
                                color = Color(0xFF5D4037),
                                lineHeight = 15.sp
                            )
                        }
                    }
                }

                if (successMsg != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(successMsg, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                }

                // Quick 1-Click Version Increment
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        val currentNum = targetCode.toIntOrNull() ?: currentCode
                        val nextCode = if (currentNum <= currentCode) currentCode + 1 else currentNum + 1
                        onTargetCodeChange(nextCode.toString())
                        onTargetNameChange("2.3.$nextCode")
                    },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SaffronPrimary)
                ) {
                    Text(if (isHindi) "➕ नया वर्जन कोड (${(targetCode.toIntOrNull() ?: currentCode) + 1}) सेट करें" else "➕ Set Next Version Code (+1)", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onSave,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary)
                ) {
                    Text(if (isHindi) "अपडेट विन्यास सुरक्षित व जारी करें" else "Save & Publish Update", fontWeight = FontWeight.Bold)
                }

                // Live Preview / Test Dialog Button
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { showTestPopup = true },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaroonPrimary)
                ) {
                    Text(if (isHindi) "👁️ अपडेट पॉपअप का तुरंत टेस्ट देखें (Preview Popup)" else "👁️ Preview In-App Update Popup", fontWeight = FontWeight.Bold)
                }

                if (apkUrl.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    val context = LocalContext.current
                    OutlinedButton(
                        onClick = {
                            AppUpdateManager.downloadAndInstallUpdate(context, apkUrl)
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaroonPrimary)
                    ) {
                        Text(if (isHindi) "📲 अभी अपडेट डाउनलोड व इंस्टॉल करें" else "📲 Download & Install Update Now", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // --- TEST PREVIEW UPDATE POPUP DIALOG ---
    if (showTestPopup) {
        val testVersionName = if (targetName.isNotBlank()) targetName else "2.3.0"
        val testVersionCode = targetCode.ifBlank { "${currentCode + 1}" }
        val testNotes = notes.ifBlank { "8 नए सुपर एडमिन नियंत्रण फीचर्स, पासवर्ड चेंज, CSV एक्सपोर्ट, टोकन कोटा..." }

        Dialog(onDismissRequest = { showTestPopup = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(8.dp),
                modifier = Modifier.fillMaxWidth().padding(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🚀", fontSize = 34.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (isHindi) "नया संस्करण उपलब्ध है! (पूर्वावलोकन)" else "New Version Available! (Preview)",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaroonPrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = MaroonPrimary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "v$currentName ➔ v$testVersionName (Build #$testVersionCode)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaroonPrimary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (isHindi) "नवीनतम बदलाव (What's New):" else "What's New in this Update:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.DarkGray,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = Color(0xFFE8F5E9),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFF81C784)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🛡️", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (isHindi) "सुरक्षा पैच एवं सिस्टम स्थिरता सुधार" else "Security Patch & System Stability",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1B5E20)
                                )
                                Text(
                                    text = if (isHindi) "टोकन व दर्शन सेवा की गति एवं सुरक्षा बढ़ाई गई है।" else "Enhanced token & darshan service performance and security.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            showTestPopup = false
                            if (apkUrl.isNotBlank()) {
                                AppUpdateManager.downloadAndInstallUpdate(context, apkUrl)
                            } else {
                                Toast.makeText(context, "परीक्षण पॉपअप सफल! भक्त इसी प्रकार अपडेट करेंगे।", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(if (isHindi) "⚡ तुरंत अपडेट करें (Update Now)" else "Update Now", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { showTestPopup = false }) {
                        Text(if (isHindi) "बंद करें (Close Preview)" else "Close Preview", color = Color.Gray)
                    }
                }
            }
        }
    }
}

// ==========================================
// CUSTOM DISTANCES TAB
// ==========================================
@Composable
fun CustomDistancesTab(
    isHindi: Boolean,
    customDistances: List<CustomCityDistance>,
    onAddDistance: (String, Float) -> Unit,
    onDeleteDistance: (Long) -> Unit
) {
    var cityNameInput by remember { mutableStateOf("") }
    var distanceKmInput by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var successMsg by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isHindi) "📍 स्थानीय गाँव / शहर एवं सड़क दूरी जोड़ें" else "📍 Add Custom Village / City Distance",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaroonPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isHindi) 
                            "श्री बालाजी कृपा धाम, डुंगरा जाट से अपने स्थानीय गाँव या शहर की सड़क दूरी जोड़ें। टोकन रजिस्ट्रेशन में यह दूरी तुरंत स्वतः आएगी।"
                            else "Add road distance to any local village/city from Shri Balaji Kripa Dham.",
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = cityNameInput,
                        onValueChange = { cityNameInput = it; errorMsg = null },
                        label = { Text(if (isHindi) "गाँव या शहर का नाम (जैसे: जहांगीरपुर, औरंगाबाद)" else "Village or City Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = distanceKmInput,
                        onValueChange = { distanceKmInput = it; errorMsg = null },
                        label = { Text(if (isHindi) "आश्रम से सड़क दूरी (किमी में, जैसे: 18.5)" else "Road Distance (in KM, e.g.: 18.5)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    if (errorMsg != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(errorMsg!!, color = Color.Red, fontSize = 12.sp)
                    }
                    if (successMsg != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(successMsg!!, color = Color(0xFF2E7D32), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val name = cityNameInput.trim()
                            val dist = distanceKmInput.toFloatOrNull()
                            if (name.isBlank()) {
                                errorMsg = if (isHindi) "कृपया गाँव या शहर का नाम लिखें" else "Please enter village/city name"
                                return@Button
                            }
                            if (dist == null || dist < 0) {
                                errorMsg = if (isHindi) "कृपया मान्य दूरी (किमी में) लिखें" else "Please enter valid distance in km"
                                return@Button
                            }
                            onAddDistance(name, dist)
                            successMsg = if (isHindi) "स्थान '$name' (${dist} KM) सफलतापूर्वक जोड़ा गया!" else "Location '$name' (${dist} KM) added successfully!"
                            cityNameInput = ""
                            distanceKmInput = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) {
                        Text(if (isHindi) "➕ नया स्थान व दूरी सुरक्षित करें" else "➕ Save Location & Distance", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Text(
                text = if (isHindi) "सूचीबद्ध कस्टम स्थान (${customDistances.size})" else "Custom Added Locations (${customDistances.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaroonPrimary
            )
        }

        if (customDistances.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(20.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (isHindi) "अभी तक कोई कस्टम स्थान नहीं जोड़ा गया है।" else "No custom locations added yet.",
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }

        items(customDistances) { item ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(10.dp),
                elevation = CardDefaults.cardElevation(1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(item.cityName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("सड़क दूरी: ${item.distanceKm} किमी", fontSize = 12.sp, color = SaffronDark)
                    }
                    IconButton(onClick = { onDeleteDistance(item.id) }) {
                        Text("🗑️", fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

// ==========================================
// SUPER CONTROL TAB
// ==========================================
@Composable
fun SuperControlTab(
    isHindi: Boolean,
    settings: AshramSettings,
    superAdmin: Admin? = null,
    repository: AshramRepository? = null,
    onRefreshData: () -> Unit = {},
    onUpdateSuperAdminProfile: (String, String, String, String?, String?, String?, (Boolean, String) -> Unit) -> Unit = { _, _, _, _, _, _, cb -> cb(true, "") },
    onUpdateMasterPassword: (String, String, (Boolean, String) -> Unit) -> Unit,
    onUpdateMaxDailyTokens: (Int) -> Unit,
    onUpdateEnforcedLayout: (String, Boolean) -> Unit,
    onUpdateCloudSync: (String, Boolean) -> Unit,
    onTriggerCloudSync: ((Boolean, String) -> Unit) -> Unit,
    onExportDatabaseBackup: ((Boolean, String) -> Unit) -> Unit,
    onRestoreDatabaseBackup: (String, (Boolean, String) -> Unit) -> Unit
) {
    val context = LocalContext.current

    // Super Admin Profile State
    var superNameInput by remember(superAdmin?.id, superAdmin?.name) { mutableStateOf(superAdmin?.name ?: "सुपर एडमिन") }
    var superPhoneInput by remember(superAdmin?.id, superAdmin?.phoneNumber, settings.contactPhone) { mutableStateOf(superAdmin?.phoneNumber?.ifBlank { settings.contactPhone } ?: settings.contactPhone) }
    var superUsernameInput by remember(superAdmin?.id, superAdmin?.username) { mutableStateOf(superAdmin?.username ?: "superadmin") }
    var superPasswordInput by remember { mutableStateOf("") }
    var superPinInput by remember { mutableStateOf("") }
    var superPhotoUri by remember(superAdmin?.id, superAdmin?.photoUri) { mutableStateOf(superAdmin?.photoUri ?: "") }
    var superProfileErrorMsg by remember { mutableStateOf<String?>(null) }
    var superProfileSuccessMsg by remember { mutableStateOf<String?>(null) }
    var isSavingSuperProfile by remember { mutableStateOf(false) }

    // Ashram Parichay, History, Rules and Yatra Diary State
    var parichayHiInput by remember(settings.ashramParichayHindi) { mutableStateOf(settings.ashramParichayHindi) }
    var parichayEnInput by remember(settings.ashramParichayEnglish) { mutableStateOf(settings.ashramParichayEnglish) }
    var historyHiInput by remember(settings.ashramHistoryHindi) { mutableStateOf(settings.ashramHistoryHindi) }
    var rulesHiInput by remember(settings.ashramRulesHindi) { mutableStateOf(settings.ashramRulesHindi) }
    var canDevoteeYatraDiaryChecked by remember(settings.canDevoteeViewYatraDiary) { mutableStateOf(settings.canDevoteeViewYatraDiary) }
    var ashramInfoSaveMsg by remember { mutableStateOf<String?>(null) }
    var isSavingAshramInfo by remember { mutableStateOf(false) }

    val superCameraLauncher = rememberLauncherForActivityResult(
        contract = TakeAnyPicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            val savedPath = DevoteePhotoHelper.saveDevoteePhoto(context, bitmap, "super_admin")
            if (savedPath.isNotBlank()) {
                superPhotoUri = savedPath
                Toast.makeText(context, if (isHindi) "📸 सुपर एडमिन फोटो सेट, क्लाउड सिंक जारी..." else "Photo set, syncing...", Toast.LENGTH_SHORT).show()
                kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                    val safeName = "super_admin_${System.currentTimeMillis()}.jpg"
                    val cloudUrl = com.example.shribalajikripadham.data.network.GitHubLiveSyncManager.uploadPhotoToGitHub(context, savedPath, safeName)
                    if (!cloudUrl.isNullOrBlank()) {
                        withContext(Dispatchers.Main) {
                            superPhotoUri = cloudUrl
                        }
                    }
                }
            }
        }
    }

    val superGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val bmp = DevoteePhotoHelper.loadBitmap(context, uri.toString())
            if (bmp != null) {
                val savedPath = DevoteePhotoHelper.saveDevoteePhoto(context, bmp, "super_admin")
                if (savedPath.isNotBlank()) {
                    superPhotoUri = savedPath
                    Toast.makeText(context, if (isHindi) "📁 गैलरी से फोटो चुनी गई, क्लाउड सिंक जारी..." else "Photo selected, syncing...", Toast.LENGTH_SHORT).show()
                    kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                        val safeName = "super_admin_${System.currentTimeMillis()}.jpg"
                        val cloudUrl = com.example.shribalajikripadham.data.network.GitHubLiveSyncManager.uploadPhotoToGitHub(context, savedPath, safeName)
                        if (!cloudUrl.isNullOrBlank()) {
                            withContext(Dispatchers.Main) {
                                superPhotoUri = cloudUrl
                            }
                        }
                    }
                }
            }
        }
    }

    var currentPassInput by remember { mutableStateOf("") }
    var newPassInput by remember { mutableStateOf("") }
    var confirmPassInput by remember { mutableStateOf("") }
    var passErrorMsg by remember { mutableStateOf<String?>(null) }
    var passSuccessMsg by remember { mutableStateOf<String?>(null) }

    var quotaInput by remember(settings.maxDailyTokens) { mutableStateOf(settings.maxDailyTokens.toString()) }
    var quotaSuccessMsg by remember { mutableStateOf<String?>(null) }

    var selectedLayoutKey by remember(settings.activeUiLayout) { mutableStateOf(settings.activeUiLayout) }
    var isEnforcedChecked by remember(settings.isUiLayoutEnforced) { mutableStateOf(settings.isUiLayoutEnforced) }
    var layoutSuccessMsg by remember { mutableStateOf<String?>(null) }

    var cloudUrlInput by remember(settings.cloudSyncUrl) { mutableStateOf(settings.cloudSyncUrl) }
    var isCloudEnabledChecked by remember(settings.isCloudSyncEnabled) { mutableStateOf(settings.isCloudSyncEnabled) }
    var cloudSyncStatusMsg by remember { mutableStateOf<String?>(null) }
    var isCloudSyncing by remember { mutableStateOf(false) }

    var backupRestoreStatusMsg by remember { mutableStateOf<String?>(null) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var restoreJsonText by remember { mutableStateOf("") }

    // --- Token TTS Human Voice State ---
    var selectedVoicePreset by remember(settings.tokenVoicePreset) { mutableStateOf(settings.tokenVoicePreset) }
    var voiceSuccessMsg by remember { mutableStateOf<String?>(null) }

    // --- Ashram Main Home Banner Manager State ---
    var bannerPhotoUriInput by remember(settings.bannerPhotoUri) { mutableStateOf(settings.bannerPhotoUri) }
    var isBannerVisibleChecked by remember(settings.isBannerVisible) { mutableStateOf(settings.isBannerVisible) }
    var bannerTitleInput by remember(settings.bannerTitle) { mutableStateOf(settings.bannerTitle) }
    var bannerSubtitleInput by remember(settings.bannerSubtitle) { mutableStateOf(settings.bannerSubtitle) }
    var bannerActionUrlInput by remember(settings.bannerActionUrl) { mutableStateOf(settings.bannerActionUrl) }
    var bannerSaveMsg by remember { mutableStateOf<String?>(null) }

    val bannerGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val bmp = DevoteePhotoHelper.loadBitmap(context, uri.toString())
            if (bmp != null) {
                val savedPath = DevoteePhotoHelper.saveDevoteePhoto(context, bmp, "ashram_banner")
                if (savedPath.isNotBlank()) {
                    bannerPhotoUriInput = savedPath
                    Toast.makeText(context, if (isHindi) "बैनर फोटो सेट, क्लाउड सिंक जारी..." else "Banner photo set, syncing...", Toast.LENGTH_SHORT).show()
                    kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                        val safeName = "banner_" + System.currentTimeMillis() + ".jpg"
                        val cloudUrl = com.example.shribalajikripadham.data.network.GitHubLiveSyncManager.uploadPhotoToGitHub(context, savedPath, safeName)
                        if (!cloudUrl.isNullOrBlank()) {
                            withContext(Dispatchers.Main) {
                                bannerPhotoUriInput = cloudUrl
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Future Ads & Devotee Sponsorship Master Control State ---
    var isAdsEnabledChecked by remember(settings.isAdsEnabled) { mutableStateOf(settings.isAdsEnabled) }
    var adTypeInput by remember(settings.adType) { mutableStateOf(settings.adType) }
    var adBannerPhotoUriInput by remember(settings.adBannerPhotoUri) { mutableStateOf(settings.adBannerPhotoUri) }
    var adBannerTitleInput by remember(settings.adBannerTitle) { mutableStateOf(settings.adBannerTitle) }
    var adBannerDescInput by remember(settings.adBannerDescription) { mutableStateOf(settings.adBannerDescription) }
    var adTargetUrlInput by remember(settings.adTargetUrl) { mutableStateOf(settings.adTargetUrl) }
    var adPlacementInput by remember(settings.adPlacement) { mutableStateOf(settings.adPlacement) }
    var adSaveMsg by remember { mutableStateOf<String?>(null) }

    val adBannerGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val bmp = DevoteePhotoHelper.loadBitmap(context, uri.toString())
            if (bmp != null) {
                val savedPath = DevoteePhotoHelper.saveDevoteePhoto(context, bmp, "ad_banner")
                if (savedPath.isNotBlank()) {
                    adBannerPhotoUriInput = savedPath
                    Toast.makeText(context, if (isHindi) "विज्ञापन बैनर फोटो सेट!" else "Ad banner photo set!", Toast.LENGTH_SHORT).show()
                    kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                        val safeName = "ad_banner_" + System.currentTimeMillis() + ".jpg"
                        val cloudUrl = com.example.shribalajikripadham.data.network.GitHubLiveSyncManager.uploadPhotoToGitHub(context, savedPath, safeName)
                        if (!cloudUrl.isNullOrBlank()) {
                            withContext(Dispatchers.Main) {
                                adBannerPhotoUriInput = cloudUrl
                            }
                        }
                    }
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 0. SUPER ADMIN PROFILE & CONTACT DETAILS CARD
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isHindi) "👑 सुपर एडमिन प्रोफ़ाइल व संपर्क विवरण" else "👑 Super Admin Profile & Contact Details",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaroonPrimary
                    )
                    Text(
                        text = if (isHindi) "यहाँ से आप अपना नाम, मोबाइल नंबर, यूजर आईडी, मास्टर पासवर्ड, सुरक्षा पिन व फोटो बदल सकते हैं। यहाँ से अपडेट किया गया मोबाइल नंबर स्वतः सभी भक्तों व आश्रम संपर्क विवरण में लाइव क्लाउड सिंक हो जाएगा।" else "Update your name, mobile phone, username, master password, PIN, and photo. The phone number syncs to all devotees' apps instantly.",
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )

                    superProfileErrorMsg?.let { err ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                            border = BorderStroke(1.dp, Color(0xFFE57373))
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("⚠️", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(err, color = Color(0xFFC62828), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    superProfileSuccessMsg?.let { msg ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                            border = BorderStroke(1.dp, Color(0xFFA5D6A7))
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("✓", fontSize = 16.sp, color = Color(0xFF2E7D32))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(msg, color = Color(0xFF2E7D32), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = superNameInput,
                        onValueChange = { superNameInput = it; superProfileErrorMsg = null; superProfileSuccessMsg = null },
                        label = { Text(if (isHindi) "सुपर एडमिन का नाम / पद" else "Full Name / Title") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = superPhoneInput,
                        onValueChange = { superPhoneInput = it; superProfileErrorMsg = null; superProfileSuccessMsg = null },
                        label = { Text(if (isHindi) "मोबाइल / आश्रम संपर्क नंबर (Live Phone for Devotees)" else "Mobile / Ashram Phone") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = superUsernameInput,
                        onValueChange = { superUsernameInput = it; superProfileErrorMsg = null; superProfileSuccessMsg = null },
                        label = { Text(if (isHindi) "यूजर आईडी (Unique Username)" else "Unique Username") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = superPasswordInput,
                        onValueChange = { superPasswordInput = it; superProfileErrorMsg = null; superProfileSuccessMsg = null },
                        label = { Text(if (isHindi) "नया मास्टर पासवर्ड (खाली छोड़ने पर पुराना रहेगा)" else "New Master Password (optional)") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = superPinInput,
                        onValueChange = { superPinInput = it; superProfileErrorMsg = null; superProfileSuccessMsg = null },
                        label = { Text(if (isHindi) "नया 4-अंकीय मास्टर पिन (खाली छोड़ने पर पुराना रहेगा)" else "New 4-digit PIN (optional)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFFFE082))
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (isHindi) "📷 सुपर एडमिन प्रोफाइल फोटो" else "📷 Super Admin Photo",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = MaroonPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            SacredAvatar(photoUri = superPhotoUri, name = superNameInput.ifEmpty { "Super Admin" }, size = 64.dp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { superGalleryLauncher.launch("image/*") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1565C0))
                                ) {
                                    Text(if (isHindi) "🖼️ गैलरी" else "🖼️ Gallery", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = { superCameraLauncher.launch(null) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2E7D32))
                                ) {
                                    Text(if (isHindi) "📷 कैमरा" else "📷 Camera", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                if (superPhotoUri.isNotBlank()) {
                                    OutlinedButton(
                                        onClick = {
                                            val rotated = DevoteePhotoHelper.rotateSavedPhoto(context, superPhotoUri, 90f)
                                            if (rotated.isNotBlank()) {
                                                superPhotoUri = rotated
                                                Toast.makeText(context, if (isHindi) "🔄 फोटो 90° सीधी हो गई!" else "🔄 Photo rotated 90°!", Toast.LENGTH_SHORT).show()
                                                kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                                                    val safeName = "super_admin_${System.currentTimeMillis()}.jpg"
                                                    val cloudUrl = com.example.shribalajikripadham.data.network.GitHubLiveSyncManager.uploadPhotoToGitHub(context, rotated, safeName)
                                                    if (!cloudUrl.isNullOrBlank()) {
                                                        withContext(Dispatchers.Main) {
                                                            superPhotoUri = cloudUrl
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SaffronPrimary)
                                    ) {
                                        Text("🔄 90°", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    OutlinedButton(
                                        onClick = { superPhotoUri = "" },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                                    ) {
                                        Text("❌", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                        onClick = {
                            isSavingSuperProfile = true
                            superProfileErrorMsg = null
                            superProfileSuccessMsg = null
                            onUpdateSuperAdminProfile(
                                superNameInput,
                                superPhoneInput,
                                superUsernameInput,
                                superPasswordInput.ifBlank { null },
                                superPinInput.ifBlank { null },
                                superPhotoUri
                            ) { success, msg ->
                                isSavingSuperProfile = false
                                if (success) {
                                    superProfileSuccessMsg = if (isHindi) "✓ सुपर एडमिन प्रोफाइल व संपर्क नंबर सफलतापूर्वक अपडेट व लाइव क्लाउड सिंक हो गए!" else "Profile & phone updated and synced to cloud!"
                                    superPasswordInput = ""
                                    superPinInput = ""
                                } else {
                                    superProfileErrorMsg = msg
                                }
                            }
                        },
                        enabled = !isSavingSuperProfile,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = if (isSavingSuperProfile) (if (isHindi) "सुरक्षित हो रहा है..." else "Saving...") else (if (isHindi) "💾 विवरण सुरक्षित करें व क्लाउड सिंक करें" else "Save Profile & Sync Cloud"),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 1. CHANGE MASTER PASSWORD CARD
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isHindi) "🔑 सुपर एडमिन मास्टर पासवर्ड बदलें" else "🔑 Change Super Admin Password",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaroonPrimary
                    )
                    Text(
                        text = if (isHindi) "यहाँ से आप सुपर एडमिन का मुख्य पासवर्ड सीधे बदल सकते हैं।" else "Update your master login password securely.",
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = currentPassInput,
                        onValueChange = { currentPassInput = it; passErrorMsg = null },
                        label = { Text(if (isHindi) "वर्तमान पासवर्ड दर्ज करें" else "Current Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPassInput,
                        onValueChange = { newPassInput = it; passErrorMsg = null },
                        label = { Text(if (isHindi) "नया पासवर्ड दर्ज करें" else "New Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirmPassInput,
                        onValueChange = { confirmPassInput = it; passErrorMsg = null },
                        label = { Text(if (isHindi) "नए पासवर्ड की पुष्टि करें" else "Confirm New Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    if (passErrorMsg != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(passErrorMsg!!, color = Color.Red, fontSize = 12.sp)
                    }
                    if (passSuccessMsg != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(passSuccessMsg!!, color = Color(0xFF2E7D32), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            if (currentPassInput.isBlank()) {
                                passErrorMsg = if (isHindi) "कृपया वर्तमान पासवर्ड दर्ज करें" else "Enter current password"
                                return@Button
                            }
                            if (newPassInput.length < 4) {
                                passErrorMsg = if (isHindi) "नया पासवर्ड कम से कम 4 अक्षरों का होना चाहिए" else "Password must be at least 4 characters"
                                return@Button
                            }
                            if (newPassInput != confirmPassInput) {
                                passErrorMsg = if (isHindi) "दोनों नए पासवर्ड आपस में मेल नहीं खाते!" else "New passwords do not match!"
                                return@Button
                            }

                            onUpdateMasterPassword(currentPassInput, newPassInput) { success, msg ->
                                if (success) {
                                    passSuccessMsg = msg
                                    passErrorMsg = null
                                    currentPassInput = ""
                                    newPassInput = ""
                                    confirmPassInput = ""
                                } else {
                                    passErrorMsg = msg
                                    passSuccessMsg = null
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) {
                        Text(if (isHindi) "🔒 पासवर्ड अपडेट करें" else "🔒 Update Password", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 2. DAILY TOKEN LIMIT (QUOTA) CARD
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isHindi) "📊 प्रतिदिन अधिकतम टोकन सीमा (Daily Quota)" else "📊 Daily Max Token Quota",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaroonPrimary
                    )
                    Text(
                        text = if (isHindi) 
                            "दरबार में प्रतिदिन कितने टोकन काटे जा सकते हैं। सीमा पूरी होने पर टोकन बुकिंग स्वतः बंद हो जाएगी। (0 = असीमित/कोई सीमा नहीं)" 
                            else "Set max tokens per Sunday Darbar. Booking locks automatically once limit is reached. (0 = unlimited)",
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = quotaInput,
                        onValueChange = { quotaInput = it; quotaSuccessMsg = null },
                        label = { Text(if (isHindi) "टोकन संख्या (जैसे: 500)" else "Quota (e.g. 500)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    if (quotaSuccessMsg != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(quotaSuccessMsg!!, color = Color(0xFF2E7D32), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            val num = quotaInput.toIntOrNull() ?: 0
                            onUpdateMaxDailyTokens(num)
                            quotaSuccessMsg = if (isHindi) "दैनिक टोकन सीमा ${if (num == 0) "असीमित" else "$num टोकन"} सुरक्षित की गई!" else "Daily token quota updated!"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) {
                        Text(if (isHindi) "💾 टोकन कोटा सुरक्षित करें" else "💾 Save Token Quota", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 3. ENFORCE UI LAYOUT FOR DEVOTEES
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isHindi) "🎨 डिफ़ॉल्ट UI लेआउट व अनिवार्यता नियंत्रण" else "🎨 Default UI Layout & Enforcement",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaroonPrimary
                    )
                    Text(
                        text = if (isHindi) 
                            "सुपर एडमिन यह तय कर सकता है कि सभी भक्तों के फोन पर कौन सा UI लेआउट डिफॉल्ट दिखेगा और क्या भक्त इसे बदल सकते हैं।"
                            else "Select default UI layout and choose whether to enforce it across all devotees.",
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    AppUiLayout.entries.forEach { layout ->
                        val isSelected = (selectedLayoutKey == layout.id)
                        Surface(
                            onClick = { selectedLayoutKey = layout.id },
                            color = if (isSelected) Color(0xFFFFF3E0) else Color(0xFFF9F9F9),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (isSelected) SaffronPrimary else Color(0xFFE0E0E0)),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(layout.icon, fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isHindi) "${layout.titleHindi} (${layout.titleEnglish})" else layout.titleEnglish,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp,
                                        color = if (isSelected) MaroonPrimary else Color.Black
                                    )
                                    Text(
                                        text = layout.subtitleHindi,
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }
                                if (isSelected) {
                                    Text("✓ सक्रिय", color = SaffronPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = isEnforcedChecked, onCheckedChange = { isEnforcedChecked = it })
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isHindi) "🔒 सभी भक्तों के फोन पर अनिवार्य करें (भक्त बदल नहीं सकेंगे)" else "Enforce on all devotees (devotees cannot change)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (layoutSuccessMsg != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(layoutSuccessMsg!!, color = Color(0xFF2E7D32), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            onUpdateEnforcedLayout(selectedLayoutKey, isEnforcedChecked)
                            layoutSuccessMsg = if (isHindi) "UI लेआउट नियम सफलतापूर्वक सुरक्षित हुआ और सभी उपयोगकर्ताओं पर लागू हुआ!" else "Layout policy saved successfully!"
                            Toast.makeText(context, if (isHindi) "✓ लेआउट सेटिंग्स सुरक्षित!" else "Layout settings saved!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) {
                        Text(if (isHindi) "💾 लेआउट नियम सुरक्षित करें" else "💾 Save Layout Policy", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 3.1 TOKEN TTS REAL HUMAN VOICE SELECTION & TEST AUDIO (GENUINE HUMAN & RECORDED VOICES)
        item {
            var isRecordingAudio by remember { mutableStateOf(false) }
            var hasRecording by remember { mutableStateOf(AshramVoiceAnnouncementManager.hasCustomRecording(context)) }

            val recordAudioLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                if (isGranted) {
                    val started = AshramVoiceAnnouncementManager.startCustomRecording(context)
                    if (started) {
                        isRecordingAudio = true
                        Toast.makeText(context, "🎙️ रिकॉर्डिंग शुरू... स्पष्ट बोलें", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "रिकॉर्डिंग शुरू करने में विफल", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "ऑडियो रिकॉर्डिंग हेतु माइक्रोफ़ोन अनुमति आवश्यक है", Toast.LENGTH_SHORT).show()
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🎙️", fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isHindi) "टोकन उद्घोषणा: वास्तविक इंसानी आवाज़ें एवं लाइव रिकॉर्डिंग" else "Token Voice: Real Human Audio & Live Recording",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaroonPrimary
                        )
                    }
                    Text(
                        text = if (isHindi)
                            "100% असली प्राकृतिक HD आवाज़ (पुरुष व महिला), आपकी अपनी लाइव रिकॉर्डेड आवाज़, अथवा फ़ोन का डिफ़ॉल्ट ऑफ़लाइन स्वर। किसी भी आवाज़ को '▶️ सुनें' दबाकर तुरंत टेस्ट करें।"
                            else "100% Real human HD audio, in-app live microphone recording, or offline device TTS. Test any voice instantly.",
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Live In-App Voice Recording Studio
                    Surface(
                        color = if (isRecordingAudio) Color(0xFFFFEBEE) else Color(0xFFF1F8E9),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.2.dp, if (isRecordingAudio) Color(0xFFD32F2F) else Color(0xFF81C784)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(if (isRecordingAudio) "🔴" else "🎙️", fontSize = 18.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = if (isRecordingAudio) "माइक से रिकॉर्डिंग जारी है..." else "आश्रम लाइव वॉयस रिकॉर्डिंग स्टूडियो",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isRecordingAudio) Color(0xFFC62828) else Color(0xFF2E7D32)
                                        )
                                        Text(
                                            text = if (isRecordingAudio) "स्पष्ट टोकन उद्घोषणा बोलें..." else "पंडित जी / मुख्य सेवादार की आवाज़ में रिकॉर्ड करें",
                                            fontSize = 10.sp,
                                            color = Color.DarkGray
                                        )
                                    }
                                }
                                if (hasRecording && !isRecordingAudio) {
                                    Surface(color = Color(0xFFE8F5E9), shape = RoundedCornerShape(4.dp)) {
                                        Text("✓ रिकॉर्डिंग सुरक्षित है", fontSize = 10.sp, color = Color(0xFF2E7D32), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (isRecordingAudio) {
                                    Button(
                                        onClick = {
                                            AshramVoiceAnnouncementManager.stopCustomRecording(context)
                                            isRecordingAudio = false
                                            hasRecording = true
                                            selectedVoicePreset = AshramVoiceAnnouncementManager.PRESET_CUSTOM_RECORDED
                                            AshramVoiceAnnouncementManager.setVoicePreset(context, selectedVoicePreset)
                                            voiceSuccessMsg = "✓ आपकी असली आवाज़ रिकॉर्ड व सेट हो गई!"
                                            Toast.makeText(context, "✓ रिकॉर्डिंग सुरक्षित हुई!", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("⏹️ रिकॉर्डिंग रोकें व सुरक्षित करें", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = {
                                            recordAudioLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                        },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2E7D32)),
                                        border = BorderStroke(1.dp, Color(0xFF4CAF50)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("🎙️ नई आवाज़ रिकॉर्ड करें", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    if (hasRecording) {
                                        OutlinedButton(
                                            onClick = {
                                                AshramVoiceAnnouncementManager.playCustomRecording(context) { }
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp)
                                        ) {
                                            Text("▶️ सुनें", fontSize = 12.sp)
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                AshramVoiceAnnouncementManager.deleteCustomRecording(context)
                                                hasRecording = false
                                                if (selectedVoicePreset == AshramVoiceAnnouncementManager.PRESET_CUSTOM_RECORDED) {
                                                    selectedVoicePreset = AshramVoiceAnnouncementManager.PRESET_NATURAL_MALE
                                                    AshramVoiceAnnouncementManager.setVoicePreset(context, selectedVoicePreset)
                                                }
                                                Toast.makeText(context, "रिकॉर्डिंग हटा दी गई", Toast.LENGTH_SHORT).show()
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                                            border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f)),
                                            contentPadding = PaddingValues(horizontal = 8.dp)
                                        ) {
                                            Text("🗑️", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text("उपलब्ध प्राकृतिक स्वर विकल्प:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaroonPrimary)
                    Spacer(modifier = Modifier.height(6.dp))

                    AshramVoiceAnnouncementManager.AVAILABLE_VOICE_PRESETS.forEach { preset ->
                        val isSelected = (selectedVoicePreset == preset.id)
                        Surface(
                            onClick = { selectedVoicePreset = preset.id; voiceSuccessMsg = null },
                            color = if (isSelected) Color(0xFFFFF3E0) else Color(0xFFF9F9F9),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.2.dp, if (isSelected) SaffronPrimary else Color(0xFFE0E0E0)),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(preset.icon, fontSize = 22.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (isHindi) preset.nameHindi else preset.nameEnglish,
                                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            color = if (isSelected) MaroonPrimary else Color.Black
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        val (badgeText, badgeBg, badgeFg) = when (preset.category) {
                                            "CUSTOM" -> Triple("लाइव माइक", Color(0xFFE8F5E9), Color(0xFF2E7D32))
                                            "DEVICE" -> Triple("ऑफलाइन", Color(0xFFEDE7F6), Color(0xFF512DA8))
                                            "FEMALE" -> Triple("महिला HD", Color(0xFFFCE4EC), Color(0xFFC2185B))
                                            else -> Triple("पुरुष HD", Color(0xFFE3F2FD), Color(0xFF1976D2))
                                        }
                                        Surface(
                                            color = badgeBg,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = badgeText,
                                                color = badgeFg,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = preset.description,
                                        fontSize = 11.sp,
                                        color = Color.Gray,
                                        lineHeight = 15.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                OutlinedButton(
                                    onClick = {
                                        AshramVoiceAnnouncementManager.testVoice(context, preset.id)
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SaffronPrimary),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("▶️ सुनें", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    if (voiceSuccessMsg != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(voiceSuccessMsg!!, color = Color(0xFF2E7D32), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                                repository?.updateTokenVoicePreset(selectedVoicePreset)
                                AshramVoiceAnnouncementManager.setVoicePreset(context, selectedVoicePreset)
                                withContext(Dispatchers.Main) {
                                    voiceSuccessMsg = if (isHindi) "✓ टोकन उद्घोषणा आवाज़ सुरक्षित व लागू हुई!" else "Voice preset updated successfully!"
                                    Toast.makeText(context, if (isHindi) "✓ आवाज़ सुरक्षित हुई!" else "Voice preset saved!", Toast.LENGTH_SHORT).show()
                                    onRefreshData()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) {
                        Text(if (isHindi) "💾 टोकन आवाज़ सुरक्षित करें" else "💾 Save Voice Preset", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 3.2 ASHRAM MAIN HERO BANNER MANAGER (SUPER ADMIN DIRECT CONTROL)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🖼️", fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isHindi) "आश्रम मुख्य होम स्क्रीन बैनर (Hero Banner Manager)" else "Ashram Main Home Screen Banner",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaroonPrimary
                        )
                    }
                    Text(
                        text = if (isHindi)
                            "सभी भक्तों के फोन पर होम स्क्रीन के सबसे ऊपर पावन धाम का मुख्य बैनर, शीर्षक व विवरण प्रदर्शित करें।"
                            else "Display majestic ashram hero photo, title and description at top of devotee home screen.",
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = isBannerVisibleChecked,
                            onCheckedChange = { isBannerVisibleChecked = it; bannerSaveMsg = null }
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isBannerVisibleChecked) (if (isHindi) "✅ मुख्य बैनर सक्रिय (दिखेगा)" else "Banner Visible") else (if (isHindi) "🔒 मुख्य बैनर छिपा हुआ है" else "Banner Hidden"),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFE0E0E0))
                    ) {
                        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            if (bannerPhotoUriInput.isNotBlank()) {
                                SacredAvatar(
                                    photoUri = bannerPhotoUriInput,
                                    fallbackText = "बैनर",
                                    size = 80.dp,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { bannerGalleryLauncher.launch("image/*") },
                                    colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(if (isHindi) "🖼️ गैलरी से बैनर फोटो चुनें" else "Pick Banner Photo", fontSize = 12.sp)
                                }
                                if (bannerPhotoUriInput.isNotBlank()) {
                                    OutlinedButton(
                                        onClick = { bannerPhotoUriInput = "" },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                                    ) {
                                        Text("❌ हटाएं", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = bannerPhotoUriInput,
                        onValueChange = { bannerPhotoUriInput = it; bannerSaveMsg = null },
                        label = { Text(if (isHindi) "बैनर फोटो फ़ाइल पाथ / URL" else "Banner Photo URI") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = bannerTitleInput,
                        onValueChange = { bannerTitleInput = it; bannerSaveMsg = null },
                        label = { Text(if (isHindi) "बैनर मुख्य शीर्षक (Title)" else "Banner Title") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = bannerSubtitleInput,
                        onValueChange = { bannerSubtitleInput = it; bannerSaveMsg = null },
                        label = { Text(if (isHindi) "बैनर उपशीर्षक (Subtitle / विवरण)" else "Banner Subtitle") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = bannerActionUrlInput,
                        onValueChange = { bannerActionUrlInput = it; bannerSaveMsg = null },
                        label = { Text(if (isHindi) "क्लिक करने पर खुलने वाला लिंक / URL (वैकल्पिक)" else "Action Link URL (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (bannerSaveMsg != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(bannerSaveMsg!!, color = Color(0xFF2E7D32), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                                var targetUri = bannerPhotoUriInput.trim()
                                if (targetUri.isNotBlank() && !targetUri.startsWith("http://") && !targetUri.startsWith("https://")) {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, if (isHindi) "क्लाउड पर फोटो अपलोड हो रही है..." else "Uploading photo to cloud...", Toast.LENGTH_SHORT).show()
                                    }
                                    val safeName = "banner_" + System.currentTimeMillis() + ".jpg"
                                    val cloudUrl = com.example.shribalajikripadham.data.network.GitHubLiveSyncManager.uploadPhotoToGitHub(context, targetUri, safeName)
                                    if (!cloudUrl.isNullOrBlank()) {
                                        targetUri = cloudUrl
                                    }
                                }
                                repository?.updateBannerSettings(
                                    photoUri = targetUri,
                                    isVisible = isBannerVisibleChecked,
                                    title = bannerTitleInput.trim(),
                                    subtitle = bannerSubtitleInput.trim(),
                                    actionUrl = bannerActionUrlInput.trim()
                                )
                                withContext(Dispatchers.Main) {
                                    bannerPhotoUriInput = targetUri
                                    bannerSaveMsg = if (isHindi) "✓ मुख्य बैनर सुरक्षित व क्लाउड पर लाइव!" else "Banner saved & live on cloud!"
                                    Toast.makeText(context, if (isHindi) "✓ बैनर सुरक्षित हुआ!" else "Banner saved!", Toast.LENGTH_SHORT).show()
                                    onRefreshData()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) {
                        Text(if (isHindi) "💾 मुख्य बैनर सुरक्षित करें" else "💾 Save Banner Settings", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 3.3 FUTURE ADS & DEVOTEE SPONSORSHIP MASTER CONTROL (SUPER ADMIN DIRECT CONTROL)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📢", fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isHindi) "भविष्य के विज्ञापन व भक्त प्रायोजन (Ads & Sponsorship)" else "Ads & Sponsorship Master Control",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaroonPrimary
                        )
                    }
                    Text(
                        text = if (isHindi)
                            "100% सुपर एडमिन के नियंत्रण में: भविष्य में आश्रम सेवा, गौशाला सहयोग, या प्रायोजक विज्ञापन को ऑन/ऑफ करें।"
                            else "100% Super Admin controlled: Enable custom sponsor banners or ads on devotee home screen.",
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = isAdsEnabledChecked,
                            onCheckedChange = { isAdsEnabledChecked = it; adSaveMsg = null }
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isAdsEnabledChecked) (if (isHindi) "✅ विज्ञापन / प्रायोजक बैनर चालू है" else "Ads Enabled") else (if (isHindi) "🔒 विज्ञापन बंद हैं (कोई ऐड नहीं दिखेगा)" else "Ads Disabled"),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }

                    if (isAdsEnabledChecked) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDE7)),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFFFFF176))
                        ) {
                            Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                if (adBannerPhotoUriInput.isNotBlank()) {
                                    SacredAvatar(
                                        photoUri = adBannerPhotoUriInput,
                                        fallbackText = "प्रायोजक",
                                        size = 70.dp,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                                Button(
                                    onClick = { adBannerGalleryLauncher.launch("image/*") },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(if (isHindi) "🖼️ विज्ञापन बैनर फोटो अपलोड करें" else "Upload Ad Banner Photo", fontSize = 12.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = adBannerPhotoUriInput,
                            onValueChange = { adBannerPhotoUriInput = it; adSaveMsg = null },
                            label = { Text(if (isHindi) "विज्ञापन बैनर फोटो URI" else "Ad Banner Photo URI") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = adBannerTitleInput,
                            onValueChange = { adBannerTitleInput = it; adSaveMsg = null },
                            label = { Text(if (isHindi) "विज्ञापन / सहयोग शीर्षक" else "Ad Title") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = adBannerDescInput,
                            onValueChange = { adBannerDescInput = it; adSaveMsg = null },
                            label = { Text(if (isHindi) "विज्ञापन विवरण" else "Ad Description") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = adTargetUrlInput,
                            onValueChange = { adTargetUrlInput = it; adSaveMsg = null },
                            label = { Text(if (isHindi) "टारगेट वेबसाइट / सहयोग लिंक" else "Target Link URL") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (adSaveMsg != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(adSaveMsg!!, color = Color(0xFF2E7D32), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                                var targetUri = adBannerPhotoUriInput.trim()
                                if (targetUri.isNotBlank() && !targetUri.startsWith("http://") && !targetUri.startsWith("https://")) {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, if (isHindi) "क्लाउड पर विज्ञापन फोटो अपलोड हो रही है..." else "Uploading ad photo to cloud...", Toast.LENGTH_SHORT).show()
                                    }
                                    val safeName = "ad_banner_" + System.currentTimeMillis() + ".jpg"
                                    val cloudUrl = com.example.shribalajikripadham.data.network.GitHubLiveSyncManager.uploadPhotoToGitHub(context, targetUri, safeName)
                                    if (!cloudUrl.isNullOrBlank()) {
                                        targetUri = cloudUrl
                                    }
                                }
                                repository?.updateAdsSettings(
                                    isAdsEnabled = isAdsEnabledChecked,
                                    adType = adTypeInput.trim(),
                                    bannerPhotoUri = targetUri,
                                    title = adBannerTitleInput.trim(),
                                    description = adBannerDescInput.trim(),
                                    targetUrl = adTargetUrlInput.trim(),
                                    placement = adPlacementInput.trim()
                                )
                                withContext(Dispatchers.Main) {
                                    adBannerPhotoUriInput = targetUri
                                    adSaveMsg = if (isHindi) "✓ विज्ञापन व प्रायोजक सेटिंग्स सुरक्षित व लाइव!" else "Ads settings saved & live!"
                                    Toast.makeText(context, if (isHindi) "✓ विज्ञापन सेटिंग्स सुरक्षित!" else "Settings saved!", Toast.LENGTH_SHORT).show()
                                    onRefreshData()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) {
                        Text(if (isHindi) "💾 विज्ञापन सेटिंग्स सुरक्षित करें" else "💾 Save Ads Settings", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 3.5 ASHRAM PARICHAY, HISTORY, RULES & YATRA DIARY PRIVACY (SUPER ADMIN DIRECT CONTROL)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isHindi) "📖 आश्रम परिचय, इतिहास, नियम व यात्रा डायरी संपादक" else "📖 Ashram Info, History, Rules & Yatra Diary",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaroonPrimary
                    )
                    Text(
                        text = if (isHindi)
                            "सुपर एडमिन सीधे यहाँ से संपूर्ण आश्रम का परिचय, इतिहास, नियम व यात्रा खर्च डायरी की गोपनीयता नियंत्रित कर सकते हैं।"
                            else "Control Ashram Introduction, History, Rules, and Yatra Khata Diary access.",
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Yatra Diary Privacy Toggle
                    Card(
                        colors = CardDefaults.cardColors(containerColor = if (canDevoteeYatraDiaryChecked) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)),
                        border = BorderStroke(1.dp, if (canDevoteeYatraDiaryChecked) Color(0xFF81C784) else Color(0xFFFFB74D)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isHindi) "यात्रा खर्च डायरी (Yatra Diary) प्राइवेसी" else "Yatra Expense Diary Privacy",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaroonPrimary
                                    )
                                    Text(
                                        text = if (canDevoteeYatraDiaryChecked)
                                            (if (isHindi) "🔓 सार्वजनिक: भक्त भी यात्रा खर्च देख सकते हैं।" else "🔓 PUBLIC: Devotees can view expenses.")
                                        else
                                            (if (isHindi) "🔒 केवल सुपर एडमिन (अनुशंसित): भक्तों से पूर्णतः गोपनीय।" else "🔒 PRIVATE: Super Admin exclusive privilege."),
                                        fontSize = 11.sp,
                                        color = Color.DarkGray
                                    )
                                }
                                Switch(
                                    checked = canDevoteeYatraDiaryChecked,
                                    onCheckedChange = { canDevoteeYatraDiaryChecked = it }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = parichayHiInput,
                        onValueChange = { parichayHiInput = it; ashramInfoSaveMsg = null },
                        label = { Text(if (isHindi) "आश्रम परिचय (हिंदी)" else "Ashram Info (Hindi)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = parichayEnInput,
                        onValueChange = { parichayEnInput = it; ashramInfoSaveMsg = null },
                        label = { Text("Ashram Introduction (English)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = historyHiInput,
                        onValueChange = { historyHiInput = it; ashramInfoSaveMsg = null },
                        label = { Text(if (isHindi) "आश्रम का पावन इतिहास (हिंदी)" else "Ashram History (Hindi)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = rulesHiInput,
                        onValueChange = { rulesHiInput = it; ashramInfoSaveMsg = null },
                        label = { Text(if (isHindi) "आश्रम नियम व मर्यादा (हिंदी)" else "Ashram Rules & Guidelines") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )

                    if (ashramInfoSaveMsg != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(ashramInfoSaveMsg!!, color = Color(0xFF2E7D32), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            isSavingAshramInfo = true
                            ashramInfoSaveMsg = null
                            kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                                if (repository != null) {
                                    repository.updateAshramParichayAndRules(
                                        parichayHindi = parichayHiInput.trim(),
                                        parichayEnglish = parichayEnInput.trim(),
                                        historyHindi = historyHiInput.trim(),
                                        rulesHindi = rulesHiInput.trim()
                                    )
                                    repository.updateCanDevoteeViewYatraDiary(canDevoteeYatraDiaryChecked)
                                    try {
                                        repository.publishCurrentSettingsToGitHub(superAdmin?.name ?: "SUPER_ADMIN")
                                    } catch (e: Exception) {}
                                }
                                withContext(Dispatchers.Main) {
                                    isSavingAshramInfo = false
                                    ashramInfoSaveMsg = if (isHindi) "✓ आश्रम परिचय, इतिहास, नियम व प्राइवेसी सेटिंग्स सुरक्षित!" else "Settings saved!"
                                    Toast.makeText(context, if (isHindi) "✓ आश्रम विवरण व प्राइवेसी सुरक्षित!" else "Saved successfully!", Toast.LENGTH_SHORT).show()
                                    onRefreshData()
                                }
                            }
                        },
                        enabled = !isSavingAshramInfo,
                        colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) {
                        Text(
                            text = if (isSavingAshramInfo) (if (isHindi) "सुरक्षित हो रहा है..." else "Saving...") else (if (isHindi) "💾 आश्रम विवरण व प्राइवेसी सुरक्षित करें" else "💾 Save Info & Privacy"),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 4. CENTRAL CLOUD DATA SYNC
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isHindi) "🌐 सेंट्रल क्लाउड डेटा सिंक (Cloud Server Sync)" else "🌐 Central Cloud Data Sync",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaroonPrimary
                    )
                    Text(
                        text = if (isHindi)
                            "सभी सेवादारों और भक्तों के डेटा को केंद्रीय सर्वर/एंडपॉइंट से सिंक करने हेतु URL विन्यास।"
                            else "Sync tokens, settings, and darbar queue with central cloud server.",
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = cloudUrlInput,
                        onValueChange = { cloudUrlInput = it; cloudSyncStatusMsg = null },
                        label = { Text("Cloud Server Endpoint (POST / JSON)") },
                        placeholder = { Text("https://shribalajikripadham.org/api/sync") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = isCloudEnabledChecked, onCheckedChange = { isCloudEnabledChecked = it })
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(if (isHindi) "क्लाउड सिंक सक्षम करें (Enable Cloud Sync)" else "Enable Cloud Sync", fontSize = 13.sp)
                    }

                    if (cloudSyncStatusMsg != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(cloudSyncStatusMsg!!, color = if (cloudSyncStatusMsg!!.contains("सफल") || cloudSyncStatusMsg!!.contains("success")) Color(0xFF2E7D32) else Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                onUpdateCloudSync(cloudUrlInput, isCloudEnabledChecked)
                                cloudSyncStatusMsg = if (isHindi) "क्लाउड सेटिंग्स सुरक्षित हुईं!" else "Cloud settings saved!"
                            },
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(if (isHindi) "सेटिंग्स सेव करें" else "Save Settings")
                        }

                        Button(
                            onClick = {
                                if (cloudUrlInput.isBlank()) {
                                    cloudSyncStatusMsg = if (isHindi) "कृपया पहले क्लाउड सर्वर URL दर्ज करें!" else "Please enter Cloud Server URL first!"
                                    Toast.makeText(context, cloudSyncStatusMsg, Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                isCloudSyncing = true
                                cloudSyncStatusMsg = if (isHindi) "क्लाउड से सिंक हो रहा है..." else "Syncing with cloud..."
                                onTriggerCloudSync { success, msg ->
                                    isCloudSyncing = false
                                    cloudSyncStatusMsg = msg
                                }
                            },
                            enabled = !isCloudSyncing,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isCloudSyncing) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                            } else {
                                Text(if (isHindi) "🔄 अभी सिंक करें" else "🔄 Sync Now", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 5. 1-CLICK DATABASE BACKUP & RESTORE
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isHindi) "💾 1-क्लिक डेटाबेस बैकअप एवं रिस्टोर" else "💾 1-Click Backup & Restore",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaroonPrimary
                    )
                    Text(
                        text = if (isHindi) 
                            "अपने पूरे ऐप डेटाबेस (सेटिंग्स, टोकन, सेवादार, दूरियाँ) का सुरक्षित बैकअप बनाएं या किसी नए फोन में रिस्टोर करें।" 
                            else "Backup complete app database to JSON or restore onto any new device.",
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )

                    if (backupRestoreStatusMsg != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(backupRestoreStatusMsg!!, color = Color(0xFF2E7D32), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                onExportDatabaseBackup { success, result ->
                                    if (success) {
                                        backupRestoreStatusMsg = if (isHindi) "बैकअप सफलतापूर्वक तैयार हुआ!" else "Backup generated!"
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_SUBJECT, "Shri Balaji Kripa Dham DB Backup")
                                            putExtra(Intent.EXTRA_TEXT, result)
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "बैकअप JSON शेयर/सेव करें"))
                                    } else {
                                        backupRestoreStatusMsg = "त्रुटि: $result"
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(46.dp)
                        ) {
                            Text(if (isHindi) "💾 बैकअप लें" else "💾 Export Backup", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { showRestoreDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaroonPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(46.dp)
                        ) {
                            Text(if (isHindi) "📥 रिस्टोर करें" else "📥 Restore", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text(if (isHindi) "डेटाबेस बैकअप रिस्टोर करें" else "Restore Database Backup", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        if (isHindi) "यहाँ बैकअप JSON टेक्स्ट पेस्ट करें जिसे आपने पहले एक्सपोर्ट किया था:" else "Paste the exported backup JSON text here:",
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = restoreJsonText,
                        onValueChange = { restoreJsonText = it },
                        modifier = Modifier.fillMaxWidth().height(160.dp),
                        placeholder = { Text("{\"version\": 11, ...}") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (restoreJsonText.isBlank()) return@Button
                        onRestoreDatabaseBackup(restoreJsonText) { success, msg ->
                            showRestoreDialog = false
                            backupRestoreStatusMsg = msg
                            restoreJsonText = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary)
                ) {
                    Text(if (isHindi) "रिस्टोर करें" else "Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) {
                    Text(if (isHindi) "रद्द करें" else "Cancel")
                }
            }
        )
    }
}


@Composable
fun UiBoxControlTab(
    isHindi: Boolean,
    sections: List<UiSectionConfig>,
    onSaveSections: (List<UiSectionConfig>) -> Unit,
    onResetToDefault: () -> Unit,
    onPublishToGitHub: suspend (List<UiSectionConfig>) -> Pair<Boolean, String> = { Pair(false, "Not configured") }
) {
    val scope = rememberCoroutineScope()
    var localSections by remember(sections) {
        mutableStateOf(
            if (sections.isNotEmpty()) sections.sortedBy { it.orderIndex }
            else UiSectionConfig.defaultSections()
        )
    }
    var showConfirmResetDialog by remember { mutableStateOf(false) }
    var hasUnsavedChanges by remember { mutableStateOf(false) }
    var isPublishing by remember { mutableStateOf(false) }
    var publishResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var showPublishResultDialog by remember { mutableStateOf(false) }

    // Section Edit Dialog State
    var editingSection by remember { mutableStateOf<UiSectionConfig?>(null) }
    var editTitleHi by remember { mutableStateOf("") }
    var editTitleEn by remember { mutableStateOf("") }
    var editIcon by remember { mutableStateOf("") }
    var editSubtitleHi by remember { mutableStateOf("") }
    var editSubtitleEn by remember { mutableStateOf("") }
    var editContentHi by remember { mutableStateOf("") }
    var editContentEn by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp)
    ) {
        // Master UI Control Header Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(4.dp),
            border = BorderStroke(1.5.dp, MaroonPrimary.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaroonPrimary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🎛️", fontSize = 24.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isHindi) "होम स्क्रीन UI बॉक्स व लेआउट नियंत्रण" else "Home Screen Dynamic UI Control",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaroonPrimary
                        )
                        Text(
                            text = if (isHindi)
                                "सुपर एडमिन / एडमिन को होम स्क्रीन के प्रत्येक बॉक्स को ऊपर-नीचे करने, छुपाने और दिखाने का पूर्ण अधिकार है।"
                            else
                                "Full control to reorder, hide or show any box/section on devotee Home Screen.",
                            fontSize = 12.sp,
                            color = Color.DarkGray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showConfirmResetDialog = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                    ) {
                        Text("↺ " + (if (isHindi) "डिफ़ॉल्ट क्रम" else "Reset Order"), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            onSaveSections(localSections)
                            hasUnsavedChanges = false
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (hasUnsavedChanges) Color(0xFF2E7D32) else MaroonPrimary)
                    ) {
                        Text("💾 " + (if (isHindi) "क्रम सहेजें" else "Save Order"), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 🚀 GRAND 1-TAP PUBLISH LIVE TO ALL DEVOTEES BUTTON
                Button(
                    onClick = {
                        scope.launch {
                            isPublishing = true
                            val res = onPublishToGitHub(localSections)
                            isPublishing = false
                            hasUnsavedChanges = false
                            publishResult = res
                            showPublishResultDialog = true
                        }
                    },
                    enabled = !isPublishing,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1B5E20)
                    ),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    if (isPublishing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isHindi) "GitHub पर लाइव पब्लिश हो रहा है..." else "Publishing to GitHub Cloud...",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                    } else {
                        Text("🚀 ", fontSize = 16.sp)
                        Text(
                            text = if (isHindi) "सभी भक्तों के फोन में लाइव पब्लिश करें" else "Publish Live to All Devotees (1-Tap)",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                }

                // Cloud Status Badge
                Surface(
                    color = Color(0xFFE8F5E9),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF81C784)),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🟢", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isHindi) "GitHub Live Sync सक्रिय: सभी भक्तों के ऐप में तुरंत लोड होगा" else "GitHub Live Sync Active: Instantly synced across all devotee apps",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1B5E20)
                        )
                    }
                }

                if (hasUnsavedChanges) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isHindi) "⚠️ आपके पास कुछ अ-सहेजे बदलाव हैं! कृपया 'क्रम सहेजें' बटन दबाएं।" else "⚠️ You have unsaved changes. Tap 'Save Order' to apply.",
                        fontSize = 11.sp,
                        color = Color(0xFFE65100),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Stats Summary
        val visibleCount = localSections.count { it.isVisible }
        val hiddenCount = localSections.size - visibleCount
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("👁️", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(if (isHindi) "सक्रिय (दिखाई देंगे)" else "Visible Boxes", fontSize = 11.sp, color = Color(0xFF2E7D32))
                        Text("$visibleCount / ${localSections.size}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20))
                    }
                }
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("🚫", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(if (isHindi) "छिपे हुए बॉक्स" else "Hidden Boxes", fontSize = 11.sp, color = Color(0xFFC62828))
                        Text("$hiddenCount", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB71C1C))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (isHindi) "होम स्क्रीन बॉक्स क्रम सूची (⬆️ ऊपर / ⬇️ नीचे / 👁️ टॉगल)" else "Home Screen Sections List (⬆️ Up / ⬇️ Down / 👁️ Toggle)",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaroonPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Render each section card
        localSections.forEachIndexed { index, section ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (section.isVisible) Color.White else Color(0xFFF0F0F0)
                ),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(if (section.isVisible) 3.dp else 1.dp),
                border = BorderStroke(
                    1.dp,
                    if (section.isVisible) MaroonPrimary.copy(alpha = 0.3f) else Color.LightGray
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Position Badge & Icon
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(44.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (section.isVisible) MaroonPrimary else Color.Gray,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${index + 1}",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(section.icon, fontSize = 20.sp)
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Title & Visibility Badge
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isHindi) section.titleHindi else section.titleEnglish,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (section.isVisible) Color.Black else Color.Gray
                        )
                        Text(
                            text = if (isHindi) section.titleEnglish else section.titleHindi,
                            fontSize = 11.sp,
                            color = Color.DarkGray
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (section.isVisible) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                            ) {
                                Text(
                                    text = if (section.isVisible)
                                        (if (isHindi) "● दिखाई देगा" else "● Visible")
                                    else
                                        (if (isHindi) "○ छिपा हुआ" else "○ Hidden"),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (section.isVisible) Color(0xFF2E7D32) else Color(0xFFC62828)
                                )
                            }
                            if (section.customSubtitleHindi.isNotBlank() || section.customContentHindi.isNotBlank()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFFFF3E0)
                                ) {
                                    Text(
                                        text = "✏️ " + (if (isHindi) "संपादित" else "Edited"),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFE65100)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Controls: Up, Down, Visibility Toggle
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Move Up
                        IconButton(
                            onClick = {
                                if (index > 0) {
                                    val list = localSections.toMutableList()
                                    val temp = list[index]
                                    list[index] = list[index - 1]
                                    list[index - 1] = temp
                                    val reindexed = list.mapIndexed { idx, itm -> itm.copy(orderIndex = idx) }
                                    localSections = reindexed
                                    hasUnsavedChanges = false
                                    onSaveSections(reindexed)
                                }
                            },
                            enabled = index > 0,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Text("⬆️", fontSize = 16.sp)
                        }

                        // Move Down
                        IconButton(
                            onClick = {
                                if (index < localSections.size - 1) {
                                    val list = localSections.toMutableList()
                                    val temp = list[index]
                                    list[index] = list[index + 1]
                                    list[index + 1] = temp
                                    val reindexed = list.mapIndexed { idx, itm -> itm.copy(orderIndex = idx) }
                                    localSections = reindexed
                                    hasUnsavedChanges = false
                                    onSaveSections(reindexed)
                                }
                            },
                            enabled = index < localSections.size - 1,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Text("⬇️", fontSize = 16.sp)
                        }

                        // Edit Button (✏️)
                        IconButton(
                            onClick = {
                                editingSection = section
                                editTitleHi = section.titleHindi
                                editTitleEn = section.titleEnglish
                                editIcon = section.icon
                                editSubtitleHi = section.customSubtitleHindi
                                editSubtitleEn = section.customSubtitleEnglish
                                editContentHi = section.customContentHindi
                                editContentEn = section.customContentEnglish
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Text("✏️", fontSize = 16.sp)
                        }

                        // Visibility Toggle (Eye / Eye-off) -> Auto-save & Live Publish
                        IconButton(
                            onClick = {
                                val list = localSections.toMutableList()
                                list[index] = list[index].copy(isVisible = !list[index].isVisible)
                                localSections = list
                                hasUnsavedChanges = false
                                onSaveSections(list)
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Text(if (section.isVisible) "👁️" else "🚫", fontSize = 18.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Bottom Save Button
        Button(
            onClick = {
                onSaveSections(localSections)
                hasUnsavedChanges = false
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary)
        ) {
            Text(
                text = "💾 " + (if (isHindi) "होम स्क्रीन का नया क्रम सुरक्षित करें" else "Save Home Screen Layout"),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color.White
            )
        }
    }

    // Reset Confirmation Dialog
    if (showConfirmResetDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmResetDialog = false },
            title = {
                Text(
                    if (isHindi) "डिफ़ॉल्ट UI क्रम रीसेट करें?" else "Reset to Default Layout?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    if (isHindi)
                        "क्या आप होम स्क्रीन के सभी 12 बॉक्स को उनके मूल डिफ़ॉल्ट क्रम में रीसेट करना चाहते हैं? सभी छिपे हुए बॉक्स पुनः सक्रिय हो जाएंगे।"
                    else
                        "Do you want to reset all 12 home screen sections to their original default order? All hidden boxes will become visible."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmResetDialog = false
                        val defaults = UiSectionConfig.defaultSections()
                        localSections = defaults
                        onResetToDefault()
                        onSaveSections(defaults)
                        hasUnsavedChanges = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary)
                ) {
                    Text(if (isHindi) "हाँ, रीसेट करें" else "Yes, Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmResetDialog = false }) {
                    Text(if (isHindi) "रद्द करें" else "Cancel")
                }
            }
        )
    }

    // Section Edit Dialog
    if (editingSection != null) {
        val target = editingSection!!
        AlertDialog(
            onDismissRequest = { editingSection = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(editIcon.ifEmpty { target.icon }, fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isHindi) "बॉक्स सामग्री संपादित करें" else "Edit Box Content",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaroonPrimary
                        )
                        Text(
                            text = "ID: ${target.sectionId}",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (isHindi)
                            "सुपर एडमिन इस बॉक्स का नाम, आइकन, सब-टाइटल व विशेष सूचना/नियम बदल सकते हैं। सेव करने पर यह सभी भक्तों के फोन में तुरंत लाइव अपडेट हो जाएगा।"
                        else
                            "Edit title, icon, subtitle & custom announcements/rules for this box. Live syncs to all devotee handsets.",
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )

                    OutlinedTextField(
                        value = editTitleHi,
                        onValueChange = { editTitleHi = it },
                        label = { Text(if (isHindi) "शीर्षक (हिंदी)" else "Title (Hindi)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editTitleEn,
                        onValueChange = { editTitleEn = it },
                        label = { Text(if (isHindi) "शीर्षक (अंग्रेज़ी)" else "Title (English)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editIcon,
                        onValueChange = { editIcon = it },
                        label = { Text(if (isHindi) "आइकन / इमोजी" else "Icon / Emoji") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editSubtitleHi,
                        onValueChange = { editSubtitleHi = it },
                        label = { Text(if (isHindi) "उप-शीर्षक (हिंदी) (वैकल्पिक)" else "Subtitle (Hindi) (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editSubtitleEn,
                        onValueChange = { editSubtitleEn = it },
                        label = { Text(if (isHindi) "उप-शीर्षक (अंग्रेज़ी) (वैकल्पिक)" else "Subtitle (English) (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editContentHi,
                        onValueChange = { editContentHi = it },
                        label = { Text(if (isHindi) "विशेष घोषणा / नियम / सामग्री (हिंदी)" else "Custom Content / Rules (Hindi)") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editContentEn,
                        onValueChange = { editContentEn = it },
                        label = { Text(if (isHindi) "विशेष घोषणा / सामग्री (अंग्रेज़ी)" else "Custom Content (English)") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val list = localSections.toMutableList()
                        val idx = list.indexOfFirst { it.sectionId == target.sectionId }
                        if (idx >= 0) {
                            list[idx] = list[idx].copy(
                                titleHindi = editTitleHi.trim().ifEmpty { target.titleHindi },
                                titleEnglish = editTitleEn.trim().ifEmpty { target.titleEnglish },
                                icon = editIcon.trim().ifEmpty { target.icon },
                                customSubtitleHindi = editSubtitleHi.trim(),
                                customSubtitleEnglish = editSubtitleEn.trim(),
                                customContentHindi = editContentHi.trim(),
                                customContentEnglish = editContentEn.trim()
                            )
                            localSections = list
                            hasUnsavedChanges = false
                            onSaveSections(list)
                        }
                        editingSection = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary)
                ) {
                    Text(if (isHindi) "सहेजें व लाइव पब्लिश करें" else "Save & Publish Live", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingSection = null }) {
                    Text(if (isHindi) "रद्द करें" else "Cancel")
                }
            }
        )
    }

    if (showPublishResultDialog && publishResult != null) {
        val isSuccess = publishResult!!.first
        val msg = publishResult!!.second
        AlertDialog(
            onDismissRequest = { showPublishResultDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (isSuccess) "✅" else "⚠️", fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isSuccess) (if (isHindi) "लाइव पब्लिश सफल!" else "Live Publish Successful!")
                        else (if (isHindi) "लाइव सिंक विफल" else "Live Sync Failed"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (isSuccess) Color(0xFF1B5E20) else Color.Red
                    )
                }
            },
            text = {
                Text(
                    text = msg,
                    fontSize = 13.sp,
                    color = Color.DarkGray
                )
            },
            confirmButton = {
                Button(
                    onClick = { showPublishResultDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSuccess) Color(0xFF1B5E20) else MaroonPrimary
                    )
                ) {
                    Text(if (isHindi) "उत्कृष्ट (OK)" else "OK", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}


// =========================================================================
// 🌐 WEBSITE & CMS MANAGER TAB (Full-featured Superadmin Website & App Control)
// =========================================================================
@Composable
fun WebsiteAndCmsManagerTab(
    isHindi: Boolean,
    admin: Admin,
    settings: AshramSettings,
    repository: AshramRepository,
    onRefresh: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isPublishing by remember { mutableStateOf(false) }
    var runningToken by remember { mutableIntStateOf(settings.runningTokenNumber) }
    var isDarbarActive by remember { mutableStateOf(settings.isDarbarActive) }
    var isTokenServiceEnabled by remember { mutableStateOf(settings.isTokenServiceEnabled) }

    // Website Content States
    var websiteBannerTitle by remember { mutableStateOf(settings.bannerTitle) }
    var websiteBannerSubtitle by remember { mutableStateOf(settings.bannerSubtitle) }
    var isWebsiteBannerVisible by remember { mutableStateOf(settings.isBannerVisible) }

    var emergencyNotice by remember { mutableStateOf(settings.emergencyNoticeText) }
    var isEmergencyNoticeVisible by remember { mutableStateOf(settings.isEmergencyNoticeVisible) }

    var darbarTimings by remember { mutableStateOf(settings.darbarTimings) }
    var gurujiPhotoUri by remember { mutableStateOf(settings.gurujiPhotoUri) }

    var sevadarsList by remember { mutableStateOf<List<SevadarProfile>>(emptyList()) }
    var donorsList by remember { mutableStateOf<List<DonorProfile>>(emptyList()) }

    // Dialogs for Adding & Editing Sevadar
    var showAddSevadarDialog by remember { mutableStateOf(false) }
    var newSevName by remember { mutableStateOf("") }
    var newSevRole by remember { mutableStateOf("सेवादार") }
    var newSevPhone by remember { mutableStateOf("") }
    var newSevPhotoUri by remember { mutableStateOf("") }

    var editingSevadar by remember { mutableStateOf<SevadarProfile?>(null) }
    var editSevName by remember { mutableStateOf("") }
    var editSevRole by remember { mutableStateOf("") }
    var editSevPhone by remember { mutableStateOf("") }
    var editSevPhotoUri by remember { mutableStateOf("") }

    // Dialogs for Adding & Editing Donor
    var showAddDonorDialog by remember { mutableStateOf(false) }
    var newDonorName by remember { mutableStateOf("") }
    var newDonorAddress by remember { mutableStateOf("ग्राम डूँगरा जाट") }
    var newDonorTitle by remember { mutableStateOf("मंदिर निर्माण सहयोगी") }
    var newDonorPhotoUri by remember { mutableStateOf("") }

    var editingDonor by remember { mutableStateOf<DonorProfile?>(null) }
    var editDonorName by remember { mutableStateOf("") }
    var editDonorAddress by remember { mutableStateOf("") }
    var editDonorTitle by remember { mutableStateOf("") }
    var editDonorPhotoUri by remember { mutableStateOf("") }

    // Photo Launchers for Sevadars, Donors & Guruji
    val newSevPhotoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val bmp = DevoteePhotoHelper.loadBitmap(context, uri.toString())
            if (bmp != null) {
                val saved = DevoteePhotoHelper.saveDevoteePhoto(context, bmp, "sevadar")
                newSevPhotoUri = saved
                scope.launch(Dispatchers.IO) {
                    val cloudUrl = GitHubLiveSyncManager.uploadPhotoToGitHub(context, saved, "sevadar_${System.currentTimeMillis()}.jpg")
                    if (!cloudUrl.isNullOrBlank()) {
                        withContext(Dispatchers.Main) { newSevPhotoUri = cloudUrl }
                    }
                }
            }
        }
    }
    val newSevPhotoCamera = rememberLauncherForActivityResult(TakeAnyPicturePreview()) { bmp ->
        if (bmp != null) {
            val saved = DevoteePhotoHelper.saveDevoteePhoto(context, bmp, "sevadar")
            newSevPhotoUri = saved
            scope.launch(Dispatchers.IO) {
                val cloudUrl = GitHubLiveSyncManager.uploadPhotoToGitHub(context, saved, "sevadar_${System.currentTimeMillis()}.jpg")
                if (!cloudUrl.isNullOrBlank()) {
                    withContext(Dispatchers.Main) { newSevPhotoUri = cloudUrl }
                }
            }
        }
    }

    val editSevPhotoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val bmp = DevoteePhotoHelper.loadBitmap(context, uri.toString())
            if (bmp != null) {
                val saved = DevoteePhotoHelper.saveDevoteePhoto(context, bmp, "sevadar")
                editSevPhotoUri = saved
                scope.launch(Dispatchers.IO) {
                    val cloudUrl = GitHubLiveSyncManager.uploadPhotoToGitHub(context, saved, "sevadar_${System.currentTimeMillis()}.jpg")
                    if (!cloudUrl.isNullOrBlank()) {
                        withContext(Dispatchers.Main) { editSevPhotoUri = cloudUrl }
                    }
                }
            }
        }
    }
    val editSevPhotoCamera = rememberLauncherForActivityResult(TakeAnyPicturePreview()) { bmp ->
        if (bmp != null) {
            val saved = DevoteePhotoHelper.saveDevoteePhoto(context, bmp, "sevadar")
            editSevPhotoUri = saved
            scope.launch(Dispatchers.IO) {
                val cloudUrl = GitHubLiveSyncManager.uploadPhotoToGitHub(context, saved, "sevadar_${System.currentTimeMillis()}.jpg")
                if (!cloudUrl.isNullOrBlank()) {
                    withContext(Dispatchers.Main) { editSevPhotoUri = cloudUrl }
                }
            }
        }
    }

    val newDonorPhotoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val bmp = DevoteePhotoHelper.loadBitmap(context, uri.toString())
            if (bmp != null) {
                val saved = DevoteePhotoHelper.saveDevoteePhoto(context, bmp, "donor")
                newDonorPhotoUri = saved
                scope.launch(Dispatchers.IO) {
                    val cloudUrl = GitHubLiveSyncManager.uploadPhotoToGitHub(context, saved, "donor_${System.currentTimeMillis()}.jpg")
                    if (!cloudUrl.isNullOrBlank()) {
                        withContext(Dispatchers.Main) { newDonorPhotoUri = cloudUrl }
                    }
                }
            }
        }
    }
    val newDonorPhotoCamera = rememberLauncherForActivityResult(TakeAnyPicturePreview()) { bmp ->
        if (bmp != null) {
            val saved = DevoteePhotoHelper.saveDevoteePhoto(context, bmp, "donor")
            newDonorPhotoUri = saved
            scope.launch(Dispatchers.IO) {
                val cloudUrl = GitHubLiveSyncManager.uploadPhotoToGitHub(context, saved, "donor_${System.currentTimeMillis()}.jpg")
                if (!cloudUrl.isNullOrBlank()) {
                    withContext(Dispatchers.Main) { newDonorPhotoUri = cloudUrl }
                }
            }
        }
    }

    val editDonorPhotoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val bmp = DevoteePhotoHelper.loadBitmap(context, uri.toString())
            if (bmp != null) {
                val saved = DevoteePhotoHelper.saveDevoteePhoto(context, bmp, "donor")
                editDonorPhotoUri = saved
                scope.launch(Dispatchers.IO) {
                    val cloudUrl = GitHubLiveSyncManager.uploadPhotoToGitHub(context, saved, "donor_${System.currentTimeMillis()}.jpg")
                    if (!cloudUrl.isNullOrBlank()) {
                        withContext(Dispatchers.Main) { editDonorPhotoUri = cloudUrl }
                    }
                }
            }
        }
    }
    val editDonorPhotoCamera = rememberLauncherForActivityResult(TakeAnyPicturePreview()) { bmp ->
        if (bmp != null) {
            val saved = DevoteePhotoHelper.saveDevoteePhoto(context, bmp, "donor")
            editDonorPhotoUri = saved
            scope.launch(Dispatchers.IO) {
                val cloudUrl = GitHubLiveSyncManager.uploadPhotoToGitHub(context, saved, "donor_${System.currentTimeMillis()}.jpg")
                if (!cloudUrl.isNullOrBlank()) {
                    withContext(Dispatchers.Main) { editDonorPhotoUri = cloudUrl }
                }
            }
        }
    }

    val gurujiPhotoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val bmp = DevoteePhotoHelper.loadBitmap(context, uri.toString())
            if (bmp != null) {
                val saved = DevoteePhotoHelper.saveDevoteePhoto(context, bmp, "guruji")
                gurujiPhotoUri = saved
                scope.launch(Dispatchers.IO) {
                    val cloudUrl = GitHubLiveSyncManager.uploadPhotoToGitHub(context, saved, "guruji_profile.jpg")
                    if (!cloudUrl.isNullOrBlank()) {
                        repository.updateGurujiPhoto(cloudUrl)
                        withContext(Dispatchers.Main) {
                            gurujiPhotoUri = cloudUrl
                            Toast.makeText(context, "✅ पूज्य गुरुदेव जी फोटो क्लाउड पर सुरक्षित हो गई!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }
    val gurujiPhotoCamera = rememberLauncherForActivityResult(TakeAnyPicturePreview()) { bmp ->
        if (bmp != null) {
            val saved = DevoteePhotoHelper.saveDevoteePhoto(context, bmp, "guruji")
            gurujiPhotoUri = saved
            scope.launch(Dispatchers.IO) {
                val cloudUrl = GitHubLiveSyncManager.uploadPhotoToGitHub(context, saved, "guruji_profile.jpg")
                if (!cloudUrl.isNullOrBlank()) {
                    repository.updateGurujiPhoto(cloudUrl)
                    withContext(Dispatchers.Main) {
                        gurujiPhotoUri = cloudUrl
                        Toast.makeText(context, "✅ पूज्य गुरुदेव जी फोटो क्लाउड पर सुरक्षित हो गई!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    fun loadData() {
        scope.launch {
            sevadarsList = repository.getAllSevadars()
            donorsList = repository.getAllDonors()
        }
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. MASTER 1-CLICK INSTANT PUBLISH BANNER
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF4A0000)),
                border = BorderStroke(2.dp, AmberGold),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "🚀 1-क्लिक में वेबसाइट व ऐप पर लाइव पब्लिश करें",
                        color = AmberGold,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "वेबसाइट: shribalajikripadham.online • सभी भक्तों के ऐप में तुरंत लाइव होगा",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                isPublishing = true
                                repository.updateWebsiteHeroBanner(websiteBannerTitle, websiteBannerSubtitle, isWebsiteBannerVisible)
                                repository.updateEmergencyNoticeBanner(emergencyNotice, isEmergencyNoticeVisible)
                                repository.updateDarbarScheduleTimings(darbarTimings)
                                val res = repository.publishEverythingToWebsiteAndCloud(admin.name)
                                isPublishing = false
                                Toast.makeText(context, res.second, Toast.LENGTH_LONG).show()
                                onRefresh()
                            }
                        },
                        enabled = !isPublishing,
                        colors = ButtonDefaults.buttonColors(containerColor = AmberGold),
                        shape = RoundedCornerShape(25.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text(
                            text = if (isPublishing) "🔄 पब्लिश हो रहा है..." else "⚡ अभी तुरंत पब्लिश करें (Instant Live)",
                            color = Color(0xFF3E2723),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // 2. LIVE RUNNING TOKEN & DARBAR CONTROLLER
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFFFFD54F))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🔴 लाइव टोकन व दरबार स्थिति", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaroonPrimary)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDarbarActive) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                        ) {
                            Text(
                                text = if (isDarbarActive) "दरबार खुला है (Open)" else "विश्राम (Closed)",
                                color = if (isDarbarActive) Color(0xFF2E7D32) else Color(0xFFC62828),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (runningToken > 0) {
                                    runningToken--
                                    scope.launch {
                                        repository.updateRunningTokenNumber(runningToken)
                                        onRefresh()
                                    }
                                }
                            }
                        ) { Text("-1 पिछला") }

                        Text("#$runningToken", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = SaffronPrimary)

                        Button(
                            onClick = {
                                runningToken++
                                scope.launch {
                                    repository.updateRunningTokenNumber(runningToken)
                                    onRefresh()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary)
                        ) { Text("+1 अगला") }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                isDarbarActive = !isDarbarActive
                                scope.launch {
                                    repository.updateDarbarActiveStatus(isDarbarActive)
                                    onRefresh()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = if (isDarbarActive) Color(0xFFC62828) else Color(0xFF2E7D32)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (isDarbarActive) "दरबार बंद करें" else "दरबार खोलें")
                        }
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://shribalajikripadham.online"))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("🌐 वेबसाइट खोलें")
                        }
                    }
                }
            }
        }

        // 3. 🌐 WEBSITE HERO BANNER EDITOR
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFFFFD54F))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🌐 वेबसाइट मुख्य बैनर (Hero Banner)", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaroonPrimary)
                    OutlinedTextField(
                        value = websiteBannerTitle,
                        onValueChange = { websiteBannerTitle = it },
                        label = { Text("मुख्य शीर्षक (Banner Title)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = websiteBannerSubtitle,
                        onValueChange = { websiteBannerSubtitle = it },
                        label = { Text("उप-शीर्षक / स्थान (Banner Subtitle)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("वेबसाइट पर बैनर दिखाएं", fontSize = 13.sp)
                        Switch(checked = isWebsiteBannerVisible, onCheckedChange = { isWebsiteBannerVisible = it })
                    }
                    Button(
                        onClick = {
                            scope.launch {
                                repository.updateWebsiteHeroBanner(websiteBannerTitle, websiteBannerSubtitle, isWebsiteBannerVisible)
                                Toast.makeText(context, "✅ वेबसाइट बैनर अपडेट हो गया!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("💾 बैनर सेटिंग्स सेव करें")
                    }
                }
            }
        }

        // 4. 📢 EMERGENCY / SPECIAL NOTICE EDITOR
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFFFFD54F))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("📢 आपातकालीन / विशेष सूचना (Emergency Notice)", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaroonPrimary)
                    OutlinedTextField(
                        value = emergencyNotice,
                        onValueChange = { emergencyNotice = it },
                        label = { Text("विशेष सूचना का संदेश") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("वेबसाइट पर सूचना प्रदर्शित करें", fontSize = 13.sp)
                        Switch(checked = isEmergencyNoticeVisible, onCheckedChange = { isEmergencyNoticeVisible = it })
                    }
                    Button(
                        onClick = {
                            scope.launch {
                                repository.updateEmergencyNoticeBanner(emergencyNotice, isEmergencyNoticeVisible)
                                Toast.makeText(context, "✅ विशेष सूचना अपडेट हो गई!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("💾 विशेष सूचना सेव करें")
                    }
                }
            }
        }

        // 5. ⏰ DARBAR SCHEDULE & TIMINGS
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFFFFD54F))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("⏰ दरबार समय सारणी (Schedule & Timings)", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaroonPrimary)
                    OutlinedTextField(
                        value = darbarTimings,
                        onValueChange = { darbarTimings = it },
                        label = { Text("दरबार व दर्शन समय") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            scope.launch {
                                repository.updateDarbarScheduleTimings(darbarTimings)
                                Toast.makeText(context, "✅ समय सारणी अपडेट हो गई!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("💾 समय सारणी सेव करें")
                    }
                }
            }
        }

        // 6. 🙏 GURUJI PROFILE PHOTO (APP & WEBSITE)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDF5)),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFFFFD54F))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("🙏 पूज्य गुरुदेव जी प्रोफाइल फोटो (वेबसाइट व ऐप)", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaroonPrimary)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SacredAvatar(photoUri = gurujiPhotoUri, fallbackText = "गुरुदेव", size = 64.dp)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(
                                onClick = { gurujiPhotoCamera.launch(null) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("📸 कैमरा से फोटो लें", fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = { gurujiPhotoPicker.launch("image/*") },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("📁 गैलरी से चुनें", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // 7. SEVADARS MANAGEMENT SECTION
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFFFFD54F))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🙏 सेवादल मंडल (${sevadarsList.size})", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaroonPrimary)
                        Button(
                            onClick = {
                                newSevName = ""
                                newSevRole = "सेवादार"
                                newSevPhone = ""
                                newSevPhotoUri = ""
                                showAddSevadarDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("+ नया सेवादार", fontSize = 11.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    sevadarsList.forEach { sev ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF9F9F9),
                            border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    SacredAvatar(photoUri = sev.photoUri, fallbackText = sev.name, size = 44.dp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(sev.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("${sev.roleTitleHindi} • 📞 ${sev.phoneNumber}", fontSize = 11.sp, color = Color.DarkGray)
                                    }
                                }
                                Row {
                                    IconButton(
                                        onClick = {
                                            editingSevadar = sev
                                            editSevName = sev.name
                                            editSevRole = sev.roleTitleHindi
                                            editSevPhone = sev.phoneNumber
                                            editSevPhotoUri = sev.photoUri
                                        }
                                    ) {
                                        Text("✏️", fontSize = 16.sp)
                                    }
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                repository.deleteSevadar(sev.id)
                                                loadData()
                                            }
                                        }
                                    ) {
                                        Text("🗑️", fontSize = 16.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 8. PROMINENT DONORS MANAGEMENT (STRICT PRIVACY: NO PHONE NUMBERS PUBLIC)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDF5)),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFFFFD54F))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("🌟 दानदाता एवं संरक्षक मंडल (${donorsList.size})", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaroonPrimary)
                            Text("⚠️ प्राइवेसी नियम: फोन नंबर वेबसाइट पर कभी नहीं दिखेगा", fontSize = 10.sp, color = Color(0xFFE65100))
                        }
                        Button(
                            onClick = {
                                newDonorName = ""
                                newDonorAddress = "ग्राम डूँगरा जाट"
                                newDonorTitle = "मंदिर निर्माण सहयोगी"
                                newDonorPhotoUri = ""
                                showAddDonorDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB78103)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("+ नया दानदाता", fontSize = 11.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    donorsList.forEach { donor ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFFFE082)),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    SacredAvatar(photoUri = donor.photoUri, fallbackText = donor.name, size = 44.dp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(donor.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("📍 ${donor.cityAddress} • ✨ ${donor.title}", fontSize = 11.sp, color = Color.DarkGray)
                                    }
                                }
                                Row {
                                    IconButton(
                                        onClick = {
                                            editingDonor = donor
                                            editDonorName = donor.name
                                            editDonorAddress = donor.cityAddress
                                            editDonorTitle = donor.title
                                            editDonorPhotoUri = donor.photoUri
                                        }
                                    ) {
                                        Text("✏️", fontSize = 16.sp)
                                    }
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                repository.deleteDonor(donor.id)
                                                loadData()
                                            }
                                        }
                                    ) {
                                        Text("🗑️", fontSize = 16.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Sevadar Dialog with Photo Picker
    if (showAddSevadarDialog) {
        AlertDialog(
            onDismissRequest = { showAddSevadarDialog = false },
            title = { Text("नया सेवादार जोड़ें", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SacredAvatar(photoUri = newSevPhotoUri, fallbackText = newSevName.ifEmpty { "सेवादार" }, size = 50.dp)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = { newSevPhotoCamera.launch(null) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("📸 कैमरा", fontSize = 11.sp)
                                }
                                OutlinedButton(
                                    onClick = { newSevPhotoPicker.launch("image/*") },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("📁 गैलरी", fontSize = 11.sp)
                                }
                            }
                            if (newSevPhotoUri.isNotBlank()) {
                                Text("✓ फोटो चयनित", fontSize = 10.sp, color = Color(0xFF2E7D32))
                            }
                        }
                    }
                    OutlinedTextField(value = newSevName, onValueChange = { newSevName = it }, label = { Text("सेवादार का नाम") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = newSevRole, onValueChange = { newSevRole = it }, label = { Text("सेवा / दायित्व पद") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = newSevPhone, onValueChange = { newSevPhone = it }, label = { Text("फोन नंबर") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newSevName.isNotBlank() && newSevPhone.isNotBlank()) {
                            scope.launch {
                                repository.saveSevadar(
                                    SevadarProfile(
                                        name = newSevName.trim(),
                                        roleTitleHindi = newSevRole.trim(),
                                        roleTitleEnglish = newSevRole.trim(),
                                        phoneNumber = newSevPhone.trim(),
                                        photoUri = newSevPhotoUri.trim(),
                                        displayOrder = sevadarsList.size + 1
                                    )
                                )
                                showAddSevadarDialog = false
                                newSevName = ""
                                newSevPhone = ""
                                newSevPhotoUri = ""
                                loadData()
                            }
                        }
                    }
                ) { Text("जोड़ें व सिंक करें") }
            },
            dismissButton = {
                TextButton(onClick = { showAddSevadarDialog = false }) { Text("रद्द करें") }
            }
        )
    }

    // Edit Sevadar Dialog with Photo Picker
    if (editingSevadar != null) {
        AlertDialog(
            onDismissRequest = { editingSevadar = null },
            title = { Text("सेवादार विवरण संपादित करें", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SacredAvatar(photoUri = editSevPhotoUri, fallbackText = editSevName.ifEmpty { "सेवादार" }, size = 50.dp)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = { editSevPhotoCamera.launch(null) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("📸 कैमरा", fontSize = 11.sp)
                                }
                                OutlinedButton(
                                    onClick = { editSevPhotoPicker.launch("image/*") },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("📁 गैलरी", fontSize = 11.sp)
                                }
                            }
                            if (editSevPhotoUri.isNotBlank()) {
                                Text("✓ फोटो चयनित", fontSize = 10.sp, color = Color(0xFF2E7D32))
                            }
                        }
                    }
                    OutlinedTextField(value = editSevName, onValueChange = { editSevName = it }, label = { Text("सेवादार का नाम") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = editSevRole, onValueChange = { editSevRole = it }, label = { Text("सेवा / दायित्व पद") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = editSevPhone, onValueChange = { editSevPhone = it }, label = { Text("फोन नंबर") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val curr = editingSevadar
                        if (curr != null && editSevName.isNotBlank() && editSevPhone.isNotBlank()) {
                            scope.launch {
                                repository.saveSevadar(
                                    curr.copy(
                                        name = editSevName.trim(),
                                        roleTitleHindi = editSevRole.trim(),
                                        roleTitleEnglish = editSevRole.trim(),
                                        phoneNumber = editSevPhone.trim(),
                                        photoUri = editSevPhotoUri.trim()
                                    )
                                )
                                editingSevadar = null
                                loadData()
                            }
                        }
                    }
                ) { Text("सेव करें व सिंक करें") }
            },
            dismissButton = {
                TextButton(onClick = { editingSevadar = null }) { Text("रद्द करें") }
            }
        )
    }

    // Add Donor Dialog with Photo Picker
    if (showAddDonorDialog) {
        AlertDialog(
            onDismissRequest = { showAddDonorDialog = false },
            title = { Text("नया दानदाता / संरक्षक जोड़ें", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SacredAvatar(photoUri = newDonorPhotoUri, fallbackText = newDonorName.ifEmpty { "दानदाता" }, size = 50.dp)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = { newDonorPhotoCamera.launch(null) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB78103)),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("📸 कैमरा", fontSize = 11.sp)
                                }
                                OutlinedButton(
                                    onClick = { newDonorPhotoPicker.launch("image/*") },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("📁 गैलरी", fontSize = 11.sp)
                                }
                            }
                            if (newDonorPhotoUri.isNotBlank()) {
                                Text("✓ फोटो चयनित", fontSize = 10.sp, color = Color(0xFF2E7D32))
                            }
                        }
                    }
                    OutlinedTextField(value = newDonorName, onValueChange = { newDonorName = it }, label = { Text("दानदाता का नाम") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = newDonorAddress, onValueChange = { newDonorAddress = it }, label = { Text("शहर / पता") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = newDonorTitle, onValueChange = { newDonorTitle = it }, label = { Text("सहयोग पद / सेवा विवरण") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newDonorName.isNotBlank()) {
                            scope.launch {
                                repository.saveDonor(
                                    DonorProfile(
                                        name = newDonorName.trim(),
                                        cityAddress = newDonorAddress.trim(),
                                        title = newDonorTitle.trim(),
                                        photoUri = newDonorPhotoUri.trim(),
                                        displayOrder = donorsList.size + 1
                                    )
                                )
                                showAddDonorDialog = false
                                newDonorName = ""
                                newDonorPhotoUri = ""
                                loadData()
                            }
                        }
                    }
                ) { Text("जोड़ें व सिंक करें") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDonorDialog = false }) { Text("रद्द करें") }
            }
        )
    }

    // Edit Donor Dialog with Photo Picker
    if (editingDonor != null) {
        AlertDialog(
            onDismissRequest = { editingDonor = null },
            title = { Text("दानदाता विवरण संपादित करें", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SacredAvatar(photoUri = editDonorPhotoUri, fallbackText = editDonorName.ifEmpty { "दानदाता" }, size = 50.dp)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = { editDonorPhotoCamera.launch(null) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB78103)),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("📸 कैमरा", fontSize = 11.sp)
                                }
                                OutlinedButton(
                                    onClick = { editDonorPhotoPicker.launch("image/*") },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("📁 गैलरी", fontSize = 11.sp)
                                }
                            }
                            if (editDonorPhotoUri.isNotBlank()) {
                                Text("✓ फोटो चयनित", fontSize = 10.sp, color = Color(0xFF2E7D32))
                            }
                        }
                    }
                    OutlinedTextField(value = editDonorName, onValueChange = { editDonorName = it }, label = { Text("दानदाता का नाम") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = editDonorAddress, onValueChange = { editDonorAddress = it }, label = { Text("शहर / पता") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = editDonorTitle, onValueChange = { editDonorTitle = it }, label = { Text("सहयोग पद / सेवा विवरण") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val curr = editingDonor
                        if (curr != null && editDonorName.isNotBlank()) {
                            scope.launch {
                                repository.saveDonor(
                                    curr.copy(
                                        name = editDonorName.trim(),
                                        cityAddress = editDonorAddress.trim(),
                                        title = editDonorTitle.trim(),
                                        photoUri = editDonorPhotoUri.trim()
                                    )
                                )
                                editingDonor = null
                                loadData()
                            }
                        }
                    }
                ) { Text("सेव करें व सिंक करें") }
            },
            dismissButton = {
                TextButton(onClick = { editingDonor = null }) { Text("रद्द करें") }
            }
        )
    }
}
