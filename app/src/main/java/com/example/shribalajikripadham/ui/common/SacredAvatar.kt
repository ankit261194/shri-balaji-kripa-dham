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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
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
import java.io.File
import java.io.InputStream

@Composable
fun SacredAvatar(
    photoUri: String,
    fallbackText: String = "",
    name: String = fallbackText,
    size: Dp = 56.dp,
    primaryColor: Color = SaffronPrimary,
    borderColor: Color = GoldSecondary,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val effectiveText = name.ifBlank { fallbackText }

    val bitmap: Bitmap? = remember(photoUri) {
        if (photoUri.isBlank()) null
        else {
            try {
                if (photoUri.startsWith("content://") || photoUri.startsWith("android.resource://")) {
                    val uri = Uri.parse(photoUri)
                    val input: InputStream? = context.contentResolver.openInputStream(uri)
                    input?.use { BitmapFactory.decodeStream(it) }
                } else if (photoUri.startsWith("file://") || photoUri.startsWith("/")) {
                    val path = if (photoUri.startsWith("file://")) photoUri.removePrefix("file://") else photoUri
                    val file = File(path)
                    if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    val safeBitmap = remember(bitmap) {
        bitmap?.let { com.example.shribalajikripadham.util.DevoteePhotoHelper.toSoftwareBitmap(it) }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .border(1.5.dp, borderColor, CircleShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(primaryColor, primaryColor.copy(alpha = 0.85f))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        if (safeBitmap != null) {
            Image(
                bitmap = safeBitmap.asImageBitmap(),
                contentDescription = effectiveText,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
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
}
