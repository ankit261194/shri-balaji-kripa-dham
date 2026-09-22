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

    // Form inputs
    var patientName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var originAddress by remember { mutableStateOf("") }
    var estimatedDistanceKm by remember { mutableFloatStateOf(-1f) }
    var isCalculatingDistance by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    // Devotee Photo State (Optional Selfie)
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var capturedPhotoUri by remember { mutableStateOf("") }
    var nameSuggestions by remember { mutableStateOf<List<DevoteeFaceProfile>>(emptyList()) }
    var directorySuggestions by remember { mutableStateOf<List<DevoteeDirectoryEntry>>(emptyList()) }
    var locationSuggestions by remember { mutableStateOf<List<com.example.shribalajikripadham.util.IndiaLocation>>(emptyList()) }
    var showLocationDropdown by remember { mutableStateOf(false) }
    var autoFillBanner by remember { mutableStateOf<String?>(null) }

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

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                cameraLauncher.launch(null)
            } catch (e: Exception) {
                e.printStackTrace()
                errorMessage = if (isHindi) "कैमरा खोलने में त्रुटि हुई।" else "Error opening camera."
            }
        } else {
            errorMessage = if (isHindi)
                "कैमरा अनुमति अस्वीकृत: कृपया सेटिंग्स से अनुमति दें या नीचे गैलरी से फोटो चुनें।"
            else
                "Camera permission denied. Please allow camera in settings or pick from gallery."
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

    var showPermissionSettingsDialog by remember { mutableStateOf(false) }

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
            showPermissionSettingsDialog = true
        }
    }

    var todayActiveTokens by remember { mutableIntStateOf(0) }

    // Geofencing Live Location State (Strictly enforced real GPS)
    var userLatitude by remember { mutableDoubleStateOf(0.0) }
    var userLongitude by remember { mutableDoubleStateOf(0.0) }

    // Live road distance calculation to Shri Balaji Kripa Dham, Dungra Jaat
    LaunchedEffect(city, originAddress, userLatitude, userLongitude) {
        val query = if (originAddress.isNotBlank()) originAddress else city
        if (query.isNotBlank()) {
            isCalculatingDistance = true
            val res = DistanceCalculatorService.resolveDrivingDistance(
                origin = query,
                deviceLat = if (userLatitude != 0.0) userLatitude else 28.3972915,
                deviceLng = if (userLongitude != 0.0) userLongitude else 78.1460410
            )
            estimatedDistanceKm = res.distanceKm
            isCalculatingDistance = false
        } else {
            estimatedDistanceKm = -1f
        }
    }

    var isRefreshingLocation by remember { mutableStateOf(false) }
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
                if (city.isBlank() || originAddress.isBlank() || city == "डूँगरा जाट (स्थानीय)") {
                    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        val detectedPlace = GeofenceLocationManager.resolveVillageAndCity(context, loc.latitude, loc.longitude)
                        if (detectedPlace.isNotBlank()) {
                            withContext(kotlinx.coroutines.Dispatchers.Main) {
                                city = detectedPlace
                                originAddress = detectedPlace
                            }
                        }
                    }
                }
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
        if (!hasLocationPermission || !hasCameraPermission) {
            showPermissionSettingsDialog = true
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
            GeofenceLocationManager.calculateDistanceMeters(
                userLatitude, userLongitude,
                settings.latitude, settings.longitude
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
            existingToken = repository.checkDeviceRegisteredToday(id)
            todayActiveTokens = repository.getTodayActiveTokenCount()

            // Check & request unified permissions (Location + Camera + Notifications)
            val missingPerms = requiredPermissions.filter { perm ->
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context, perm
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            }

            if (missingPerms.isNotEmpty()) {
                unifiedPermissionLauncher.launch(missingPerms.toTypedArray())
            } else {
                hasLocationPermission = true
                hasCameraPermission = true
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

    // Live search of Pan-India Locations & Road Distance
    LaunchedEffect(originAddress) {
        val q = originAddress.trim()
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
            isCalculatingDistance = true
            try {
                val res = DistanceCalculatorService.calculateRoadDistance(q)
                if (res.distanceKm >= 0f) {
                    estimatedDistanceKm = res.distanceKm
                }
            } catch (e: Exception) {
                // Ignore
            } finally {
                isCalculatingDistance = false
            }
        } else {
            locationSuggestions = emptyList()
            showLocationDropdown = false
            estimatedDistanceKm = -1f
        }
    }

    // Auto-save Royal Token Card to Photo Gallery whenever existingToken is available
    LaunchedEffect(existingToken) {
        if (existingToken != null && savedImageUri == null) {
            savedImageUri = TokenCardExporter.saveTokenToGallery(context, existingToken!!, settings)
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
                // Check Sunday 8:30 AM - 5:00 PM Weekly Schedule & Upcoming Sunday Date
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
                                    } catch (e: Exception) {}
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

                // Missing Core Permissions Banner
                if (!hasLocationPermission || !hasCameraPermission) {
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
                            Text(text = "🔐", fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isHindi) "आवश्यक अनुमतियाँ बंद हैं" else "Permissions Disabled",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE65100)
                                )
                                Text(
                                    text = if (isHindi)
                                        "टोकन व फोटो हेतु लोकेशन और कैमरा अनुमति चालू करें।"
                                    else
                                        "Location & Camera permissions are required.",
                                    fontSize = 11.sp,
                                    color = Color(0xFFBF360C)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Button(
                                onClick = {
                                    showPermissionSettingsDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(if (isHindi) "अनुमति दें" else "Allow", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                                            city = entry.city
                                            originAddress = entry.city
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
                                originAddress = it
                                city = it
                            },
                            label = { Text(text = if (isHindi) "कहाँ से आए हैं / कहाँ के निवासी हैं *" else "Resident Address / Origin *", fontWeight = FontWeight.SemiBold) },
                            placeholder = { Text(text = if (isHindi) "उदा. डूँगरा जाट, बुलन्दशहर, खुर्जा, नोएडा, दिल्ली..." else "e.g. Dungra Jaat, Bulandshahr, Delhi...", color = Color(0xFF757575)) },
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

                        // Pan-India Autocomplete Suggestions Dropdown
                        if (showLocationDropdown && locationSuggestions.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA)),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, SaffronPrimary.copy(alpha = 0.5f)),
                                elevation = CardDefaults.cardElevation(3.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (isHindi) "🔍 स्थान सुझाव (Tap to Select):" else "🔍 Location Suggestions:",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaroonPrimary
                                        )
                                        Text(
                                            text = "✕",
                                            fontSize = 12.sp,
                                            color = Color.Gray,
                                            modifier = Modifier.clickable { showLocationDropdown = false }
                                        )
                                    }
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
                                                    originAddress = formattedLoc
                                                    city = formattedLoc
                                                    if (loc.distanceKm >= 0f) {
                                                        estimatedDistanceKm = loc.distanceKm
                                                    }
                                                    showLocationDropdown = false
                                                }
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = when (loc.category) {
                                                    "स्थानीय गाँव" -> "🏡"
                                                    "तहसील / कस्बा" -> "🏘️"
                                                    "ज़िला (UP)" -> "🏛️"
                                                    "राज्य / UT" -> "🇮🇳"
                                                    else -> "📍"
                                                },
                                                fontSize = 14.sp
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = loc.nameHindi,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp,
                                                        color = Color(0xFF111111)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(
                                                        color = Color(0xFFECEFF1),
                                                        shape = RoundedCornerShape(4.dp)
                                                    ) {
                                                        Text(
                                                            text = loc.category,
                                                            fontSize = 9.sp,
                                                            color = Color(0xFF455A64),
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }
                                                val locDetails = listOfNotNull(
                                                    loc.districtHindi.takeIf { it.isNotBlank() },
                                                    loc.stateHindi.takeIf { it.isNotBlank() }
                                                ).joinToString(", ")
                                                Text(
                                                    text = if (locDetails.isNotBlank()) "${loc.nameEnglish} • $locDetails" else "${loc.nameEnglish} • ${loc.stateHindi}",
                                                    fontSize = 10.sp,
                                                    color = Color.DarkGray
                                                )
                                            }
                                            if (loc.distanceKm >= 0f) {
                                                Surface(
                                                    color = if (loc.distanceKm <= 0.2f) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        text = if (loc.distanceKm <= 0.2f) "स्थानीय" else "${loc.distanceKm.toInt()} km",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (loc.distanceKm <= 0.2f) Color(0xFF2E7D32) else Color(0xFFE65100),
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                        HorizontalDivider(color = Color(0xFFE0E0E0), thickness = 0.5.dp)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Quick City Chips
                        Text(
                            text = if (isHindi) "त्वरित चयन (Quick Select):" else "Quick Select:",
                            fontSize = 12.sp,
                            color = Color(0xFF333333),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val quickCities = listOf(
                            "डूँगरा जाट", "जहाँगीरपुर", "झाझर", "जेवर", "खुर्जा", "बुलन्दशहर", "शिकारपुर", "पहासू", "अरनिया", "छतारी", "दानपुर", "डिबाई", "अनूपशहर", "स्याना", "गुलावठी", "सिकंदराबाद", "ककोड", "अलीगढ़", "हापुड़", "मेरठ", "नोएडा", "दिल्ली", "गाजियाबाद"
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            quickCities.forEach { cName ->
                                SuggestionChip(
                                    onClick = {
                                        originAddress = cName
                                        city = cName
                                    },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = Color(0xFFF5F5F5),
                                        labelColor = Color(0xFF212121)
                                    ),
                                    border = BorderStroke(1.dp, Color(0xFFBDBDBD)),
                                    label = { Text(cName, fontSize = 12.sp, fontWeight = FontWeight.Medium) }
                                )
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
                                                    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                                        val detectedPlace = GeofenceLocationManager.resolveVillageAndCity(context, loc.latitude, loc.longitude)
                                                        if (detectedPlace.isNotBlank()) {
                                                            withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                                if (city.isBlank() || city == "डूँगरा जाट (स्थानीय)") city = detectedPlace
                                                                if (originAddress.isBlank() || originAddress == "डूँगरा जाट (स्थानीय)") originAddress = detectedPlace
                                                            }
                                                        }
                                                    }
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
                                // 1. Check Sunday Schedule (08:30 AM to 05:00 PM)
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
                                    SundayScheduleState.Open -> { /* Open! Proceed */ }
                                }

                                // 1b. Check Required Permissions
                                if (!hasLocationPermission) {
                                    showPermissionSettingsDialog = true
                                    errorMessage = if (isHindi) "टोकन पंजीकरण हेतु लोकेशन अनुमति आवश्यक है।" else "Location permission is required."
                                    return@Button
                                }
                                if (!hasCameraPermission) {
                                    showPermissionSettingsDialog = true
                                    errorMessage = if (isHindi) "टोकन पंजीकरण हेतु कैमरा अनुमति आवश्यक है।" else "Camera permission is required."
                                    return@Button
                                }

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
                                if (phoneNumber.length < 10) {
                                    errorMessage = if (isHindi) "कृपया 10 अंकों का मोबाइल नंबर दर्ज करें।" else "Please enter valid 10-digit mobile number."
                                    return@Button
                                }
                                val devoteeVillageOrCity = originAddress.trim().ifEmpty { city.trim() }
                                if (devoteeVillageOrCity.isBlank()) {
                                    errorMessage = if (isHindi) "कृपया अपने गाँव या शहर का नाम अवश्य दर्ज करें।" else "Please enter your village or city name."
                                    return@Button
                                }

                                isSubmitting = true
                                errorMessage = null
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
                                            errorMessage = if (isHindi) "⚠️ वैध जीपीएस लोकेशन नहीं मिली। कृपया GPS चालू करें और पुनः प्रयास करें।" else "Valid GPS location required. Please turn on GPS."
                                            isSubmitting = false
                                            return@launch
                                        }

                                        val cloudPhotoUrl = if (capturedPhotoUri.isNotBlank()) {
                                            try {
                                                val rawPath = capturedPhotoUri.removePrefix("file://")
                                                val f = File(rawPath)
                                                if (f.exists() && f.length() > 0) {
                                                    com.example.shribalajikripadham.data.network.HostingerCentralSyncManager.uploadPhoto(f) ?: capturedPhotoUri
                                                } else capturedPhotoUri
                                            } catch (e: Exception) { capturedPhotoUri }
                                        } else ""

                                        val created = repository.registerToken(
                                            patientName = patientName.trim(),
                                            phoneNumber = phoneNumber.trim(),
                                            deviceId = deviceId,
                                            latitude = finalLat,
                                            longitude = finalLon,
                                            city = devoteeVillageOrCity,
                                            registeredBy = "SELF",
                                            photoUri = if (cloudPhotoUrl.isNotBlank()) cloudPhotoUrl else capturedPhotoUri,
                                            isMockLocation = isMock,
                                            locationAccuracy = accuracy,
                                            originAddress = devoteeVillageOrCity,
                                            destinationAddress = "श्री बालाजी कृपा धाम, डुंगरा जाट",
                                            distanceKm = estimatedDistanceKm
                                        )

                                        // If devotee captured a photo, extract invariant vector & enroll to universal registry
                                        if (capturedBitmap != null) {
                                            try {
                                                val vector = FaceEmbeddingEngine.extractVectorFromBitmap(capturedBitmap!!)
                                                repository.upsertDevoteeProfile(
                                                    name = patientName.trim(),
                                                    phone = phoneNumber.trim(),
                                                    city = devoteeVillageOrCity,
                                                    faceVector = vector,
                                                    photoUri = if (cloudPhotoUrl.isNotBlank()) cloudPhotoUrl else capturedPhotoUri,
                                                    registeredBy = "SELF"
                                                )
                                            } catch (e: Exception) {}
                                        }
                                        existingToken = created
                                        try {
                                            val myTokPrefs = context.getSharedPreferences("sbkd_devotee_my_token_prefs", Context.MODE_PRIVATE)
                                            myTokPrefs.edit()
                                                .putInt("my_token_number", created.tokenNumber)
                                                .putString("my_token_date", created.darbarDate)
                                                .putString("my_patient_name", created.patientName)
                                                .apply()
                                        } catch (e: Exception) {}
                                        try {
                                            com.example.shribalajikripadham.notification.AshramFirebaseMessagingService.registerDevoteePhone(
                                                context, phoneNumber.trim()
                                            )
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

        // Alert Dialog: Mandatory Permissions Required
        if (showPermissionSettingsDialog) {
            AlertDialog(
                onDismissRequest = { showPermissionSettingsDialog = false },
                containerColor = Color.White,
                icon = { Text("🔐", fontSize = 36.sp) },
                title = {
                    Text(
                        text = if (isHindi) "अनुमतियाँ आवश्यक हैं" else "Permissions Required",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF8B0000),
                        fontSize = 18.sp
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = if (isHindi)
                                "श्री बालाजी कृपा धाम के नियमों के अनुसार टोकन जनरेट करने के लिए निम्नलिखित अनुमतियाँ अनिवार्य हैं:"
                            else
                                "The following permissions are strictly required to generate your token:",
                            fontSize = 14.sp,
                            color = Color(0xFF212121)
                        )
                        Text(
                            text = if (isHindi)
                                "📍 1. लोकेशन (GPS): आश्रम दूरी (200मी / 30किमी) नियम सत्यापन हेतु।\n📷 2. कैमरा: भक्त की लाइव फोटो व टोकन दर्शन हेतु।"
                            else
                                "📍 1. Location (GPS): To verify Ashram distance policy.\n📷 2. Camera: For devotee live verification photo.",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = Color(0xFF333333)
                        )
                        Text(
                            text = if (isHindi)
                                "कृपया '⚙️ सेटिंग्स में अनुमति दें' बटन दबाएं और Permissions में Location तथा Camera को Allow करें।"
                            else
                                "Please tap 'Open Settings' and allow Location and Camera permissions.",
                            fontSize = 12.sp,
                            color = Color.DarkGray
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showPermissionSettingsDialog = false
                            try {
                                val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = android.net.Uri.fromParts("package", context.packageName, null)
                                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (isHindi) "⚙️ सेटिंग्स में अनुमति दें" else "⚙️ Open Settings", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPermissionSettingsDialog = false }) {
                        Text(if (isHindi) "रद्द करें" else "Cancel", color = Color.Gray)
                    }
                }
            )
        }
    }
}
