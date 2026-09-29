package com.example.shribalajikripadham.ui.status

import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.shribalajikripadham.data.model.BhaktiStatusItem
import com.example.shribalajikripadham.data.model.DailySuvichar
import com.example.shribalajikripadham.data.model.StatusViewer
import com.example.shribalajikripadham.data.network.StatusSyncManager
import com.example.shribalajikripadham.theme.LocalSacredStyle
import com.example.shribalajikripadham.util.DevoteePhotoHelper
import com.example.shribalajikripadham.util.StatusPosterGenerator
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StatusViewerDialog(
    status: BhaktiStatusItem,
    dailySuvichar: DailySuvichar,
    isAdmin: Boolean,
    currentDeviceId: String,
    currentUserName: String,
    currentUserPhone: String,
    isHindi: Boolean,
    onDismiss: () -> Unit,
    onStatusDeleted: (Long) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentTheme = LocalSacredStyle.current.theme

    // Progress bar animation (8 seconds per status story)
    val progress = remember { Animatable(0f) }
    var isPaused by remember { mutableStateOf(false) }

    // Admin viewer list modal state
    var showViewersDialog by remember { mutableStateOf(false) }
    var viewerList by remember { mutableStateOf<List<StatusViewer>>(emptyList()) }
    var isLoadingViewers by remember { mutableStateOf(false) }

    // Record View on Open
    LaunchedEffect(status.id) {
        StatusSyncManager.recordStatusView(
            statusId = status.id,
            viewerDeviceId = currentDeviceId,
            viewerName = currentUserName,
            viewerPhone = currentUserPhone
        )
    }

    // Auto advance progress bar
    LaunchedEffect(isPaused) {
        if (!isPaused) {
            val remaining = (1f - progress.value)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = (8000 * remaining).toInt().coerceAtLeast(100),
                    easing = LinearEasing
                )
            )
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Main Story Content / Poster Rendering
            var posterBitmap by remember { mutableStateOf<Bitmap?>(null) }

            LaunchedEffect(status.id) {
                // Generate high-resolution poster for this status
                val bmp = StatusPosterGenerator.generateBhaktiPoster(
                    context = context,
                    devoteePhoto = null,
                    devoteeName = status.userName,
                    devoteeCity = status.city,
                    suvichar = dailySuvichar
                )
                posterBitmap = bmp
            }

            posterBitmap?.let { bmp ->
                val safeBmp = remember(bmp) { DevoteePhotoHelper.toSoftwareBitmap(bmp) }
                androidx.compose.foundation.Image(
                    bitmap = safeBmp.asImageBitmap(),
                    contentDescription = "Bhakti Story",
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable {
                            isPaused = !isPaused
                        }
                )
            } ?: run {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFFFFD54F))
                }
            }

            // Top Overlay: Story Progress Bar + Devotee Info Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 14.dp, end = 14.dp)
            ) {
                // Progress Bar
                LinearProgressIndicator(
                    progress = { progress.value },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.5.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.35f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Devotee Info Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = currentTheme.primaryColor,
                            border = BorderStroke(1.5.dp, Color(0xFFFFD54F)),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(if (status.isOfficial) "🚩" else "🙏", fontSize = 18.sp)
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = status.userName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                if (status.isOfficial) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFF2E7D32)
                                    ) {
                                        Text(
                                            text = "आधिकारिक",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                            val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(status.createdAt * 1000))
                            Text(
                                text = if (status.city.isNotBlank()) "${status.city} • $timeStr" else timeStr,
                                fontSize = 11.5.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }

                    // Admin Delete Button
                    if (isAdmin && !status.isOfficial) {
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    val res = StatusSyncManager.deleteStatus(status.id, currentDeviceId)
                                    if (res.isSuccess) {
                                        Toast.makeText(context, "स्टेटस हटा दिया गया", Toast.LENGTH_SHORT).show()
                                        onStatusDeleted(status.id)
                                        onDismiss()
                                    } else {
                                        Toast.makeText(context, res.exceptionOrNull()?.message ?: "हटाने में त्रुटि", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        ) {
                            Text("🗑️", fontSize = 18.sp)
                        }
                    }

                    // Close Button
                    IconButton(onClick = onDismiss) {
                        Text("✕", fontSize = 20.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Bottom Action Bar: WhatsApp Status Share + Admin Viewers Button
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
                    .padding(16.dp)
            ) {
                // Admin Viewers Tracker Strip
                if (isAdmin) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, Color(0xFFFFD54F).copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                isPaused = true
                                showViewersDialog = true
                                isLoadingViewers = true
                                coroutineScope.launch {
                                    val res = StatusSyncManager.fetchStatusViewers(status.id)
                                    viewerList = res.getOrDefault(emptyList())
                                    isLoadingViewers = false
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "👁️ ${status.viewsCount} भक्तों ने देखा (Admin Viewers List)",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFD54F)
                            )
                            Text("देखें ➔", fontSize = 12.sp, color = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // 1-Click WhatsApp Status Button
                Button(
                    onClick = {
                        posterBitmap?.let { bmp ->
                            StatusPosterGenerator.shareToWhatsApp(
                                context = context,
                                posterBitmap = bmp,
                                caption = "🚩 *श्री बालाजी कृपा धाम (डूँगरा जाट)* 🚩\n\nआज का दिव्य दर्शन व अमृत सुविचार:\n“${dailySuvichar.quote}”\n\nधाम का आधिकारिक मोबाइल ऐप डाउनलोड करें:\n🌐 https://shribalajikripadham.online/download.php"
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📲", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isHindi) "व्हाट्सएप स्टेटस पर लगाएं (1-Click Status)" else "Share to WhatsApp Status",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }
            }

            // Admin Viewers List Dialog
            if (showViewersDialog) {
                Dialog(onDismissRequest = {
                    showViewersDialog = false
                    isPaused = false
                }) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.7f)
                            .padding(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "👁️ स्टेटस देखने वाले भक्त",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = currentTheme.primaryColor
                                )
                                IconButton(onClick = {
                                    showViewersDialog = false
                                    isPaused = false
                                }) {
                                    Text("✕", fontWeight = FontWeight.Bold)
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                            if (isLoadingViewers) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = currentTheme.primaryColor)
                                }
                            } else if (viewerList.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "अभी तक किसी भक्त ने यह स्टेटस नहीं देखा है।",
                                        fontSize = 13.sp,
                                        color = Color.Gray
                                    )
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(viewerList) { viewer ->
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color(0xFFF5F5F5),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column {
                                                    Text(
                                                        text = viewer.viewerName,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp,
                                                        color = Color(0xFF212121)
                                                    )
                                                    if (viewer.viewerPhone.isNotBlank()) {
                                                        Text(
                                                            text = viewer.viewerPhone,
                                                            fontSize = 11.5.sp,
                                                            color = Color(0xFF757575)
                                                        )
                                                    }
                                                }
                                                val seenTime = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(viewer.viewedAt * 1000))
                                                Text(
                                                    text = seenTime,
                                                    fontSize = 10.5.sp,
                                                    color = Color.Gray
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
