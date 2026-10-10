package com.example.shribalajikripadham.ui.home.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.shribalajikripadham.R
import com.example.shribalajikripadham.theme.SacredTheme
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

/**
 * Pro-Tier Daily Darshan Hero Card.
 * Clean, consecrated presentation of Balaji Maharaj's daily darshan using native Android Bitmap rendering.
 */
@Composable
fun DailyDarshanHeroCard(
    isHindi: Boolean,
    currentTheme: SacredTheme,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var photoUrl by remember { mutableStateOf("") }
    var darshanBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var darshanDateStr by remember { mutableStateOf("") }
    var viewsCount by remember { mutableIntStateOf(0) }
    var isRefreshing by remember { mutableStateOf(false) }
    var showFullscreenDialog by remember { mutableStateOf(false) }

    val todayStr = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date())
    }

    fun loadDarshan(forceRefresh: Boolean = false) {
        scope.launch {
            if (forceRefresh) isRefreshing = true
            withContext(Dispatchers.IO) {
                try {
                    val cacheFile = File(context.filesDir, "daily_darshan_consecrated.jpg")
                    if (forceRefresh && cacheFile.exists()) {
                        cacheFile.delete()
                    }

                    // 1. If cached bitmap exists and not force refresh, use it immediately
                    if (!forceRefresh && cacheFile.exists() && cacheFile.length() > 0) {
                        val cachedBmp = BitmapFactory.decodeFile(cacheFile.absolutePath)
                        if (cachedBmp != null) {
                            withContext(Dispatchers.Main) {
                                darshanBitmap = cachedBmp
                            }
                        }
                    }

                    // 2. Fetch fresh API info
                    val apiUrl = "https://shribalajikripadham.online/api/daily_darshan.php?action=get_latest" +
                            if (forceRefresh) "&cb=${System.currentTimeMillis()}" else ""
                    val conn = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 6000
                        readTimeout = 6000
                        requestMethod = "GET"
                    }
                    if (conn.responseCode in 200..299) {
                        val body = conn.inputStream.bufferedReader().use { it.readText() }
                        val json = JSONObject(body)
                        if (json.optBoolean("success", false) || json.has("data")) {
                            val data = if (json.has("data")) json.getJSONObject("data") else json
                            val rawUrl = data.optString("photo_url", "")
                            val finalUrl = when {
                                rawUrl.startsWith("http") -> rawUrl
                                rawUrl.isNotBlank() -> "https://shribalajikripadham.online/$rawUrl"
                                else -> "https://shribalajikripadham.online/uploads/darshan_latest.jpg"
                            }
                            photoUrl = finalUrl
                            darshanDateStr = data.optString("darshan_date", todayStr)
                            viewsCount = data.optInt("views_count", 0)

                            // 3. Download fresh image if forceRefresh or bitmap is null
                            if (forceRefresh || darshanBitmap == null) {
                                val imgConn = (URL("$finalUrl?cb=${System.currentTimeMillis()}").openConnection() as HttpURLConnection).apply {
                                    connectTimeout = 8000
                                    readTimeout = 8000
                                }
                                if (imgConn.responseCode in 200..299) {
                                    val bmp = BitmapFactory.decodeStream(imgConn.inputStream)
                                    if (bmp != null) {
                                        try {
                                            FileOutputStream(cacheFile).use { fos ->
                                                bmp.compress(Bitmap.CompressFormat.JPEG, 92, fos)
                                            }
                                        } catch (ignored: Exception) {}
                                        withContext(Dispatchers.Main) {
                                            darshanBitmap = bmp
                                        }
                                    }
                                }
                                imgConn.disconnect()
                            }
                        }
                    }
                    conn.disconnect()
                } catch (e: Exception) {
                    // Fallback to local default image if nothing is loaded
                    if (darshanBitmap == null) {
                        val defBmp = BitmapFactory.decodeResource(context.resources, R.drawable.img_balaji_darshan)
                        withContext(Dispatchers.Main) {
                            darshanBitmap = defBmp
                        }
                    }
                } finally {
                    withContext(Dispatchers.Main) {
                        isRefreshing = false
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        loadDarshan(forceRefresh = false)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = currentTheme.surfaceLight),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, currentTheme.cardBorderColor.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header Row: Title + Date + Views + Refresh
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isHindi) "🚩 आज का अलौकिक दर्शन" else "🚩 Divine Daily Darshan",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = currentTheme.primaryColor
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Genuine Views Count Badge
                    if (viewsCount > 0) {
                        Surface(
                            color = currentTheme.primaryColor.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "👁️ $viewsCount",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = currentTheme.primaryColor,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // 1-Touch Refresh
                    IconButton(
                        onClick = { loadDarshan(forceRefresh = true) },
                        modifier = Modifier.size(30.dp)
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = currentTheme.primaryColor
                            )
                        } else {
                            Text("🔄", fontSize = 14.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main Darshan Photo Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1A1A1A))
                    .clickable { showFullscreenDialog = true },
                contentAlignment = Alignment.Center
            ) {
                val currentBmp = darshanBitmap
                if (currentBmp != null) {
                    Image(
                        bitmap = currentBmp.asImageBitmap(),
                        contentDescription = "Daily Darshan",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.app_logo),
                        contentDescription = "Placeholder",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(80.dp)
                    )
                }

                // Bottom Gradient Scrim with View Hint
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                            )
                        ),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isHindi) "॥ ॐ श्री हनुमते नमः ॥" else "॥ Om Shri Hanumate Namah ॥",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD700)
                        )
                        Text(
                            text = if (isHindi) "🔍 दर्शन विस्तार हेतु टैप करें" else "🔍 Tap to view fullscreen",
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons Row: Fullscreen, Share
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showFullscreenDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (isHindi) "🔍 दर्शन करें" else "🔍 View",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Button(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            var shareUri: Uri? = null
                            try {
                                val cacheFile = File(context.filesDir, "daily_darshan_consecrated.jpg")
                                val bmpToShare = darshanBitmap ?: if (cacheFile.exists() && cacheFile.length() > 0) {
                                    BitmapFactory.decodeFile(cacheFile.absolutePath)
                                } else {
                                    BitmapFactory.decodeResource(context.resources, R.drawable.app_logo)
                                }

                                if (bmpToShare != null) {
                                    val shareDir = File(context.cacheDir, "darshan_shares")
                                    if (!shareDir.exists()) shareDir.mkdirs()
                                    val shareFile = File(shareDir, "Darshan_${System.currentTimeMillis()}.jpg")
                                    FileOutputStream(shareFile).use { fos ->
                                        bmpToShare.compress(Bitmap.CompressFormat.JPEG, 92, fos)
                                        fos.flush()
                                    }
                                    shareUri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        shareFile
                                    )
                                }
                            } catch (e: Exception) {
                                shareUri = null
                            }

                            withContext(Dispatchers.Main) {
                                val shareCaption = if (isHindi) {
                                    "🚩 *श्री बालाजी कृपा धाम, डूँगरा जाट* 🚩\n" +
                                    "*परम पूज्य गुरुजी तेजवीर सिंह जी*\n" +
                                    "══════════════════════════\n" +
                                    "🌺 *आज का पावन अलौकिक श्रृंगार दर्शन* 🌺\n" +
                                    "📅 *तिथि:* $darshanDateStr\n" +
                                    "══════════════════════════\n" +
                                    "🙏 *भूत-प्रेत व मानसिक समस्याओं का पूर्णतः निःशुल्क इलाज*\n" +
                                    "📲 *रविवार टोकन व लाइव दर्शन हेतु ऐप:* https://shribalajikripadham.online/app"
                                } else {
                                    "🚩 *Shri Balaji Kripa Dham, Dungra Jaat* 🚩\n" +
                                    "*Param Poojya Guruji Tejveer Singh Ji*\n" +
                                    "══════════════════════════\n" +
                                    "🌺 *Today's Divine Alokik Darshan* 🌺\n" +
                                    "📅 *Date:* $darshanDateStr\n" +
                                    "══════════════════════════\n" +
                                    "📲 *Official App:* https://shribalajikripadham.online/app"
                                }

                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    if (shareUri != null) {
                                        type = "image/jpeg"
                                        putExtra(Intent.EXTRA_STREAM, shareUri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    } else {
                                        type = "text/plain"
                                    }
                                    putExtra(Intent.EXTRA_TEXT, shareCaption)
                                    putExtra(Intent.EXTRA_SUBJECT, if (isHindi) "आज का पावन अलौकिक दर्शन" else "Divine Darshan")
                                }
                                try {
                                    context.startActivity(Intent.createChooser(shareIntent, if (isHindi) "अलौकिक दर्शन शेयर करें" else "Share Sacred Darshan"))
                                } catch (e: Exception) {
                                    Toast.makeText(context, if (isHindi) "शेयर करने में असमर्थ" else "Unable to share", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = currentTheme.primaryColor),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (isHindi) "📲 शेयर करें" else "📲 Share",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }

    // Fullscreen Zoom Dialog
    if (showFullscreenDialog) {
        Dialog(
            onDismissRequest = { showFullscreenDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                val fullBmp = darshanBitmap
                if (fullBmp != null) {
                    Image(
                        bitmap = fullBmp.asImageBitmap(),
                        contentDescription = "Full Darshan",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.app_logo),
                        contentDescription = "Full Darshan",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Close Button
                IconButton(
                    onClick = { showFullscreenDialog = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                ) {
                    Text("✕", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
