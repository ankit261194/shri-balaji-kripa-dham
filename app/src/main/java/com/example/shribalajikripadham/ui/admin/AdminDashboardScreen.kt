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
import com.example.shribalajikripadham.data.model.*
import com.example.shribalajikripadham.data.repository.AshramRepository
import com.example.shribalajikripadham.hardware.GeofenceLocationManager
import com.example.shribalajikripadham.theme.*
import com.example.shribalajikripadham.ui.common.SacredAvatar
import com.example.shribalajikripadham.util.*
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch

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
                            text = if (isHindi) "👑 मुख्य व्यवस्थापक" else "👑 Super Admin",
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
                                text = if (isHindi) "मुख्य व्यवस्थापक प्रवेश" else "Super Admin Portal",
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
                                    text = if (isHindi) "👑 मुख्य व्यवस्थापक लॉगिन" else "👑 Login as Super Admin",
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
                                    text = admin.name,
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
                                onSyncFromGoogleSheet = {
                                    scope.launch {
                                        val res = repository.syncTokensFromGoogleSheet()
                                        refreshData()
                                        if (res.first) {
                                            Toast.makeText(context, if (isHindi) "✅ Google Sheet से ${res.second} नए टोकन सिंक हुए!" else "✅ Synced ${res.second} new tokens from Google Sheet!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, if (isHindi) "Google Sheet सिंक: कोई नया टोकन नहीं मिला या वेबहुक लिंक सेट नहीं है" else "Google Sheet sync: No new tokens or webhook not set", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            )
                        }
                        currentTabTitle == "मैनुअल टोकन" || currentTabTitle == "Manual" -> {
                            ManualTokenTab(
                                isHindi = isHindi,
                                adminName = admin.name,
                                name = manualName,
                                onNameChange = { manualName = it },
                                phone = manualPhone,
                                onPhoneChange = { manualPhone = it },
                                successMsg = manualSuccessMsg,
                                onSubmit = {
                                    scope.launch {
                                        try {
                                            val t = repository.registerToken(
                                                patientName = manualName,
                                                phoneNumber = manualPhone,
                                                deviceId = "MANUAL_BY_${admin.id}_${System.currentTimeMillis()}",
                                                latitude = settings.latitude,
                                                longitude = settings.longitude,
                                                registeredBy = "DESK_${admin.name}"
                                            )
                                            manualSuccessMsg = if (isHindi)
                                                "सफलतापूर्वक टोकन #${t.tokenNumber} जारी किया गया!"
                                            else
                                                "Successfully issued Token #${t.tokenNumber}!"
                                            manualName = ""
                                            manualPhone = ""
                                            refreshData()
                                        } catch (e: Exception) {
                                            manualSuccessMsg = "त्रुटि: ${e.localizedMessage}"
                                        }
                                    }
                                }
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
                                            val radVal = radiusInput.toDouble()
                                            repository.updateAshramLocation(admin, latVal, longVal, radVal, geofenceEnforced)
                                            locationSuccessMsg = if (isHindi) "लोकेशन व परिधि सुरक्षित की गई!" else "GPS coordinates and radius updated!"
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
                                        // Trigger heads-up system alert
                                        NotificationHelper.showSystemNotification(context, notifTitle, notifMsg)
                                        notifSuccessMsg = if (isHindi) "सूचना तुरंत प्रसारित कर दी गई!" else "Notification successfully broadcasted!"
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
                                            targetAdmin.canEditAshramInfo,
                                            targetAdmin.canViewDevoteePhotos,
                                            !targetAdmin.isActive
                                        )
                                        refreshData()
                                    }
                                },
                                onDelete = { targetAdmin ->
                                    scope.launch {
                                        repository.deleteAdmin(targetAdmin.id)
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
                                        svcSuccessMsg = if (isHindi)
                                            "सेवाएं व टोकन समय-निर्धारण सफलतापूर्वक सुरक्षित हुआ!"
                                        else
                                            "Service visibility & scheduled opening saved successfully!"
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
                                        customizerSuccessMsg = if (isHindi) "आश्रम का विवरण, गुरुजी फोटो व सोशल लिंक्स सुरक्षित किए गए!" else "Ashram details, Guruji photo & social links saved!"
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
                                        refreshData()
                                    }
                                },
                                onUpdateEnforcedLayout = { layoutKey, isEnforced ->
                                    scope.launch {
                                        repository.updateActiveUiLayoutEnforced(layoutKey, isEnforced)
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
                                        refreshData()
                                        Toast.makeText(context, if (isHindi) "UI लेआउट क्रम सफलतापूर्वक अपडेट हो गया!" else "UI layout updated successfully!", Toast.LENGTH_SHORT).show()
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
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            if (newSevName.isNotBlank() && newSevUsername.isNotBlank() && newSevPassword.isNotBlank()) {
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
                                    photoUri = newSevPhotoUri
                                )
                                showCreateSevadarDialog = false
                                newSevName = ""
                                newSevUsername = ""
                                newSevPassword = ""
                                newSevPhone = ""
                                newSevPin = ""
                                newSevPhotoUri = ""
                                newSevCanPhotos = false
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
                                isActive = target.isActive
                            )
                            repository.updateAdminPhoto(target.id, editSevPhotoUri)
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
    onUpdateRunningToken: (Int) -> Unit,
    onUpdateStatus: (Long, TokenStatus) -> Unit,
    onToggleDarshan: (Long, Boolean) -> Unit,
    onCancelToken: ((Long) -> Unit)? = null,
    onDeleteToken: ((Long) -> Unit)? = null,
    onSyncFromGoogleSheet: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val syncManager = com.example.shribalajikripadham.data.network.GoogleSheetTokenSyncManager
    var showSheetConfigDialog by remember { mutableStateOf(false) }
    var sheetWebhookUrlInput by remember { mutableStateOf(syncManager.getWebhookUrl(context)) }
    var isSyncingSheet by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedDistanceFilter by remember { mutableStateOf(DistanceFilter.ALL) }
    var selectedSortOrder by remember { mutableStateOf(TokenSortOrder.TOKEN_NUMBER) }
    var isExportingPdf by remember { mutableStateOf(false) }
    var tokenToDelete by remember { mutableStateOf<Token?>(null) }

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
        modifier = Modifier.fillMaxSize(),
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

                    // 📈 CENTRAL GOOGLE SHEETS LIVE SYNC CARD
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9)),
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
                                    Text("📈", fontSize = 20.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = if (isHindi) "Google Sheets टोकन सिंक" else "Google Sheets Token Sync",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = Color(0xFF1B5E20)
                                        )
                                        Text(
                                            text = if (syncManager.isConfigured(context)) "🟢 सिंक सक्रिय (Active)" else "⚠️ लिंक सेट करें",
                                            fontSize = 11.sp,
                                            color = if (syncManager.isConfigured(context)) Color(0xFF2E7D32) else Color(0xFFE65100),
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
                                        if (onSyncFromGoogleSheet != null) {
                                            isSyncingSheet = true
                                            onSyncFromGoogleSheet()
                                            isSyncingSheet = false
                                        }
                                    },
                                    enabled = !isSyncingSheet,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "🔄 " + (if (isHindi) "शीट से सिंक करें" else "Sync Sheet Tokens"),
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
                                        text = "🔗 " + (if (isHindi) "वेबहुक सेटिंग्स" else "Webhook Config"),
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
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Text("✕")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

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

                    // Optional Devotee Photo if permitted
                    if (canViewPhotos) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (token.photoUri.isNotBlank()) {
                                SacredAvatar(photoUri = token.photoUri, name = token.patientName, size = 32.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("📸 सत्यापित फोटो संलग्न", fontSize = 11.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.SemiBold)
                            } else {
                                Text("📷 फोटो संलग्न नहीं", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
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
                }
            }
        }
    }
}

@Composable
fun ManualTokenTab(
    isHindi: Boolean,
    adminName: String,
    name: String,
    onNameChange: (String) -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit,
    successMsg: String?,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (isHindi) "मैनुअल टोकन डेस्क (बुजुर्ग/अशिक्षित भक्तों हेतु)" else "Manual Desk Registration (Walk-ins/Elderly)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaroonPrimary
                )
                Text(
                    text = if (isHindi)
                        "जिनके पास स्मार्टफोन नहीं है, सेवादार उनका विवरण दर्ज करके टोकन जारी कर सकते हैं।"
                    else
                        "Issue a token for elderly or visitors without smartphones directly.",
                    fontSize = 13.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text(if (isHindi) "भक्त / मरीज का नाम" else "Devotee / Patient Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = onPhoneChange,
                    label = { Text(if (isHindi) "संपर्क फोन नंबर" else "Mobile Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )

                if (successMsg != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(successMsg, color = if (successMsg.startsWith("त्रुटि")) Color.Red else Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onSubmit,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary)
                ) {
                    Text(if (isHindi) "टोकन पर्ची जारी करें" else "Issue Token Pass", fontWeight = FontWeight.Bold)
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
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = radius,
                    onValueChange = onRadiusChange,
                    label = { Text(if (isHindi) "स्वीकृत परिधि (मीटर में)" else "Allowed Radius (Meters)") },
                    enabled = canChangeLocation,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = isEnforced,
                        onCheckedChange = onEnforcedChange,
                        enabled = canChangeLocation
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        if (isHindi) "सख्त जिओफेंसिंग लागू रखें (200m के अंदर ही टोकन)" else "Strict Geofencing Enforced (Inside 200m only)",
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
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary)
                ) {
                    Text(if (isHindi) "कोऑर्डिनेट्स सुरक्षित करें" else "Save Coordinates", fontWeight = FontWeight.Bold)
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
                Button(
                    onClick = onOpenCreate,
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary)
                ) {
                    Text(if (isHindi) "+ नया सेवादार" else "+ Add Sevadar")
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
                            if (a.canViewDevoteePhotos) "भक्त फोटो" else null
                        ).joinToString(", "),
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )

                    if (a.role != AdminRole.SUPER_ADMIN) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
