package com.example.shribalajikripadham.ui.admin

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.shribalajikripadham.R
import com.example.shribalajikripadham.data.model.AshramSettings
import com.example.shribalajikripadham.data.network.HostingerCentralSyncManager
import com.example.shribalajikripadham.data.repository.AshramRepository
import com.example.shribalajikripadham.theme.*
import com.example.shribalajikripadham.ui.home.DailyDarshanData
import com.example.shribalajikripadham.ui.home.DailyDarshanHelper
import com.example.shribalajikripadham.util.DevoteePhotoHelper
import com.example.shribalajikripadham.util.TakeAnyPicturePreview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDailyDarshanTab(
    isHindi: Boolean,
    repository: AshramRepository,
    settings: AshramSettings
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var todayDateHindi by remember { mutableStateOf(DailyDarshanHelper.getTodayHindiDate()) }
    var darshanTitle by remember { mutableStateOf("श्री बालाजी महाराज दैनिक दिव्य अलौकिक श्रृंगार दर्शन") }
    var photoUrl by remember { mutableStateOf("https://shribalajikripadham.online/media/balaji_darshan_today.jpg") }
    var guruVicharText by remember { mutableStateOf(DailyDarshanHelper.getTodayGuruVichar()) }
    var viewsCount by remember { mutableIntStateOf(128) }

    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isUploadingPhoto by remember { mutableStateOf(false) }
    var isPublishing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var showVicharPicker by remember { mutableStateOf(false) }
    var vicharSearchQuery by remember { mutableStateOf("") }

    // Fetch existing live darshan details on mount
    LaunchedEffect(Unit) {
        val live = DailyDarshanHelper.fetchTodayDarshan()
        todayDateHindi = live.dateHindi
        darshanTitle = live.title
        photoUrl = live.photoUrl
        guruVicharText = live.quote
        viewsCount = live.viewsCount

        withContext(Dispatchers.IO) {
            try {
                val conn = URL(live.photoUrl).openConnection() as HttpURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                if (conn.responseCode == 200) {
                    val bmp = BitmapFactory.decodeStream(conn.inputStream)
                    if (bmp != null) {
                        withContext(Dispatchers.Main) {
                            previewBitmap = bmp
                        }
                    }
                }
            } catch (ignored: Exception) {}
        }
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = TakeAnyPicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            previewBitmap = bitmap
            val localPath = DevoteePhotoHelper.saveDevoteePhoto(context, bitmap, "darshan")
            if (localPath.isNotBlank()) {
                isUploadingPhoto = true
                statusMessage = if (isHindi) "📸 आज के श्रृंगार की फोटो अपलोड हो रही है..." else "Uploading consecrated photo..."
                scope.launch(Dispatchers.IO) {
                    val cloudUrl = HostingerCentralSyncManager.uploadPhoto(
                        context,
                        localPath,
                        "darshan_${System.currentTimeMillis()}.jpg",
                        "darshan"
                    )
                    withContext(Dispatchers.Main) {
                        isUploadingPhoto = false
                        if (!cloudUrl.isNullOrBlank()) {
                            photoUrl = cloudUrl
                            statusMessage = if (isHindi) "✅ फोटो सुरक्षित हुई! अब 'पब्लिश करें' दबाएं।" else "✅ Consecrated photo uploaded! Click Publish."
                        } else {
                            statusMessage = if (isHindi) "⚠️ फोटो अपलोड में त्रुटि, लोकल सुरक्षित है।" else "Photo upload failed, local saved."
                        }
                    }
                }
            }
        }
    }

    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val bmp = DevoteePhotoHelper.loadBitmap(context, uri.toString())
            if (bmp != null) {
                previewBitmap = bmp
                val localPath = DevoteePhotoHelper.saveDevoteePhoto(context, bmp, "darshan")
                if (localPath.isNotBlank()) {
                    isUploadingPhoto = true
                    statusMessage = if (isHindi) "📁 गैलरी से फोटो अपलोड हो रही है..." else "Uploading gallery photo..."
                    scope.launch(Dispatchers.IO) {
                        val cloudUrl = HostingerCentralSyncManager.uploadPhoto(
                            context,
                            localPath,
                            "darshan_${System.currentTimeMillis()}.jpg",
                            "darshan"
                        )
                        withContext(Dispatchers.Main) {
                            isUploadingPhoto = false
                            if (!cloudUrl.isNullOrBlank()) {
                                photoUrl = cloudUrl
                                statusMessage = if (isHindi) "✅ फोटो सुरक्षित हुई! अब 'पब्लिश करें' दबाएं।" else "✅ Consecrated photo uploaded! Click Publish."
                            } else {
                                statusMessage = if (isHindi) "⚠️ फोटो अपलोड में त्रुटि, लोकल सुरक्षित है।" else "Photo upload failed, local saved."
                            }
                        }
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp)
    ) {
        // Master Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaroonPrimary),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(AmberGold, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🌺", fontSize = 22.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isHindi) "दैनिक दर्शन व गुरु विचार लाइव स्टूडियो" else "Daily Darshan & Guru Vichar Studio",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                            Text(
                                text = "👑 सुपर एडमिन लाइव पब्लिशर पोर्टल",
                                fontSize = 11.5.sp,
                                color = AmberGold
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF2E7D32)
                    ) {
                        Text(
                            text = "🟢 LIVE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = if (isHindi)
                        "यहाँ से आप प्रतिदिन श्री बालाजी महाराज के नए अलौकिक श्रृंगार दर्शन की फोटो, शीर्षक एवं पूज्य गुरुदेव जी का पावन विचार सीधे ऐप एवं वेबसाइट (shribalajikripadham.online) पर 1-क्लिक में लाइव पब्लिश कर सकते हैं।"
                    else
                        "Upload today's consecrated Balaji Shringar photo, customize Darshan title, and broadcast today's Guru Vichar live to both mobile app and website in 1-click.",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    lineHeight = 17.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Live Preview Card (What Devotees See)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.2.dp, AmberGold.copy(alpha = 0.6f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "👁️ लाइव दर्शन पूर्वावलोकन (Preview)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaroonPrimary
                        )
                        Text(
                            text = "📅 $todayDateHindi • 👥 $viewsCount+ दर्शनार्थी",
                            fontSize = 11.5.sp,
                            color = Color.DarkGray
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFFF3E0),
                        border = BorderStroke(1.dp, SaffronPrimary.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "100% सिंक्रनाइज़्ड",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE65100),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Image Preview Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF5F5F5))
                        .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (previewBitmap != null) {
                        Image(
                            bitmap = previewBitmap!!.asImageBitmap(),
                            contentDescription = "दर्शन फोटो",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.app_logo),
                            contentDescription = "दर्शन फोटो",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp)
                        )
                    }

                    if (isUploadingPhoto) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.55f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = AmberGold, strokeWidth = 3.dp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("क्लाउड सर्वर पर अपलोड जारी...", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Bottom ribbon
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                                )
                            )
                            .padding(8.dp)
                    ) {
                        Text(
                            text = darshanTitle,
                            color = Color(0xFFFFD54F),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Current Guru Vichar Box
                Surface(
                    color = Color(0xFFF9F9F9),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val clip = ClipData.newPlainText("Guru Vichar", guruVicharText)
                            (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
                            Toast.makeText(context, if (isHindi) "✨ विचार कॉपी हुआ" else "Copied", Toast.LENGTH_SHORT).show()
                        }
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("✨", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "आज का पावन विचार:",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaroonPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = guruVicharText,
                            fontSize = 12.sp,
                            color = Color(0xFF263238),
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Photo Upload / Camera Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "📸 1. आज के पावन श्रृंगार की नई फोटो सेट करें",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.5.sp,
                    color = MaroonPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { cameraLauncher.launch(null) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary)
                    ) {
                        Text("📸 कैमरा से खींचें", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaroonPrimary),
                        border = BorderStroke(1.dp, MaroonPrimary)
                    ) {
                        Text("📁 गैलरी से चुनें", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = photoUrl,
                    onValueChange = { photoUrl = it },
                    label = { Text("सीधे फोटो URL (Direct Cloud Link)", fontSize = 12.sp) },
                    placeholder = { Text("https://shribalajikripadham.online/media/...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Title & Guru Vichar Editor Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "✍️ 2. दर्शन शीर्षक एवं पावन गुरु विचार",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.5.sp,
                    color = MaroonPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = darshanTitle,
                    onValueChange = { darshanTitle = it },
                    label = { Text("दर्शन मुख्य शीर्षक (Darshan Title)", fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "पावन गुरु विचार व संदेश:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.DarkGray
                    )

                    TextButton(
                        onClick = { showVicharPicker = true }
                    ) {
                        Text(
                            text = "📜 60 गुरु विचारों में से चुनें",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaroonAccent
                        )
                    }
                }

                OutlinedTextField(
                    value = guruVicharText,
                    onValueChange = { guruVicharText = it },
                    label = { Text("आज का पावन गुरु विचार (संपादित करें)") },
                    placeholder = { Text("यहाँ पूज्य गुरुदेव जी का पावन संदेश या विचार लिखें...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    maxLines = 8,
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Status message if any
        statusMessage?.let { msg ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = if (msg.startsWith("✅")) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
            ) {
                Text(
                    text = msg,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (msg.startsWith("✅")) Color(0xFF1B5E20) else Color(0xFFE65100),
                    modifier = Modifier.padding(12.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // 1-Click Master Publish Button
        Button(
            onClick = {
                if (darshanTitle.isBlank() || guruVicharText.isBlank() || photoUrl.isBlank()) {
                    Toast.makeText(context, if (isHindi) "कृपया शीर्षक, फोटो एवं विचार भरें" else "Please fill title, photo and quote", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                isPublishing = true
                statusMessage = null

                scope.launch {
                    try {
                        val (success, serverMsg) = DailyDarshanHelper.updateDailyDarshan(
                            title = darshanTitle.trim(),
                            photoUrl = photoUrl.trim(),
                            quote = guruVicharText.trim()
                        )

                        isPublishing = false
                        if (success) {
                            try {
                                val cacheFile = java.io.File(context.filesDir, "daily_darshan_consecrated.jpg")
                                if (cacheFile.exists()) cacheFile.delete()
                                val prefs = context.getSharedPreferences("daily_darshan_cache_prefs", Context.MODE_PRIVATE)
                                prefs.edit().clear().apply()
                            } catch (ignored: Exception) {}
                            statusMessage = if (isHindi)
                                "✅ आज का अलौकिक दर्शन ऐप एवं वेबसाइट पर तुरंत लाइव पब्लिश हो गया है!"
                            else
                                "✅ Consecrated Daily Darshan is now LIVE across App & Website!"
                            Toast.makeText(context, statusMessage, Toast.LENGTH_LONG).show()
                        } else {
                            statusMessage = "⚠️ पब्लिश त्रुटि: $serverMsg"
                            Toast.makeText(context, statusMessage, Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        isPublishing = false
                        statusMessage = "त्रुटि: ${e.message}"
                        Toast.makeText(context, statusMessage, Toast.LENGTH_SHORT).show()
                    }
                }
            },
            enabled = !isPublishing && !isUploadingPhoto,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary)
        ) {
            if (isPublishing) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text("लाइव पब्लिश हो रहा है...", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            } else {
                Text(
                    text = if (isHindi) "🚀 1-क्लिक लाइव दर्शन पब्लिश करें (Push Live)" else "🚀 1-Click Push Live to App & Website",
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // WhatsApp share test button
        OutlinedButton(
            onClick = {
                val previewData = DailyDarshanData(
                    dateHindi = todayDateHindi,
                    title = darshanTitle,
                    photoUrl = photoUrl,
                    quote = guruVicharText,
                    viewsCount = viewsCount
                )
                DailyDarshanHelper.shareDarshanOnWhatsApp(context, previewData, previewBitmap)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2E7D32)),
            border = BorderStroke(1.2.dp, Color(0xFF2E7D32))
        ) {
            Text(
                text = if (isHindi) "📲 व्हाट्सएप स्टेटस पर टेस्ट शेयर करें (चेक करें)" else "📲 Test Share on WhatsApp Status",
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(30.dp))
    }

    // Dialog: Pick from 60 Guru Vichar Collection
    if (showVicharPicker) {
        Dialog(onDismissRequest = { showVicharPicker = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f),
                shape = RoundedCornerShape(16.dp),
                color = Color.White
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📜 60 पावन गुरु विचार संग्रह",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaroonPrimary
                        )
                        IconButton(onClick = { showVicharPicker = false }) {
                            Text("✕", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = vicharSearchQuery,
                        onValueChange = { vicharSearchQuery = it },
                        placeholder = { Text("विचार खोजें (शब्द लिखें)...", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    val filteredList = DailyDarshanHelper.GURU_VICHAR_COLLECTION.filter {
                        vicharSearchQuery.isBlank() || it.contains(vicharSearchQuery, ignoreCase = true)
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(filteredList) { index, quote ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (guruVicharText == quote) Color(0xFFFFF3E0) else Color(0xFFF9F9F9),
                                border = BorderStroke(
                                    1.dp,
                                    if (guruVicharText == quote) AmberGold else Color(0xFFE0E0E0)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        guruVicharText = quote
                                        showVicharPicker = false
                                        Toast.makeText(context, if (isHindi) "विचार सेट हुआ!" else "Quote selected!", Toast.LENGTH_SHORT).show()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaroonPrimary.copy(alpha = 0.1f),
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("${index + 1}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaroonPrimary)
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = quote,
                                        fontSize = 12.5.sp,
                                        lineHeight = 18.sp,
                                        color = Color(0xFF37474F),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { showVicharPicker = false },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (isHindi) "बंद करें" else "Close", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
