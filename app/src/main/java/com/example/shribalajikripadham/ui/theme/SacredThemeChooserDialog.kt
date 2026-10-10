package com.example.shribalajikripadham.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.shribalajikripadham.theme.LocalSacredStyle
import com.example.shribalajikripadham.theme.SacredTheme
import com.example.shribalajikripadham.theme.ThemePreferences

/**
 * Pro MI-Style 360-Degree Universal Sacred Theme Chooser Dialog.
 * Allows devotees, sevadars, and administrators to seamlessly preview and switch between:
 * 1. 🌟 4 Divine Sacred Themes (Tiranga, Deepawali, Sherawali Maiya, Veer Hanuman)
 * 2. 🍃 8 Modern Peaceful Themes (WhatsApp, Telegram, Sage, Slate, etc.)
 */
@Composable
fun SacredThemeChooserDialog(
    currentTheme: SacredTheme,
    isHindi: Boolean = true,
    onDismissRequest: () -> Unit,
    onThemeSelected: (SacredTheme) -> Unit
) {
    val context = LocalContext.current
    var previewSelectedTheme by remember { mutableStateOf(currentTheme) }
    val scrollState = rememberScrollState()

    val divineThemes = remember { ThemePreferences.getDivineThemes() }
    val peacefulThemes = remember { ThemePreferences.getPeacefulThemes() }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.90f),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Header with Title & Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "🎨",
                                fontSize = 22.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isHindi) "पावन थीम का चयन करें" else "Choose Sacred Theme",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = if (isHindi) "MI-स्टाइल 360° सम्पूर्ण दृश्य बदलाव (कार्ड, रंग व पर्चा)" else "MI-style 360° visual overhaul across entire app",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Text(
                            text = "✕",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                )

                // Scrollable List of Themes
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // SECTION 1: 🌟 4 DIVINE THEMES
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFFFBEB),
                        border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("✨", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isHindi) "दिव्य व उत्सव थीम्स (4 Divine Sacred Themes)" else "Divine & Festive Themes (4 Sacred Themes)",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB45309)
                            )
                        }
                    }

                    divineThemes.forEach { theme ->
                        val isSelected = (previewSelectedTheme == theme)
                        DivineThemeCard(
                            theme = theme,
                            isSelected = isSelected,
                            isHindi = isHindi,
                            onSelect = {
                                previewSelectedTheme = theme
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // SECTION 2: 🍃 MODERN PEACEFUL THEMES
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF0FDF4),
                        border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🍃", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isHindi) "शांत व सात्विक थीम्स (Modern Peaceful Themes)" else "Modern Peaceful Themes",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF166534)
                            )
                        }
                    }

                    peacefulThemes.forEach { theme ->
                        val isSelected = (previewSelectedTheme == theme)
                        PeacefulThemeCard(
                            theme = theme,
                            isSelected = isSelected,
                            isHindi = isHindi,
                            onSelect = {
                                previewSelectedTheme = theme
                            }
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 10.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                )

                // Bottom Action Buttons: Cancel and Apply Theme
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = if (isHindi) "रद्द करें" else "Cancel",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = {
                            ThemePreferences.setSelectedTheme(context, previewSelectedTheme)
                            onThemeSelected(previewSelectedTheme)
                            onDismissRequest()
                        },
                        modifier = Modifier.weight(1.4f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = previewSelectedTheme.primaryColor,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = if (isHindi) "लागू करें (${previewSelectedTheme.icon})" else "Apply (${previewSelectedTheme.icon})",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Rich Preview Card for the 4 Divine Themes (Tiranga, Deepawali, Sherawali, Hanuman).
 */
@Composable
private fun DivineThemeCard(
    theme: SacredTheme,
    isSelected: Boolean,
    isHindi: Boolean,
    onSelect: () -> Unit
) {
    val borderBrush = when (theme) {
        SacredTheme.TIRANGA -> Brush.linearGradient(
            colors = listOf(Color(0xFFFF9933), Color(0xFFFFFFFF), Color(0xFF138808), Color(0xFFFF9933))
        )
        SacredTheme.DIVYA_DEEPAWALI -> Brush.linearGradient(
            colors = listOf(Color(0xFFF59E0B), Color(0xFFFDE68A), Color(0xFFFBBF24), Color(0xFFD97706))
        )
        SacredTheme.SHERAWALI_MAIYA -> Brush.linearGradient(
            colors = listOf(Color(0xFFD4AF37), Color(0xFFEF4444), Color(0xFFD4AF37))
        )
        SacredTheme.VEER_HANUMAN -> Brush.linearGradient(
            colors = listOf(Color(0xFFC2410C), Color(0xFFF59E0B), Color(0xFFB45309), Color(0xFFC2410C))
        )
        else -> Brush.linearGradient(
            colors = listOf(theme.primaryColor, theme.secondaryColor)
        )
    }

    val cardBg = if (theme.isDark) Color(0xFF131B2E) else Color(0xFFFFFDFB)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .then(
                if (isSelected) {
                    Modifier.border(BorderStroke(2.5.dp, borderBrush), RoundedCornerShape(14.dp))
                } else {
                    Modifier.border(BorderStroke(1.dp, Color(0xFFE2E8F0)), RoundedCornerShape(14.dp))
                }
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Emblem circle
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(theme.primaryColor.copy(alpha = 0.15f))
                            .border(1.dp, theme.primaryColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = theme.icon,
                            fontSize = 20.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isHindi) theme.nameHindi else theme.nameEnglish,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (theme.isDark) Color(0xFFF1F5F9) else Color(0xFF1E293B)
                            )
                            if (isSelected) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = theme.primaryColor
                                ) {
                                    Text(
                                        text = if (isHindi) "✓ सक्रिय" else "✓ Active",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = theme.styleBadge,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (theme.isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                    }
                }

                // Swatches preview
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(theme.primaryColor)
                            .border(0.8.dp, Color.White, CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(theme.secondaryColor)
                            .border(0.8.dp, Color.White, CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(theme.accentGold)
                            .border(0.8.dp, Color.White, CircleShape)
                    )
                }
            }

            // Subtitle & Watermark Pill
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isHindi) theme.divineChantHindi.ifBlank { theme.styleNameHindi } else theme.divineChantEnglish,
                    fontSize = 10.8.sp,
                    color = if (theme.isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (theme.divineMotif.isNotBlank() || theme.watermarkText.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = theme.primaryColor.copy(alpha = 0.12f),
                        border = BorderStroke(0.6.dp, theme.primaryColor.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = if (theme.divineMotif.isNotBlank()) "${theme.divineMotif.take(4).trim()} ${theme.watermarkText}" else "${theme.watermarkIcon} ${theme.watermarkText}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = theme.primaryColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Compact Preview Card for the 8 Modern Peaceful Themes.
 */
@Composable
private fun PeacefulThemeCard(
    theme: SacredTheme,
    isSelected: Boolean,
    isHindi: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .then(
                if (isSelected) {
                    Modifier.border(BorderStroke(2.dp, theme.primaryColor), RoundedCornerShape(12.dp))
                } else {
                    Modifier.border(BorderStroke(0.8.dp, Color(0xFFE2E8F0)), RoundedCornerShape(12.dp))
                }
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (theme.isDark) Color(0xFF1E293B) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 0.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = theme.icon,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isHindi) theme.nameHindi else theme.nameEnglish,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (theme.isDark) Color(0xFFF1F5F9) else Color(0xFF1E293B)
                        )
                        if (isSelected) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "✓",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = theme.primaryColor
                            )
                        }
                    }
                    Text(
                        text = if (isHindi) theme.divineChantHindi.ifBlank { theme.styleBadge } else theme.divineChantEnglish,
                        fontSize = 10.5.sp,
                        color = if (theme.isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Small color dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(theme.primaryColor)
                )
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(theme.secondaryColor)
                )
            }
        }
    }
}
