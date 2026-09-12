package com.example.shribalajikripadham.ui.token

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

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
    var autoFillBanner by remember { mutableStateOf<String?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = TakeFrontPicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            capturedBitmap = bitmap
            capturedPhotoUri = DevoteePhotoHelper.saveDevoteePhoto(context, bitmap, "devotee_selfie")
            errorMessage = null
        }
    }

    var todayActiveTokens by remember { mutableIntStateOf(0) }

    // Geofencing Simulation / Live State
    var simulateInsideAshram by remember { mutableStateOf(true) }
    var userLatitude by remember { mutableDoubleStateOf(28.4089) }
    var userLongitude by remember { mutableDoubleStateOf(77.8789) }

    // Live road distance calculation to Shri Balaji Kripa Dham, Dungra Jaat
    LaunchedEffect(city, originAddress, userLatitude, userLongitude) {
        val query = if (originAddress.isNotBlank()) originAddress else city
        if (query.isNotBlank()) {
            isCalculatingDistance = true
            val res = DistanceCalculatorService.resolveDrivingDistance(
                origin = query,
                deviceLat = userLatitude,
                deviceLng = userLongitude
            )
            estimatedDistanceKm = res.distanceKm
            isCalculatingDistance = false
        } else {
            estimatedDistanceKm = -1f
        }
    }

    val distanceMeters = remember(userLatitude, userLongitude, settings) {
        GeofenceLocationManager.calculateDistanceMeters(
            userLatitude, userLongitude,
            settings.latitude, settings.longitude
        )
    }
    val isInsideGeofence = remember(distanceMeters, settings, simulateInsideAshram) {
        if (simulateInsideAshram) true else distanceMeters <= settings.allowedRadiusMeters
    }
    val isQuotaExceeded = remember(settings.maxDailyTokens, todayActiveTokens) {
        settings.maxDailyTokens > 0 && todayActiveTokens >= settings.maxDailyTokens
    }

    // Load initial data and sync cloud devotee registry
    LaunchedEffect(Unit) {
        val id = DeviceFingerprintManager.getDeviceId(context)
        deviceId = id
        settings = repository.getSettings()
        existingToken = repository.checkDeviceRegisteredToday(id)
        todayActiveTokens = repository.getTodayActiveTokenCount()

        // Try getting actual location
        val loc = GeofenceLocationManager.getLastKnownLocation(context)
        if (loc != null) {
            userLatitude = loc.latitude
            userLongitude = loc.longitude
        }

        // Background cloud sync to pull all devotee profiles from any phone
        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try { repository.syncDevoteesFromCloud() } catch (e: Exception) {}
        }
    }

    // Auto-search devotee when 10-digit phone number is entered
    LaunchedEffect(phoneNumber) {
        val clean = phoneNumber.trim().replace("+91", "").replace(" ", "").replace("-", "")
        if (clean.length == 10) {
            val devotee = repository.searchDevoteeByPhone(clean)
            if (devotee != null) {
                if (patientName.isBlank()) patientName = devotee.patientName
                if (city.isBlank() || city == "डूँगरा जाट (स्थानीय)") {
                    city = devotee.city
                    originAddress = devotee.city
                }
                if (capturedPhotoUri.isBlank() && devotee.photoUri.isNotBlank()) {
                    capturedPhotoUri = devotee.photoUri
                }
                autoFillBanner = if (isHindi)
                    "पूर्व पंजीकृत भक्त: ${devotee.patientName} (${devotee.city}) का विवरण स्वतः भरा गया!"
                else
                    "Found record: ${devotee.patientName} (${devotee.city}) auto-filled!"
            }
        } else {
            autoFillBanner = null
        }
    }

    // Name suggestions when typing name
    LaunchedEffect(patientName) {
        val q = patientName.trim()
        if (q.length >= 2) {
            nameSuggestions = repository.searchDevoteesByName(q, limit = 5)
        } else {
            nameSuggestions = emptyList()
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

            // 2. GEOFENCING STATUS BADGE
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isInsideGeofence) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                ),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(if (isInsideGeofence) StatusInsideAshram else StatusOutsideAshram)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isInsideGeofence)
                                if (isHindi) "✓ आश्रम परिसर के अंदर (उपस्थित)" else "✓ Inside Ashram Premises (Present)"
                            else
                                if (isHindi) "✗ आश्रम परिसर से बाहर" else "✗ Outside Ashram Geofence",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isInsideGeofence) StatusInsideAshram else StatusOutsideAshram
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isInsideGeofence)
                            if (isHindi)
                                "आप आश्रम की 200m परिधि के अंदर हैं। टोकन पंजीकरण की अनुमति है।"
                            else
                                "You are inside the 200m Ashram perimeter. Token issuance is allowed."
                        else
                            if (isHindi)
                                "दूरी: ${(distanceMeters / 1000.0).let { "%.1f".format(it) }} किमी। टोकन केवल आश्रम में उपस्थित होने पर ही जारी होगा।"
                            else
                                "Distance: ${(distanceMeters / 1000.0).let { "%.1f".format(it) }} km. Tokens can only be issued upon physical presence at the Ashram.",
                        fontSize = 12.sp,
                        color = TextPrimaryDark
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Demo Toggle for Simulation
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isHindi) "लोकेशन सिम्युलेटर (परीक्षण हेतु):" else "GPS Simulation Toggle:",
                            fontSize = 11.sp,
                            color = TextSecondaryDark
                        )
                        Switch(
                            checked = simulateInsideAshram,
                            onCheckedChange = { simulateInsideAshram = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = SaffronPrimary)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. HARDWARE FINGERPRINT & STRICT RULE BANNER
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
                            text = if (isHindi) "हार्डवेयर फिंगरप्रिंट नियम: 1 फोन = 1 टोकन" else "Hardware Rule: 1 Device = 1 Sunday Token",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = SaffronDark
                        )
                        Text(
                            text = if (isHindi)
                                "एक मोबाइल डिवाइस से प्रत्येक रविवार को केवल 1 टोकन लिया जा सकता है।"
                            else
                                "Each physical handset is strictly restricted to 1 token per Sunday.",
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
                // Check if Token Opening is Pre-Scheduled
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
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("⏳", fontSize = 26.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (isHindi) "टोकन पंजीकरण का समय पूर्व-निर्धारित है" else "Token Registration Scheduled",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color(0xFFE65100)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isHindi)
                                    "टोकन जारी होना $scheduledTimeStr पर स्वतः प्रारंभ होगा। कृपया निर्धारित समय पर ही टोकन प्राप्त करें।"
                                else
                                    "Token generation is scheduled to open at $scheduledTimeStr. Please return at that time.",
                                fontSize = 12.sp,
                                color = TextPrimaryDark
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // REGISTRATION FORM
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(3.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = if (isHindi) "टोकन विवरण दर्ज करें" else "Enter Token Details",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaroonAccent
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = patientName,
                            onValueChange = { patientName = it },
                            label = { Text(text = if (isHindi) "मरीज / भक्त का पूरा नाम *" else "Patient Full Name *") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Name Auto-Suggestion Chips
                        if (nameSuggestions.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isHindi) "सुझाव (Tap to Auto-fill):" else "Suggestions (Tap to fill):",
                                fontSize = 11.sp,
                                color = MaroonAccent,
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
                                        },
                                        label = {
                                            Text("${sugg.patientName} (${sugg.city})", fontSize = 11.sp)
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
                                        fontSize = 11.sp,
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
                            label = { Text(text = if (isHindi) "मोबाइल नंबर *" else "Mobile Number *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = originAddress.ifEmpty { city },
                            onValueChange = {
                                originAddress = it
                                city = it
                            },
                            label = { Text(text = if (isHindi) "कहाँ से आए हैं / कहाँ के निवासी हैं *" else "Resident Address / Origin *") },
                            placeholder = { Text(text = if (isHindi) "उदा. डूँगरा जाट, बुलन्दशहर, खुर्जा, नोएडा, दिल्ली..." else "e.g. Dungra Jaat, Bulandshahr, Delhi...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Quick City Chips
                        Text(
                            text = if (isHindi) "त्वरित चयन (Quick Select):" else "Quick Select:",
                            fontSize = 11.sp,
                            color = Color.DarkGray,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val quickCities = listOf(
                            "डूँगरा जाट (स्थानीय)", "बुलन्दशहर", "खुर्जा", "नोएडा", "दिल्ली", "मेरठ", "अलीगढ़", "हापुड़", "गाजियाबाद"
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
                                    label = { Text(cName, fontSize = 11.sp) }
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
                                    OutlinedButton(
                                        onClick = { cameraLauncher.launch(null) },
                                        shape = RoundedCornerShape(20.dp),
                                        border = BorderStroke(1.dp, SaffronPrimary),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = if (isHindi) "🔄 पुनः फोटो खींचें (Retake)" else "🔄 Retake Photo",
                                            fontSize = 11.sp,
                                            color = SaffronPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                } else {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = if (isHindi) "फोटो वैकल्पिक है - आप चाहें तो सेल्फी जोड़ सकते हैं" else "Photo is optional - capture if you want face recognition",
                                            fontSize = 11.sp,
                                            color = Color.Gray,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        OutlinedButton(
                                            onClick = { cameraLauncher.launch(null) },
                                            border = BorderStroke(1.dp, SaffronPrimary),
                                            shape = RoundedCornerShape(20.dp),
                                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = if (isHindi) "📷 सेल्फी फोटो जोड़ें (वैकल्पिक)" else "📷 Add Selfie Photo (Optional)",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = MaroonAccent
                                            )
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
                                if (isQuotaExceeded) {
                                    errorMessage = if (isHindi)
                                        "आज के सभी ${settings.maxDailyTokens} टोकन पूरे हो चुके हैं।"
                                        else "Daily token quota reached."
                                    return@Button
                                }
                                if (patientName.isBlank()) {
                                    errorMessage = if (isHindi) "कृपया मरीज का नाम दर्ज करें।" else "Please enter patient name."
                                    return@Button
                                }
                                if (phoneNumber.length < 10) {
                                    errorMessage = if (isHindi) "कृपया 10 अंकों का मोबाइल नंबर दर्ज करें।" else "Please enter valid 10-digit mobile number."
                                    return@Button
                                }
// Photo is now optional for everyone
                                if (isBeforeSchedule) {
                                    val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                                    val scheduledTimeStr = sdf.format(Date(settings.scheduledTokenOpenTimestamp))
                                    errorMessage = if (isHindi)
                                        "टोकन पंजीकरण अभी बंद है। खुलने का समय: $scheduledTimeStr"
                                    else
                                        "Token issuance is not yet open. Scheduled to open at: $scheduledTimeStr"
                                    return@Button
                                }
                                if (!settings.isTokenServiceEnabled) {
                                    errorMessage = if (isHindi)
                                        "रविवार टोकन सेवा वर्तमान में व्यवस्थापक द्वारा स्थगित की गई है।"
                                    else
                                        "Sunday token registration is currently paused by Ashram Administration."
                                    return@Button
                                }
                                if (!isInsideGeofence) {
                                    errorMessage = if (isHindi)
                                        "आप आश्रम परिसर से बाहर हैं। टोकन केवल आश्रम में उपस्थित होने पर मिलेगा।"
                                    else
                                        "You are outside Ashram. Tokens are only issued inside premises."
                                    return@Button
                                }

                                isSubmitting = true
                                errorMessage = null
                                scope.launch {
                                    try {
                                        val loc = GeofenceLocationManager.getLastKnownLocation(context)
                                        val isMock = if (simulateInsideAshram) false else GeofenceLocationManager.isMockLocation(loc, context)
                                        val accuracy = if (loc != null && loc.hasAccuracy()) loc.accuracy else (if (simulateInsideAshram) 5.0f else 200.0f)

                                        val created = repository.registerToken(
                                            patientName = patientName.trim(),
                                            phoneNumber = phoneNumber.trim(),
                                            deviceId = deviceId,
                                            latitude = userLatitude,
                                            longitude = userLongitude,
                                            city = city.trim().ifEmpty { "डूँगरा जाट (स्थानीय)" },
                                            registeredBy = "SELF",
                                            photoUri = capturedPhotoUri,
                                            isMockLocation = isMock,
                                            locationAccuracy = accuracy,
                                            originAddress = originAddress.trim().ifEmpty { city.trim().ifEmpty { "डूँगरा जाट (स्थानीय)" } },
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
                                                    city = city.trim().ifEmpty { "डूँगरा जाट (स्थानीय)" },
                                                    faceVector = vector,
                                                    photoUri = capturedPhotoUri,
                                                    registeredBy = "SELF"
                                                )
                                            } catch (e: Exception) {}
                                        }
                                        existingToken = created
                                    } catch (e: SecurityException) {
                                        errorMessage = e.message ?: "Security Exception: Spoofed Location or Duplicate Device Request Denied."
                                    } catch (e: Exception) {
                                        errorMessage = e.message ?: "Security Exception: Spoofed Location or Duplicate Device Request Denied."
                                    } finally {
                                        isSubmitting = false
                                    }
                                }
                            },
                            enabled = isInsideGeofence && !isSubmitting && !isBeforeSchedule && !isQuotaExceeded && capturedBitmap != null,
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
                                    text = when {
                                        isQuotaExceeded -> if (isHindi) "⛔ आज की सीमा समाप्त" else "⛔ Quota Full"
                                        isBeforeSchedule -> if (isHindi) "🔒 पंजीकरण अभी बंद है" else "🔒 Registration Locked"
                                        capturedBitmap == null -> if (isHindi) "🔒 पहले फोटो खींचें (टोकन अनलॉक होगा)" else "🔒 Capture Photo to Unlock"
                                        else -> if (isHindi) "टोकन प्राप्त करें  ➔" else "Generate Sunday Token  ➔"
                                    },
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
    }
}
