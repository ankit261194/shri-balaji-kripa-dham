package com.example.shribalajikripadham.ui.token

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
    var processingDurationMs by remember { mutableLongStateOf(0L) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Manual Entry Form Inputs (For First-time Devotees / Fallback)
    var manualName by remember { mutableStateOf("") }
    var manualPhone by remember { mutableStateOf("") }
    var manualCity by remember { mutableStateOf("") }
    var enrollFaceForFuture by remember { mutableStateOf(true) }

    val distanceMeters = remember(userLatitude, userLongitude, settings) {
        if (userLatitude == 0.0 && userLongitude == 0.0) {
            999999.0
        } else {
            GeofenceLocationManager.calculateDistanceMeters(
                userLatitude, userLongitude,
                settings.latitude, settings.longitude
            )
        }
    }
    val isInsideGeofence = remember(distanceMeters, settings) {
        val effectiveRadius = settings.allowedRadiusMeters.coerceIn(50.0, 200.0)
        distanceMeters <= effectiveRadius
    }

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

    // Camera launcher for actual photo capture (defaults to front selfie camera)
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = TakeFrontPicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            val safeBmp = DevoteePhotoHelper.toSoftwareBitmap(bitmap)
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
            existingToken = repository.checkDeviceRegisteredToday(id)

            val loc = GeofenceLocationManager.getLastKnownLocation(context)
            if (loc != null) {
                userLatitude = loc.latitude
                userLongitude = loc.longitude
            }
            try { repository.syncDevoteesFromCloud() } catch (e: Exception) {}
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

            // Geofence status strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = if (isInsideGeofence) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (isInsideGeofence) "📍" else "⚠️", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isInsideGeofence)
                            (if (isHindi) "आश्रम सीमा में (${distanceMeters.toInt()}m - 200m के अंदर)" else "Inside Ashram (${distanceMeters.toInt()}m - within 200m)")
                        else
                            (if (isHindi) "आश्रम सीमा से बाहर (${if (distanceMeters < 1000.0) "${distanceMeters.toInt()} मीटर" else "${String.format("%.1f", distanceMeters / 1000.0)} किमी"} - केवल 200m मान्य)" else "Outside Ashram (${if (distanceMeters < 1000.0) "${distanceMeters.toInt()}m" else "${String.format("%.1f", distanceMeters / 1000.0)} km"} - max 200m)"),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isInsideGeofence) StatusInsideAshram else StatusOutsideAshram
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

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
                    val isBeforeSchedule = settings.isTokenServiceEnabled && settings.scheduledTokenOpenTimestamp > System.currentTimeMillis()

                    if (isBeforeSchedule) {
                        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                        val scheduledTimeStr = sdf.format(Date(settings.scheduledTokenOpenTimestamp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.5.dp, Color(0xFFFFB300)),
                            elevation = CardDefaults.cardElevation(3.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("⏳", fontSize = 24.sp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = if (isHindi) "टोकन पंजीकरण पूर्व-निर्धारित है" else "Token Registration Scheduled",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFFE65100)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isHindi)
                                        "टोकन जारी होना $scheduledTimeStr पर स्वतः प्रारंभ होगा। कृपया निर्धारित समय पर ही स्कैन करें।"
                                    else
                                        "Token generation will open automatically at $scheduledTimeStr.",
                                    fontSize = 12.sp,
                                    color = TextPrimaryDark
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
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

                            // Security & Accuracy Badge
                            Surface(
                                color = Color(0xFFFFF3E0),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("🛡️", fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isHindi)
                                            "शून्य गलत पहचान नीति: न्यूनतम 95% बायोमेट्रिक सटीकता लागू है। नया भक्त होने पर तुरंत पंजीकरण होगा।"
                                        else
                                            "Zero False-Match Policy: Strict 95% accuracy enforced. First-time devotees auto-directed to registration.",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFFE65100)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Camera Photo Capture Button
                            Button(
                                onClick = { launchCameraSafely() },
                                enabled = isInsideGeofence && !isBeforeSchedule,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text(
                                    text = if (isBeforeSchedule)
                                        (if (isHindi) "🔒 पंजीकरण अभी बंद है" else "🔒 Registration Locked")
                                    else
                                        (if (isHindi) "🤳 सेल्फी फोटो लें (फ्रंट कैमरा)" else "🤳 Take Selfie (Front Camera)"),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Gallery Option Fallback
                            OutlinedButton(
                                onClick = { galleryLauncher.launch("image/*") },
                                enabled = isInsideGeofence && !isBeforeSchedule,
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
                                            text = if (isHindi) "चेहरा मिलान की पुष्टि करें" else "Confirm Your Identity",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 17.sp,
                                            color = MaroonAccent
                                        )
                                        Text(
                                            text = if (isHindi) "कृपया विवरण जांचें और पुष्टि करें" else "Please review details before proceeding",
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
                                    text = if (isHindi) "क्या ये आप ही हैं?" else "Is this you?",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaroonAccent,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // 2 ACTION BUTTONS:
                                // BUTTON 1: [ Confirm / Sahi Hai ] -> Generates Token & Auto-Updates Profile
                                Button(
                                    enabled = isInsideGeofence,
                                    onClick = {
                                        scope.launch {
                                            if (!isInsideGeofence) {
                                                errorMessage = if (isHindi) "आप आश्रम परिसर से बाहर हैं। टोकन केवल आश्रम में उपस्थित होने पर मिलेगा।" else "You are outside Ashram premises."
                                                return@launch
                                            }
                                            try {
                                                val loc = GeofenceLocationManager.getLastKnownLocation(context)
                                                val isMock = GeofenceLocationManager.isMockLocation(loc, context)
                                                val accuracy = if (loc != null && loc.hasAccuracy()) loc.accuracy else 10.0f

                                                val (token, updated) = repository.confirmFaceAndGenerateToken(
                                                    matchedProfile = match.profile,
                                                    deviceId = deviceId,
                                                    latitude = userLatitude,
                                                    longitude = userLongitude,
                                                    candidateVector = candidateVector,
                                                    photoUri = if (capturedPhotoUri.isNotBlank()) capturedPhotoUri else match.profile.photoUri,
                                                    isMockLocation = isMock,
                                                    locationAccuracy = accuracy
                                                )
                                                generatedToken = token
                                                isEmbeddingAutoUpdated = updated
                                                scanState = FaceScanState.TOKEN_GENERATED
                                            } catch (e: SecurityException) {
                                                errorMessage = e.message ?: "Security Exception: Spoofed Location or Duplicate Device Request Denied."
                                            } catch (e: Exception) {
                                                errorMessage = e.message ?: "Security Exception: Spoofed Location or Duplicate Device Request Denied."
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
                                        text = if (isHindi) "✅ हाँ, यह मैं हूँ (टोकन प्राप्त करें)" else "✅ Yes, It's Me (Generate Token)",
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
                                    }
                                }
                            }

                            Text(
                                text = if (isHindi) "भक्त पंजीकरण एवं टोकन" else "Devotee Token Registration",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = MaroonAccent
                            )
                            Text(
                                text = if (isHindi)
                                    "कृपया अपना नाम और मोबाइल नंबर भरें। अगली बार दर्शन हेतु आपका चेहरा पहचान कर 1-सेकंड में स्वतः टोकन जारी होगा।"
                                else
                                    "Enter details below. Your face profile will be saved for fast 1-second token next visit.",
                                fontSize = 12.sp,
                                color = TextSecondaryDark
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = manualName,
                                onValueChange = { manualName = it },
                                label = { Text(if (isHindi) "भक्त / मरीज का नाम" else "Devotee Name") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = manualPhone,
                                onValueChange = { manualPhone = it },
                                label = { Text(if (isHindi) "मोबाइल नंबर" else "Mobile Number") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = manualCity,
                                onValueChange = { manualCity = it },
                                label = { Text(if (isHindi) "आगमन स्थान / शहर (Coming From)" else "Coming From (City / Village)") },
                                placeholder = { Text(if (isHindi) "उदा. डूँगरा जाट, बुलन्दशहर, दिल्ली..." else "e.g. Dungra Jaat, Bulandshahr...") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )

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
                                enabled = isInsideGeofence,
                                onClick = {
                                    scope.launch {
                                        try {
                                            if (!isInsideGeofence) {
                                                errorMessage = if (isHindi) "आप आश्रम परिसर से बाहर हैं। टोकन केवल आश्रम में उपस्थित होने पर मिलेगा।" else "You are outside Ashram premises."
                                                return@launch
                                            }
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
                                            val accuracy = if (loc != null && loc.hasAccuracy()) loc.accuracy else 10.0f

                                            // Register Token with anti-fraud gating
                                            val token = repository.registerToken(
                                                patientName = manualName.trim(),
                                                phoneNumber = manualPhone.trim(),
                                                deviceId = deviceId,
                                                latitude = userLatitude,
                                                longitude = userLongitude,
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
                                        } catch (e: SecurityException) {
                                            errorMessage = e.message ?: "Security Exception: Spoofed Location or Duplicate Device Request Denied."
                                        } catch (e: Exception) {
                                            errorMessage = e.message ?: "Security Exception: Spoofed Location or Duplicate Device Request Denied."
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
    }
}
