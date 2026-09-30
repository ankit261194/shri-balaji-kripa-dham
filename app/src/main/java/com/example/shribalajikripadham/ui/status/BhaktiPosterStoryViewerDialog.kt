package com.example.shribalajikripadham.ui.status

import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.shribalajikripadham.data.model.DailySuvichar
import com.example.shribalajikripadham.theme.LocalSacredStyle
import com.example.shribalajikripadham.util.DevoteePhotoHelper
import com.example.shribalajikripadham.util.StatusPosterGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun BhaktiPosterStoryViewerDialog(
    posterType: String,
    dailySuvichar: DailySuvichar,
    isHindi: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentTheme = LocalSacredStyle.current.theme

    // Story progress animation (12 seconds)
    val progress = remember { Animatable(0f) }
    var isPaused by remember { mutableStateOf(false) }

    var posterBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isGenerating by remember { mutableStateOf(true) }

    val (icon, title, subtitle, caption) = remember(posterType, dailySuvichar) {
        when (posterType) {
            "DARSHAN" -> Quadruple(
                "🪔",
                if (isHindi) "श्री बालाजी दिव्य दर्शन" else "Daily Consecrated Darshan",
                if (isHindi) "आज का पावन बाल स्वरूप दर्शन" else "Today's Sacred Darshan",
                "🚩 *श्री बालाजी महाराज दिव्य दर्शन* 🚩\n\nश्री बालाजी कृपा धाम, डूँगरा जाट\n\n👉 धाम का आधिकारिक मोबाइल ऐप डाउनलोड करें:\n🌐 https://shribalajikripadham.online/app"
            )
            "CHALISA" -> Quadruple(
                "🚩",
                if (isHindi) "श्री हनुमान चालीसा" else "Shri Hanuman Chalisa",
                if (isHindi) "नित्य पाठ • सकल शुभ सिद्ध करैं हनुमान" else "Daily Sacred Recitation",
                "🚩 *श्री हनुमान चालीसा - नित्य पाठ* 🚩\n\nजो यह पढ़ै हनुमान चालीसा।\nहोय सिद्धि साखी गौरीसा॥\n\n👉 धाम का आधिकारिक ऐप डाउनलोड करें:\n🌐 https://shribalajikripadham.online/app"
            )
            "BAJRANG_BAAN" -> Quadruple(
                "🛡️",
                if (isHindi) "श्री बजरंग बाण" else "Shri Bajrang Baan",
                if (isHindi) "संकटमोचन महा-रक्षा कवच" else "Protection Shield",
                "🛡️ *श्री बजरंग बाण - संकटमोचन रक्षा कवच* 🛡️\n\nनिश्चय प्रेम प्रतीति ते, बिनय करैं सनमान।\nतेहि के कारज सकल शुभ, सिद्ध करैं हनुमान॥\n\n👉 धाम का आधिकारिक ऐप डाउनलोड करें:\n🌐 https://shribalajikripadham.online/app"
            )
            else -> Quadruple(
                "🌅",
                if (isHindi) "आज का अमृत सुविचार" else "Today's Sacred Quote",
                if (isHindi) "परम पूज्य गुरुदेव कृपा विचार" else "Gurudev Divine Thought",
                "🌅 *आज का अमृत सुविचार* 🌅\n\n“${dailySuvichar.quote}”\n\n— श्री बालाजी कृपा धाम, डूँगरा जाट\n👉 धाम का आधिकारिक ऐप डाउनलोड करें:\n🌐 https://shribalajikripadham.online/app"
            )
        }
    }

    // Generate high resolution poster in background
    LaunchedEffect(posterType) {
        isGenerating = true
        val bmp = withContext(Dispatchers.Default) {
            when (posterType) {
                "DARSHAN" -> StatusPosterGenerator.generateDarshanPoster(
                    context = context,
                    quoteText = dailySuvichar.quote.ifBlank { "संकट कटे मिटे सब पीरा, जो सुमिरै हनुमत बलबीरा।" }
                )
                "CHALISA" -> StatusPosterGenerator.generateChalisaPoster(context)
                "BAJRANG_BAAN" -> StatusPosterGenerator.generateBajrangBaanPoster(context)
                else -> StatusPosterGenerator.generateBhaktiPoster(
                    context = context,
                    devoteePhoto = null,
                    devoteeName = "श्री बालाजी कृपा धाम",
                    devoteeCity = "डूँगरा जाट",
                    suvichar = dailySuvichar
                )
            }
        }
        posterBitmap = bmp
        isGenerating = false
    }

    // Progress bar auto advance
    LaunchedEffect(isPaused, isGenerating) {
        if (!isPaused && !isGenerating) {
            val remaining = (1f - progress.value)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = (12000 * remaining).toInt().coerceAtLeast(100),
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
                .background(Color(0xFF0F070A))
        ) {
            // Main Poster Image Display
            if (isGenerating || posterBitmap == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFFFFD54F), strokeWidth = 3.dp)
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (isHindi) "दिव्य HD स्टेटस पोस्टर तैयार हो रहा है..." else "Generating sacred HD poster...",
                            color = Color(0xFFFFE082),
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            } else {
                val safeBmp = remember(posterBitmap) { DevoteePhotoHelper.toSoftwareBitmap(posterBitmap!!) }
                Image(
                    bitmap = safeBmp.asImageBitmap(),
                    contentDescription = title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    isPaused = true
                                    tryAwaitRelease()
                                    isPaused = false
                                }
                            )
                        }
                )
            }

            // Top Overlay: Story Progress Bar + Title Header
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent)
                        )
                    )
                    .padding(top = 16.dp, start = 14.dp, end = 14.dp, bottom = 20.dp)
            ) {
                // Progress Bar
                LinearProgressIndicator(
                    progress = { progress.value },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.5.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = Color(0xFFFFD54F),
                    trackColor = Color.White.copy(alpha = 0.35f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Header Info Bar
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
                                Text(icon, fontSize = 20.sp)
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = title,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = subtitle,
                                fontSize = 11.5.sp,
                                color = Color(0xFFFFE082)
                            )
                        }
                    }

                    // Close Button
                    IconButton(onClick = onDismiss) {
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.5f),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("✕", fontSize = 17.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Bottom Action Bar: 1-Click WhatsApp Status & Save to Gallery
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.92f))
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 18.dp)
            ) {
                // Primary Action: 1-Click WhatsApp Status
                Button(
                    onClick = {
                        isPaused = true
                        posterBitmap?.let { bmp ->
                            StatusPosterGenerator.shareToWhatsApp(
                                context = context,
                                posterBitmap = bmp,
                                caption = caption
                            )
                        }
                    },
                    enabled = !isGenerating && posterBitmap != null,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                    shape = RoundedCornerShape(14.dp),
                    elevation = ButtonDefaults.buttonElevation(6.dp),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 13.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📲", fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isHindi) "सीधे WhatsApp Status पर लगाएं" else "Set as WhatsApp Status",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Secondary Action: Save to Gallery
                OutlinedButton(
                    onClick = {
                        isPaused = true
                        posterBitmap?.let { bmp ->
                            val uri = StatusPosterGenerator.savePosterToGallery(
                                context = context,
                                posterBitmap = bmp,
                                title = posterType
                            )
                            if (uri != null) {
                                Toast.makeText(
                                    context,
                                    if (isHindi) "✅ फोटो आपकी गैलरी (Gallery) में सेव हो गई!" else "Saved to Gallery!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    context,
                                    if (isHindi) "गैलरी में सेव नहीं हो सका" else "Failed to save to Gallery",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    },
                    enabled = !isGenerating && posterBitmap != null,
                    border = BorderStroke(1.dp, Color(0xFFFFD54F).copy(alpha = 0.7f)),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White.copy(alpha = 0.12f),
                        contentColor = Color(0xFFFFD54F)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 9.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⬇️", fontSize = 15.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isHindi) "गैलरी में सेव करें (Save Image)" else "Save to Gallery",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFFFD54F)
                        )
                    }
                }
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
