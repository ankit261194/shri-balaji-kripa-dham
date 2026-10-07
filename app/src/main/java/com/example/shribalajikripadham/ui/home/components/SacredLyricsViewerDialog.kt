package com.example.shribalajikripadham.ui.home.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.shribalajikripadham.data.sacred.SacredTrack
import com.example.shribalajikripadham.theme.AmberGold
import com.example.shribalajikripadham.theme.MaroonPrimary
import com.example.shribalajikripadham.theme.SacredTheme

/**
 * Pro-Tier Complete Sacred Lyrics & Stotra Reader.
 * Supports font size adjustment, copy full text, and complete authentic verses.
 */
@Composable
fun SacredLyricsViewerDialog(
    track: SacredTrack,
    currentTheme: SacredTheme,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var fontSizeSp by remember { mutableIntStateOf(16) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFFFFFDF7),
            border = BorderStroke(1.5.dp, AmberGold),
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track.titleHindi,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaroonPrimary
                        )
                        Text(
                            text = "${track.subtitleHindi} • संपूर्ण पावन पाठ",
                            fontSize = 12.sp,
                            color = Color(0xFF795548),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFEEEEEE),
                            modifier = Modifier.size(30.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("✕", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                            }
                        }
                    }
                }

                // Toolbar: Font Size Adjuster & Copy Button
                Surface(
                    color = Color(0xFFFFF8E1),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(0.8.dp, Color(0xFFFFD54F)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Font Size Adjuster
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("अक्षर आकार:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5D4037))
                            Spacer(modifier = Modifier.width(6.dp))
                            FilledTonalButton(
                                onClick = { if (fontSizeSp > 13) fontSizeSp -= 2 },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                modifier = Modifier.height(28.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("A-", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            FilledTonalButton(
                                onClick = { if (fontSizeSp < 26) fontSizeSp += 2 },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                modifier = Modifier.height(28.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("A+", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Copy Text Button
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText(track.titleHindi, "${track.titleHindi}\n\n${track.lyricsHindi}")
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "📋 सम्पूर्ण पाठ कॉपी हो गया", Toast.LENGTH_SHORT).show()
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier.height(28.dp),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, MaroonPrimary)
                        ) {
                            Text("📋 कॉपी करें", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = MaroonPrimary)
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp), color = Color(0xFFFFE082))

                // Scrollable Complete Sacred Lyrics
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .background(Color(0xFFFFFDF5), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = track.lyricsHindi,
                        fontSize = fontSizeSp.sp,
                        lineHeight = (fontSizeSp + 10).sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF2C1810),
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Close Button
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("जय श्री बालाजी • बन्द करें (Close)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                }
            }
        }
    }
}
