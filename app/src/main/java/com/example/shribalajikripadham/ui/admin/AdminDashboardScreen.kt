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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.widget.Toast
import com.example.shribalajikripadham.data.local.DatabaseHelper
import com.example.shribalajikripadham.ai.FaceEmbeddingEngine
import com.example.shribalajikripadham.data.model.*
import com.example.shribalajikripadham.data.repository.AshramRepository
import com.example.shribalajikripadham.hardware.GeofenceLocationManager
import com.example.shribalajikripadham.theme.*
import com.example.shribalajikripadham.ui.common.SacredAvatar
import com.example.shribalajikripadham.util.*
import androidx.activity.compose.rememberLauncherForActivityResult
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { AshramRepository(context) }
    val scope = rememberCoroutineScope()

    var loggedInAdmin by remember { mutableStateOf<Admin?>(null) }
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
    var newSevPhotoUri by remember { mutableStateOf("") }

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
    var editingAdmin by remember { mutableStateOf<Admin?>(null) }
    var editSevPhotoUri by remember { mutableStateOf("") }
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
    var customDistancesList by remember { mutableStateOf<List<CustomCityDistance>>(emptyList()) }
    var uiSectionsList by remember { mutableStateOf<List<UiSectionConfig>>(emptyList()) }

    fun refreshData() {
        scope.launch {
            val s = repository.getSettings()
            settings = s
            latInput = s.latitude.toString()
            longInput = s.longitude.toString()
            radiusInput = s.allowedRadiusMeters.toString()
            geofenceEnforced = s.isGeofenceEnforced

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

            updVersionCode = s.latestVersionCode.toString()
            updVersionName = s.latestVersionName
            updNotes = s.updateNotes
            updApkUrl = s.apkDownloadUrl
            updIsForce = s.isForceUpdate

            todayTokens = repository.getAllTokensToday()
            adminsList = repository.getAllAdmins()
            eventsList = repository.getAllEvents()
            notificationsList = repository.getAllNotifications()
            customDistancesList = repository.getAllCustomCityDistances()
            uiSectionsList = repository.getUiSectionConfigs()
        }
    }

    LaunchedEffect(loggedInAdmin) {
        if (loggedInAdmin != null) {
            refreshData()
            try {
                repository.syncLiveConfigFromGitHub()
                repository.syncLiveTokensFromCloud()
                repository.syncAdminsFromGitHub()
                refreshData()
            } catch (e: Exception) {}

            // Auto-refresh token queue from cloud every 20 seconds while admin stays on dashboard
            while (isActive) {
                delay(20000)
                try {
                    val (hasNew, count) = repository.syncLiveTokensFromCloud()
                    if (hasNew && count > 0) {
                        todayTokens = repository.getAllTokensToday()
                    }
                } catch (e: Exception) {}
            }
        }
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
                    TextButton(onClick = onBack) {
                        Text(if (isHindi) "← वापस" else "← Back", color = SaffronLight, fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    if (loggedInAdmin != null) {
                        TextButton(onClick = {
                            loggedInAdmin = null
                            usernameInput = ""
                            passwordInput = ""
                            pinInput = ""
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
                                label = { Text(if (isHindi) "सुपर एडमिन पासवर्ड दर्ज करें" else "Enter Super Admin Password") },
                                leadingIcon = { Text("🔑") },
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
                                        val admin = repository.authenticateSuperAdminByPasswordOnly(pass)
                                        if (admin != null) {
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
                                    label = { Text(if (isHindi) "सेवादार यूजरनेम" else "Sevadar Username") },
                                    leadingIcon = { Text("👤") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = passwordInput,
                                    onValueChange = { passwordInput = it },
                                    label = { Text(if (isHindi) "पासवर्ड" else "Password") },
                                    leadingIcon = { Text("🔑") },
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
                                    label = { Text(if (isHindi) "4-अंकीय सेवादार पिन दर्ज करें" else "Enter 4-Digit Sevadar PIN") },
                                    leadingIcon = { Text("🔢") },
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
                                        val admin = if (loginWithCreds) {
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

                                        if (admin != null) {
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
            if (isSuper) {
                allowedTabs.add(if (isHindi) "सेवादार खाते" else "Sevadars")
                allowedTabs.add(if (isHindi) "सेवाएं ऑन/ऑफ" else "Services")
                allowedTabs.add(if (isHindi) "ऐप कस्टमाइजर" else "Customizer")
                allowedTabs.add(if (isHindi) "कस्टम दूरियाँ" else "Distances")
                allowedTabs.add(if (isHindi) "सुपर कंट्रोल" else "Super Control")
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
                            Triple("रजिस्टर स्कैन", admin.canScanPaperRegister || isSuper, "📷"),
                            Triple("आश्रम पर्चे", admin.canManageParchas || isSuper, "📜"),
                            Triple("बालाजी यात्रा", admin.canManageYatra || isSuper, "🚌"),
                            Triple("आय-व्यय", admin.canManageExpenses || isSuper, "💰"),
                            Triple("GPS दायरा", admin.canChangeLocation || isSuper, "📍"),
                            Triple("सूचना प्रसारण", admin.canSendNotifications || isSuper, "📢"),
                            Triple("आश्रम विवरण", admin.canEditAshramInfo || isSuper, "⚙️"),
                            Triple("सेवादार नियंत्रण", isSuper, "👥"),
                            Triple("भक्त फोटो", admin.canViewDevoteePhotos || isSuper, "📸")
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
                                }
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
                                successMsg = locationSuccessMsg,
                                errorMsg = locationErrorMsg,
                                onSave = {
                                    scope.launch {
                                        try {
                                            val latVal = latInput.toDouble()
                                            val longVal = longInput.toDouble()
                                            val radVal = radiusInput.toDouble().coerceIn(50.0, 200.0)
                                            repository.updateAshramLocation(admin, latVal, longVal, radVal, geofenceEnforced)
                                            try { repository.publishCurrentSettingsToGitHub(admin.name) } catch (e: Exception) {}
                                            locationSuccessMsg = if (isHindi) "✓ नई GPS लोकेशन सुरक्षित व क्लाउड द्वारा सभी भक्तों के फोन पर लाइव अपडेट हो गई!" else "GPS coordinates updated & broadcast to all users live!"
                                            locationErrorMsg = null
                                            refreshData()
                                        } catch (e: Exception) {
                                            locationErrorMsg = e.localizedMessage
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
                                        try { repository.publishCurrentSettingsToGitHub(admin.name) } catch (e: Exception) {}
                                        svcSuccessMsg = if (isHindi)
                                            "सेवाएं व टोकन समय-निर्धारण सुरक्षित व सभी भक्तों के फोन पर लाइव अपडेट हुआ!"
                                        else
                                            "Service visibility & scheduled opening saved & broadcast to all devotees!"
                                        refreshData()
                                    }
                                }
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
                                successMsg = customizerSuccessMsg,
                                events = eventsList,
                                onOpenAddEvent = { showAddEventDialog = true },
                                onDeleteEvent = { evId ->
                                    scope.launch {
                                        repository.deleteEvent(evId)
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
                                        try { repository.publishCurrentSettingsToGitHub(admin.name) } catch (e: Exception) {}
                                        customizerSuccessMsg = if (isHindi) "✓ आश्रम विवरण, फोटो व सोशल लिंक्स सुरक्षित व सभी भक्तों के फोन पर लाइव अपडेट हो गए!" else "Ashram details saved & published live to all users!"
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
                                        refreshData()
                                    }
                                },
                                onUpdateEnforcedLayout = { layoutKey, isEnforced ->
                                    scope.launch {
                                        repository.updateActiveUiLayoutEnforced(layoutKey, isEnforced)
                                        try { repository.publishCurrentSettingsToGitHub(admin.name) } catch (e: Exception) {}
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
                    OutlinedTextField(
                        value = newSevName,
                        onValueChange = { newSevName = it },
                        label = { Text(if (isHindi) "सेवादार का नाम" else "Full Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newSevUsername,
                        onValueChange = { newSevUsername = it },
                        label = { Text(if (isHindi) "यूजरनेम (Unique Username)" else "Unique Username") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newSevPassword,
                        onValueChange = { newSevPassword = it },
                        label = { Text(if (isHindi) "पासवर्ड (Password)" else "Password") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newSevPhone,
                        onValueChange = { newSevPhone = it },
                        label = { Text(if (isHindi) "मोबाइल नंबर" else "Mobile Number") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newSevPin,
                        onValueChange = { newSevPin = it },
                        label = { Text(if (isHindi) "त्वरित 4-अंकीय पिन" else "Quick 4-Digit PIN") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newSevPhotoUri,
                        onValueChange = { newSevPhotoUri = it },
                        label = { Text(if (isHindi) "सेवादार फोटो (URL या फ़ाइल पाथ)" else "Photo URL / File Path") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (newSevPhotoUri.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SacredAvatar(photoUri = newSevPhotoUri, name = newSevName.ifEmpty { "सेवादार" }, size = 44.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isHindi) "फोटो पूर्वावलोकन" else "Photo Preview", fontSize = 12.sp, color = Color.Gray)
                        }
                    }

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
                        Text(if (isHindi) "🔢 मनचाहा टोकन नंबर डालने की अनुमति (Custom Token #)" else "Allow Custom Token Number", fontSize = 13.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = newSevCanExportPdf, onCheckedChange = { newSevCanExportPdf = it })
                        Text(if (isHindi) "📄 आज की टोकन सूची PDF डाउनलोड (Export PDF)" else "Allow Export PDF", fontSize = 13.sp)
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

                                repository.createSevadarAdmin(
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
                                    photoUri = newSevPhotoUri
                                )

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
            title = { Text(if (isHindi) "सेवादार अनुमतियाँ व फोटो संपादित करें" else "Edit Sevadar Permissions & Photo", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "सेवादार: ${target.name} (${target.username})",
                        fontWeight = FontWeight.Bold,
                        color = MaroonPrimary
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = editSevPhotoUri,
                        onValueChange = { editSevPhotoUri = it },
                        label = { Text(if (isHindi) "सेवादार फोटो (URL या फ़ाइल पाथ)" else "Photo URL / File Path") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (editSevPhotoUri.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SacredAvatar(photoUri = editSevPhotoUri, name = target.name, size = 48.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(if (isHindi) "फोटो पूर्वावलोकन" else "Photo Preview", fontSize = 12.sp, color = Color.Gray)
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
                        Text(if (isHindi) "🔢 मनचाहा टोकन नंबर डालने की अनुमति (Custom Token #)" else "Allow Custom Token Number", fontSize = 13.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = editSevCanExportPdf, onCheckedChange = { editSevCanExportPdf = it })
                        Text(if (isHindi) "📄 आज की टोकन सूची PDF डाउनलोड (Export PDF)" else "Allow Export PDF", fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            repository.updateAdminPermissions(
                                adminId = target.id,
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
                                isActive = target.isActive
                            )
                            repository.updateAdminPhoto(target.id, editSevPhotoUri)
                            try { repository.publishAdminsToGitHub() } catch (e: Exception) {}
                            editingAdmin = null
                            refreshData()
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
    onSyncFromGoogleSheet: (() -> Unit)? = null
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
    var tokenToCancel by remember { mutableStateOf<Token?>(null) }
    var tokenToDelete by remember { mutableStateOf<Token?>(null) }
    var zoomedPhotoToken by remember { mutableStateOf<Token?>(null) }

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

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onUpdateRunningToken((settings.runningTokenNumber - 1).coerceAtLeast(1)) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("-1 पिछला", color = Color.Black)
                        }
                        Button(
                            onClick = { onUpdateRunningToken(settings.runningTokenNumber + 1) },
                            colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("+1 अगला टोकन", color = Color.White, fontWeight = FontWeight.Bold)
                        }
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
                    placeholder = { Text(if (isHindi) "नाम, फोन नंबर या शहर से खोजें..." else "Search devotee by name, phone or city...") },
                    leadingIcon = { Text("🔍") },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = {
                                    searchQuery = ""
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                }) {
                                    Text("✕")
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
                            IconButton(onClick = {
                                formCapturedBitmap = null
                                formPhotoUri = ""
                            }) {
                                Text("✕", color = Color.Red, fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
                                onLatChange("28.4089")
                                onLongChange("77.8789")
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
                        if (num != null && num > 200.0) {
                            onRadiusChange("200.0")
                        } else {
                            onRadiusChange(clean)
                        }
                    },
                    label = { Text(if (isHindi) "स्वीकृत परिधि (100m - 200m)" else "Allowed Radius (100m - 200m)") },
                    supportingText = {
                        Text(
                            text = if (isHindi)
                                "नियम: भक्त केवल आश्रम के 100m से 200m के दायरे में ही टोकन बना सकते हैं (अधिकतम सीमा: 200m)।"
                            else
                                "Rule: Devotees can only generate tokens within 100m - 200m of Ashram (Max: 200m).",
                            fontSize = 11.sp,
                            color = MaroonAccent
                        )
                    },
                    enabled = canChangeLocation,
                    modifier = Modifier.fillMaxWidth()
                )

                if (canChangeLocation) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onRadiusChange("100.0") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("100m (सख्त)", fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = { onRadiusChange("150.0") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("150m", fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = { onRadiusChange("200.0") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("200m (मानक)", fontSize = 11.sp)
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

                if (successMsg != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(successMsg, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                }
                if (errorMsg != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(errorMsg, color = Color.Red, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onSave,
                    enabled = canChangeLocation,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary)
                ) {
                    Text(if (isHindi) "🌐 लोकेशन सुरक्षित करें व सभी भक्तों के फोन पर लाइव भेजें" else "🌐 Save & Broadcast Coordinates Live", fontWeight = FontWeight.Bold)
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
    successMsg: String?,
    onSave: () -> Unit
) {
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
                            // Calculate upcoming Sunday 06:00 AM
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

        // SECTION 2: MASTER FEATURE VISIBILITY MATRIX
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

                // 1. Bus Seat Booking (Permanent Hidden Control)
                Surface(
                    color = if (isYatra) Color(0xFFFFF8E1) else Color(0xFFFAFAFA),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (isYatra) Color(0xFFFFD54F) else Color(0xFFE0E0E0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isHindi) "🚌 श्री बालाजी यात्रा व बस बुकिंग" else "🚌 Balaji Yatra & Bus Seat Booking",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isYatra) Color(0xFFE65100) else Color.DarkGray
                                )
                                Text(
                                    text = if (isYatra)
                                        "सक्रिय: आम भक्तों को बस सीट बुकिंग विकल्प दिखाई दे रहा है।"
                                    else
                                        "🔒 स्थायी रूप से बंद: यह विकल्प भक्तों से पूरी तरह छिपा हुआ है।",
                                    fontSize = 11.sp,
                                    color = if (isYatra) Color(0xFF2E7D32) else Color.Gray
                                )
                            }
                            Switch(checked = isYatra, onCheckedChange = onYatraChange)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

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
    successMsg: String?,
    events: List<AshramEvent>,
    onOpenAddEvent: () -> Unit,
    onDeleteEvent: (Long) -> Unit,
    onSave: () -> Unit
) {
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
                    OutlinedTextField(
                        value = gurujiPhotoUri,
                        onValueChange = onGurujiPhotoUriChange,
                        label = { Text(if (isHindi) "पूज्य गुरुजी फोटो (URL या फ़ाइल पाथ)" else "Guruji Photo URL / File Path") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (gurujiPhotoUri.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SacredAvatar(photoUri = gurujiPhotoUri, name = gurujiName, size = 52.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                if (isHindi) "गुरुजी फोटो पूर्वावलोकन (Preview)" else "Guruji Photo Preview",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
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
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = testNotes,
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.fillMaxWidth()
                    )
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
    onUpdateMasterPassword: (String, String, (Boolean, String) -> Unit) -> Unit,
    onUpdateMaxDailyTokens: (Int) -> Unit,
    onUpdateEnforcedLayout: (String, Boolean) -> Unit,
    onUpdateCloudSync: (String, Boolean) -> Unit,
    onTriggerCloudSync: ((Boolean, String) -> Unit) -> Unit,
    onExportDatabaseBackup: ((Boolean, String) -> Unit) -> Unit,
    onRestoreDatabaseBackup: (String, (Boolean, String) -> Unit) -> Unit
) {
    val context = LocalContext.current
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

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
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

                    val layouts = listOf(
                        Triple("CLASSIC_DARBAR", "श्री दरबार (Classic)", "🛕"),
                        Triple("MODERN_CARDS", "आधुनिक कार्ड (Modern)", "📱"),
                        Triple("VEDIC_GRID", "वैदिक ग्रिड (Grid)", "🏛️"),
                        Triple("COMPACT_LIST", "सरल सूची (Compact)", "📋"),
                        Triple("DIVINE_FEED", "दिव्य प्रवाह (Divine)", "✨")
                    )

                    layouts.forEach { (key, title, icon) ->
                        Surface(
                            onClick = { selectedLayoutKey = key },
                            color = if (selectedLayoutKey == key) Color(0xFFFFF3E0) else Color(0xFFF9F9F9),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (selectedLayoutKey == key) SaffronPrimary else Color(0xFFE0E0E0)),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(icon, fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(title, fontWeight = if (selectedLayoutKey == key) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp)
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
                            layoutSuccessMsg = if (isHindi) "UI लेआउट नियम सफलतापूर्वक सुरक्षित हुआ!" else "Layout policy saved successfully!"
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
                                isCloudSyncing = true
                                cloudSyncStatusMsg = if (isHindi) "क्लाउड से सिंक हो रहा है..." else "Syncing with cloud..."
                                onTriggerCloudSync { success, msg ->
                                    isCloudSyncing = false
                                    cloudSyncStatusMsg = msg
                                }
                            },
                            enabled = !isCloudSyncing && cloudUrlInput.isNotBlank(),
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
                                    hasUnsavedChanges = true
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
                                    hasUnsavedChanges = true
                                }
                            },
                            enabled = index < localSections.size - 1,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Text("⬇️", fontSize = 16.sp)
                        }

                        // Visibility Toggle (Eye / Eye-off)
                        IconButton(
                            onClick = {
                                val list = localSections.toMutableList()
                                list[index] = list[index].copy(isVisible = !list[index].isVisible)
                                localSections = list
                                hasUnsavedChanges = true
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
                        localSections = UiSectionConfig.defaultSections()
                        onResetToDefault()
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
