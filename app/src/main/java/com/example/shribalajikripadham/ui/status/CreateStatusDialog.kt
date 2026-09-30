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

    // Live Generated Poster Preview
    var posterPreview by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(devoteeBitmap, userName, userCity) {
        val bmp = StatusPosterGenerator.generateBhaktiPoster(
            context = context,
            devoteePhoto = devoteeBitmap,
            devoteeName = userName,
            devoteeCity = userCity,
            suvichar = suvichar
        )
        posterPreview = bmp
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
                .fillMaxHeight(0.92f)
                .padding(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isHindi) "🚩 दिव्य स्टेटस व पोस्टर मेकर" else "🚩 Bhakti Status Frame Maker",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = currentTheme.primaryColor
                        )
                        Text(
                            text = if (isHindi) "बालाजी महाराज व गुरुदेव के साथ अपना स्टेटस लगाएं" else "Create personalized WhatsApp story with Balaji",
                            fontSize = 11.5.sp,
                            color = Color.Gray
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Text("✕", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Gray)
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
                    // Frame Preview Box
                    Surface(
                        shape = RoundedCornerShape(16.dp),
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

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Share & Upload Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Action 1: 1-Click WhatsApp Share
                    Button(
                        onClick = {
                            posterPreview?.let { bmp ->
                                StatusPosterGenerator.shareToWhatsApp(
                                    context = context,
                                    posterBitmap = bmp,
                                    caption = "🚩 *श्री बालाजी कृपा धाम (डूँगरा जाट)* 🚩\n\nआज का दिव्य दर्शन व अमृत सुविचार:\n“${suvichar.quote}”\n\n👉 धाम का आधिकारिक ऐप डाउनलोड करें (यहाँ टच करें):\n🌐 https://shribalajikripadham.online/app"
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
                            posterPreview?.let { bmp ->
                                isUploading = true
                                coroutineScope.launch {
                                    val res = StatusSyncManager.uploadDevoteeStatus(
                                        context = context,
                                        deviceId = deviceId,
                                        userName = userName,
                                        phone = userPhone,
                                        city = userCity,
                                        caption = suvichar.quote,
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
