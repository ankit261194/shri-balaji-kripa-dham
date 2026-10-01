package com.example.shribalajikripadham.ui.status

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.shribalajikripadham.data.model.DailySuvichar
import com.example.shribalajikripadham.data.network.StatusSyncManager
import com.example.shribalajikripadham.theme.LocalSacredStyle
import com.example.shribalajikripadham.util.DevoteePhotoHelper
import com.example.shribalajikripadham.util.StatusPosterGenerator
import kotlinx.coroutines.launch
import java.io.InputStream

@Composable
fun CreateStatusDialog(
    initialUserName: String,
    initialUserPhone: String,
    initialUserCity: String,
    deviceId: String,
    suvichar: DailySuvichar,
    isHindi: Boolean,
    onDismiss: () -> Unit,
    onStatusUploaded: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentTheme = LocalSacredStyle.current.theme

    var userName by remember { mutableStateOf(initialUserName.ifBlank { "श्री बालाजी भक्त" }) }
    var userPhone by remember { mutableStateOf(initialUserPhone) }
    var userCity by remember { mutableStateOf(initialUserCity) }
    var customCaption by remember { mutableStateOf("") }
    var devoteeBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isUploading by remember { mutableStateOf(false) }

    // Gallery Picker Launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val input: InputStream? = context.contentResolver.openInputStream(uri)
                val bmp = BitmapFactory.decodeStream(input)
                input?.close()
                if (bmp != null) {
                    devoteeBitmap = DevoteePhotoHelper.toSoftwareBitmap(bmp)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "फ़ोटो लोड करने में त्रुटि", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Camera Selfie Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = com.example.shribalajikripadham.util.TakeFrontPicturePreview()
    ) { bmp: Bitmap? ->
        if (bmp != null) {
            devoteeBitmap = DevoteePhotoHelper.toSoftwareBitmap(bmp)
        }
    }

    // Status Mode: Direct Photo or Bhakti Poster Frame
    var isDirectPhotoMode by remember { mutableStateOf(true) }

    // Selected Deity Wallpaper (Defaults to today's sacred day theme)
    var selectedDeityRes by remember { mutableIntStateOf(StatusPosterGenerator.getDefaultDeityResForDay()) }

    // Live Generated Poster Preview
    var posterPreview by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(isDirectPhotoMode, devoteeBitmap, userName, userCity, customCaption, selectedDeityRes) {
        if (isDirectPhotoMode) {
            if (devoteeBitmap != null) {
                val bmp = StatusPosterGenerator.generateDirectPhotoStatus(
                    context = context,
                    photo = devoteeBitmap!!,
                    devoteeName = userName,
                    devoteeCity = userCity,
                    caption = customCaption
                )
                posterPreview = bmp
            } else {
                posterPreview = null
            }
        } else {
            val bmp = StatusPosterGenerator.generateBhaktiPoster(
                context = context,
                devoteePhoto = devoteeBitmap,
                devoteeName = userName,
                devoteeCity = userCity,
                suvichar = suvichar,
                deityImageRes = selectedDeityRes
            )
            posterPreview = bmp
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = currentTheme.cardShape,
            colors = CardDefaults.cardColors(containerColor = currentTheme.surfaceLight),
            border = BorderStroke(1.5.dp, currentTheme.cardBorderColor),
            elevation = CardDefaults.cardElevation(10.dp),
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.94f)
                .padding(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isHindi) "🚩 भक्ति स्टेटस बनाएं" else "🚩 Create Bhakti Status",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = currentTheme.primaryColor
                        )
                        Text(
                            text = if (isHindi) "अपनी कोई भी फ़ोटो लगाएं या दिव्य फ़्रेम चुनें" else "Post any photo or choose divine holy frame",
                            fontSize = 11.5.sp,
                            color = Color.Gray
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Text("✕", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Mode Selector Tabs (Direct Photo vs Bhakti Frame)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(currentTheme.primaryColor.copy(alpha = 0.08f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        onClick = { isDirectPhotoMode = true },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isDirectPhotoMode) currentTheme.primaryColor else Color.Transparent,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 7.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("📸", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isHindi) "सीधा फ़ोटो स्टेटस" else "Direct Photo",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDirectPhotoMode) Color.White else currentTheme.primaryColor
                            )
                        }
                    }

                    Surface(
                        onClick = { isDirectPhotoMode = false },
                        shape = RoundedCornerShape(10.dp),
                        color = if (!isDirectPhotoMode) currentTheme.primaryColor else Color.Transparent,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 7.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🎨", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isHindi) "भक्ति फ़्रेम पोस्टर" else "Bhakti Frame",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (!isDirectPhotoMode) Color.White else currentTheme.primaryColor
                            )
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = currentTheme.cardBorderColor.copy(alpha = 0.5f))

                // Scrollable Customization Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isDirectPhotoMode) {
                        // MODE 1: DIRECT PHOTO STATUS
                        if (devoteeBitmap == null) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = currentTheme.primaryColor.copy(alpha = 0.05f),
                                border = BorderStroke(1.5.dp, currentTheme.primaryColor.copy(alpha = 0.35f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clickable { galleryLauncher.launch("image/*") }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text("📸", fontSize = 42.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (isHindi) "गैलरी से कोई भी फ़ोटो चुनें" else "Pick any photo from gallery",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = currentTheme.primaryColor,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (isHindi) "आप अपनी कोई भी फ़ोटो, दर्शन या विचार स्टेटस पर लगा सकते हैं" else "Post any photo of your choice to your status",
                                        fontSize = 11.sp,
                                        color = Color.Gray,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color.Black,
                                border = BorderStroke(2.dp, Color(0xFFFFD54F)),
                                modifier = Modifier
                                    .width(220.dp)
                                    .height(340.dp)
                            ) {
                                posterPreview?.let { bmp ->
                                    androidx.compose.foundation.Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = "Status Preview",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } ?: run {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(color = Color(0xFFFFD54F))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Photo Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { galleryLauncher.launch("image/*") },
                                shape = currentTheme.buttonShape,
                                border = BorderStroke(1.dp, currentTheme.primaryColor),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = if (devoteeBitmap == null) "🖼️ फ़ोटो चुनें" else "🔄 फ़ोटो बदलें",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = currentTheme.primaryColor
                                )
                            }

                            Button(
                                onClick = { cameraLauncher.launch(null) },
                                colors = ButtonDefaults.buttonColors(containerColor = currentTheme.primaryColor),
                                shape = currentTheme.buttonShape,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "📸 सेल्फी / कैमरा",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        if (devoteeBitmap != null) {
                            TextButton(onClick = { devoteeBitmap = null }) {
                                Text("❌ फ़ोटो हटाएं", fontSize = 11.5.sp, color = Color.Red)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Custom Caption Input
                        OutlinedTextField(
                            value = customCaption,
                            onValueChange = { customCaption = it },
                            label = { Text(if (isHindi) "स्टेटस संदेश / विचार (वैकल्पिक)" else "Caption / Thought (Optional)") },
                            placeholder = { Text(if (isHindi) "उदा. जय श्री राम, शुभ प्रभात, आज का दिन शुभ हो..." else "e.g. Jai Shri Ram...") },
                            maxLines = 3,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { customCaption = suvichar.quote }) {
                                Text("✨ आज का सुविचार भरें", fontSize = 11.5.sp, color = currentTheme.primaryColor)
                            }
                        }

                    } else {
                        // MODE 2: BHAKTI POSTER FRAME
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color.Black,
                            border = BorderStroke(2.dp, Color(0xFFFFD54F)),
                            modifier = Modifier
                                .width(220.dp)
                                .height(340.dp)
                        ) {
                            posterPreview?.let { bmp ->
                                androidx.compose.foundation.Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "Poster Preview",
                                    modifier = Modifier.fillMaxSize()
                                )
                            } ?: run {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = Color(0xFFFFD54F))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 🪔 Selectable Deity Wallpapers Row
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = if (isHindi) "🪔 भगवान का पावन स्वरूप चुनें:" else "Select Sacred Deity:",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = currentTheme.primaryColor,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(StatusPosterGenerator.DEITY_WALLPAPERS) { deity ->
                                    val isSelected = selectedDeityRes == deity.resId
                                    Card(
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(
                                            if (isSelected) 2.5.dp else 1.dp,
                                            if (isSelected) currentTheme.primaryColor else Color.LightGray.copy(alpha = 0.6f)
                                        ),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) currentTheme.primaryColor.copy(alpha = 0.08f) else Color.White
                                        ),
                                        modifier = Modifier
                                            .width(135.dp)
                                            .clickable { selectedDeityRes = deity.resId }
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.padding(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(62.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                            ) {
                                                androidx.compose.foundation.Image(
                                                    painter = androidx.compose.ui.res.painterResource(id = deity.resId),
                                                    contentDescription = deity.titleHindi,
                                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                                if (isSelected) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .background(Color.Black.copy(alpha = 0.3f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text("✓", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = deity.titleHindi,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) currentTheme.primaryColor else Color(0xFF263238),
                                                textAlign = TextAlign.Center,
                                                maxLines = 2,
                                                lineHeight = 13.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Photo Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { galleryLauncher.launch("image/*") },
                                shape = currentTheme.buttonShape,
                                border = BorderStroke(1.dp, currentTheme.primaryColor),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = if (devoteeBitmap == null) "🖼️ अपनी फ़ोटो जोड़ें" else "🔄 फ़ोटो बदलें",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = currentTheme.primaryColor
                                )
                            }

                            Button(
                                onClick = { cameraLauncher.launch(null) },
                                colors = ButtonDefaults.buttonColors(containerColor = currentTheme.primaryColor),
                                shape = currentTheme.buttonShape,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "📸 सेल्फी लें",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        if (devoteeBitmap != null) {
                            TextButton(onClick = { devoteeBitmap = null }) {
                                Text("❌ अपनी फ़ोटो हटाएं (केवल आश्रम स्टेटस रखें)", fontSize = 11.5.sp, color = Color.Red)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Suvichar Card Highlight
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, currentTheme.cardBorderColor),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "🚩 आज का पावन सुविचार:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = currentTheme.primaryColor
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = suvichar.quote,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF263238)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Devotee Name Input
                    OutlinedTextField(
                        value = userName,
                        onValueChange = { userName = it },
                        label = { Text(if (isHindi) "आपका नाम (Name on Status)" else "Your Name") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Devotee City Input
                    OutlinedTextField(
                        value = userCity,
                        onValueChange = { userCity = it },
                        label = { Text(if (isHindi) "आपका शहर/ग्राम (City / Village)" else "Your City") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Share & Upload Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Action 1: 1-Click WhatsApp Share
                    Button(
                        onClick = {
                            if (isDirectPhotoMode && devoteeBitmap == null) {
                                Toast.makeText(context, "कृपया पहले गैलरी या कैमरा से फ़ोटो चुनें", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            posterPreview?.let { bmp ->
                                val shareCaption = if (isDirectPhotoMode) {
                                    if (customCaption.isNotBlank()) {
                                        "$customCaption\n\n🚩 *श्री बालाजी कृपा धाम (डूँगरा जाट)* 🚩\n👉 धाम का आधिकारिक ऐप डाउनलोड करें:\n🌐 https://shribalajikripadham.online/app"
                                    } else {
                                        "🚩 *श्री बालाजी कृपा धाम (डूँगरा जाट)* 🚩\n👉 धाम का आधिकारिक ऐप डाउनलोड करें:\n🌐 https://shribalajikripadham.online/app"
                                    }
                                } else {
                                    "🚩 *श्री बालाजी कृपा धाम (डूँगरा जाट)* 🚩\n\nआज का दिव्य दर्शन व अमृत सुविचार:\n“${suvichar.quote}”\n\n👉 धाम का आधिकारिक ऐप डाउनलोड करें (यहाँ टच करें):\n🌐 https://shribalajikripadham.online/app"
                                }
                                StatusPosterGenerator.shareToWhatsApp(
                                    context = context,
                                    posterBitmap = bmp,
                                    caption = shareCaption
                                )
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        shape = currentTheme.buttonShape,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 11.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📲", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isHindi) "सीधे व्हाट्सएप स्टेटस पर लगाएं" else "Direct Share to WhatsApp Status",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // Action 2: Post to Ashram App (24 Hours)
                    Button(
                        onClick = {
                            if (isDirectPhotoMode && devoteeBitmap == null) {
                                Toast.makeText(context, "कृपया पहले गैलरी या कैमरा से फ़ोटो चुनें", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            posterPreview?.let { bmp ->
                                isUploading = true
                                val uploadCaption = if (isDirectPhotoMode) {
                                    customCaption.ifBlank { "श्री बालाजी भक्त स्टेटस" }
                                } else {
                                    suvichar.quote
                                }
                                coroutineScope.launch {
                                    val res = StatusSyncManager.uploadDevoteeStatus(
                                        context = context,
                                        deviceId = deviceId,
                                        userName = userName,
                                        phone = userPhone,
                                        city = userCity,
                                        caption = uploadCaption,
                                        bitmap = bmp
                                    )
                                    isUploading = false
                                    if (res.isSuccess) {
                                        Toast.makeText(context, res.getOrNull() ?: "स्टेटस लग गया!", Toast.LENGTH_LONG).show()
                                        onStatusUploaded()
                                        onDismiss()
                                    } else {
                                        Toast.makeText(context, res.exceptionOrNull()?.message ?: "अपलोड विफल", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        },
                        enabled = !isUploading,
                        colors = ButtonDefaults.buttonColors(containerColor = currentTheme.primaryColor),
                        shape = currentTheme.buttonShape,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 11.dp)
                    ) {
                        if (isUploading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🌐", fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isHindi) "धाम ऐप में स्टेटस लगाएं (24 घंटे हेतु)" else "Post to Ashram App (24 Hours)",
                                    fontSize = 13.5.sp,
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
}
