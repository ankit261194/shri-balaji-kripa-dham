package com.example.shribalajikripadham.ui.token

import android.content.Context
import android.net.Uri
import android.widget.Toast
import java.io.File
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shribalajikripadham.ai.FaceEmbeddingEngine
import com.example.shribalajikripadham.data.model.DevoteeFaceProfile
import com.example.shribalajikripadham.data.model.DevoteeDirectoryEntry
import com.example.shribalajikripadham.data.model.AshramSettings
import com.example.shribalajikripadham.data.model.Token
import com.example.shribalajikripadham.data.repository.AshramRepository
import com.example.shribalajikripadham.hardware.DeviceFingerprintManager
import com.example.shribalajikripadham.hardware.GeofenceLocationManager
import com.example.shribalajikripadham.theme.*
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.example.shribalajikripadham.util.DevoteePhotoHelper
import com.example.shribalajikripadham.util.TakeFrontPicturePreview
import com.example.shribalajikripadham.util.DistanceCalculatorService
import com.example.shribalajikripadham.util.TokenCardExporter
import com.example.shribalajikripadham.util.SundayTokenScheduleHelper
import com.example.shribalajikripadham.util.SundayScheduleState
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

data class ScheduleBannerVisual(
    val bannerBg: Color,
    val borderCol: Color,
    val iconText: String,
    val titleText: String,
    val descText: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TokenRegistrationScreen(
    isHindi: Boolean,
    onBack: () -> Unit,
    onNavigateToFaceToken: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val repository = remember { AshramRepository(context) }
    val scope = rememberCoroutineScope()

    var settings by remember { mutableStateOf(AshramSettings()) }
    var existingToken by remember { mutableStateOf<Token?>(null) }
    var savedImageUri by remember { mutableStateOf<Uri?>(null) }
    var deviceId by remember { mutableStateOf("") }

    // Form inputs (Preserved across rotation and process death)
    var patientName by rememberSaveable { mutableStateOf("") }
    var phoneNumber by rememberSaveable { mutableStateOf("") }
    var city by rememberSaveable { mutableStateOf("") }
    var originAddress by rememberSaveable { mutableStateOf("") }
    var estimatedDistanceKm by rememberSaveable { mutableFloatStateOf(-1f) }
    var isCalculatingDistance by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    // Devotee Photo State (Optional Selfie)
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var capturedPhotoUri by remember { mutableStateOf("") }
    var nameSuggestions by remember { mutableStateOf<List<DevoteeFaceProfile>>(emptyList()) }
    var directorySuggestions by remember { mutableStateOf<List<DevoteeDirectoryEntry>>(emptyList()) }
    var isLocationAutoFetched by remember { mutableStateOf(false) }
    var isResolvingLocationName by remember { mutableStateOf(false) }
    var autoFillBanner by remember { mutableStateOf<String?>(null) }

    var hasLocationPermission by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    var hasCameraPermission by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
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
            capturedBitmap = safeBmp
            capturedPhotoUri = DevoteePhotoHelper.saveDevoteePhoto(context, safeBmp, "devotee_selfie")
            errorMessage = null
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
                    capturedPhotoUri = DevoteePhotoHelper.saveDevoteePhoto(context, safeBmp, "devotee_gallery")
                    errorMessage = null
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (isGranted) {
            try {
                cameraLauncher.launch(null)
            } catch (e: Exception) {
                e.printStackTrace()
                errorMessage = if (isHindi) "कैमरा खोलने में त्रुटि हुई।" else "Error opening camera."
            }
        } else {
            errorMessage = if (isHindi)
                "कैमरा उपलब्ध नहीं है। आप नीचे 'गैलरी से फोटो चुनें' द्वारा फोटो लगा सकते हैं।"
            else
                "Camera not available. You can choose photo from gallery."
            try {
                galleryLauncher.launch("image/*")
            } catch (_: Exception) {}
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
            // Do NOT re-prompt system permission! Smoothly open gallery for photo selection:
            errorMessage = if (isHindi)
                "कैमरा उपलब्ध नहीं है। कृपया नीचे 'गैलरी से फोटो चुनें' द्वारा फोटो लगाएं।"
            else
                "Camera not available. Please choose photo from gallery."
            try {
                galleryLauncher.launch("image/*")
            } catch (_: Exception) {}
        }
    }

    var todayActiveTokens by remember { mutableIntStateOf(0) }

    // Geofencing Live Location State (Strictly enforced real GPS)
    var userLatitude by remember { mutableDoubleStateOf(0.0) }
    var userLongitude by remember { mutableDoubleStateOf(0.0) }

    // Live road distance calculation to Shri Balaji Kripa Dham, Dungra Jaat
    LaunchedEffect(city, originAddress, userLatitude, userLongitude, settings) {
        if (userLatitude != 0.0 && userLongitude != 0.0) {
            val ashLat = if (settings.latitude != 0.0) settings.latitude else 28.3972915
            val ashLon = if (settings.longitude != 0.0) settings.longitude else 78.1460410
            val distMeters = GeofenceLocationManager.calculateDistanceMeters(
                userLatitude, userLongitude,
                ashLat, ashLon
            )
            val straightKm = (distMeters / 1000.0).toFloat()
            if (distMeters <= settings.allowedRadiusMeters) {
                estimatedDistanceKm = (kotlin.math.round(straightKm * 10) / 10)
            } else {
                estimatedDistanceKm = (kotlin.math.round(straightKm * 1.28f * 10) / 10)
            }
        } else {
            val query = if (originAddress.isNotBlank()) originAddress else city
            if (query.isNotBlank()) {
                isCalculatingDistance = true
                val res = DistanceCalculatorService.resolveDrivingDistance(
                    origin = query,
                    deviceLat = 28.3972915,
                    deviceLng = 78.1460410
                )
                estimatedDistanceKm = res.distanceKm
                isCalculatingDistance = false
            } else {
                estimatedDistanceKm = -1f
            }
        }
    }

    var isRefreshingLocation by remember { mutableStateOf(false) }
    var isFreshLocationMock by remember { mutableStateOf(false) }
    var freshLocationAccuracy by remember { mutableFloatStateOf(10.0f) }
    var showLocationAlertDialog by remember { mutableStateOf(false) }
    var locationAlertTitle by remember { mutableStateOf("") }
    var locationAlertMessage by remember { mutableStateOf("") }
    var showScheduleAlertDialog by remember { mutableStateOf(false) }
    var scheduleAlertTitle by remember { mutableStateOf("") }
    var scheduleAlertMessage by remember { mutableStateOf("") }


    val requiredPermissions = remember {
        val list = mutableListOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.CAMERA
        )
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            list.add(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        list.toTypedArray()
    }



    fun triggerFreshLocationFix() {
        val locManager = context.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager
        val isHardwareGpsOn = locManager?.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) == true ||
                              locManager?.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER) == true
        if (!isHardwareGpsOn) {
            return
        }
        isRefreshingLocation = true
        GeofenceLocationManager.requestFreshLocation(context) { loc ->
            isRefreshingLocation = false
            if (loc != null) {
                userLatitude = loc.latitude
                userLongitude = loc.longitude
                isFreshLocationMock = if (GeofenceLocationManager.isMockCheckGloballyEnabled) {
                    GeofenceLocationManager.isMockLocation(loc, context)
                } else false
                freshLocationAccuracy = if (loc.hasAccuracy()) loc.accuracy else 10.0f
            }
        }
    }

    val unifiedPermissionLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLoc = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseLoc = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        val cam = permissions[android.Manifest.permission.CAMERA] == true
        hasLocationPermission = fineLoc || coarseLoc
        hasCameraPermission = cam

        if (hasLocationPermission) {
            triggerFreshLocationFix()
        }
    }

    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                val fineGranted = androidx.core.content.ContextCompat.checkSelfPermission(
                    context, android.Manifest.permission.ACCESS_FINE_LOCATION
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                val coarseGranted = androidx.core.content.ContextCompat.checkSelfPermission(
                    context, android.Manifest.permission.ACCESS_COARSE_LOCATION
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                val camGranted = androidx.core.content.ContextCompat.checkSelfPermission(
                    context, android.Manifest.permission.CAMERA
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                hasLocationPermission = fineGranted || coarseGranted
                hasCameraPermission = camGranted
                if (hasLocationPermission) {
                    triggerFreshLocationFix()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val distanceMeters = remember(userLatitude, userLongitude, settings) {
        if (userLatitude == 0.0 && userLongitude == 0.0) {
            if (!settings.isGeofenceEnforced) 0.0 else -1.0
        } else {
            val ashLat = if (settings.latitude != 0.0) settings.latitude else 28.3972915
            val ashLon = if (settings.longitude != 0.0) settings.longitude else 78.1460410
            GeofenceLocationManager.calculateDistanceMeters(
                userLatitude, userLongitude,
                ashLat, ashLon
            )
        }
    }
    val isDistanceEligible = remember(distanceMeters, settings) {
        if (distanceMeters < 0.0) {
            false
        } else {
            GeofenceLocationManager.isTokenDistancePermitted(
                distanceMeters = distanceMeters,
                isGeofenceEnforced = settings.isGeofenceEnforced,
                allowedRadiusMeters = settings.allowedRadiusMeters,
                isOutstationAdvanceAllowed = settings.isOutstationAdvanceAllowed,
                outstationMinDistanceKm = settings.outstationMinDistanceKm
            )
        }
    }
    val isInsideGeofence = isDistanceEligible
    val isQuotaExceeded = remember(settings.maxDailyTokens, todayActiveTokens) {
        settings.maxDailyTokens > 0 && todayActiveTokens >= settings.maxDailyTokens
    }

    // Load initial data and sync cloud devotee registry
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
            val targetDate = if (settings.darbarDate.isNotBlank()) settings.darbarDate else com.example.shribalajikripadham.data.local.DatabaseHelper.getTodayDateString()
            val savedPhone = try {
                context.getSharedPreferences("sbkd_devotee_my_token_prefs", Context.MODE_PRIVATE).getString("my_phone_number", "") ?: ""
            } catch (e: Exception) { "" }

            var tok = repository.checkDeviceRegisteredToday(id, targetDate)
            if (tok == null) {
                try {
                    val serverTok = com.example.shribalajikripadham.data.network.HostingerCentralSyncManager.checkDeviceRegisteredOnServer(id, targetDate, savedPhone)
                    if (serverTok != null) {
                        tok = serverTok
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            if (tok == null) {
                tok = com.example.shribalajikripadham.hardware.PersistentTokenReceiptHelper.readPersistentReceipt(id, targetDate)
            }
            existingToken = tok
            todayActiveTokens = repository.getTodayActiveTokenCount()

            // Passively evaluate permissions already requested upfront at app launch
            val fineGranted = androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            val coarseGranted = androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            val camGranted = androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            hasLocationPermission = fineGranted || coarseGranted
            hasCameraPermission = camGranted
            if (hasLocationPermission) {
                triggerFreshLocationFix()
            }

            // Background cloud sync to pull all devotee profiles from any phone
            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try { repository.syncDevoteesFromCloud() } catch (e: Exception) {}
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Auto-search devotee when 10-digit phone number is entered or partial phone is typed
    LaunchedEffect(phoneNumber) {
        val clean = phoneNumber.trim().replace("+91", "").replace(" ", "").replace("-", "")
        if (clean.length == 10) {
            val devotee = repository.searchDevoteeByPhone(clean)
            if (devotee != null) {
                patientName = devotee.patientName
                if (devotee.city.isNotBlank()) {
                    city = devotee.city
                    originAddress = devotee.city
                }
                if (devotee.photoUri.isNotBlank()) {
                    capturedPhotoUri = devotee.photoUri
                }
                autoFillBanner = if (isHindi)
                    "✅ पूर्व पंजीकृत भक्त: ${devotee.patientName} (${devotee.city}) का समस्त विवरण स्वतः भर दिया गया है!"
                else
                    "✅ Devotee Record Found: ${devotee.patientName} (${devotee.city}) auto-filled!"
            }
            directorySuggestions = emptyList()
        } else if (clean.length in 3..9) {
            directorySuggestions = repository.searchDevoteeDirectory(clean, limit = 5)
            autoFillBanner = null
        } else {
            directorySuggestions = emptyList()
            autoFillBanner = null
        }
    }

    // Name suggestions when typing name
    LaunchedEffect(patientName) {
        val q = patientName.trim()
        if (q.length >= 2) {
            nameSuggestions = repository.searchDevoteesByName(q, limit = 5)
            val dirMatches = repository.searchDevoteeDirectory(q, limit = 5)
            if (dirMatches.isNotEmpty() && directorySuggestions.isEmpty()) {
                directorySuggestions = dirMatches
            }
        } else {
            nameSuggestions = emptyList()
            if (phoneNumber.length < 3) {
                directorySuggestions = emptyList()
            }
        }
    }

    // Auto-fetch devotee location & road distance when GPS coordinates are available (> 30 km)
    LaunchedEffect(userLatitude, userLongitude, settings) {
        if (userLatitude != 0.0 && userLongitude != 0.0) {
            val ashLat = if (settings.latitude != 0.0) settings.latitude else 28.3972915
            val ashLon = if (settings.longitude != 0.0) settings.longitude else 78.1460410
            val distMeters = GeofenceLocationManager.calculateDistanceMeters(
                userLatitude, userLongitude,
                ashLat, ashLon
            )
            val straightKm = (distMeters / 1000.0).toFloat()
            val roadKm = (kotlin.math.round(straightKm * 1.28f * 10) / 10)

            if (distMeters > settings.outstationMinDistanceKm * 1000.0) {
                // Outstation devotee (> 30 km): Auto-fetch & auto-fill their location from GPS
                estimatedDistanceKm = roadKm
                isResolvingLocationName = true
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    val resolved = GeofenceLocationManager.resolveVillageAndCity(context, userLatitude, userLongitude)
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        isResolvingLocationName = false
                        if (resolved.isNotBlank()) {
                            originAddress = resolved
                            city = resolved
                            isLocationAutoFetched = true
                        }
                    }
                }
            } else if (distMeters <= settings.allowedRadiusMeters) {
                // Local devotee at Ashram (<= 200m)
                estimatedDistanceKm = (kotlin.math.round(straightKm * 10) / 10)
                isLocationAutoFetched = false
            } else {
                // Devotee within 30 km, but outside 200m
                estimatedDistanceKm = roadKm
                isLocationAutoFetched = false
            }
        }
    }

    // Auto-save Royal Token Card to Photo Gallery whenever existingToken is available
    LaunchedEffect(existingToken) {
        if (existingToken != null && savedImageUri == null) {
            savedImageUri = TokenCardExporter.saveTokenToGallery(context, existingToken!!, settings)
        }
    }

    // 🔔 Live Smart Token Calling Evaluation (Sound + Vibration + TTS Voice)
    // Devotee viewing token or generating token will NOT trigger unwanted alarm sound.
    // Alert only triggers when runningTokenNumber ADVANCES forward while actively waiting.
    var initialServingSeen by remember { mutableIntStateOf(-1) }
    LaunchedEffect(existingToken, settings.runningTokenNumber) {
        val tok = existingToken
        if (tok != null && settings.runningTokenNumber > 0) {
            if (initialServingSeen == -1) {
                // First launch / token view: simply record current serving number so viewing doesn't sound alarm
                initialServingSeen = settings.runningTokenNumber
                com.example.shribalajikripadham.util.SmartTokenAlertHelper.markCurrentServingAcknowledged(
                    context,
                    settings.runningTokenNumber
                )
            } else if (settings.runningTokenNumber > initialServingSeen) {
                // Queue advanced forward to a new serving number while devotee is waiting on screen!
                try {
                    com.example.shribalajikripadham.util.SmartTokenAlertHelper.evaluateAndTriggerAlert(
                        context = context,
                        myToken = tok.tokenNumber,
                        currentServing = settings.runningTokenNumber
                    )
                } catch (e: Exception) {}
                initialServingSeen = settings.runningTokenNumber
            }
        }
    }

    // Auto-silence all alerts if devotee navigates back or leaves screen
    DisposableEffect(Unit) {
        onDispose {
            com.example.shribalajikripadham.util.SmartTokenAlertHelper.stopAllAlerts(context)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isHindi) "रविवार टोकन पंजीकरण" else "Sunday Token Registration",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
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
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Quick Face Token Banner
            if (onNavigateToFaceToken != null && existingToken == null) {
                Surface(
                    color = Color(0xFFFFF3E0),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SaffronPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Text("⚡", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (isHindi) "सुपरफास्ट चेहरा टोकन (< 1s)" else "Fast Face Token (< 1s)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaroonAccent
                                )
                                Text(
                                    text = if (isHindi) "फॉर्म भरे बिना चेहरे से तुरंत टोकन पाएं" else "Instant token using your face profile",
                                    fontSize = 11.sp,
                                    color = TextSecondaryDark
                                )
                            }
                        }
                        Button(
                            onClick = onNavigateToFaceToken,
                            colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(if (isHindi) "फेस स्कैन" else "Face Scan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 1. LIVE RUNNING QUEUE BANNER
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isHindi) "दरबार में चालू नंबर" else "Currently Calling",
                            fontSize = 12.sp,
                            color = TextSecondaryDark,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(SaffronPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "#${settings.runningTokenNumber}",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(50.dp)
                            .background(Color.LightGray)
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isHindi) "आपका टोकन नंबर" else "Your Token Number",
                            fontSize = 12.sp,
                            color = TextSecondaryDark,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(if (existingToken != null) GoldDark else Color(0xFFB0BEC5)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (existingToken != null) "#${existingToken!!.tokenNumber}" else "-",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))


            // 3. HARDWARE FINGERPRINT & STRICT RULE BANNER / CUSTOM NOTICE
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

            Spacer(modifier = Modifier.height(18.dp))

            // 4. MAIN CONTENT: EITHER TOKEN PASS OR REGISTRATION FORM
            if (existingToken != null) {
                // ROYAL GOLDEN/SAFFRON TOKEN PASS CARD (Auto-saved to Gallery)
                PremiumRoyalTokenCard(
                    token = existingToken!!,
                    settings = settings,
                    isHindi = isHindi,
                    savedImageUri = savedImageUri,
                    onBackToHome = onBack
                )
            } else {
                // Check Sunday 8:00 AM - 5:00 PM Weekly Schedule & Upcoming Sunday Date
                var currentTimestampMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
                LaunchedEffect(Unit) {
                    while (true) {
                        kotlinx.coroutines.delay(1000L)
                        currentTimestampMillis = System.currentTimeMillis()
                    }
                }
                val scheduleState = SundayTokenScheduleHelper.evaluateSchedule(settings, currentTimestampMillis)

                if (scheduleState !is SundayScheduleState.Open) {
                    val visual = when (scheduleState) {
                        is SundayScheduleState.CountdownActive -> ScheduleBannerVisual(
                            bannerBg = Color(0xFFFFF8E1),
                            borderCol = Color(0xFFFF9800),
                            iconText = "⏳",
                            titleText = if (isHindi) "रविवार टोकन पंजीकरण: 12 घंटे पूर्व उल्टी गिनती" else "Sunday Token Registration Countdown",
                            descText = if (isHindi)
                                "टोकन खुलने में शेष समय: ${SundayTokenScheduleHelper.formatCountdownHindi(scheduleState.remainingMillis)} [ ${SundayTokenScheduleHelper.formatCountdown(scheduleState.remainingMillis)} ]\n\nटोकन ${scheduleState.formattedTarget} पर स्वतः खुल जाएंगे।"
                            else
                                "Time remaining: ${SundayTokenScheduleHelper.formatCountdown(scheduleState.remainingMillis)} (${scheduleState.messageEnglish})"
                        )
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
                            titleText = if (isHindi) "टोकन आज सुबह 8:00 बजे से मिलेंगे" else "Opens at 8:00 AM Today",
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

                // Device GPS Hardware Off Warning
                val locManager = context.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager
                val isHardwareGpsOn = locManager?.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) == true ||
                                      locManager?.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER) == true

                if (!isHardwareGpsOn) {
                    Surface(
                        color = Color(0xFFFFEBEE),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, Color(0xFFE57373)),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "📍", fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isHindi) "फोन का GPS (लोकेशन) बंद है" else "Phone GPS is OFF",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFC62828)
                                )
                                Text(
                                    text = if (isHindi)
                                        "टोकन व आश्रम दूरी सत्यापन हेतु GPS चालू करना अनिवार्य है।"
                                    else
                                        "Please turn ON location for token verification.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF7F0000)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Button(
                                onClick = {
                                    try {
                                        context.startActivity(android.content.Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(context, if (isHindi) "कृपया फोन सेटिंग्स से लोकेशन चालू करें" else "Please enable Location in Phone Settings", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(if (isHindi) "GPS ऑन करें" else "Turn ON", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Missing Location Permission Banner (Only if Geofence is active and location is not granted)
                if (!hasLocationPermission && settings.isGeofenceEnforced) {
                    Surface(
                        color = Color(0xFFFFF3E0),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, Color(0xFFFFB74D)),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "📍", fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isHindi) "जीपीएस लोकेशन अनुमति" else "GPS Location Needed",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE65100)
                                )
                                Text(
                                    text = if (isHindi)
                                        "आश्रम दूरी व रविवार टोकन सत्यापन हेतु लोकेशन चालू करें।"
                                    else
                                        "Location is required for Ashram token verification.",
                                    fontSize = 11.sp,
                                    color = Color(0xFFBF360C)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Button(
                                onClick = {
                                    triggerFreshLocationFix()
                                    android.widget.Toast.makeText(context, if (isHindi) "📍 GPS लोकेशन रिफ्रेश हो रही है..." else "Refreshing GPS location...", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(if (isHindi) "GPS रिफ्रेश करें" else "Refresh GPS", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // REGISTRATION FORM
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, Color(0xFFDCDCDC)),
                    elevation = CardDefaults.cardElevation(3.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = if (isHindi) "टोकन विवरण दर्ज करें" else "Enter Token Details",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF8B0000)
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = patientName,
                            onValueChange = { patientName = it },
                            label = { Text(text = if (isHindi) "मरीज / भक्त का पूरा नाम *" else "Patient Full Name *", fontWeight = FontWeight.SemiBold) },
                            textStyle = androidx.compose.ui.text.TextStyle(color = Color(0xFF111111), fontSize = 15.sp, fontWeight = FontWeight.Medium),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
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

                        // Name Auto-Suggestion Chips
                        if (nameSuggestions.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isHindi) "सुझाव (Tap to Auto-fill):" else "Suggestions (Tap to fill):",
                                fontSize = 12.sp,
                                color = Color(0xFF8B0000),
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                nameSuggestions.forEach { sugg ->
                                    SuggestionChip(
                                        onClick = {
                                            patientName = sugg.patientName
                                            phoneNumber = sugg.phoneNumber
                                            city = sugg.city
                                            originAddress = sugg.city
                                            if (sugg.photoUri.isNotBlank()) capturedPhotoUri = sugg.photoUri
                                            nameSuggestions = emptyList()
                                            directorySuggestions = emptyList()
                                        },
                                        colors = SuggestionChipDefaults.suggestionChipColors(
                                            containerColor = Color(0xFFFFF3E0),
                                            labelColor = Color(0xFF5C001E)
                                        ),
                                        border = BorderStroke(1.dp, Color(0xFFFFB300)),
                                        label = {
                                            Text("${sugg.patientName} (${sugg.city})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    )
                                }
                            }
                        }

                        if (autoFillBanner != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                color = Color(0xFFE8F5E9),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFF81C784)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("✓", color = Color(0xFF1B5E20), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = autoFillBanner!!,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF1B5E20)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = { if (it.length <= 10) phoneNumber = it },
                            label = { Text(text = if (isHindi) "मोबाइल नंबर *" else "Mobile Number *", fontWeight = FontWeight.SemiBold) },
                            textStyle = androidx.compose.ui.text.TextStyle(color = Color(0xFF111111), fontSize = 15.sp, fontWeight = FontWeight.Medium),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
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

                        val isOutstationDevotee = distanceMeters > (settings.outstationMinDistanceKm * 1000.0)

                        if (directorySuggestions.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isHindi) "📱 भक्त डायरेक्टरी सुझाव (1-टैप ऑटो-फिल):" else "📱 Devotee Directory Suggestions (1-Tap Auto-fill):",
                                fontSize = 12.sp,
                                color = Color(0xFF00695C),
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                directorySuggestions.forEach { entry ->
                                    SuggestionChip(
                                        onClick = {
                                            phoneNumber = entry.phoneNumber
                                            patientName = entry.patientName
                                            if (!isOutstationDevotee) {
                                                city = entry.city
                                                originAddress = entry.city
                                            }
                                            if (entry.photoUri.isNotBlank()) capturedPhotoUri = entry.photoUri
                                            directorySuggestions = emptyList()
                                        },
                                        colors = SuggestionChipDefaults.suggestionChipColors(
                                            containerColor = Color(0xFFE0F2F1),
                                            labelColor = Color(0xFF004D40)
                                        ),
                                        border = BorderStroke(1.dp, Color(0xFF80CBC4)),
                                        label = {
                                            Text(
                                                text = "${entry.patientName} • ${entry.phoneNumber} (${entry.city})",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = originAddress.ifEmpty { city },
                            onValueChange = {
                                if (!isOutstationDevotee) {
                                    originAddress = it
                                    city = it
                                    isLocationAutoFetched = false
                                }
                            },
                            readOnly = isOutstationDevotee,
                            label = {
                                Text(
                                    text = if (isOutstationDevotee)
                                        (if (isHindi) "गाँव / कस्बा / शहर (GPS द्वारा स्वतः लॉक)" else "Village / Town / City (GPS Locked)")
                                    else
                                        (if (isHindi) "गाँव / कस्बा / शहर" else "Village / Town / City"),
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            placeholder = { Text(text = if (isHindi) "उदा. अपना गाँव, कस्बा या शहर का नाम लिखें..." else "e.g. Enter your village, town or city...", color = Color(0xFF757575)) },
                            trailingIcon = {
                                if (isOutstationDevotee) {
                                    Text(
                                        text = "🔒 GPS लॉक",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = Color(0xFF2E7D32),
                                        modifier = Modifier.padding(end = 12.dp)
                                    )
                                }
                            },
                            textStyle = androidx.compose.ui.text.TextStyle(color = Color(0xFF111111), fontSize = 15.sp, fontWeight = FontWeight.Medium),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
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

                        // GPS Auto-Fetch Status & Local Devotee Typing Status Badge
                        if (isResolvingLocationName) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = MaroonPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isHindi) "जीपीएस से आपका गाँव/स्थान खोजा जा रहा है..." else "Resolving village name from GPS...",
                                    fontSize = 12.sp,
                                    color = MaroonPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        } else if (isOutstationDevotee && originAddress.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                color = Color(0xFFE8F5E9),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFF81C784)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("🔒", fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isHindi) "30 किमी से अधिक दूरी: स्थान GPS द्वारा स्वतः लॉक है (${if (estimatedDistanceKm >= 0f) "%.1f किमी".format(estimatedDistanceKm) else ""})" else "Outstation (>30km): Location locked by GPS (${if (estimatedDistanceKm >= 0f) "%.1f km".format(estimatedDistanceKm) else ""})",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2E7D32)
                                    )
                                }
                            }
                        } else if (isLocationAutoFetched && originAddress.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                color = Color(0xFFE8F5E9),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFF81C784)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("📍", fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isHindi) "GPS द्वारा स्वतः सत्यापित स्थान (${if (estimatedDistanceKm >= 0f) "%.1f किमी".format(estimatedDistanceKm) else ""})" else "GPS Verified Location (${if (estimatedDistanceKm >= 0f) "%.1f km".format(estimatedDistanceKm) else ""})",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2E7D32)
                                    )
                                }
                            }
                        } else if (distanceMeters >= 0.0 && distanceMeters <= settings.allowedRadiusMeters) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                color = Color(0xFFE3F2FD),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFF90CAF9)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("✍️", fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isHindi) "आश्रम परिसर में उपस्थित: आप अपना गाँव/कस्बा स्वयं लिख सकते हैं।" else "At Ashram: Enter your home village/city freely.",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF1565C0)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Live Automatic Distance Calculation Preview Card
                        Surface(
                            color = Color(0xFFF1F8E9),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFAED581)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("📍", fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isHindi) "गंतव्य: श्री बालाजी कृपा धाम, डुंगरा जाट" else "Destination: Shri Balaji Kripa Dham, Dungra Jaat",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color(0xFF2E7D32)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("🚗", fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isCalculatingDistance) {
                                            if (isHindi) "सड़क दूरी की गणना हो रही है..." else "Calculating road distance..."
                                        } else if (estimatedDistanceKm >= 0f) {
                                            if (isHindi) "अनुमानित सड़क दूरी: ${if (estimatedDistanceKm <= 0.2f) "आश्रम परिसर (<200m)" else "%.1f किमी (KM)".format(estimatedDistanceKm)}"
                                            else "Estimated Road Distance: ${if (estimatedDistanceKm <= 0.2f) "Ashram Campus (<200m)" else "%.1f km".format(estimatedDistanceKm)}"
                                        } else {
                                            if (isHindi) "पता दर्ज करें (दूरी स्वतः निर्धारित होगी)" else "Enter location to calculate distance"
                                        },
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        color = if (estimatedDistanceKm >= 0f) Color(0xFFE65100) else Color.DarkGray
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Text(if (isDistanceEligible) "🟢" else "📍", fontSize = 14.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (!settings.isGeofenceEnforced) {
                                                if (isHindi) "जियोफेंस: सभी स्थानों से खुला है" else "Geofence: Open everywhere"
                                            } else if (distanceMeters < 0.0) {
                                                if (isHindi) "⚠️ जीपीएस प्रतीक्षारत (कृपया GPS चालू करें)" else "Waiting for GPS signal"
                                            } else if (settings.isOutstationAdvanceAllowed && distanceMeters > (settings.outstationMinDistanceKm * 1000.0)) {
                                                val kmStr = String.format(java.util.Locale.US, "%.1f km", distanceMeters/1000.0)
                                                if (isHindi) "🟢 दूरस्थ भक्त ($kmStr): अग्रिम टोकन मान्य" else "🟢 Outstation ($kmStr): Advance Token Eligible"
                                            } else if (distanceMeters <= settings.allowedRadiusMeters) {
                                                val radText = if (settings.allowedRadiusMeters >= 1000.0) "${String.format(java.util.Locale.US, "%.1f", settings.allowedRadiusMeters/1000.0)}km" else "${settings.allowedRadiusMeters.toInt()}m"
                                                if (isHindi) "🟢 आश्रम परिसर में उपस्थित ($radText सत्यापित)" else "🟢 Inside Ashram Premises ($radText Verified)"
                                            } else {
                                                val kmStr = String.format(java.util.Locale.US, "%.1f km", distanceMeters/1000.0)
                                                if (isHindi) "🔴 स्थानीय दायरा ($kmStr): आश्रम परिसर में आकर लें" else "🔴 Local ($kmStr): Collect at Ashram"
                                            },
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isDistanceEligible) Color(0xFF2E7D32) else Color(0xFFC62828)
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            isRefreshingLocation = true
                                            GeofenceLocationManager.requestFreshLocation(context) { loc ->
                                                if (loc != null) {
                                                    userLatitude = loc.latitude
                                                    userLongitude = loc.longitude
                                                }
                                                isRefreshingLocation = false
                                            }
                                        },
                                        shape = RoundedCornerShape(20.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        if (isRefreshingLocation) {
                                            CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp)
                                        } else {
                                            Text(if (isHindi) "🔄 GPS रीफ्रेश" else "🔄 Refresh GPS", fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }

                        
                        Spacer(modifier = Modifier.height(14.dp))

                        // 📸 MANDATORY DEVOTEE SELFIE VERIFICATION CARD
                        Surface(
                            color = if (capturedBitmap != null) Color(0xFFF1F8E9) else Color(0xFFFFF3E0),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.5.dp, if (capturedBitmap != null) Color(0xFF2E7D32) else SaffronPrimary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("📸", fontSize = 20.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isHindi) "भक्त का फोटो (वैकल्पिक / Optional) 📸" else "Devotee Photo (Optional) 📸",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = if (capturedBitmap != null) Color(0xFF1B5E20) else MaroonAccent
                                        )
                                        Text(
                                            text = if (isHindi)
                                                "फोटो खींचने पर अगली बार किसी भी फोन से चेहरा पहचान स्वतः हो जाएगी। बिना फोटो के भी टोकन ले सकते हैं।"
                                            else
                                                "Capturing photo enables cross-phone face recognition. Token can also be generated without photo.",
                                            fontSize = 11.sp,
                                            color = TextSecondaryDark
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                if (capturedBitmap != null) {
                                    Box(
                                        modifier = Modifier
                                            .size(130.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .border(2.dp, SaffronPrimary, RoundedCornerShape(16.dp))
                                    ) {
                                        Image(
                                            bitmap = capturedBitmap!!.asImageBitmap(),
                                            contentDescription = "Devotee Photo",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("✓", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isHindi) "फोटो सफलतापूर्वक ली गई" else "Photo Captured",
                                            fontSize = 12.sp,
                                            color = Color(0xFF2E7D32),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                     Spacer(modifier = Modifier.height(8.dp))
                                     Row(
                                         horizontalArrangement = Arrangement.spacedBy(6.dp),
                                         verticalAlignment = Alignment.CenterVertically,
                                         modifier = Modifier.fillMaxWidth()
                                     ) {
                                         Button(
                                             onClick = {
                                                 capturedBitmap?.let { bmp ->
                                                     val rotated = DevoteePhotoHelper.rotateBitmap(bmp, 90f)
                                                     capturedBitmap = rotated
                                                     capturedPhotoUri = DevoteePhotoHelper.saveDevoteePhoto(context, rotated, "devotee_rotated")
                                                     Toast.makeText(context, if (isHindi) "🔄 फोटो 90° घुमाई गई" else "Photo rotated 90°", Toast.LENGTH_SHORT).show()
                                                 }
                                             },
                                             shape = RoundedCornerShape(20.dp),
                                             colors = ButtonDefaults.buttonColors(containerColor = MaroonAccent),
                                             contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                         ) {
                                             Text(
                                                 text = if (isHindi) "🔄 फोटो घुमाएं (90°)" else "🔄 Rotate 90°",
                                                 fontSize = 11.sp,
                                                 color = AmberGold,
                                                 fontWeight = FontWeight.Bold
                                             )
                                         }
                                         OutlinedButton(
                                             onClick = { launchCameraSafely() },
                                             shape = RoundedCornerShape(20.dp),
                                             border = BorderStroke(1.dp, SaffronPrimary),
                                             contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                         ) {
                                             Text(
                                                 text = if (isHindi) "📸 पुनः लें" else "📸 Retake",
                                                 fontSize = 11.sp,
                                                 color = SaffronPrimary,
                                                 fontWeight = FontWeight.Bold
                                             )
                                         }
                                         OutlinedButton(
                                             onClick = { galleryLauncher.launch("image/*") },
                                             shape = RoundedCornerShape(20.dp),
                                             border = BorderStroke(1.dp, Color(0xFF1976D2)),
                                             contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                         ) {
                                             Text(
                                                 text = if (isHindi) "🖼️ गैलरी" else "🖼️ Gallery",
                                                 fontSize = 11.sp,
                                                 color = Color(0xFF1976D2),
                                                 fontWeight = FontWeight.Bold
                                             )
                                         }
                                         TextButton(
                                             onClick = {
                                                 capturedBitmap = null
                                                 capturedPhotoUri = ""
                                             },
                                             contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                                         ) {
                                             Text(
                                                 text = if (isHindi) "❌ हटाएं" else "❌ Remove",
                                                 fontSize = 11.sp,
                                                 color = Color.Red
                                             )
                                         }
                                     }
                                } else {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = if (isHindi) "फोटो वैकल्पिक है - आप चाहें तो सेल्फी या गैलरी से फोटो जोड़ सकते हैं" else "Photo is optional - take selfie or choose from gallery",
                                            fontSize = 11.sp,
                                            color = Color.Gray,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedButton(
                                                onClick = { launchCameraSafely() },
                                                border = BorderStroke(1.dp, SaffronPrimary),
                                                shape = RoundedCornerShape(20.dp),
                                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    text = if (isHindi) "📷 सेल्फी फोटो लें" else "📷 Take Selfie",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = MaroonAccent
                                                )
                                            }
                                            OutlinedButton(
                                                onClick = { galleryLauncher.launch("image/*") },
                                                border = BorderStroke(1.dp, Color(0xFF1976D2)),
                                                shape = RoundedCornerShape(20.dp),
                                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    text = if (isHindi) "🖼️ गैलरी से चुनें" else "🖼️ Choose Gallery",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = Color(0xFF1976D2)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = errorMessage!!,
                                color = StatusOutsideAshram,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        if (isQuotaExceeded) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFFEF5350)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("⛔", fontSize = 22.sp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = if (isHindi) "आज की टोकन सीमा पूरी हो चुकी है" else "Daily Token Quota Full",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color(0xFFC62828)
                                        )
                                        Text(
                                            text = if (isHindi)
                                                "आज के अधिकतम ${settings.maxDailyTokens} टोकन पूरे हो चुके हैं। कृपया अगले दरबार में प्रयास करें।"
                                                else "All ${settings.maxDailyTokens} tokens for today are booked. Please try next time.",
                                            fontSize = 11.sp,
                                            color = Color.DarkGray
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Button(
                            onClick = {
                                // 1. Check Sunday Schedule (08:00 AM to 05:00 PM)
                                val currentSchedule = SundayTokenScheduleHelper.evaluateSchedule(settings)
                                when (currentSchedule) {
                                    is SundayScheduleState.CountdownActive -> {
                                        scheduleAlertTitle = if (isHindi) "⏳ टोकन उल्टी गिनती जारी है" else "⏳ Countdown Active"
                                        scheduleAlertMessage = if (isHindi)
                                            "रविवार टोकन पंजीकरण में शेष समय: ${SundayTokenScheduleHelper.formatCountdownHindi(currentSchedule.remainingMillis)} [ ${SundayTokenScheduleHelper.formatCountdown(currentSchedule.remainingMillis)} ]।\n\nटोकन ${currentSchedule.formattedTarget} स्वतः खुल जाएंगे। कृपया उस समय पुनः प्रयास करें।"
                                        else
                                            "Tokens open in: ${SundayTokenScheduleHelper.formatCountdown(currentSchedule.remainingMillis)} (Will open automatically at ${currentSchedule.formattedTarget})."
                                        showScheduleAlertDialog = true
                                        errorMessage = scheduleAlertMessage
                                        return@Button
                                    }
                                    is SundayScheduleState.NonSunday -> {
                                        scheduleAlertTitle = if (isHindi) "📅 टोकन केवल रविवार को मिलते हैं" else "📅 Tokens Only On Sunday"
                                        scheduleAlertMessage = if (isHindi) currentSchedule.messageHindi else currentSchedule.messageEnglish
                                        showScheduleAlertDialog = true
                                        errorMessage = scheduleAlertMessage
                                        return@Button
                                    }
                                    is SundayScheduleState.SundayBeforeStart -> {
                                        scheduleAlertTitle = if (isHindi) "⏳ टोकन प्रातः 8:30 बजे से मिलेंगे" else "⏳ Opens at 8:30 AM"
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
                                    SundayScheduleState.Open -> { /* Open! Proceed */ }
                                }

                                // 1b. Check Required Permissions & Devotee Photo
                                if (settings.isGeofenceEnforced && (!hasLocationPermission || (userLatitude == 0.0 && userLongitude == 0.0))) {
                                    triggerFreshLocationFix()
                                    errorMessage = if (isHindi) "टोकन पंजीकरण हेतु लोकेशन (GPS) चालू होना आवश्यक है। कृपया GPS ऑन करें।" else "GPS location is required for token registration. Please turn on GPS."
                                    return@Button
                                }
                                // Devotee photo is optional - proceeds smoothly with or without selfie

                                // 2. Check Ashram Location & Dual-Distance Geofence Policy
                                if (settings.isGeofenceEnforced) {
                                    if (userLatitude == 0.0 || userLongitude == 0.0 || distanceMeters < 0.0) {
                                        val msg = if (isHindi)
                                            "⚠️ जीपीएस लोकेशन प्राप्त नहीं हो सकी!\n\nटोकन प्राप्त करने हेतु आपके फोन का GPS चालू होना अनिवार्य है।\n\nकृपया अपने फोन की लोकेशन (GPS) चालू करें और '🔄 GPS रीफ्रेश' बटन दबाएँ।"
                                        else
                                            "⚠️ GPS location required! Please turn on device GPS and tap '🔄 GPS Refresh'."
                                        locationAlertTitle = if (isHindi) "📍 जीपीएस लोकेशन अनिवार्य है" else "📍 GPS Required"
                                        locationAlertMessage = msg
                                        showLocationAlertDialog = true
                                        errorMessage = msg
                                        return@Button
                                    }
                                    if (!isDistanceEligible) {
                                        val distKm = if (distanceMeters < 999990.0) String.format(Locale.US, "%.1f किमी", distanceMeters / 1000.0) else "अज्ञात"
                                        val outKm = settings.outstationMinDistanceKm.toInt()
                                        val radM = if (settings.allowedRadiusMeters >= 1000.0) "${String.format(Locale.US, "%.1f", settings.allowedRadiusMeters / 1000.0)} किमी" else "${settings.allowedRadiusMeters.toInt()} मीटर"
                                        locationAlertTitle = if (isHindi) "📍 आश्रम दूरी नियम (स्थानीय भक्त)" else "📍 Ashram Distance Policy"
                                        locationAlertMessage = if (isHindi) {
                                            if (settings.isOutstationAdvanceAllowed) {
                                                "⚠️ आप अभी आश्रम से $distKm दूर हैं!\n\nनियम: जो भक्त $outKm किमी से अधिक दूरी पर हैं, वे घर से अग्रिम टोकन ले सकते हैं। परंतु $outKm किमी के दायरे वाले स्थानीय भक्तों को टोकन केवल आश्रम परिसर ($radM के भीतर) में आकर ही मिलेगा।\n\nकृपया आश्रम पहुँचकर ही टोकन जनरेट करें ताकि व्यवस्था सुचारू रहे।"
                                            } else {
                                                "⚠️ आप अभी आश्रम से $distKm दूर हैं!\n\nनियम: टोकन केवल आश्रम परिसर ($radM के भीतर) में उपस्थित होने पर ही मिलेगा।"
                                            }
                                        } else {
                                            "⚠️ You are $distKm away from Ashram! Must be within $radM of Ashram premises to register."
                                        }
                                        showLocationAlertDialog = true
                                        errorMessage = if (isHindi) "⚠️ $outKm किमी दायरे वाले स्थानीय भक्त आश्रम परिसर ($radM) में आकर ही टोकन प्राप्त कर सकते हैं।" else "Must be at Ashram (within $radM)."
                                        return@Button
                                    }
                                }

                                // 3. Check Daily Quota
                                if (isQuotaExceeded) {
                                    val nextSun = SundayTokenScheduleHelper.getNextSundayDate()
                                    val nextSunStr = SundayTokenScheduleHelper.formatNextSundayDateHindi(nextSun)
                                    scheduleAlertTitle = if (isHindi) "🔴 आज के टोकन पूरे हो गए हैं" else "🔴 Daily Quota Full"
                                    scheduleAlertMessage = if (isHindi)
                                        "आज के टोकन पूरे हो गए हैं (अधिकतम ${settings.maxDailyTokens} टोकन)।\n\nअब टोकन आगामी रविवार, $nextSunStr को सुबह 8:30 बजे से मिलना शुरू होंगे।"
                                    else
                                        "Today's tokens are complete. Next tokens will be available on Sunday from 8:30 AM."
                                    showScheduleAlertDialog = true
                                    errorMessage = scheduleAlertMessage
                                    return@Button
                                }

                                // 4. Form Validation
                                if (patientName.isBlank()) {
                                    errorMessage = if (isHindi) "कृपया मरीज/भक्त का नाम दर्ज करें।" else "Please enter patient name."
                                    return@Button
                                }
                                val cleanPhone = phoneNumber.trim().filter { it.isDigit() }
                                if (cleanPhone.length != 10 || cleanPhone[0] !in '6'..'9') {
                                    errorMessage = if (isHindi) "कृपया मान्य 10 अंकों का मोबाइल नंबर दर्ज करें (6, 7, 8 या 9 से प्रारंभ)।" else "Please enter a valid 10-digit Indian mobile number starting with 6-9."
                                    return@Button
                                }
                                val devoteeVillageOrCity = originAddress.trim().ifEmpty { city.trim() }.ifEmpty { if (isHindi) "स्थानीय" else "Local" }

                                isSubmitting = true
                                errorMessage = null
                                scope.launch {
                                    try {
                                        // 1. Actively acquire fresh GPS satellite location to eliminate Cold-Start 0.0 or Stale Cache
                                        var loc = GeofenceLocationManager.getLastKnownLocation(context)
                                        if (userLatitude == 0.0 || userLongitude == 0.0 || loc == null || (System.currentTimeMillis() - loc.time) > 20_000L || (loc.hasAccuracy() && loc.accuracy > GeofenceLocationManager.MAX_ALLOWED_ACCURACY_METERS)) {
                                            val fresh = GeofenceLocationManager.awaitFreshLocation(context, timeoutMs = 3500L)
                                            if (fresh != null) {
                                                loc = fresh
                                                userLatitude = fresh.latitude
                                                userLongitude = fresh.longitude
                                                freshLocationAccuracy = if (fresh.hasAccuracy()) fresh.accuracy else 15.0f
                                                isFreshLocationMock = if (GeofenceLocationManager.isMockCheckGloballyEnabled) {
                                                    GeofenceLocationManager.isMockLocation(fresh, context)
                                                } else false
                                            }
                                        }

                                        val isMock = if (GeofenceLocationManager.isMockCheckGloballyEnabled) {
                                            isFreshLocationMock || GeofenceLocationManager.isMockLocation(loc, context)
                                        } else false
                                        if (isMock) {
                                            errorMessage = if (isHindi)
                                                "⚠️ फ़ेक जीपीएस चेतावनी: आपके डिवाइस में नकली लोकेशन / Fake GPS स्पूफिंग का उपयोग पकड़ा गया है। श्री बालाजी कृपा धाम के नियमों के अनुसार केवल वास्तविक जीपीएस से ही टोकन मान्य है। कृपया फ़ेक ऐप बंद करके पुनः प्रयास करें।"
                                            else
                                                "⚠️ Fake GPS Alert: Mock location or spoofing detected. Please disable Fake GPS and use genuine location."
                                            try {
                                                repository.logAuditEvent(
                                                    action = "SECURITY_BLOCKED_FAKE_GPS",
                                                    performedBy = patientName.trim().ifBlank { "अज्ञात भक्त" },
                                                    role = "BLOCKED_DEVICE",
                                                    reason = "फ़ेक जीपीएस (Mock Location / Fake GPS) का उपयोग पकड़ा गया",
                                                    details = "Phone: ${phoneNumber.trim()}"
                                                )
                                            } catch (ignored: Exception) {}
                                            isSubmitting = false
                                            return@launch
                                        }

                                        val isRooted = GeofenceLocationManager.isDeviceRooted(context)
                                        if (isRooted) {
                                            errorMessage = if (isHindi)
                                                "⚠️ सुरक्षा चेतावनी: आपके डिवाइस में रूट (Root / Magisk) का उपयोग पकड़ा गया है। सुरक्षा कारणों से रूटेड डिवाइस पर टोकन पंजीकरण अवरुद्ध है।"
                                            else
                                                "⚠️ Security Alert: Rooted device detected. Token registration is blocked on rooted devices."
                                            try {
                                                repository.logAuditEvent(
                                                    action = "SECURITY_BLOCKED_ROOT",
                                                    performedBy = patientName.trim().ifBlank { "अज्ञात भक्त" },
                                                    role = "BLOCKED_DEVICE",
                                                    reason = "रूटेड डिवाइस (Root / Magisk / KernelSU) से टोकन प्रयास",
                                                    details = "Phone: ${phoneNumber.trim()}"
                                                )
                                            } catch (ignored: Exception) {}
                                            isSubmitting = false
                                            return@launch
                                        }

                                        val accuracy = if (freshLocationAccuracy in 0.1f..250.0f) freshLocationAccuracy else (if (loc != null && loc.hasAccuracy()) loc.accuracy else 10.0f)
                                        if (settings.isGeofenceEnforced && accuracy > GeofenceLocationManager.MAX_ALLOWED_ACCURACY_METERS) {
                                            val maxAcc = GeofenceLocationManager.MAX_ALLOWED_ACCURACY_METERS.toInt()
                                            errorMessage = if (isHindi)
                                                "⚠️ कमजोर जीपीएस सिग्नल (${String.format(Locale.US, "%.0f", accuracy)}m)। कृपया खुले आसमान के नीचे आकर पुनः प्रयास करें (सटीकता $maxAcc मीटर से कम होनी चाहिए)।"
                                            else
                                                "⚠️ Inaccurate GPS signal (${String.format(Locale.US, "%.0f", accuracy)}m). Please stand under open sky (must be within $maxAcc meters)."
                                            try {
                                                repository.logAuditEvent(
                                                    action = "SECURITY_BLOCKED_ACCURACY",
                                                    performedBy = patientName.trim().ifBlank { "अज्ञात भक्त" },
                                                    role = "BLOCKED_DEVICE",
                                                    reason = "कमजोर जीपीएस सिग्नल (${accuracy.toInt()}m > $maxAcc m)",
                                                    details = "Phone: ${phoneNumber.trim()}"
                                                )
                                            } catch (ignored: Exception) {}
                                            isSubmitting = false
                                            return@launch
                                        }
                                        val finalLat = if (userLatitude != 0.0) userLatitude else (loc?.latitude ?: 0.0)
                                        val finalLon = if (userLongitude != 0.0) userLongitude else (loc?.longitude ?: 0.0)

                                        if (settings.isGeofenceEnforced && (finalLat == 0.0 || finalLon == 0.0)) {
                                            errorMessage = if (isHindi) "⚠️ वैध जीपीएस लोकेशन नहीं मिली। कृपया GPS चालू करें और पुनः प्रयास करें।" else "Valid GPS location required. Please turn on GPS."
                                            isSubmitting = false
                                            return@launch
                                        }

                                        if (settings.isGeofenceEnforced) {
                                            val ashLat = if (settings.latitude != 0.0) settings.latitude else 28.3972915
                                            val ashLon = if (settings.longitude != 0.0) settings.longitude else 78.1460410
                                            val isCentroidSpoof = (kotlin.math.abs(finalLat - ashLat) < 0.000005 && kotlin.math.abs(finalLon - ashLon) < 0.000005)
                                            if (isCentroidSpoof) {
                                                errorMessage = if (isHindi)
                                                    "⚠️ सुरक्षा चेतावनी: नकली लोकेशन / मैप पिन इंजेक्शन पकड़ा गया है। कृपया वास्तविक फोन जीपीएस चालू करें।"
                                                else
                                                    "Security Warning: Mock location / map pin injection detected. Please use real GPS."
                                                isSubmitting = false
                                                return@launch
                                            }
                                            val currentGpsMeters = GeofenceLocationManager.calculateDistanceMeters(finalLat, finalLon, ashLat, ashLon)
                                            val outstationM = settings.outstationMinDistanceKm * 1000.0
                                            val isAtAshram = currentGpsMeters <= settings.allowedRadiusMeters
                                            val isOutstationAdvance = currentGpsMeters > outstationM && settings.isOutstationAdvanceAllowed
                                            if (!isAtAshram && !isOutstationAdvance) {
                                                val distKm = String.format(Locale.US, "%.1f", currentGpsMeters / 1000.0)
                                                val outKm = settings.outstationMinDistanceKm.toInt()
                                                val radM = if (settings.allowedRadiusMeters >= 1000.0) "${String.format(Locale.US, "%.1f", settings.allowedRadiusMeters / 1000.0)} किमी" else "${settings.allowedRadiusMeters.toInt()} मीटर"
                                                errorMessage = if (isHindi)
                                                    "⚠️ आश्रम दूरी नियम: ${outKm} किमी के दायरे में रहने वाले स्थानीय भक्तों हेतु टोकन पंजीकरण केवल आश्रम परिसर ($radM के भीतर) में ही मान्य है। आपकी वास्तविक दूरी $distKm किमी है। कृपया परिसर में पहुँचकर ही टोकन जनरेट करें।"
                                                else
                                                    "Local devotees within $outKm km can only register inside Ashram premises ($radM). Your distance is $distKm km."
                                                try {
                                                    repository.logAuditEvent(
                                                        action = "SECURITY_BLOCKED_GEOFENCE",
                                                        performedBy = patientName.trim().ifBlank { "अज्ञात भक्त" },
                                                        role = "BLOCKED_DEVICE",
                                                        reason = "लोकल दायरे ($outKm KM) में बिना आश्रम ($radM) आए टोकन प्रयास",
                                                        details = "Phone: ${phoneNumber.trim()}, Distance: $distKm km"
                                                    )
                                                } catch (ignored: Exception) {}
                                                isSubmitting = false
                                                return@launch
                                            }

                                            // Anti-Spoof: Mismatch check between claimed local address and spoofed GPS location (> 30 km)
                                            val localKeywords = listOf("डूंगरा", "डुंगरा", "अनूपशहर", "जहांगीराबाद", "जहागीराबाद", "डिबाई", "शिकारपुर", "औरंगाबाद", "स्याना", "बुलंदशहर", "बुलन्दशहर", "dungra", "anupshahr", "anupshahar", "jahangirabad", "dibai", "shikarpur", "bulandshahr")
                                            val enteredText = "${devoteeVillageOrCity.trim()} ${originAddress.trim()}".lowercase(Locale.ROOT)
                                            val isClaimingLocalTown = localKeywords.any { enteredText.contains(it) }
                                            if (isClaimingLocalTown && currentGpsMeters > outstationM) {
                                                errorMessage = if (isHindi)
                                                    "⚠️ पता व लोकेशन विसंगति: आपने स्थानीय क्षेत्र ($devoteeVillageOrCity) दर्ज किया है, जबकि फोन की जीपीएस लोकेशन 30 किमी से अधिक दूर दिख रही है। कृपया फ़ेक ऐप बंद करें अथवा सही वास्तविक लोकेशन से प्रयास करें।"
                                                else
                                                    "Address & GPS mismatch: Local address claimed ($devoteeVillageOrCity) but GPS distance is > 30 km. Please disable mock GPS."
                                                isSubmitting = false
                                                return@launch
                                            }
                                        }

                                        val ashLat = if (settings.latitude != 0.0) settings.latitude else 28.3972915
                                        val ashLon = if (settings.longitude != 0.0) settings.longitude else 78.1460410
                                        val distFromDarbarM = GeofenceLocationManager.calculateDistanceMeters(finalLat, finalLon, ashLat, ashLon)
                                        val finalDistanceKm = if (distFromDarbarM <= settings.allowedRadiusMeters) {
                                            (kotlin.math.round((distFromDarbarM / 1000.0) * 10) / 10).toFloat()
                                        } else {
                                            (kotlin.math.round((distFromDarbarM / 1000.0) * 1.28 * 10) / 10).toFloat()
                                        }

                                        val effectiveDeviceId = if (deviceId.isNotBlank()) deviceId else DeviceFingerprintManager.getDeviceId(context)
                                        val effectiveDate = if (settings.darbarDate.isNotBlank()) settings.darbarDate else com.example.shribalajikripadham.data.local.DatabaseHelper.getTodayDateString()

                                        // Instant token registration: register immediately without blocking UI on heavy photo upload
                                        val created = repository.registerToken(
                                            patientName = patientName.trim(),
                                            phoneNumber = phoneNumber.trim(),
                                            deviceId = effectiveDeviceId,
                                            latitude = finalLat,
                                            longitude = finalLon,
                                            city = devoteeVillageOrCity,
                                            registeredBy = "SELF",
                                            photoUri = capturedPhotoUri,
                                            isMockLocation = isMock,
                                            locationAccuracy = accuracy,
                                            originAddress = devoteeVillageOrCity,
                                            destinationAddress = "श्री बालाजी कृपा धाम, डुंगरा जाट",
                                            distanceKm = finalDistanceKm,
                                            darbarDate = effectiveDate
                                        )

                                        existingToken = created
                                        isSubmitting = false

                                        // Newly created token: acknowledge current serving token so alert tune DOES NOT blast
                                        com.example.shribalajikripadham.util.SmartTokenAlertHelper.markCurrentServingAcknowledged(
                                            context,
                                            settings.runningTokenNumber
                                        )

                                        // Store devotee personal token preferences immediately
                                        try {
                                            val myTokPrefs = context.getSharedPreferences("sbkd_devotee_my_token_prefs", Context.MODE_PRIVATE)
                                            myTokPrefs.edit()
                                                .putInt("my_token_number", created.tokenNumber)
                                                .putString("my_token_date", created.darbarDate)
                                                .putString("my_patient_name", created.patientName)
                                                .putString("my_phone_number", created.phoneNumber)
                                                .putString("my_city", created.city)
                                                .apply()
                                        } catch (e: Exception) {}

                                        // Immediate background telemetry heartbeat with devotee credentials
                                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                            try {
                                                com.example.shribalajikripadham.data.network.AppTelemetryManager.recordAppHeartbeat(
                                                    context = context,
                                                    devoteeName = created.patientName,
                                                    devoteePhone = created.phoneNumber,
                                                    city = created.city,
                                                    role = "USER"
                                                )
                                            } catch (e: Exception) {}
                                        }

                                        // Asynchronous non-blocking background tasks: Photo upload, Face embedding, FCM
                                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                            try {
                                                if (capturedBitmap != null) {
                                                    val vector = FaceEmbeddingEngine.extractVectorFromBitmap(capturedBitmap!!)
                                                    repository.upsertDevoteeProfile(
                                                        name = patientName.trim(),
                                                        phone = phoneNumber.trim(),
                                                        city = devoteeVillageOrCity,
                                                        faceVector = vector,
                                                        photoUri = capturedPhotoUri,
                                                        registeredBy = "SELF"
                                                    )
                                                }
                                            } catch (e: Exception) {}
                                            try {
                                                com.example.shribalajikripadham.notification.AshramFirebaseMessagingService.registerDevoteePhone(
                                                    context, phoneNumber.trim()
                                                )
                                            } catch (e: Exception) {}
                                        }
                                    } catch (e: SecurityException) {
                                        val err = e.message ?: "Security Exception: Spoofed Location or Duplicate Device Request Denied."
                                        errorMessage = err
                                        try {
                                            repository.logAuditEvent(
                                                action = "SECURITY_BLOCKED_DEVICE",
                                                performedBy = patientName.trim().ifBlank { "अज्ञात भक्त" },
                                                role = "BLOCKED_DEVICE",
                                                reason = err,
                                                details = "Phone: ${phoneNumber.trim()}"
                                            )
                                        } catch (ignored: Exception) {}
                                    } catch (e: Exception) {
                                        val err = e.message ?: "Security Exception: Spoofed Location or Duplicate Device Request Denied."
                                        errorMessage = err
                                        if (err.contains("सुरक्षा") || err.contains("Security") || err.contains("दूरी") || err.contains("1 फोन") || err.contains("नियम")) {
                                            try {
                                                repository.logAuditEvent(
                                                    action = if (err.contains("1 फोन")) "SECURITY_BLOCKED_DUPLICATE_DEVICE" else "SECURITY_BLOCKED_POLICY",
                                                    performedBy = patientName.trim().ifBlank { "अज्ञात भक्त" },
                                                    role = "BLOCKED_DEVICE",
                                                    reason = err,
                                                    details = "Phone: ${phoneNumber.trim()}"
                                                )
                                            } catch (ignored: Exception) {}
                                        }
                                    } finally {
                                        isSubmitting = false
                                    }
                                }
                            },
                            enabled = !isSubmitting,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(25.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SaffronPrimary,
                                disabledContainerColor = Color.LightGray
                            )
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
                            } else {
                                Text(
                                    text = if (isHindi) "टोकन प्राप्त करें  ➔" else "Generate Sunday Token  ➔",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
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
                            isRefreshingLocation = true
                            GeofenceLocationManager.requestFreshLocation(context) { loc ->
                                if (loc != null) {
                                    userLatitude = loc.latitude
                                    userLongitude = loc.longitude
                                }
                                isRefreshingLocation = false
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
