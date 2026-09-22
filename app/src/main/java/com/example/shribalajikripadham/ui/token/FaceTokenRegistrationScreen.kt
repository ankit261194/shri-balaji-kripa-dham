package com.example.shribalajikripadham.ui.token

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shribalajikripadham.ai.FaceEmbeddingEngine
import com.example.shribalajikripadham.data.model.AshramSettings
import com.example.shribalajikripadham.data.model.DevoteeFaceProfile
import com.example.shribalajikripadham.data.model.FaceMatchResult
import com.example.shribalajikripadham.data.model.Token
import com.example.shribalajikripadham.data.repository.AshramRepository
import com.example.shribalajikripadham.hardware.DeviceFingerprintManager
import com.example.shribalajikripadham.hardware.GeofenceLocationManager
import com.example.shribalajikripadham.theme.*
import com.example.shribalajikripadham.util.TokenCardExporter
import com.example.shribalajikripadham.util.SundayTokenScheduleHelper
import com.example.shribalajikripadham.util.SundayScheduleState
import com.example.shribalajikripadham.util.DevoteePhotoHelper
import com.example.shribalajikripadham.util.TakeFrontPicturePreview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*



enum class FaceScanState {
    SCANNING,
    PROCESSING,
    CONFIRMATION_SCREEN,
    NO_MATCH_FALLBACK,
    MANUAL_ENTRY,
    TOKEN_GENERATED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaceTokenRegistrationScreen(
    isHindi: Boolean,
    onBack: () -> Unit,
    onNavigateToManualForm: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val repository = remember { AshramRepository(context) }
    val scope = rememberCoroutineScope()

    var scanState by remember { mutableStateOf(FaceScanState.SCANNING) }
    var settings by remember { mutableStateOf(AshramSettings()) }
    var deviceId by remember { mutableStateOf("") }
    var existingToken by remember { mutableStateOf<Token?>(null) }
    var savedImageUri by remember { mutableStateOf<Uri?>(null) }

    // Geofencing background pre-warming state (Strict real GPS enforcement)
    var userLatitude by remember { mutableDoubleStateOf(0.0) }
    var userLongitude by remember { mutableDoubleStateOf(0.0) }

    // Real Camera Captured Photo & Face Recognition State
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var capturedPhotoUri by remember { mutableStateOf("") }
    var matchResult by remember { mutableStateOf<FaceMatchResult?>(null) }
    var candidateVector by remember { mutableStateOf(FloatArray(FaceEmbeddingEngine.EMBEDDING_DIM)) }
    var generatedToken by remember { mutableStateOf<Token?>(null) }
    var isEmbeddingAutoUpdated by remember { mutableStateOf(false) }
    var showLocationAlertDialog by remember { mutableStateOf(false) }
    var locationAlertTitle by remember { mutableStateOf("") }
    var locationAlertMessage by remember { mutableStateOf("") }
    var showScheduleAlertDialog by remember { mutableStateOf(false) }
    var scheduleAlertTitle by remember { mutableStateOf("") }
    var scheduleAlertMessage by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    var processingDurationMs by remember { mutableLongStateOf(0L) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Manual Entry Form Inputs (For First-time Devotees / Fallback)
    var manualName by remember { mutableStateOf("") }
    var manualPhone by remember { mutableStateOf("") }
    var manualCity by remember { mutableStateOf("") }
    var enrollFaceForFuture by remember { mutableStateOf(true) }
    var locationSuggestions by remember { mutableStateOf<List<com.example.shribalajikripadham.util.IndiaLocation>>(emptyList()) }
    var showLocationDropdown by remember { mutableStateOf(false) }

    LaunchedEffect(manualCity) {
        val q = manualCity.trim()
        if (q.length >= 2) {
            val localResults = com.example.shribalajikripadham.util.IndiaLocationsDatabase.search(q, maxLimit = 8)
            if (localResults.isNotEmpty()) {
                locationSuggestions = localResults
                showLocationDropdown = true
            }
            // Always fetch online results (villages, hamlets, tehsils across India) and update suggestions
            try {
                val fullResults = com.example.shribalajikripadham.util.IndiaLocationsDatabase.searchWithOnlineFallback(q, maxLimit = 15)
                if (fullResults.isNotEmpty()) {
                    locationSuggestions = fullResults
                    showLocationDropdown = true
                }
            } catch (e: Exception) {}
        } else {
            locationSuggestions = emptyList()
            showLocationDropdown = false
        }
    }

    LaunchedEffect(Unit) {
        FaceEmbeddingEngine.init(context)
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                      permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            GeofenceLocationManager.requestFreshLocation(context) { loc ->
                if (loc != null) {
                    userLatitude = loc.latitude
                    userLongitude = loc.longitude
                }
            }
        }
    }

    val distanceMeters = remember(userLatitude, userLongitude, settings) {
        if (userLatitude == 0.0 && userLongitude == 0.0) {
            if (!settings.isGeofenceEnforced) 0.0 else -1.0
        } else {
            GeofenceLocationManager.calculateDistanceMeters(
                userLatitude, userLongitude,
                settings.latitude, settings.longitude
            )
        }
    }
    val isDistanceEligible = remember(distanceMeters, settings) {
        GeofenceLocationManager.isTokenDistancePermitted(
            distanceMeters = distanceMeters,
            isGeofenceEnforced = settings.isGeofenceEnforced,
            allowedRadiusMeters = settings.allowedRadiusMeters,
            isOutstationAdvanceAllowed = settings.isOutstationAdvanceAllowed,
            outstationMinDistanceKm = settings.outstationMinDistanceKm
        )
    }
    val isInsideGeofence = isDistanceEligible

    // Process photo captured from real camera or gallery
    fun processCapturedFace(bitmap: Bitmap) {
        if (settings.isTokenServiceEnabled && settings.scheduledTokenOpenTimestamp > System.currentTimeMillis()) {
            val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            val timeStr = sdf.format(Date(settings.scheduledTokenOpenTimestamp))
            errorMessage = if (isHindi)
                "टोकन पंजीकरण अभी बंद है। खुलने का समय: $timeStr"
            else
                "Token generation not yet open. Scheduled to open at: $timeStr"
            return
        }

        scope.launch {
            try {
                scanState = FaceScanState.PROCESSING
                errorMessage = null
                val startTime = System.currentTimeMillis()

                // Guaranteed conversion to software ARGB_8888 bitmap to prevent Config.HARDWARE getPixels() crash
                val safeBitmap = DevoteePhotoHelper.toSoftwareBitmap(bitmap)
                capturedBitmap = safeBitmap

                // AI Facial Liveness & Anti-Spoofing Verification
                val liveness = withContext(Dispatchers.Default) {
                    FaceEmbeddingEngine.verifyLiveness(safeBitmap)
                }
                if (!liveness.isLiveHuman) {
                    errorMessage = if (isHindi)
                        "⚠️ जीवंतता सत्यापन विफल: ${liveness.failureReason ?: "कृपया कैमरे के सामने वास्तविक जीवित व्यक्ति ही आएं।"}"
                    else
                        "⚠️ Liveness verification failed: ${liveness.failureReason ?: "Live person required"}"
                    scanState = FaceScanState.SCANNING
                    return@launch
                }

                val (vector, match) = withContext(Dispatchers.Default) {
                    // 1. Extract 128-d invariant feature vector from captured bitmap safely
                    val v = FaceEmbeddingEngine.extractVectorFromBitmap(safeBitmap)
                    // 2. Query database with strict >= 95% SLA (Zero Cross-Match Policy)
                    val m = try {
                        repository.matchFaceVector(v, threshold = FaceEmbeddingEngine.MINIMUM_CONFIDENCE_THRESHOLD)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                    Pair(v, m)
                }

                processingDurationMs = System.currentTimeMillis() - startTime
                candidateVector = vector

                // Gating: Only returning devotees with saved profile in DB see confirmation
                if (match != null && match.confidence >= FaceEmbeddingEngine.MINIMUM_CONFIDENCE_THRESHOLD) {
                    matchResult = match
                    scanState = FaceScanState.CONFIRMATION_SCREEN
                } else {
                    // New devotee / no matching profile found in database
                    matchResult = null
                    scanState = FaceScanState.MANUAL_ENTRY
                }
            } catch (t: Throwable) {
                t.printStackTrace()
                errorMessage = if (isHindi)
                    "फोटो प्रोसेस करने में समस्या आई। कृपया नीचे विवरण भरकर आगे बढ़ें।"
                else
                    "Error processing photo. Please enter details below."
                scanState = FaceScanState.MANUAL_ENTRY
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = TakeFrontPicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            var safeBmp = DevoteePhotoHelper.toSoftwareBitmap(bitmap)
            // Front camera on Android phones is usually landscape sensor (width > height). Auto-rotate to portrait:
            if (safeBmp.width > safeBmp.height) {
                safeBmp = DevoteePhotoHelper.rotateBitmap(safeBmp, 270f)
            }
            val oldBmp = capturedBitmap
            if (oldBmp != null && oldBmp != safeBmp && !oldBmp.isRecycled) {
                try { oldBmp.recycle() } catch (ignored: Exception) {}
            }
            capturedBitmap = safeBmp
            capturedPhotoUri = DevoteePhotoHelper.saveDevoteePhoto(context, safeBmp, "face_token")
            processCapturedFace(safeBmp)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                cameraLauncher.launch(null)
            } catch (e: Exception) {
                e.printStackTrace()
                errorMessage = if (isHindi) "कैमरा खोलने में त्रुटि: ${e.message}" else "Camera error: ${e.message}"
            }
        } else {
            errorMessage = if (isHindi)
                "कैमरा अनुमति अस्वीकृत: कृपया सेटिंग्स से अनुमति दें या नीचे 'गैलरी से फोटो चुनें' बटन दबाएं।"
            else
                "Camera permission denied. Please allow camera in settings or choose from gallery."
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val loaded = DevoteePhotoHelper.loadBitmap(context, uri.toString())
                if (loaded != null) {
                    val safeBmp = DevoteePhotoHelper.toSoftwareBitmap(loaded)
                    val oldBmp = capturedBitmap
                    if (oldBmp != null && oldBmp != safeBmp && !oldBmp.isRecycled) {
                        try { oldBmp.recycle() } catch (ignored: Exception) {}
                    }
                    capturedBitmap = safeBmp
                    capturedPhotoUri = DevoteePhotoHelper.saveDevoteePhoto(context, safeBmp, "face_gallery")
                    processCapturedFace(safeBmp)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun launchCameraSafely() {
        val permissionCheck = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CAMERA
        )
        if (permissionCheck == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            try {
                cameraLauncher.launch(null)
            } catch (e: Exception) {
                e.printStackTrace()
                errorMessage = if (isHindi) "कैमरा खोलने में समस्या: ${e.message}" else "Camera error: ${e.message}"
            }
        } else {
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    // Initialize background services instantly
    LaunchedEffect(Unit) {
        try {
            val id = DeviceFingerprintManager.getDeviceId(context)
            deviceId = id
            try {
                repository.syncLiveConfigFromGitHub()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            settings = repository.getSettings()
            // Check & request location permissions
            val fineGranted = androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            val coarseGranted = androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            if (!fineGranted && !coarseGranted) {
                locationPermissionLauncher.launch(
                    arrayOf(
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }

            GeofenceLocationManager.requestFreshLocation(context) { loc ->
                if (loc != null) {
                    userLatitude = loc.latitude
                    userLongitude = loc.longitude
                }
            }
            try { repository.syncDevoteesFromCloud() } catch (e: Exception) {}
            try { repository.syncCentralFaceProfiles() } catch (e: Exception) {}
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Auto-save Token Card to Photo Gallery whenever token is issued
    LaunchedEffect(generatedToken) {
        if (generatedToken != null && savedImageUri == null) {
            savedImageUri = TokenCardExporter.saveTokenToGallery(context, generatedToken!!, settings)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isHindi) "स्मार्ट चेहरा टोकन प्रणाली" else "Smart Face Token System",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Text(
                            text = if (isHindi) "सुपरफास्ट (<1s) • चेहरा पहचान टोकन" else "Superfast (<1s) • Face Token Verification",
                            fontSize = 11.sp,
                            color = GoldLight
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text(text = "⬅", color = Color.White, fontSize = 20.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaroonAccent)
            )
        },
        containerColor = SacredBackgroundLight
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Check if device already registered today
            if (existingToken != null && scanState != FaceScanState.TOKEN_GENERATED) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("⚠️", fontSize = 32.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isHindi) "आज का टोकन पहले से जारी है!" else "Token Already Issued Today!",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaroonAccent
                        )
                        Text(
                            text = if (isHindi)
                                "आपके फोन पर आज टोकन #${existingToken?.tokenNumber} (${existingToken?.patientName}) बन चुका है।"
                            else
                                "Your device has already claimed Token #${existingToken?.tokenNumber} (${existingToken?.patientName}) today.",
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            color = TextSecondaryDark,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                        Button(
                            onClick = {
                                generatedToken = existingToken
                                scanState = FaceScanState.TOKEN_GENERATED
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary)
                        ) {
                            Text(if (isHindi) "टोकन पास देखें" else "View Token Pass")
                        }
                    }
                }
                return@Scaffold
            }



            // Custom live notice if configured
            if (settings.sundayTokenCustomNotice.isNotBlank()) {
                Surface(
                    color = Color(0xFFFFF3E0),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFFFB74D)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "📢", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = settings.sundayTokenCustomNotice,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE65100)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Sunday Token Rule Banner
            Surface(
                color = Color(0xFFFFF8E1),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "🔒", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = settings.sundayTokenBannerTitle.ifBlank {
                                if (isHindi) "हार्डवेयर फिंगरप्रिंट नियम: 1 फोन = 1 टोकन" else "Hardware Rule: 1 Device = 1 Sunday Token"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = SaffronDark
                        )
                        Text(
                            text = settings.sundayTokenBannerText.ifBlank {
                                if (isHindi)
                                    "एक मोबाइल डिवाइस से प्रत्येक रविवार को केवल 1 टोकन लिया जा सकता है।"
                                else
                                    "Each physical handset is strictly restricted to 1 token per Sunday."
                            },
                            fontSize = 11.sp,
                            color = TextSecondaryDark
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // MAIN STATE MACHINE UI
            when (scanState) {
                FaceScanState.SCANNING -> {
                    val scheduleState = remember(settings) {
                        SundayTokenScheduleHelper.evaluateSchedule(settings)
                    }

                    if (scheduleState !is SundayScheduleState.Open) {
                        val visual = when (scheduleState) {
                            is SundayScheduleState.NonSunday -> ScheduleBannerVisual(
                                bannerBg = Color(0xFFFFFBEA),
                                borderCol = Color(0xFFD84315),
                                iconText = "📅",
                                titleText = if (isHindi) "रविवार टोकन वितरण सूचना" else "Sunday Token Notice",
                                descText = if (isHindi) scheduleState.messageHindi else scheduleState.messageEnglish
                            )
                            is SundayScheduleState.SundayBeforeStart -> ScheduleBannerVisual(
                                bannerBg = Color(0xFFFFFBEA),
                                borderCol = Color(0xFFE65100),
                                iconText = "⏳",
                                titleText = if (isHindi) "टोकन आज सुबह 8:30 बजे से मिलेंगे" else "Opens at 8:30 AM Today",
                                descText = if (isHindi) scheduleState.messageHindi else scheduleState.messageEnglish
                            )
                            is SundayScheduleState.SundayClosedEvening -> ScheduleBannerVisual(
                                bannerBg = Color(0xFFFFF5F5),
                                borderCol = Color(0xFFB71C1C),
                                iconText = "🔴",
                                titleText = if (isHindi) "आज के टोकन पूरे हो गए हैं" else "Today's Tokens Complete",
                                descText = if (isHindi) scheduleState.messageHindi else scheduleState.messageEnglish
                            )
                            is SundayScheduleState.ServiceDisabled -> ScheduleBannerVisual(
                                bannerBg = Color(0xFFFFF5F5),
                                borderCol = Color(0xFFB71C1C),
                                iconText = "🔒",
                                titleText = if (isHindi) "टोकन सेवा स्थगित" else "Token Service Paused",
                                descText = if (isHindi) scheduleState.messageHindi else scheduleState.messageEnglish
                            )
                            is SundayScheduleState.CustomScheduled -> ScheduleBannerVisual(
                                bannerBg = Color(0xFFFFFBEA),
                                borderCol = Color(0xFFD84315),
                                iconText = "⏳",
                                titleText = if (isHindi) "टोकन पंजीकरण पूर्व-निर्धारित है" else "Token Registration Scheduled",
                                descText = if (isHindi) scheduleState.messageHindi else scheduleState.messageEnglish
                            )
                            else -> ScheduleBannerVisual(Color.White, Color.Gray, "ℹ️", "", "")
                        }
                        val (bannerBg, borderCol, iconText, titleText, descText) = visual

                        Card(
                            colors = CardDefaults.cardColors(containerColor = bannerBg),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(2.dp, borderCol),
                            elevation = CardDefaults.cardElevation(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(iconText, fontSize = 28.sp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = titleText,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp,
                                        color = if (scheduleState is SundayScheduleState.SundayClosedEvening || scheduleState is SundayScheduleState.ServiceDisabled)
                                            Color(0xFFB71C1C)
                                        else
                                            Color(0xFF8B0000)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = descText,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF111111),
                                    lineHeight = 22.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // Viewfinder & Oval Reticle
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (isHindi) "चेहरे से 1-सेकंड में टोकन प्राप्त करें" else "1-Second Face Token Generation",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaroonAccent
                            )
                            Text(
                                text = if (isHindi)
                                    "चेहरे की गहरी अस्थि संरचना (Bony Invariants) से पहचान होती है। दाढ़ी, मूंछ, चश्मा या टोपी से कोई रुकावट नहीं आती।"
                                else
                                    "Identifies via deep cranial invariant features. Fully robust to beard, mustache, glasses or caps.",
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                color = TextSecondaryDark,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Camera Oval Reticle with Live/Captured View
                            Box(
                                modifier = Modifier
                                    .size(width = 220.dp, height = 260.dp)
                                    .clip(RoundedCornerShape(110.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color(0xFF2C3E50), Color(0xFF1A1A24))
                                        )
                                    )
                                    .border(
                                        width = 3.dp,
                                        brush = Brush.sweepGradient(
                                            listOf(SaffronPrimary, AmberGold, SaffronPrimary)
                                        ),
                                        shape = RoundedCornerShape(110.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (capturedBitmap != null) {
                                    Image(
                                        bitmap = capturedBitmap!!.asImageBitmap(),
                                        contentDescription = "Captured Devotee",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("📸", fontSize = 64.sp)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = if (isHindi) "चेहरा फ्रेम में रखें" else "Keep Face In Frame",
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (isHindi) "प्रकाश में सीधे देखें" else "Look Straight into Camera",
                                            color = GoldLight,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // AI Liveness & 3D Anti-Spoofing Badge
                            Surface(
                                color = Color(0xFFE8F5E9),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFF81C784)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("👁️", fontSize = 16.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (isHindi) "AI जीवंतता व 3D एंटी-स्पूफिंग सक्रिय" else "AI Liveness & 3D Anti-Spoofing Active",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1B5E20)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = if (isHindi)
                                            "🟡 सीधे कैमरे में देखें ➔ 🔵 3D बायोमेट्रिक ब्लिंक सत्यापन ➔ 🟢 प्रामाणिक जीवित भक्त। स्क्रीन व फोटो से टोकन लेना स्वतः ब्लॉक होगा।"
                                        else
                                            "Look straight ➔ 3D Biometric blink verified ➔ Genuine live devotee. Photo/Screen replay strictly blocked.",
                                        fontSize = 10.5.sp,
                                        color = Color(0xFF2E7D32),
                                        lineHeight = 15.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Camera Photo Capture Button (Always clickable!)
                            Button(
                                onClick = {
                                    val currentSchedule = SundayTokenScheduleHelper.evaluateSchedule(settings)
                                    when (currentSchedule) {
                                        is SundayScheduleState.NonSunday -> {
                                            scheduleAlertTitle = if (isHindi) "📅 टोकन केवल रविवार को मिलते हैं" else "📅 Tokens Only On Sunday"
                                            scheduleAlertMessage = if (isHindi) currentSchedule.messageHindi else currentSchedule.messageEnglish
                                            showScheduleAlertDialog = true
                                            errorMessage = scheduleAlertMessage
                                            return@Button
                                        }
                                        is SundayScheduleState.SundayBeforeStart -> {
                                            scheduleAlertTitle = if (isHindi) "⏳ टोकन सुबह 8:30 बजे से मिलेंगे" else "⏳ Opens at 8:30 AM"
                                            scheduleAlertMessage = if (isHindi) currentSchedule.messageHindi else currentSchedule.messageEnglish
                                            showScheduleAlertDialog = true
                                            errorMessage = scheduleAlertMessage
                                            return@Button
                                        }
                                        is SundayScheduleState.SundayClosedEvening -> {
                                            scheduleAlertTitle = if (isHindi) "🔴 आज के टोकन पूरे हो गए हैं" else "🔴 Today's Tokens Closed"
                                            scheduleAlertMessage = if (isHindi) currentSchedule.messageHindi else currentSchedule.messageEnglish
                                            showScheduleAlertDialog = true
                                            errorMessage = scheduleAlertMessage
                                            return@Button
                                        }
                                        is SundayScheduleState.ServiceDisabled -> {
                                            scheduleAlertTitle = if (isHindi) "🔒 टोकन सेवा स्थगित" else "🔒 Token Service Paused"
                                            scheduleAlertMessage = if (isHindi) currentSchedule.messageHindi else currentSchedule.messageEnglish
                                            showScheduleAlertDialog = true
                                            errorMessage = scheduleAlertMessage
                                            return@Button
                                        }
                                        is SundayScheduleState.CustomScheduled -> {
                                            scheduleAlertTitle = if (isHindi) "⏳ टोकन पूर्व-निर्धारित है" else "⏳ Scheduled"
                                            scheduleAlertMessage = if (isHindi) currentSchedule.messageHindi else currentSchedule.messageEnglish
                                            showScheduleAlertDialog = true
                                            errorMessage = scheduleAlertMessage
                                            return@Button
                                        }
                                        SundayScheduleState.Open -> { /* Valid! */ }
                                    }

                                    if (settings.isGeofenceEnforced && !isDistanceEligible) {
                                        val distKm = if (distanceMeters < 999990.0) String.format(Locale.US, "%.1f किमी", distanceMeters / 1000.0) else "अज्ञात"
                                        val outKm = settings.outstationMinDistanceKm.toInt()
                                        val radM = if (settings.allowedRadiusMeters >= 1000.0) "${String.format(Locale.US, "%.1f", settings.allowedRadiusMeters / 1000.0)} किमी" else "${settings.allowedRadiusMeters.toInt()} मीटर"
                                        locationAlertTitle = if (isHindi) "📍 आश्रम दूरी नियम (स्थानीय भक्त)" else "📍 Ashram Distance Policy"
                                        locationAlertMessage = if (isHindi) {
                                            if (settings.isOutstationAdvanceAllowed) {
                                                "⚠️ आप अभी आश्रम से $distKm दूर हैं!\n\nनियम: जो भक्त $outKm किमी से अधिक दूरी पर हैं, वे घर से अग्रिम टोकन ले सकते हैं। परंतु $outKm किमी के दायरे वाले स्थानीय भक्तों को टोकन केवल आश्रम परिसर ($radM के भीतर) में आकर ही मिलेगा।\n\nकृपया आश्रम पहुँचकर ही सेल्फी व टोकन प्रक्रिया करें।"
                                            } else {
                                                "⚠️ आप अभी आश्रम से $distKm दूर हैं!\n\nनियम: टोकन केवल आश्रम परिसर ($radM के भीतर) में उपस्थित होने पर ही मिलेगा।"
                                            }
                                        } else {
                                            "⚠️ You are $distKm away from Ashram! Must be within $radM of Ashram premises."
                                        }
                                        showLocationAlertDialog = true
                                        errorMessage = if (isHindi) "⚠️ $outKm किमी दायरे वाले स्थानीय भक्त आश्रम परिसर ($radM) में आकर ही टोकन प्राप्त कर सकते हैं।" else "Must be at Ashram (within $radM)."
                                        return@Button
                                    }

                                    launchCameraSafely()
                                },
                                enabled = !isSubmitting,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text(
                                    text = if (isHindi) "🤳 सेल्फी फोटो लें (फ्रंट कैमरा) ➔" else "🤳 Take Selfie (Front Camera) ➔",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Gallery Option Fallback
                            OutlinedButton(
                                onClick = {
                                    val currentSchedule = SundayTokenScheduleHelper.evaluateSchedule(settings)
                                    when (currentSchedule) {
                                        is SundayScheduleState.NonSunday -> {
                                            scheduleAlertTitle = if (isHindi) "📅 टोकन केवल रविवार को मिलते हैं" else "📅 Tokens Only On Sunday"
                                            scheduleAlertMessage = if (isHindi) currentSchedule.messageHindi else currentSchedule.messageEnglish
                                            showScheduleAlertDialog = true
                                            errorMessage = scheduleAlertMessage
                                            return@OutlinedButton
                                        }
                                        is SundayScheduleState.SundayBeforeStart -> {
                                            scheduleAlertTitle = if (isHindi) "⏳ टोकन सुबह 8:30 बजे से मिलेंगे" else "⏳ Opens at 8:30 AM"
                                            scheduleAlertMessage = if (isHindi) currentSchedule.messageHindi else currentSchedule.messageEnglish
                                            showScheduleAlertDialog = true
                                            errorMessage = scheduleAlertMessage
                                            return@OutlinedButton
                                        }
                                        is SundayScheduleState.SundayClosedEvening -> {
                                            scheduleAlertTitle = if (isHindi) "🔴 आज के टोकन पूरे हो गए हैं" else "🔴 Today's Tokens Closed"
                                            scheduleAlertMessage = if (isHindi) currentSchedule.messageHindi else currentSchedule.messageEnglish
                                            showScheduleAlertDialog = true
                                            errorMessage = scheduleAlertMessage
                                            return@OutlinedButton
                                        }
                                        is SundayScheduleState.ServiceDisabled -> {
                                            scheduleAlertTitle = if (isHindi) "🔒 टोकन सेवा स्थगित" else "🔒 Token Service Paused"
                                            scheduleAlertMessage = if (isHindi) currentSchedule.messageHindi else currentSchedule.messageEnglish
                                            showScheduleAlertDialog = true
                                            errorMessage = scheduleAlertMessage
                                            return@OutlinedButton
                                        }
                                        is SundayScheduleState.CustomScheduled -> {
                                            scheduleAlertTitle = if (isHindi) "⏳ टोकन पूर्व-निर्धारित है" else "⏳ Scheduled"
                                            scheduleAlertMessage = if (isHindi) currentSchedule.messageHindi else currentSchedule.messageEnglish
                                            showScheduleAlertDialog = true
                                            errorMessage = scheduleAlertMessage
                                            return@OutlinedButton
                                        }
                                        SundayScheduleState.Open -> { /* Valid! */ }
                                    }

                                    if (settings.isGeofenceEnforced && !isDistanceEligible) {
                                        val distKm = if (distanceMeters < 999990.0) String.format(Locale.US, "%.1f किमी", distanceMeters / 1000.0) else "अज्ञात"
                                        val outKm = settings.outstationMinDistanceKm.toInt()
                                        val radM = if (settings.allowedRadiusMeters >= 1000.0) "${String.format(Locale.US, "%.1f", settings.allowedRadiusMeters / 1000.0)} किमी" else "${settings.allowedRadiusMeters.toInt()} मीटर"
                                        locationAlertTitle = if (isHindi) "📍 आश्रम दूरी नियम (स्थानीय भक्त)" else "📍 Ashram Distance Policy"
                                        locationAlertMessage = if (isHindi) {
                                            if (settings.isOutstationAdvanceAllowed) {
                                                "⚠️ आप अभी आश्रम से $distKm दूर हैं!\n\nनियम: जो भक्त $outKm किमी से अधिक दूरी पर हैं, वे घर से अग्रिम टोकन ले सकते हैं। परंतु $outKm किमी के दायरे वाले स्थानीय भक्तों को टोकन केवल आश्रम परिसर ($radM के भीतर) में आकर ही मिलेगा।\n\nकृपया आश्रम पहुँचकर ही फोटो व टोकन प्रक्रिया करें।"
                                            } else {
                                                "⚠️ आप अभी आश्रम से $distKm दूर हैं!\n\nनियम: टोकन केवल आश्रम परिसर ($radM के भीतर) में उपस्थित होने पर ही मिलेगा।"
                                            }
                                        } else {
                                            "⚠️ You are $distKm away from Ashram! Must be within $radM of Ashram premises."
                                        }
                                        showLocationAlertDialog = true
                                        errorMessage = if (isHindi) "⚠️ $outKm किमी दायरे वाले स्थानीय भक्त आश्रम परिसर ($radM) में आकर ही टोकन प्राप्त कर सकते हैं।" else "Must be at Ashram (within $radM)."
                                        return@OutlinedButton
                                    }

                                    galleryLauncher.launch("image/*")
                                },
                                enabled = !isSubmitting,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                border = BorderStroke(1.5.dp, Color(0xFF1976D2)),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text(
                                    text = if (isHindi) "🖼️ गैलरी से फोटो चुनें (Gallery)" else "🖼️ Choose Photo from Gallery",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1976D2)
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = if (isHindi) "💡 नोट: फ्रंट (सेल्फी) कैमरा प्राथमिकता से खुलेगा। यदि आवश्यक हो तो कैमरा स्क्रीन पर फ्लिप (🔄) बटन दबाएं।"
                                else "💡 Note: Front camera opens by default. Use camera flip (🔄) button if needed.",
                                fontSize = 11.sp,
                                color = TextSecondaryDark,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Fallback to Manual Registration
                            OutlinedButton(
                                onClick = { scanState = FaceScanState.MANUAL_ENTRY },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text(
                                    text = if (isHindi) "सीधे फॉर्म भरें (Manual Registration)" else "Manual Registration Form",
                                    color = MaroonAccent
                                )
                            }
                        }
                    }
                }

                FaceScanState.PROCESSING -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = SaffronPrimary,
                                strokeWidth = 4.dp,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = if (isHindi) "चेहरा सत्यापन व मिलान जारी है..." else "Verifying Devotee Face Profile...",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaroonAccent
                            )
                            Text(
                                text = if (isHindi)
                                    "सुरक्षित पहचान सत्यापन किया जा रहा है..."
                                else
                                    "Searching enrolled profiles (<100ms)...",
                                fontSize = 12.sp,
                                color = TextSecondaryDark,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }
                }

                FaceScanState.CONFIRMATION_SCREEN -> {
                    val match = matchResult
                    if (match != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(20.dp),
                            elevation = CardDefaults.cardElevation(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Match Confirmation Header
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("🎯", fontSize = 24.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = if (isHindi) "✨ पूर्व पंजीकृत भक्त: स्वतः पहचान सफल!" else "Devotee Auto-Fetched Successfully!",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 17.sp,
                                            color = MaroonAccent
                                        )
                                        Text(
                                            text = if (isHindi) "समस्त विवरण स्वतः लोड हो गया है। बिना फॉर्म भरे 1-टैप में टोकन लें।" else "All details auto-fetched. Generate token in 1 tap.",
                                            fontSize = 12.sp,
                                            color = TextSecondaryDark
                                        )
                                    }
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), color = Color(0xFFEEEEEE))

                                // Detected Photo / Avatar Preview
                                if (capturedBitmap != null) {
                                    Image(
                                        bitmap = capturedBitmap!!.asImageBitmap(),
                                        contentDescription = "Captured Photo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(96.dp)
                                            .clip(CircleShape)
                                            .border(3.dp, Color(0xFF2E7D32), CircleShape)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(90.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(SaffronPrimary, AmberGold)
                                                )
                                            )
                                            .border(3.dp, Color(0xFF2E7D32), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("👤", fontSize = 48.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // High-Confidence Match Badge
                                Surface(
                                    color = Color(0xFFE8F5E9),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, Color(0xFF81C784))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("✅", fontSize = 13.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "${String.format("%.1f", match.confidence * 100)}% Match (${match.matchStatusDescription})",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Color(0xFF1B5E20)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Devotee Matched Profile Details
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = if (isHindi) "नाम (Name):" else "Devotee Name:",
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp,
                                                color = TextSecondaryDark
                                            )
                                            Text(
                                                text = match.profile.patientName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = MaroonAccent
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = if (isHindi) "मोबाइल (Mobile):" else "Mobile Number:",
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp,
                                                color = TextSecondaryDark
                                            )
                                            Text(
                                                text = match.profile.phoneNumber,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = TextPrimaryDark
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = if (isHindi) "शहर / गाँव (City):" else "City / Origin:",
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp,
                                                color = TextSecondaryDark
                                            )
                                            Text(
                                                text = match.profile.city,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = TextPrimaryDark
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = if (isHindi) "पूर्व दर्शन (Visits):" else "Total Visits:",
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp,
                                                color = TextSecondaryDark
                                            )
                                            Text(
                                                text = "${match.profile.visitCount} बार (Visits)",
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 13.sp,
                                                color = SaffronPrimary
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = if (isHindi) "सत्यापन समय (Time):" else "Verification Time:",
                                                fontSize = 12.sp,
                                                color = TextSecondaryDark
                                            )
                                            Text(
                                                text = "$processingDurationMs ms (< 500ms)",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = Color(0xFF2E7D32)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Auto-Update Profile Notice
                                Surface(
                                    color = Color(0xFFE3F2FD),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("🔄", fontSize = 16.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (isHindi)
                                                "पुष्टि करने पर आपका चेहरा प्रोफाइल नए रूप (दाढ़ी/चश्मा) के साथ स्वतः अपडेट हो जाएगा।"
                                            else
                                                "Confirming will automatically enrich your stored face profile with this new capture.",
                                            fontSize = 11.sp,
                                            color = Color(0xFF0D47A1)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(18.dp))

                                Text(
                                    text = if (isHindi) "✨ समस्त विवरण स्वतः लोड हो गया है। टोकन जनरेट करें:" else "Details Auto-Fetched! Generate Token:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaroonAccent,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // 2 ACTION BUTTONS:
                                // BUTTON 1: [ Confirm / Sahi Hai ] -> Generates Token & Auto-Updates Profile
                                Button(
                                    enabled = !isSubmitting,
                                    onClick = {
                                        if (settings.isGeofenceEnforced && !isDistanceEligible) {
                                            val distKm = if (distanceMeters < 999990.0) String.format(Locale.US, "%.1f किमी", distanceMeters / 1000.0) else "अज्ञात"
                                            val outKm = settings.outstationMinDistanceKm.toInt()
                                            val radM = if (settings.allowedRadiusMeters >= 1000.0) "${String.format(Locale.US, "%.1f", settings.allowedRadiusMeters / 1000.0)} किमी" else "${settings.allowedRadiusMeters.toInt()} मीटर"
                                            locationAlertTitle = if (isHindi) "📍 आश्रम दूरी नियम (स्थानीय भक्त)" else "📍 Ashram Distance Policy"
                                            locationAlertMessage = if (isHindi) {
                                                if (settings.isOutstationAdvanceAllowed) {
                                                    "⚠️ आप अभी आश्रम से $distKm दूर हैं!\n\nनियम: जो भक्त $outKm किमी से अधिक दूरी पर हैं, वे घर से अग्रिम टोकन ले सकते हैं। परंतु $outKm किमी के दायरे वाले स्थानीय भक्तों को टोकन केवल आश्रम परिसर ($radM के भीतर) में आकर ही मिलेगा।"
                                                } else {
                                                    "⚠️ आप अभी आश्रम से $distKm दूर हैं!\n\nनियम: टोकन केवल आश्रम परिसर ($radM के भीतर) में उपस्थित होने पर ही मिलेगा।"
                                                }
                                            } else {
                                                "⚠️ You are $distKm away from Ashram! Must be within $radM of Ashram premises."
                                            }
                                            showLocationAlertDialog = true
                                            errorMessage = if (isHindi) "⚠️ $outKm किमी दायरे वाले स्थानीय भक्त आश्रम परिसर ($radM) में आकर ही टोकन प्राप्त कर सकते हैं।" else "Must be at Ashram (within $radM)."
                                            return@Button
                                        }

                                        isSubmitting = true
                                        scope.launch {
                                            try {
                                                val loc = GeofenceLocationManager.getLastKnownLocation(context)
                                                val isMock = GeofenceLocationManager.isMockLocation(loc, context)
                                                if (isMock) {
                                                    errorMessage = if (isHindi)
                                                        "⚠️ फ़ेक जीपीएस चेतावनी: आपके डिवाइस में नकली लोकेशन / Fake GPS स्पूफिंग का उपयोग पकड़ा गया है। श्री बालाजी कृपा धाम के नियमों के अनुसार केवल वास्तविक जीपीएस से ही टोकन मान्य है। कृपया फ़ेक ऐप बंद करके पुनः प्रयास करें।"
                                                    else
                                                        "⚠️ Fake GPS Alert: Mock location or spoofing detected. Please disable Fake GPS and use genuine location."
                                                    isSubmitting = false
                                                    return@launch
                                                }
                                                val accuracy = if (loc != null && loc.hasAccuracy()) loc.accuracy else 10.0f
                                                val finalLat = if (userLatitude != 0.0) userLatitude else (loc?.latitude ?: 0.0)
                                                val finalLon = if (userLongitude != 0.0) userLongitude else (loc?.longitude ?: 0.0)

                                                if (settings.isGeofenceEnforced && (finalLat == 0.0 || finalLon == 0.0)) {
                                                    errorMessage = if (isHindi) "⚠️ वैध जीपीएस लोकेशन नहीं मिली। कृपया GPS चालू करें और पुनः प्रयास करें।" else "Valid GPS location required."
                                                    isSubmitting = false
                                                    return@launch
                                                }

                                                val (token, updated) = repository.confirmFaceAndGenerateToken(
                                                    matchedProfile = match.profile,
                                                    deviceId = deviceId,
                                                    latitude = finalLat,
                                                    longitude = finalLon,
                                                    candidateVector = candidateVector,
                                                    photoUri = if (capturedPhotoUri.isNotBlank()) capturedPhotoUri else match.profile.photoUri,
                                                    isMockLocation = isMock,
                                                    locationAccuracy = accuracy
                                                )
                                                generatedToken = token
                                                isEmbeddingAutoUpdated = updated
                                                scanState = FaceScanState.TOKEN_GENERATED
                                                try {
                                                    val phoneToRegister = token?.phoneNumber ?: match.profile.phoneNumber
                                                    com.example.shribalajikripadham.notification.AshramFirebaseMessagingService.registerDevoteePhone(context, phoneToRegister)
                                                } catch (e: Exception) {}
                                            } catch (e: SecurityException) {
                                                errorMessage = e.message ?: "Security Exception: Spoofed Location or Duplicate Device Request Denied."
                                            } catch (e: Exception) {
                                                errorMessage = e.message ?: "Security Exception: Spoofed Location or Duplicate Device Request Denied."
                                            } finally {
                                                isSubmitting = false
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text(
                                        text = if (isHindi) "⚡ 1-टैप में टोकन जनरेट करें (स्वतः विवरण भरा)" else "⚡ 1-Tap Generate Token (Auto-Filled)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // BUTTON 2: [ Not Me / Galat Detail ] -> Clears screen & Opens Manual Registration
                                OutlinedButton(
                                    onClick = {
                                        matchResult = null
                                        scanState = FaceScanState.MANUAL_ENTRY
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828)),
                                    border = BorderStroke(1.5.dp, Color(0xFFC62828)),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text(
                                        text = if (isHindi) "❌ यह मैं नहीं हूँ (नया पंजीकरण)" else "❌ Not Me (Register New)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // =========================================================================
                // NO MATCH / LOW CONFIDENCE FALLBACK
                // =========================================================================
                FaceScanState.NO_MATCH_FALLBACK -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🛡️", fontSize = 44.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (isHindi) "चेहरा मिलान नहीं हुआ (No Match Found)" else "Identity Could Not Be Verified",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaroonAccent
                            )
                            Text(
                                text = if (isHindi)
                                    "सटीकता स्कोर 95% से कम है। 'शून्य गलत पहचान नीति' के अनुसार सिस्टम किसी गलत व्यक्ति का टोकन नहीं दे सकता।"
                                else
                                    "Confidence score is below 95%. Per our Zero False-Match Policy, tokens cannot be guessed.",
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                color = TextSecondaryDark,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = { scanState = FaceScanState.MANUAL_ENTRY },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = if (isHindi) "मैन्युअल टोकन फॉर्म भरें (Manual Form)" else "Fill Manual Registration",
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            TextButton(onClick = { scanState = FaceScanState.SCANNING }) {
                                Text(if (isHindi) "पुनः स्कैन करें (Retry Scan)" else "Retry Face Scan")
                            }
                        }
                    }
                }

                // =========================================================================
                // MANUAL REGISTRATION FALLBACK
                // =========================================================================
                FaceScanState.MANUAL_ENTRY -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            // Devotee captured photo preview
                            if (capturedBitmap != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Image(
                                            bitmap = capturedBitmap!!.asImageBitmap(),
                                            contentDescription = "Captured Devotee",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(88.dp)
                                                .clip(CircleShape)
                                                .border(2.5.dp, SaffronPrimary, CircleShape)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Surface(
                                            color = Color(0xFFE8F5E9),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text(
                                                text = if (isHindi) "📸 फोटो सुरक्षित • नया भक्त पंजीकरण" else "📸 Photo Captured • New Devotee",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1B5E20),
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Button(
                                            onClick = {
                                                capturedBitmap?.let { bmp ->
                                                    val rotated = DevoteePhotoHelper.rotateBitmap(bmp, 90f)
                                                    capturedBitmap = rotated
                                                    capturedPhotoUri = DevoteePhotoHelper.saveDevoteePhoto(context, rotated, "face_rotated")
                                                    processCapturedFace(rotated)
                                                    Toast.makeText(context, if (isHindi) "🔄 फोटो 90° घुमाई गई" else "Photo rotated 90°", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            shape = RoundedCornerShape(20.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaroonAccent),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = if (isHindi) "🔄 फोटो घुमाएं (90°)" else "🔄 Rotate 90°",
                                                fontSize = 11.sp,
                                                color = AmberGold,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            Text(
                                text = if (isHindi) "भक्त पंजीकरण एवं टोकन" else "Devotee Token Registration",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color(0xFF8B0000)
                            )
                            Text(
                                text = if (isHindi)
                                    "कृपया अपना नाम और मोबाइल नंबर भरें। अगली बार दर्शन हेतु आपका चेहरा पहचान कर 1-सेकंड में स्वतः टोकन जारी होगा।"
                                else
                                    "Enter details below. Your face profile will be saved for fast 1-second token next visit.",
                                fontSize = 12.sp,
                                color = Color(0xFF424242)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = manualName,
                                onValueChange = { manualName = it },
                                label = { Text(if (isHindi) "भक्त / मरीज का नाम *" else "Devotee Name *", fontWeight = FontWeight.SemiBold) },
                                textStyle = androidx.compose.ui.text.TextStyle(color = Color(0xFF111111), fontSize = 15.sp, fontWeight = FontWeight.Medium),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = sacredOutlinedTextFieldColors(
                                    containerColor = Color.White,
                                    focusedContainerColor = Color.White,
                                    textColor = Color(0xFF111111),
                                    focusedBorderColor = Color(0xFF8B0000),
                                    unfocusedBorderColor = Color(0xFF757575),
                                    labelColor = Color(0xFF333333)
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = manualPhone,
                                onValueChange = { manualPhone = it },
                                label = { Text(if (isHindi) "मोबाइल नंबर *" else "Mobile Number *", fontWeight = FontWeight.SemiBold) },
                                textStyle = androidx.compose.ui.text.TextStyle(color = Color(0xFF111111), fontSize = 15.sp, fontWeight = FontWeight.Medium),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = sacredOutlinedTextFieldColors(
                                    containerColor = Color.White,
                                    focusedContainerColor = Color.White,
                                    textColor = Color(0xFF111111),
                                    focusedBorderColor = Color(0xFF8B0000),
                                    unfocusedBorderColor = Color(0xFF757575),
                                    labelColor = Color(0xFF333333)
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = manualCity,
                                onValueChange = { manualCity = it },
                                label = { Text(if (isHindi) "आगमन स्थान / शहर *" else "Coming From (City / Village) *", fontWeight = FontWeight.SemiBold) },
                                placeholder = { Text(if (isHindi) "उदा. डूँगरा जाट, बुलन्दशहर, दिल्ली..." else "e.g. Dungra Jaat, Bulandshahr...", color = Color(0xFF757575)) },
                                textStyle = androidx.compose.ui.text.TextStyle(color = Color(0xFF111111), fontSize = 15.sp, fontWeight = FontWeight.Medium),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = sacredOutlinedTextFieldColors(
                                    containerColor = Color.White,
                                    focusedContainerColor = Color.White,
                                    textColor = Color(0xFF111111),
                                    focusedBorderColor = Color(0xFF8B0000),
                                    unfocusedBorderColor = Color(0xFF757575),
                                    labelColor = Color(0xFF333333)
                                )
                            )

                            if (showLocationDropdown && locationSuggestions.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA)),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, SaffronPrimary.copy(alpha = 0.5f)),
                                    elevation = CardDefaults.cardElevation(3.dp),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                ) {
                                    Column(modifier = Modifier.padding(6.dp)) {
                                        locationSuggestions.forEach { loc ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        val formattedLoc = if (loc.districtHindi.isNotBlank() && !loc.nameHindi.contains(loc.districtHindi)) {
                                                            "${loc.nameHindi} (${loc.districtHindi}, ${loc.stateHindi})"
                                                        } else if (loc.stateHindi.isNotBlank() && !loc.nameHindi.contains(loc.stateHindi)) {
                                                            "${loc.nameHindi} (${loc.stateHindi})"
                                                        } else {
                                                            loc.nameHindi
                                                        }
                                                        manualCity = formattedLoc
                                                        showLocationDropdown = false
                                                    }
                                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("📍", fontSize = 14.sp)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                val locDetails = listOfNotNull(
                                                    loc.districtHindi.takeIf { it.isNotBlank() },
                                                    loc.stateHindi.takeIf { it.isNotBlank() }
                                                ).joinToString(", ")
                                                Text(
                                                    text = if (locDetails.isNotBlank()) "${loc.nameHindi} ($locDetails) • ${loc.distanceKm} किमी" else "${loc.nameHindi} (${loc.stateHindi}) • ${loc.distanceKm} किमी",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color(0xFF111111)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Checkbox(
                                    checked = enrollFaceForFuture,
                                    onCheckedChange = { enrollFaceForFuture = it }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isHindi)
                                        "भविष्य हेतु मेरा चेहरा प्रोफाइल सेव करें (अगली बार स्वतः टोकन)"
                                    else
                                        "Save my face profile for next Sunday (Auto-Enroll)",
                                    fontSize = 12.sp,
                                    color = TextPrimaryDark
                                )
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            Button(
                                enabled = !isSubmitting,
                                onClick = {
                                    val currentSchedule = SundayTokenScheduleHelper.evaluateSchedule(settings)
                                    when (currentSchedule) {
                                        is SundayScheduleState.NonSunday -> {
                                            scheduleAlertTitle = if (isHindi) "📅 टोकन केवल रविवार को मिलते हैं" else "📅 Tokens Only On Sunday"
                                            scheduleAlertMessage = if (isHindi) currentSchedule.messageHindi else currentSchedule.messageEnglish
                                            showScheduleAlertDialog = true
                                            errorMessage = scheduleAlertMessage
                                            return@Button
                                        }
                                        is SundayScheduleState.SundayBeforeStart -> {
                                            scheduleAlertTitle = if (isHindi) "⏳ टोकन सुबह 8:30 बजे से मिलेंगे" else "⏳ Opens at 8:30 AM"
                                            scheduleAlertMessage = if (isHindi) currentSchedule.messageHindi else currentSchedule.messageEnglish
                                            showScheduleAlertDialog = true
                                            errorMessage = scheduleAlertMessage
                                            return@Button
                                        }
                                        is SundayScheduleState.SundayClosedEvening -> {
                                            scheduleAlertTitle = if (isHindi) "🔴 आज के टोकन पूरे हो गए हैं" else "🔴 Today's Tokens Closed"
                                            scheduleAlertMessage = if (isHindi) currentSchedule.messageHindi else currentSchedule.messageEnglish
                                            showScheduleAlertDialog = true
                                            errorMessage = scheduleAlertMessage
                                            return@Button
                                        }
                                        is SundayScheduleState.ServiceDisabled -> {
                                            scheduleAlertTitle = if (isHindi) "🔒 टोकन सेवा स्थगित" else "🔒 Token Service Paused"
                                            scheduleAlertMessage = if (isHindi) currentSchedule.messageHindi else currentSchedule.messageEnglish
                                            showScheduleAlertDialog = true
                                            errorMessage = scheduleAlertMessage
                                            return@Button
                                        }
                                        is SundayScheduleState.CustomScheduled -> {
                                            scheduleAlertTitle = if (isHindi) "⏳ टोकन पूर्व-निर्धारित है" else "⏳ Scheduled"
                                            scheduleAlertMessage = if (isHindi) currentSchedule.messageHindi else currentSchedule.messageEnglish
                                            showScheduleAlertDialog = true
                                            errorMessage = scheduleAlertMessage
                                            return@Button
                                        }
                                        SundayScheduleState.Open -> { /* Valid! */ }
                                    }

                                    if (settings.isGeofenceEnforced && !isDistanceEligible) {
                                        val distKm = if (distanceMeters < 999990.0) String.format(Locale.US, "%.1f किमी", distanceMeters / 1000.0) else "अज्ञात"
                                        val outKm = settings.outstationMinDistanceKm.toInt()
                                        val radM = if (settings.allowedRadiusMeters >= 1000.0) "${String.format(Locale.US, "%.1f", settings.allowedRadiusMeters / 1000.0)} किमी" else "${settings.allowedRadiusMeters.toInt()} मीटर"
                                        locationAlertTitle = if (isHindi) "📍 आश्रम दूरी नियम (स्थानीय भक्त)" else "📍 Ashram Distance Policy"
                                        locationAlertMessage = if (isHindi) {
                                            if (settings.isOutstationAdvanceAllowed) {
                                                "⚠️ आप अभी आश्रम से $distKm दूर हैं!\n\nनियम: जो भक्त $outKm किमी से अधिक दूरी पर हैं, वे घर से अग्रिम टोकन ले सकते हैं। परंतु $outKm किमी के दायरे वाले स्थानीय भक्तों को टोकन केवल आश्रम परिसर ($radM के भीतर) में आकर ही मिलेगा।"
                                            } else {
                                                "⚠️ आप अभी आश्रम से $distKm दूर हैं!\n\nनियम: टोकन केवल आश्रम परिसर ($radM के भीतर) में उपस्थित होने पर ही मिलेगा।"
                                            }
                                        } else {
                                            "⚠️ You are $distKm away from Ashram! Must be within $radM of Ashram premises."
                                        }
                                        showLocationAlertDialog = true
                                        errorMessage = if (isHindi) "⚠️ $outKm किमी दायरे वाले स्थानीय भक्त आश्रम परिसर ($radM) में आकर ही टोकन प्राप्त कर सकते हैं।" else "Must be at Ashram (within $radM)."
                                        return@Button
                                    }

                                    isSubmitting = true
                                    scope.launch {
                                        try {
                                            if (manualName.isBlank() || manualPhone.isBlank()) {
                                                errorMessage = "कृपया नाम व फोन नंबर भरें"
                                                return@launch
                                            }

                                            if (settings.isTokenServiceEnabled && settings.scheduledTokenOpenTimestamp > System.currentTimeMillis()) {
                                                val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                                                val scheduledTimeStr = sdf.format(Date(settings.scheduledTokenOpenTimestamp))
                                                errorMessage = if (isHindi)
                                                    "टोकन पंजीकरण अभी बंद है। खुलने का समय: $scheduledTimeStr"
                                                else
                                                    "Token generation is not yet open. Scheduled: $scheduledTimeStr"
                                                return@launch
                                            }

                                            val loc = GeofenceLocationManager.getLastKnownLocation(context)
                                            val isMock = GeofenceLocationManager.isMockLocation(loc, context)
                                            if (isMock) {
                                                errorMessage = if (isHindi)
                                                    "⚠️ फ़ेक जीपीएस चेतावनी: आपके डिवाइस में नकली लोकेशन / Fake GPS स्पूफिंग का उपयोग पकड़ा गया है। श्री बालाजी कृपा धाम के नियमों के अनुसार केवल वास्तविक जीपीएस से ही टोकन मान्य है। कृपया फ़ेक ऐप बंद करके पुनः प्रयास करें।"
                                                else
                                                    "⚠️ Fake GPS Alert: Mock location or spoofing detected. Please disable Fake GPS and use genuine location."
                                                isSubmitting = false
                                                return@launch
                                            }
                                            val accuracy = if (loc != null && loc.hasAccuracy()) loc.accuracy else 10.0f
                                            val finalLat = if (userLatitude != 0.0) userLatitude else (loc?.latitude ?: settings.latitude)
                                            val finalLon = if (userLongitude != 0.0) userLongitude else (loc?.longitude ?: settings.longitude)

                                            // Register Token with anti-fraud gating
                                            val token = repository.registerToken(
                                                patientName = manualName.trim(),
                                                phoneNumber = manualPhone.trim(),
                                                deviceId = deviceId,
                                                latitude = finalLat,
                                                longitude = finalLon,
                                                city = manualCity.trim().ifEmpty { "डूँगरा जाट (स्थानीय)" },
                                                registeredBy = "MANUAL_FALLBACK",
                                                photoUri = capturedPhotoUri,
                                                isMockLocation = isMock,
                                                locationAccuracy = accuracy
                                            )

                                            // Auto-enroll face if selected
                                            if (enrollFaceForFuture) {
                                                repository.enrollFaceProfile(
                                                    name = manualName.trim(),
                                                    phone = manualPhone.trim(),
                                                    faceVector = candidateVector,
                                                    city = manualCity.trim().ifEmpty { "डूँगरा जाट (स्थानीय)" }
                                                )
                                            }

                                            generatedToken = token
                                            isEmbeddingAutoUpdated = enrollFaceForFuture
                                            scanState = FaceScanState.TOKEN_GENERATED
                                            try {
                                                com.example.shribalajikripadham.notification.AshramFirebaseMessagingService.registerDevoteePhone(context, manualPhone.trim())
                                            } catch (e: Exception) {}
                                        } catch (e: SecurityException) {
                                            errorMessage = e.message ?: "Security Exception: Spoofed Location or Duplicate Device Request Denied."
                                        } catch (e: Exception) {
                                            errorMessage = e.message ?: "Security Exception: Spoofed Location or Duplicate Device Request Denied."
                                        } finally {
                                            isSubmitting = false
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = if (isHindi) "टोकन प्राप्त करें (Generate Token)" else "Submit & Generate Token",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            TextButton(
                                onClick = { scanState = FaceScanState.SCANNING },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (isHindi) "वापस फेस स्कैन पर जाएं" else "Back to Face Scanner")
                            }
                        }
                    }
                }

                // =========================================================================
                // SUCCESS STATE: OFFICIAL ROYAL TOKEN CARD (Auto-Saved to Gallery)
                // =========================================================================
                FaceScanState.TOKEN_GENERATED -> {
                    val token = generatedToken
                    if (token != null) {
                        PremiumRoyalTokenCard(
                            token = token,
                            settings = settings,
                            isHindi = isHindi,
                            savedImageUri = savedImageUri,
                            onBackToHome = onBack
                        )
                    }
                }
            }

            // Error snackbar/banner
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    color = Color(0xFFFFEBEE),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("⚠️", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = errorMessage ?: "",
                            color = Color(0xFFB71C1C),
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { errorMessage = null }) {
                            Text("OK", color = Color(0xFFB71C1C), fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Alert Dialog: Outside Ashram Location
        if (showLocationAlertDialog) {
            AlertDialog(
                onDismissRequest = { showLocationAlertDialog = false },
                containerColor = Color.White,
                icon = { Text("📍", fontSize = 36.sp) },
                title = {
                    Text(
                        text = locationAlertTitle,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF8B0000),
                        fontSize = 18.sp
                    )
                },
                text = {
                    Text(
                        text = locationAlertMessage,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF111111),
                        lineHeight = 22.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showLocationAlertDialog = false
                            GeofenceLocationManager.requestFreshLocation(context) { loc ->
                                if (loc != null) {
                                    userLatitude = loc.latitude
                                    userLongitude = loc.longitude
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (isHindi) "🔄 GPS रीफ्रेश करें" else "🔄 Refresh GPS", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLocationAlertDialog = false }) {
                        Text(if (isHindi) "समझ गया" else "Dismiss", color = Color(0xFF424242), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            )
        }

        // Alert Dialog: Schedule Timing / Non-Sunday Notice
        if (showScheduleAlertDialog) {
            AlertDialog(
                onDismissRequest = { showScheduleAlertDialog = false },
                containerColor = Color.White,
                icon = { Text("📅", fontSize = 36.sp) },
                title = {
                    Text(
                        text = scheduleAlertTitle,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF8B0000),
                        fontSize = 18.sp
                    )
                },
                text = {
                    Text(
                        text = scheduleAlertMessage,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF111111),
                        lineHeight = 22.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { showScheduleAlertDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (isHindi) "समझ गया / ठीक है" else "Got It", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                    }
                }
            )
        }
    }
}
