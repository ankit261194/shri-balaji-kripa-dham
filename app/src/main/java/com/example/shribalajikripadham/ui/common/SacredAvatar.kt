package com.example.shribalajikripadham.ui.common

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shribalajikripadham.theme.GoldSecondary
import com.example.shribalajikripadham.theme.SaffronPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.window.Dialog

@Composable
fun SacredAvatar(
    photoUri: String,
    fallbackText: String = "",
    name: String = fallbackText,
    size: Dp = 56.dp,
    primaryColor: Color = SaffronPrimary,
    borderColor: Color = GoldSecondary,
    shape: Shape = CircleShape,
    enableFullScreenPreview: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val effectiveText = name.ifBlank { fallbackText }

    var bitmap by remember(photoUri) { mutableStateOf<Bitmap?>(null) }
    var showPreviewDialog by remember { mutableStateOf(false) }

    LaunchedEffect(photoUri) {
        if (photoUri.isBlank()) {
            bitmap = null
            return@LaunchedEffect
        }
        withContext(Dispatchers.IO) {
            val loaded = com.example.shribalajikripadham.util.DevoteePhotoHelper.loadBitmap(context, photoUri)
            bitmap = loaded
        }
    }

    val safeBitmap = remember(bitmap) {
        bitmap?.let { com.example.shribalajikripadham.util.DevoteePhotoHelper.toSoftwareBitmap(it) }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .border(1.5.dp, borderColor, shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(primaryColor, primaryColor.copy(alpha = 0.85f))
                )
            )
            .clickable(enabled = enableFullScreenPreview && safeBitmap != null) {
                showPreviewDialog = true
            },
        contentAlignment = Alignment.Center
    ) {
        if (safeBitmap != null) {
            Image(
                bitmap = safeBitmap.asImageBitmap(),
                contentDescription = effectiveText,
                contentScale = ContentScale.Crop,
                alignment = BiasAlignment(0f, -0.4f), // Face centering: shifts focus up to portrait head/face
                modifier = Modifier
                    .size(size)
                    .clip(shape)
            )
        } else {
            Text(
                text = effectiveText.take(3),
                color = Color.White,
                fontSize = (size.value * 0.32f).sp,
                fontWeight = FontWeight.Bold
            )
        }
    }

    if (showPreviewDialog && safeBitmap != null) {
        Dialog(onDismissRequest = { showPreviewDialog = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = effectiveText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color(0xFF8B0000)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Image(
                        bitmap = safeBitmap.asImageBitmap(),
                        contentDescription = effectiveText,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(onClick = { showPreviewDialog = false }) {
                        Text("बंद करें (Close)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
