package com.example.shribalajikripadham.ui.home

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.shribalajikripadham.R
import com.example.shribalajikripadham.data.model.AshramSettings
import com.example.shribalajikripadham.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

data class DailyDarshanData(
    val dateHindi: String,
    val title: String,
    val photoUrl: String,
    val quote: String,
    val viewsCount: Int
)

object DailyDarshanHelper {
    private val sdfHindiMonth = arrayOf(
        "जनवरी", "फ़रवरी", "मार्च", "अप्रैल", "मई", "जून",
        "जुलाई", "अगस्त", "सितम्बर", "अक्टूबर", "नवम्बर", "दिसम्बर"
    )

    fun getTodayHindiDate(): String {
        val cal = Calendar.getInstance()
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val month = sdfHindiMonth[cal.get(Calendar.MONTH)]
        val year = cal.get(Calendar.YEAR)
        return "$day $month $year"
    }

    suspend fun fetchTodayDarshan(): DailyDarshanData = withContext(Dispatchers.IO) {
        val fallback = DailyDarshanData(
            dateHindi = getTodayHindiDate(),
            title = "श्री बालाजी महाराज दैनिक दिव्य अलौकिक श्रृंगार दर्शन",
            photoUrl = "https://shribalajikripadham.online/media/balaji_darshan_today.jpg",
            quote = "कवन सो काज कठिन जग माहीं । जो नहिं होत तात तुम्ह पाहीं ॥",
            viewsCount = 1280
        )

        try {
            val url = URL("https://shribalajikripadham.online/backend/api/daily_darshan.php")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 4000
                readTimeout = 4000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "ShriBalajiKripaDham-Android")
            }

            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                if (json.optBoolean("success", false)) {
                    DailyDarshanData(
                        dateHindi = json.optString("date_hindi", fallback.dateHindi),
                        title = json.optString("title", fallback.title),
                        photoUrl = json.optString("photo_url", fallback.photoUrl),
                        quote = json.optString("blessings_quote", fallback.quote),
                        viewsCount = json.optInt("views_count", fallback.viewsCount)
                    )
                } else fallback
            } else fallback
        } catch (e: Exception) {
            fallback
        }
    }

    fun shareDarshanOnWhatsApp(
        context: Context,
        darshan: DailyDarshanData,
        bitmap: Bitmap? = null
    ) {
        try {
            var imageUri: Uri? = null
            if (bitmap != null) {
                val shareDir = File(context.cacheDir, "darshan_shares")
                if (!shareDir.exists()) shareDir.mkdirs()
                val shareFile = File(shareDir, "Darshan_${System.currentTimeMillis()}.png")
                FileOutputStream(shareFile).use { fos ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 95, fos)
                    fos.flush()
                }
                imageUri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    shareFile
                )
            }

            val shareText = """
🚩 *श्री बालाजी कृपा धाम, डूँगरा जाट* 🚩
*परम पूज्य गुरुजी तेजवीर सिंह जी*
══════════════════════════
🌺 *आज का पावन अलौकिक श्रृंगार दर्शन* 🌺
📅 *तिथि:* ${darshan.dateHindi}
✨ *दैनिक पावन आशीर्वाद:*
"${darshan.quote}"
══════════════════════════
🙏 *भूत-प्रेत व असाध्य मानसिक समस्याओं का पूर्णतः निःशुल्क इलाज।*
🌐 लाइव दर्शन व रविवार टोकन हेतु ऐप डाउनलोड करें:
https://shribalajikripadham.online/downloads/ShriBalajiKripaDham-release.apk
            """.trimIndent()

            val whatsappIntent = Intent(Intent.ACTION_SEND).apply {
                if (imageUri != null) {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, imageUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } else {
                    type = "text/plain"
                }
                putExtra(Intent.EXTRA_TEXT, shareText)
                setPackage("com.whatsapp")
            }

            try {
                context.startActivity(whatsappIntent)
            } catch (e: Exception) {
                try {
                    val businessIntent = Intent(Intent.ACTION_SEND).apply {
                        if (imageUri != null) {
                            type = "image/png"
                            putExtra(Intent.EXTRA_STREAM, imageUri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        } else {
                            type = "text/plain"
                        }
                        putExtra(Intent.EXTRA_TEXT, shareText)
                        setPackage("com.whatsapp.w4b")
                    }
                    context.startActivity(businessIntent)
                } catch (e2: Exception) {
                    val chooser = Intent(Intent.ACTION_SEND).apply {
                        if (imageUri != null) {
                            type = "image/png"
                            putExtra(Intent.EXTRA_STREAM, imageUri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        } else {
                            type = "text/plain"
                        }
                        putExtra(Intent.EXTRA_SUBJECT, "आज का पावन अलौकिक दर्शन")
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    context.startActivity(Intent.createChooser(chooser, "अलौकिक दर्शन शेयर करें"))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "दर्शन शेयर करने में त्रुटि: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun DailyDarshanQuickCard(
    isHindi: Boolean,
    ashramSettings: AshramSettings,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var darshanData by remember {
        mutableStateOf(
            DailyDarshanData(
                dateHindi = DailyDarshanHelper.getTodayHindiDate(),
                title = "श्री बालाजी महाराज दैनिक दिव्य अलौकिक श्रृंगार दर्शन",
                photoUrl = "https://shribalajikripadham.online/media/balaji_darshan_today.jpg",
                quote = "कवन सो काज कठिन जग माहीं । जो नहिं होत तात तुम्ह पाहीं ॥",
                viewsCount = 1280
            )
        )
    }

    var remoteBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showZoomDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val fetched = DailyDarshanHelper.fetchTodayDarshan()
        darshanData = fetched

        // Try downloading remote bitmap
        withContext(Dispatchers.IO) {
            try {
                val conn = URL(fetched.photoUrl).openConnection() as HttpURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                if (conn.responseCode == 200) {
                    val bmp = BitmapFactory.decodeStream(conn.inputStream)
                    if (bmp != null) {
                        withContext(Dispatchers.Main) {
                            remoteBitmap = bmp
                        }
                    }
                }
            } catch (ignored: Exception) {}
        }
    }

    // Sacred pulsing glow animation
    val infiniteTransition = rememberInfiniteTransition(label = "DarshanPulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowAlpha"
    )

    val goldBorderBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFFD4AF37),
            Color(0xFFFFD700).copy(alpha = glowAlpha),
            Color(0xFFFF8C00),
            Color(0xFFD4AF37)
        )
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(20.dp))
            .border(BorderStroke(2.dp, goldBorderBrush), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFFFBF5),
                            Color(0xFFFFF3E0),
                            Color(0xFFFFF8E7)
                        )
                    )
                )
                .padding(14.dp)
        ) {
            // Header: Sacred Title + Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(Color(0xFFFFE082), CircleShape)
                            .border(1.dp, Color(0xFFFFA000), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🌺", fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isHindi) "आज का पावन अलौकिक दर्शन" else "Today's Sacred Darshan",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = MaroonAccent
                        )
                        Text(
                            text = darshanData.dateHindi,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF795548)
                        )
                    }
                }

                // Devotee count tag
                Surface(
                    color = Color(0xFF800000).copy(alpha = 0.08f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFD4AF37).copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "👁️ ${darshanData.viewsCount}+ भक्त",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaroonAccent,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sacred Deity Image Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(BorderStroke(2.dp, Color(0xFFFFD700)), RoundedCornerShape(16.dp))
                    .clickable { showZoomDialog = true }
            ) {
                if (remoteBitmap != null) {
                    Image(
                        bitmap = remoteBitmap!!.asImageBitmap(),
                        contentDescription = "श्री बालाजी अलौकिक श्रृंगार दर्शन",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.app_logo),
                        contentDescription = "श्री बालाजी अलौकिक श्रृंगार दर्शन",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF4A0000))
                            .padding(16.dp)
                    )
                }

                // Bottom gradient with caption & zoom hint
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                            )
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🚩 ॥ श्री हनुमते नमः ॥",
                            color = Color(0xFFFFD700),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "🔍 स्पर्श कर बड़ा देखें",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Auspicious Chaupai / Blessing
            Surface(
                color = Color.White.copy(alpha = 0.9f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color(0xFFFFE082)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("✨", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = darshanData.quote,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF424242),
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons: Zoom & WhatsApp Share
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showZoomDialog = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaroonAccent),
                    border = BorderStroke(1.dp, MaroonAccent)
                ) {
                    Text(
                        text = if (isHindi) "🔍 दर्शन बड़ा करें" else "Zoom Darshan",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = {
                        DailyDarshanHelper.shareDarshanOnWhatsApp(
                            context = context,
                            darshan = darshanData,
                            bitmap = remoteBitmap
                        )
                    },
                    modifier = Modifier.weight(1.3f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF25D366),
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("💬", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isHindi) "WhatsApp शेयर" else "Share Darshan",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }

    // Full Screen Zoom Dialog
    if (showZoomDialog) {
        DailyDarshanZoomDialog(
            darshanData = darshanData,
            bitmap = remoteBitmap,
            onDismiss = { showZoomDialog = false },
            onShare = {
                DailyDarshanHelper.shareDarshanOnWhatsApp(
                    context = context,
                    darshan = darshanData,
                    bitmap = remoteBitmap
                )
            }
        )
    }
}

@Composable
fun DailyDarshanZoomDialog(
    darshanData: DailyDarshanData,
    bitmap: Bitmap?,
    onDismiss: () -> Unit,
    onShare: () -> Unit
) {
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xF0000000))
        ) {
            // Top Bar with Close Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "🚩 श्री बालाजी अलौकिक दर्शन",
                        color = Color(0xFFFFD700),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = darshanData.dateHindi,
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.background(Color.White.copy(alpha = 0.2f), CircleShape)
                ) {
                    Text("✕", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Central Zoomable Deity Image
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 80.dp)
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 4f)
                            if (scale > 1f) {
                                offsetX += pan.x
                                offsetY += pan.y
                            } else {
                                offsetX = 0f
                                offsetY = 0f
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "दर्शन",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offsetX,
                                translationY = offsetY
                            )
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.app_logo),
                        contentDescription = "दर्शन",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offsetX,
                                translationY = offsetY
                            )
                    )
                }
            }

            // Bottom Floating Bar: Blessing Quote & WhatsApp Share Button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.95f))
                        )
                    )
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "\"${darshanData.quote}\"",
                    color = Color(0xFFFFE082),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onShare,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF25D366),
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("💬", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "WhatsApp पर यह पावन दर्शन शेयर करें",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
