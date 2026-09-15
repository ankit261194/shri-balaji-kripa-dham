package com.example.shribalajikripadham.ui.home

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shribalajikripadham.data.model.Admin
import com.example.shribalajikripadham.data.model.AshramEvent
import com.example.shribalajikripadham.data.model.AshramSettings
import com.example.shribalajikripadham.theme.*
import com.example.shribalajikripadham.ui.common.SacredAvatar

/**
 * LAYOUT 2: MODERN CARDS (आधुनिक कार्ड्स)
 * Sleek modern card architecture with vibrant gradients, quick action tiles, and clean typography.
 */
@Composable
fun ModernCardsLayout(
    settings: AshramSettings,
    isHindi: Boolean,
    currentTheme: SacredTheme,
    activeSevadars: List<Admin>,
    dynamicEvents: List<AshramEvent>,
    onNavigateToToken: () -> Unit,
    onNavigateToFaceToken: () -> Unit,
    onNavigateToYatra: () -> Unit,
    onNavigateToInfo: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    onNavigateToParchas: () -> Unit = {},
    onNavigateToYatraExpenses: () -> Unit = {},
    onThemeChanged: (SacredTheme) -> Unit
) {
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxWidth()) {
        // 1. Hero Token Card with Saffron / Theme Gradient
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            shape = currentTheme.cardShape,
            elevation = CardDefaults.cardElevation(currentTheme.cardElevation),
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(currentTheme.primaryColor, currentTheme.secondaryColor)
                    ),
                    shape = currentTheme.cardShape
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = if (settings.isDarbarActive)
                                (if (isHindi) "● दरबार सक्रिय" else "● Darbar Active")
                            else
                                (if (isHindi) "दरबार विराम" else "Darbar Inactive"),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    Text(
                        text = "100% FREE",
                        color = currentTheme.accentGold,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = if (isHindi) "रविवार पावन दरबार दर्शन" else "Sunday Divine Darbar",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Text(
                    text = settings.darbarTimings,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onNavigateToToken,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = currentTheme.primaryColor
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (isHindi) "🎟️ टोकन लें" else "🎟️ Get Token",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    OutlinedButton(
                        onClick = onNavigateToFaceToken,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = BorderStroke(1.5.dp, Color.White),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (isHindi) "⚡ फेस टोकन" else "⚡ Face Token",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 2. Services Horizontal Carousel
        Text(
            text = if (isHindi) "त्वरित सेवा विकल्प" else "Quick Services",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = currentTheme.primaryColor
        )
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Ashram Info
            Surface(
                onClick = onNavigateToInfo,
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 3.dp,
                border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🏛️", fontSize = 28.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isHindi) "आश्रम परिचय" else "About Ashram",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = TextPrimaryDark,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Bus Yatra (If enabled)
            if (settings.isYatraServiceEnabled) {
                Surface(
                    onClick = onNavigateToYatra,
                    color = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 3.dp,
                    border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🚌", fontSize = 28.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isHindi) "बालाजी यात्रा" else "Balaji Yatra",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = TextPrimaryDark,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Sacred Parchas
            Surface(
                onClick = onNavigateToParchas,
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 3.dp,
                border = BorderStroke(1.dp, Color(0xFFCE93D8)),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("📜", fontSize = 28.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isHindi) "आश्रम पर्चे" else "Sacred Parchas",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFF4A148C),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Admin Portal
            Surface(
                onClick = onNavigateToAdmin,
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 3.dp,
                border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("⚙️", fontSize = 28.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isHindi) "व्यवस्थापक" else "Admin Portal",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = TextPrimaryDark,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 3. Guruji Profile Card with Custom Photo
        if (settings.isGurujiInfoVisible) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(3.dp),
                border = BorderStroke(1.dp, currentTheme.secondaryColor.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SacredAvatar(
                        photoUri = settings.gurujiPhotoUri,
                        fallbackText = "गुरुजी",
                        size = 88.dp,
                        primaryColor = currentTheme.primaryColor,
                        borderColor = currentTheme.secondaryColor
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = settings.gurujiName.ifEmpty { "परम पूज्य गुरुजी तेजवीर सिंह जी" },
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = currentTheme.primaryColor
                        )
                        Text(
                            text = if (isHindi) "आश्रम संस्थापक एवं दिव्य पथ-प्रदर्शक" else "Spiritual Guide & Founder",
                            fontSize = 12.sp,
                            color = SaffronDark,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isHindi) "निशुल्क चिकित्सा, प्रेतबाधा निवारण एवं भगवद् कृपा।" else "Free spiritual healing & divine blessings.",
                            fontSize = 11.sp,
                            color = TextSecondaryDark
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
        }

        // 4. Sevadars Horizontal Card List
        if (activeSevadars.isNotEmpty()) {
            Text(
                text = if (isHindi) "आश्रम सेवादार टीम" else "Ashram Sevadar Team",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = currentTheme.primaryColor
            )
            Spacer(modifier = Modifier.height(10.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(activeSevadars) { sevadar ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(2.dp),
                        border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                        modifier = Modifier.width(200.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SacredAvatar(
                                photoUri = sevadar.photoUri,
                                fallbackText = sevadar.name,
                                size = 68.dp,
                                primaryColor = currentTheme.primaryColor,
                                borderColor = currentTheme.secondaryColor
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = sevadar.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = TextPrimaryDark,
                                    maxLines = 1
                                )
                                Text(
                                    text = sevadar.phoneNumber,
                                    fontSize = 11.sp,
                                    color = TextSecondaryDark
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}

/**
 * LAYOUT 3: VEDIC MANDIR GRID (वैदिक ग्रिड)
 * 2-column balanced grid with ornate mandir styling, shlokas, and traditional aesthetics.
 */
@Composable
fun VedicGridLayout(
    settings: AshramSettings,
    isHindi: Boolean,
    currentTheme: SacredTheme,
    activeSevadars: List<Admin>,
    dynamicEvents: List<AshramEvent>,
    onNavigateToToken: () -> Unit,
    onNavigateToFaceToken: () -> Unit,
    onNavigateToYatra: () -> Unit,
    onNavigateToInfo: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    onNavigateToParchas: () -> Unit = {},
    onNavigateToYatraExpenses: () -> Unit = {},
    onThemeChanged: (SacredTheme) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Vedic Banner: Shloka Header
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.5.dp, Color(0xFFFFB300)),
            elevation = CardDefaults.cardElevation(3.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "॥ ॐ श्री हनुमते नमः ॥",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = currentTheme.primaryColor
                )
                Text(
                    text = if (isHindi)
                        "मनोजवं मारुततुल्यवेगं जितेन्द्रियं बुद्धिमतां वरिष्ठम्। वातात्मजं वानरयूथमुख्यं श्रीरामदूतं शरणं प्रपद्ये॥"
                    else
                        "Manojavam Maruta Tulya Vegam, Jitendriyam Buddhimatam Varishtham...",
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    color = Color(0xFF795548),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Guruji Card with Sacred Avatar
        if (settings.isGurujiInfoVisible) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(3.dp),
                border = BorderStroke(1.2.dp, currentTheme.secondaryColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SacredAvatar(
                        photoUri = settings.gurujiPhotoUri,
                        fallbackText = "गुरुजी",
                        size = 88.dp,
                        primaryColor = currentTheme.primaryColor,
                        borderColor = currentTheme.secondaryColor
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = settings.gurujiName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = currentTheme.primaryColor
                        )
                        Text(
                            text = if (isHindi) "ग्राम डूँगरा जाट आश्रम प्रमुख" else "Ashram Spiritual Head",
                            fontSize = 12.sp,
                            color = SaffronDark
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 2-Column Vedic Grid of Services
        Text(
            text = if (isHindi) "मंदिर दर्शन व सेवाएं" else "Mandir Darshan Services",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = currentTheme.primaryColor
        )
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Item 1: Token
            VedicGridCard(
                title = if (isHindi) "रविवार टोकन" else "Sunday Token",
                desc = if (isHindi) "दर्शन क्रमांक लें" else "Book Darshan Token",
                icon = "🎟️",
                color = currentTheme.primaryColor,
                onClick = onNavigateToToken,
                modifier = Modifier.weight(1f)
            )

            // Item 2: Face Token
            VedicGridCard(
                title = if (isHindi) "फेस टोकन" else "Face Token",
                desc = if (isHindi) "1-सेकंड त्वरित टोकन" else "1-Sec Face Scan",
                icon = "⚡",
                color = currentTheme.secondaryColor,
                onClick = onNavigateToFaceToken,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Item 3: Aarti & Darbar Timings
            VedicGridCard(
                title = if (isHindi) "आरती समय" else "Aarti Timings",
                desc = if (isHindi) "प्रातः 7:00 बजे से" else "Every Sunday 7 AM",
                icon = "⏰",
                color = Color(0xFFD84315),
                onClick = onNavigateToInfo,
                modifier = Modifier.weight(1f)
            )

            // Item 4: Ashram Info or Bus Yatra
            if (settings.isYatraServiceEnabled) {
                VedicGridCard(
                    title = if (isHindi) "बालाजी यात्रा" else "Balaji Yatra",
                    desc = if (isHindi) "बस सीट बुकिंग" else "Bus Seat Booking",
                    icon = "🚌",
                    color = Color(0xFF2E7D32),
                    onClick = onNavigateToYatra,
                    modifier = Modifier.weight(1f)
                )
            } else {
                VedicGridCard(
                    title = if (isHindi) "आश्रम परिचय" else "Ashram Info",
                    desc = if (isHindi) "स्थान व संपर्क" else "Location & Help",
                    icon = "🏛️",
                    color = Color(0xFF00695C),
                    onClick = onNavigateToInfo,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Row 3: Sacred Parchas & Admin
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            VedicGridCard(
                title = if (isHindi) "📜 आश्रम पर्चे" else "📜 Sacred Parchas",
                desc = if (isHindi) "हवन, उतारा व A4 PDF" else "Hawan, Utara & Slips",
                icon = "📜",
                color = Color(0xFF6A1B9A),
                onClick = onNavigateToParchas,
                modifier = Modifier.weight(1f)
            )
            VedicGridCard(
                title = if (isHindi) "व्यवस्थापक" else "Admin Portal",
                desc = if (isHindi) "सेवादार व सुरक्षा" else "Sevadar Access",
                icon = "⚙️",
                color = MaroonAccent,
                onClick = onNavigateToAdmin,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Sevadars Section
        if (activeSevadars.isNotEmpty()) {
            Text(
                text = if (isHindi) "समर्पित सेवादार" else "Devoted Sevadars",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = currentTheme.primaryColor
            )
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(activeSevadars) { s ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                        modifier = Modifier.width(180.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            SacredAvatar(
                                photoUri = s.photoUri,
                                fallbackText = s.name,
                                size = 68.dp,
                                primaryColor = currentTheme.primaryColor,
                                borderColor = currentTheme.secondaryColor
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = s.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                maxLines = 1
                            )
                            Text(
                                text = s.phoneNumber,
                                fontSize = 11.sp,
                                color = TextSecondaryDark
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}

@Composable
fun VedicGridCard(
    title: String,
    desc: String,
    icon: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(3.dp),
        border = BorderStroke(1.5.dp, color.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(icon, fontSize = 24.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = TextPrimaryDark,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = desc,
                fontSize = 11.sp,
                color = TextSecondaryDark,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * LAYOUT 4: COMPACT LIST (त्वरित दर्शन कॉम्पैक्ट सूची)
 * Fast, dense list layout for quick 1-tap actions with minimal scrolling.
 */
@Composable
fun CompactListLayout(
    settings: AshramSettings,
    isHindi: Boolean,
    currentTheme: SacredTheme,
    activeSevadars: List<Admin>,
    dynamicEvents: List<AshramEvent>,
    onNavigateToToken: () -> Unit,
    onNavigateToFaceToken: () -> Unit,
    onNavigateToYatra: () -> Unit,
    onNavigateToInfo: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    onNavigateToParchas: () -> Unit = {},
    onNavigateToYatraExpenses: () -> Unit = {},
    onThemeChanged: (SacredTheme) -> Unit
) {
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxWidth()) {
        // Status Row
        Surface(
            color = currentTheme.primaryColor.copy(alpha = 0.08f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, currentTheme.primaryColor.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (settings.isDarbarActive) "🟢" else "🔴", fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (settings.isDarbarActive)
                            (if (isHindi) "दरबार चालू है" else "Darbar Active")
                        else
                            (if (isHindi) "दरबार बंद है" else "Darbar Closed"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = currentTheme.primaryColor
                    )
                }

                Text(
                    text = "100% Free",
                    color = SaffronDark,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Action Item 1: Token
        CompactActionRow(
            icon = "🎟️",
            title = if (isHindi) "रविवार टोकन पंजीकरण करें" else "Register Sunday Token",
            subtitle = if (isHindi) "निशुल्क क्रमांक प्राप्त करें" else "Get free darshan queue number",
            badge = if (isHindi) "मुख्य" else "Main",
            badgeColor = currentTheme.primaryColor,
            onClick = onNavigateToToken
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Action Item 2: Face Token
        CompactActionRow(
            icon = "⚡",
            title = if (isHindi) "फेस रिकग्निशन 1-सेकंड टोकन" else "1-Second Face Token",
            subtitle = if (isHindi) "कैमरा से तुरंत सत्यापन" else "Instant camera verification",
            badge = "1-Sec",
            badgeColor = currentTheme.secondaryColor,
            onClick = onNavigateToFaceToken
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Action Item: Sacred Parchas
        CompactActionRow(
            icon = "📜",
            title = if (isHindi) "📜 आश्रम पर्चे व नियम दस्तावेज" else "📜 Sacred Parchas & Slips",
            subtitle = if (isHindi) "हवन पर्चा, मैया उतारा, अर्जी व नियम A4 PDF" else "Hawan, Utara, Arji & A4 PDF",
            badge = if (isHindi) "पर्चे" else "Parchas",
            badgeColor = Color(0xFF6A1B9A),
            onClick = onNavigateToParchas
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Action Item 3: Bus Yatra (if enabled)
        if (settings.isYatraServiceEnabled) {
            CompactActionRow(
                icon = "🚌",
                title = if (isHindi) "श्री बालाजी यात्रा बस सीट" else "Balaji Yatra Bus Seat",
                subtitle = if (isHindi) "तीर्थ यात्रा सीट बुकिंग" else "Pilgrimage bus booking",
                badge = if (isHindi) "यात्रा" else "Yatra",
                badgeColor = Color(0xFF2E7D32),
                onClick = onNavigateToYatra
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Action Item 4: Aarti & Timings
        CompactActionRow(
            icon = "⏰",
            title = if (isHindi) "आरती व दरबार समय सारणी" else "Aarti & Darbar Timings",
            subtitle = settings.darbarTimings,
            badge = if (isHindi) "समय" else "Time",
            badgeColor = Color(0xFFE65100),
            onClick = onNavigateToInfo
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Action Item 5: Ashram Contact & Helpline
        CompactActionRow(
            icon = "📞",
            title = if (isHindi) "आश्रम हेल्पलाइन डायल करें" else "Call Ashram Helpline",
            subtitle = settings.contactPhone,
            badge = if (isHindi) "कॉल" else "Call",
            badgeColor = Color(0xFF00695C),
            onClick = {
                try {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${settings.contactPhone}"))
                    context.startActivity(intent)
                } catch (_: Exception) {}
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Action Item 6: Admin Portal
        CompactActionRow(
            icon = "⚙️",
            title = if (isHindi) "व्यवस्थापक पोर्टल" else "Admin Portal",
            subtitle = if (isHindi) "टोकन व आश्रम प्रबंधन" else "Ashram & Token Controls",
            badge = "Admin",
            badgeColor = Color(0xFF455A64),
            onClick = onNavigateToAdmin
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Mini Guruji Banner
        if (settings.isGurujiInfoVisible) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SacredAvatar(
                        photoUri = settings.gurujiPhotoUri,
                        fallbackText = "गुरुजी",
                        size = 88.dp,
                        primaryColor = currentTheme.primaryColor,
                        borderColor = currentTheme.secondaryColor
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = settings.gurujiName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = currentTheme.primaryColor
                        )
                        Text(
                            text = settings.address,
                            fontSize = 11.sp,
                            color = TextSecondaryDark
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CompactActionRow(
    icon: String,
    title: String,
    subtitle: String,
    badge: String,
    badgeColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.White,
        shape = RoundedCornerShape(14.dp),
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, Color(0xFFE8E8E8)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(badgeColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(icon, fontSize = 20.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TextPrimaryDark
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = TextSecondaryDark,
                    maxLines = 1
                )
            }

            Surface(
                color = badgeColor.copy(alpha = 0.12f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = badge,
                    color = badgeColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}

/**
 * LAYOUT 5: DIVINE FEED (दिव्य दर्शन फीड / मैगज़ीन)
 * Vertical devotional magazine feed with large imagery headers, daily blessings, and community cards.
 */
@Composable
fun DivineFeedLayout(
    settings: AshramSettings,
    isHindi: Boolean,
    currentTheme: SacredTheme,
    activeSevadars: List<Admin>,
    dynamicEvents: List<AshramEvent>,
    onNavigateToToken: () -> Unit,
    onNavigateToFaceToken: () -> Unit,
    onNavigateToYatra: () -> Unit,
    onNavigateToInfo: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    onNavigateToParchas: () -> Unit = {},
    onNavigateToYatraExpenses: () -> Unit = {},
    onThemeChanged: (SacredTheme) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Feed Story 1: Daily Darshan Blessing Header
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(4.dp),
            border = BorderStroke(1.5.dp, currentTheme.secondaryColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(currentTheme.primaryColor, currentTheme.secondaryColor)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🚩 ॐ श्री हनुमते नमः 🚩", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = settings.ashramName,
                            color = currentTheme.accentGold,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp
                        )
                        Text(
                            text = settings.address,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 11.sp
                        )
                    }
                }

                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isHindi) "आज का पावन संकल्प व गुरु संदेश" else "Daily Divine Message",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = currentTheme.primaryColor
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isHindi)
                            "\"ईश्वर की शरण में आने वाले प्रत्येक भक्त का कष्ट निवारण निःशुल्क होता है। केवल पूर्ण श्रद्धा, संयम एवं गुरु आज्ञा का पालन आवश्यक है।\""
                        else
                            "\"Divine healing is free for all sincere seekers. Devotion, purity and faith are the only paths.\"",
                        fontSize = 13.sp,
                        color = TextPrimaryDark,
                        lineHeight = 19.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onNavigateToToken,
                            colors = ButtonDefaults.buttonColors(containerColor = currentTheme.primaryColor),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (isHindi) "🎟️ टोकन लें" else "🎟️ Token", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onNavigateToFaceToken,
                            border = BorderStroke(1.5.dp, currentTheme.primaryColor),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (isHindi) "⚡ फेस टोकन" else "⚡ Face Scan", color = currentTheme.primaryColor, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Feed Story: Sacred Parchas Banner
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFFBA68C8)),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToParchas() }
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("📜", fontSize = 28.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isHindi) "📜 आश्रम पर्चे व दस्तावेज" else "📜 Sacred Documents & Slips",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF4A148C)
                    )
                    Text(
                        text = if (isHindi) "हवन पर्चा, मैया उतारा, अर्जी व नियम A4 PDF डाउनलोड करें" else "Download Hawan, Utara & Rules PDF",
                        fontSize = 11.sp,
                        color = Color.DarkGray
                    )
                }
                Text("➔", color = Color(0xFF4A148C), fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Feed Story 2: Guruji Tejveer Singh Ji Spotlight
        if (settings.isGurujiInfoVisible) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(3.dp),
                border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SacredAvatar(
                            photoUri = settings.gurujiPhotoUri,
                            fallbackText = "गुरुजी",
                            size = 92.dp,
                            primaryColor = currentTheme.primaryColor,
                            borderColor = currentTheme.secondaryColor
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = settings.gurujiName,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = currentTheme.primaryColor
                            )
                            Text(
                                text = if (isHindi) "मार्गदर्शक एवं संस्थापक" else "Spiritual Guide & Founder",
                                fontSize = 12.sp,
                                color = SaffronDark,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (isHindi)
                            "गुरुजी के सानिध्य में प्रत्येक रविवार को लगने वाले दरबार में भूत-बाधा, मानसिक अशांति एवं जटिल व्याधियों का पूर्णतः निःशुल्क आध्यात्मिक समाधान होता है।"
                        else
                            "Under Guruji's divine presence, Sunday Darbar offers 100% free spiritual healing and blessings for all devotees.",
                        fontSize = 12.sp,
                        color = TextSecondaryDark,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
        }

        // Feed Story 3: Sevadar Spotlight
        if (activeSevadars.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(3.dp),
                border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isHindi) "आश्रम सेवादार मंडल" else "Ashram Sevadar Mandal",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = currentTheme.primaryColor
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    activeSevadars.forEach { sevadar ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SacredAvatar(
                                photoUri = sevadar.photoUri,
                                fallbackText = sevadar.name,
                                size = 68.dp,
                                primaryColor = currentTheme.primaryColor,
                                borderColor = currentTheme.secondaryColor
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = sevadar.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = TextPrimaryDark
                                )
                                Text(
                                    text = if (isHindi) "समर्पित सेवादार" else "Authorized Sevadar",
                                    fontSize = 11.sp,
                                    color = TextSecondaryDark
                                )
                            }
                            Text(
                                text = "📞 " + sevadar.phoneNumber,
                                fontSize = 11.sp,
                                color = currentTheme.primaryColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}

// =========================================================================================
// 🔽 REUSABLE SCROLL-DOWN ACCORDION FOR MULTI-OPTION FUNCTIONS
// "jisbhi function ke kafi sare option hote vo scroll down me open hote h"
// =========================================================================================

@Composable
fun ScrollDownFunctionAccordion(
    title: String,
    subtitle: String,
    icon: String,
    badgeText: String? = null,
    badgeColor: Color = SaffronPrimary,
    isInitiallyExpanded: Boolean = false,
    primaryColor: Color = MaroonPrimary,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    var isExpanded by remember { mutableStateOf(isInitiallyExpanded) }
    val rotationAngle by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f, label = "accordionChevron")

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = shape,
        elevation = CardDefaults.cardElevation(if (isExpanded) 4.dp else 1.5.dp),
        border = BorderStroke(
            if (isExpanded) 1.5.dp else 0.8.dp,
            if (isExpanded) primaryColor.copy(alpha = 0.8f) else Color(0xFFE0E0E0)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(primaryColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(icon, fontSize = 22.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp,
                            color = TextPrimaryDark
                        )
                        if (badgeText != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = badgeColor.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = badgeText,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = TextSecondaryDark,
                        maxLines = 1
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (isExpanded) primaryColor.copy(alpha = 0.15f) else Color(0xFFF2F2F2)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "▼",
                        fontSize = 11.sp,
                        color = if (isExpanded) primaryColor else Color.Gray,
                        modifier = Modifier.rotate(rotationAngle)
                    )
                }
            }

            if (isExpanded) {
                HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 0.8.dp)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFCFCFC))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
fun AccordionOptionRow(
    icon: String,
    title: String,
    subtitle: String,
    actionButtonText: String? = null,
    accentColor: Color = MaroonPrimary,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = Color.White,
        border = BorderStroke(0.8.dp, Color(0xFFE8E8E8)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(icon, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = TextPrimaryDark
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        fontSize = 10.5.sp,
                        color = TextSecondaryDark,
                        lineHeight = 14.sp
                    )
                }
            }
            if (actionButtonText != null) {
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    color = accentColor,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = actionButtonText,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            } else {
                Text("→", fontSize = 14.sp, color = Color.Gray)
            }
        }
    }
}

// =========================================================================================
// 🚩 LAYOUT 6: MAHABALI HERO HUB (महाबली डैशबोर्ड)
// Dominant grand hero banner of Bajrang Bali, circular quick-action dock, stats & accordions
// =========================================================================================

@Composable
fun MahabaliHeroLayout(
    settings: AshramSettings,
    isHindi: Boolean,
    currentTheme: SacredTheme,
    activeSevadars: List<Admin>,
    dynamicEvents: List<AshramEvent>,
    onNavigateToToken: () -> Unit,
    onNavigateToFaceToken: () -> Unit,
    onNavigateToYatra: () -> Unit,
    onNavigateToInfo: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    onNavigateToParchas: () -> Unit = {},
    onNavigateToYatraExpenses: () -> Unit = {},
    onThemeChanged: (SacredTheme) -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Grand Mahabali Hero Banner
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            shape = RoundedCornerShape(22.dp),
            elevation = CardDefaults.cardElevation(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(currentTheme.primaryColor, currentTheme.secondaryColor)
                    ),
                    shape = RoundedCornerShape(22.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    color = Color.White.copy(alpha = 0.22f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = if (settings.isDarbarActive)
                            (if (isHindi) "● पावन दरबार सेवा सक्रिय" else "● Darbar Service Active")
                        else
                            (if (isHindi) "दरबार विराम" else "Darbar Inactive"),
                        color = Color.White,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "🚩 श्री बालाजी कृपा धाम 🚩",
                    fontSize = 21.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = if (isHindi) "महाबली वीर हनुमान जी का अलौकिक दरबार" else "Divine Darbar of Mahabali Hanuman Ji",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Stats Pill: Aarti Timings & Tokens
                Surface(
                    color = Color.Black.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if (isHindi) "दरबार दिवस" else "Darbar Day", fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f))
                            Text(if (isHindi) "प्रत्येक रविवार" else "Every Sunday", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = currentTheme.accentGold)
                        }
                        Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.White.copy(alpha = 0.3f)))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if (isHindi) "सेवा शुल्क" else "Seva Fee", fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f))
                            Text(if (isHindi) "100% निःशुल्क" else "100% FREE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF69F0AE))
                        }
                        Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.White.copy(alpha = 0.3f)))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if (isHindi) "आरती समय" else "Aarti", fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f))
                            Text(settings.darbarTimings.ifEmpty { "प्रातः 8 बजे" }, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }

        // Circular Quick Action Dock (5 Icon Actions)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                QuickActionBubble(icon = "🎫", label = if (isHindi) "टोकन" else "Token", color = currentTheme.primaryColor, onClick = onNavigateToToken)
                QuickActionBubble(icon = "📸", label = if (isHindi) "फेस टोकन" else "Face", color = Color(0xFF2E7D32), onClick = onNavigateToFaceToken)
                QuickActionBubble(icon = "📜", label = if (isHindi) "पर्चे" else "Parcha", color = Color(0xFF00695C), onClick = onNavigateToParchas)
                QuickActionBubble(icon = "🚌", label = if (isHindi) "यात्रा" else "Yatra", color = Color(0xFFE65100), onClick = onNavigateToYatra)
                QuickActionBubble(icon = "👥", label = if (isHindi) "सेवादार" else "Sevadar", color = Color(0xFF1565C0), onClick = onNavigateToInfo)
            }
        }

        // 1. TOKEN SERVICES ACCORDION
        ScrollDownFunctionAccordion(
            title = if (isHindi) "टोकन सेवा केंद्र (विस्तृत विकल्प)" else "Token Services Hub",
            subtitle = if (isHindi) "टोकन पर्चा, बायोमेट्रिक व समय सूची देखें" else "Token booking, biometric & schedule",
            icon = "🎫",
            badgeText = if (isHindi) "5 सेवाएं" else "5 Options",
            primaryColor = currentTheme.primaryColor,
            isInitiallyExpanded = true
        ) {
            AccordionOptionRow(
                icon = "🎫",
                title = if (isHindi) "रविवार ऑनलाइन टोकन लें" else "Book Sunday Token Online",
                subtitle = if (isHindi) "दरबार में दर्शन हेतु अपना टोकन तुरंत बुक करें" else "Direct online booking for Sunday Darbar",
                actionButtonText = if (isHindi) "टोकन लें" else "Book Now",
                accentColor = currentTheme.primaryColor,
                onClick = onNavigateToToken
            )
            AccordionOptionRow(
                icon = "📸",
                title = if (isHindi) "बायोमेट्रिक / चेहरा पहचान टोकन" else "Face Recognition Biometric Token",
                subtitle = if (isHindi) "कैमरे से तुरंत चेहरा स्कैन कर टोकन पाएं" else "Instant face scan token registration",
                actionButtonText = if (isHindi) "चेहरा स्कैन" else "Scan Face",
                accentColor = Color(0xFF2E7D32),
                onClick = onNavigateToFaceToken
            )
            AccordionOptionRow(
                icon = "📜",
                title = if (isHindi) "टोकन पर्चा व स्टेटस चेक करें" else "Search & Download Sacred Token Parcha",
                subtitle = if (isHindi) "मोबाइल नंबर दर्ज करके अपना टोकन पर्चा देखें" else "Check status and download token PDF",
                actionButtonText = if (isHindi) "पर्चा देखें" else "View",
                accentColor = Color(0xFF00695C),
                onClick = onNavigateToParchas
            )
            AccordionOptionRow(
                icon = "⏰",
                title = if (isHindi) "रविवार टोकन खुलने का समय व नियम" else "Sunday Schedule & Quota Rules",
                subtitle = if (isHindi) "टोकन हर रविवार प्रातः 8:00 बजे खुलता है" else "Opens every Sunday at 8:00 AM",
                actionButtonText = if (isHindi) "नियम" else "Rules",
                accentColor = Color(0xFFE65100),
                onClick = {
                    Toast.makeText(context, if (isHindi) "टोकन प्रत्येक रविवार प्रातः 8:00 बजे खुलता है!" else "Tokens open every Sunday at 8:00 AM!", Toast.LENGTH_LONG).show()
                }
            )
            AccordionOptionRow(
                icon = "📍",
                title = if (isHindi) "आश्रम दूरी व GPS लोकेशन सत्यापन" else "Ashram Distance & Location Check",
                subtitle = settings.address.ifEmpty { "श्री बालाजी कृपा धाम" },
                actionButtonText = if (isHindi) "दूरी देखें" else "Distance",
                accentColor = Color(0xFF512DA8),
                onClick = {
                    openSocialMediaLink(context, "https://maps.google.com/?q=${Uri.encode(settings.address.ifEmpty { "Shri Balaji Kripa Dham" })}", "https://maps.google.com", errorMessage = "नक्शा खोलने में असमर्थ")
                }
            )
        }

        // 2. DARSHAN & AARTI ACCORDION
        ScrollDownFunctionAccordion(
            title = if (isHindi) "दर्शन, आरती एवं पावन उत्सव" else "Darshan, Aarti & Events",
            subtitle = if (isHindi) "दैनिक आरती समय, उत्सव एवं गुरुजी भेंट" else "Daily Aarti timings, events & Guruji darshan",
            icon = "🪔",
            badgeText = if (isHindi) "पावन समय" else "Timings",
            primaryColor = currentTheme.secondaryColor
        ) {
            AccordionOptionRow(
                icon = "🪔",
                title = if (isHindi) "दैनिक आरती का समय" else "Daily Aarti Timings",
                subtitle = settings.darbarTimings.ifEmpty { "प्रातः 8:00 बजे एवं संध्या 7:00 बजे" },
                accentColor = currentTheme.secondaryColor,
                onClick = onNavigateToInfo
            )
            if (dynamicEvents.isNotEmpty()) {
                for (ev in dynamicEvents.take(3)) {
                    AccordionOptionRow(
                        icon = "📅",
                        title = ev.titleHindi.ifEmpty { ev.titleEnglish },
                        subtitle = if (isHindi) ev.dateDescriptionHindi.ifEmpty { ev.detailsHindi } else ev.dateDescriptionEnglish.ifEmpty { ev.detailsEnglish },
                        accentColor = Color(0xFFE65100),
                        onClick = onNavigateToInfo
                    )
                }
            }
            AccordionOptionRow(
                icon = "👑",
                title = if (isHindi) "पूज्य गुरुजी भेंट व आशीर्वाद" else "Revered Guruji Blessing Hours",
                subtitle = settings.gurujiName.ifEmpty { "पूज्य गुरुजी" },
                accentColor = Color(0xFFC2185B),
                onClick = onNavigateToInfo
            )
        }

        // 3. YATRA & STAY ACCORDION
        ScrollDownFunctionAccordion(
            title = if (isHindi) "धाम यात्रा एवं विश्राम व्यवस्था" else "Yatra & Accommodation",
            subtitle = if (isHindi) "धाम बस यात्रा, धर्मशाला व व्यय विवरण" else "Yatra booking, stay & expenses",
            icon = "🚌",
            badgeText = if (isHindi) "व्यवस्था" else "Facilities",
            primaryColor = Color(0xFFE65100)
        ) {
            AccordionOptionRow(
                icon = "🚌",
                title = if (isHindi) "आगामी धाम यात्रा पंजीकरण" else "Upcoming Dham Yatra Registration",
                subtitle = if (isHindi) "बालाजी धाम दर्शन बस यात्रा हेतु सीट बुक करें" else "Book seat for upcoming spiritual yatra",
                actionButtonText = if (isHindi) "पंजीकरण" else "Register",
                accentColor = Color(0xFFE65100),
                onClick = onNavigateToYatra
            )
            if (settings.canDevoteeViewYatraDiary) {
                AccordionOptionRow(
                    icon = "💰",
                    title = if (isHindi) "यात्रा व्यय एवं हिसाब-किताब" else "Yatra Expense Tracker",
                    subtitle = if (isHindi) "पारदर्शी यात्रा व्यय व खर्च का पूरा ब्यौरा" else "View transparent yatra expense ledger",
                    actionButtonText = if (isHindi) "हिसाब देखें" else "Ledger",
                    accentColor = Color(0xFF2E7D32),
                    onClick = onNavigateToYatraExpenses
                )
            }
            AccordionOptionRow(
                icon = "🏨",
                title = if (isHindi) "धर्मशाला व विश्राम व्यवस्था" else "Dharamshala & Accommodation",
                subtitle = if (isHindi) "बाहर से आने वाले भक्तों के विश्राम की सूचना" else "Stay arrangements for outstation devotees",
                accentColor = Color(0xFF1565C0),
                onClick = onNavigateToInfo
            )
        }

        // 4. SEVADAR DIRECTORY & CONTACT ACCORDION
        ScrollDownFunctionAccordion(
            title = if (isHindi) "सेवादार मंडल एवं सहायता केंद्र" else "Sevadar Directory & Support",
            subtitle = if (isHindi) "फोन हेल्पलाइन, व्हाट्सएप व सेवादार सूची" else "Direct call, WhatsApp & Sevadars",
            icon = "👥",
            badgeText = "${activeSevadars.size} " + if (isHindi) "सेवादार" else "Sevadars",
            primaryColor = Color(0xFF1565C0)
        ) {
            AccordionOptionRow(
                icon = "📞",
                title = if (isHindi) "आश्रम मुख्य हेल्पलाइन" else "Ashram Helpline Call",
                subtitle = settings.contactPhone.ifEmpty { "+91 98765 00000" },
                actionButtonText = if (isHindi) "कॉल करें" else "Call",
                accentColor = Color(0xFF2E7D32),
                onClick = {
                    val p = settings.contactPhone.ifEmpty { "+919876500000" }
                    try { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$p"))) } catch (e: Exception) {}
                }
            )
            AccordionOptionRow(
                icon = "💬",
                title = if (isHindi) "आधिकारिक व्हाट्सएप सेवा" else "Official WhatsApp Helpdesk",
                subtitle = settings.whatsappNumber.ifEmpty { "+91 98765 43210" },
                actionButtonText = if (isHindi) "मैसेज करें" else "Chat",
                accentColor = Color(0xFF25D366),
                onClick = {
                    val wa = settings.whatsappNumber.replace(Regex("[^0-9]"), "")
                    openSocialMediaLink(context, "https://wa.me/$wa", "https://wa.me/$wa", isWhatsApp = true, errorMessage = "व्हाट्सएप खोलने में असमर्थ")
                }
            )
            for (sev in activeSevadars.take(4)) {
                AccordionOptionRow(
                    icon = "👤",
                    title = sev.name,
                    subtitle = sev.phoneNumber.ifEmpty { "सेवादार" },
                    actionButtonText = if (isHindi) "संपर्क" else "Call",
                    accentColor = Color(0xFF1565C0),
                    onClick = {
                        try { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${sev.phoneNumber}"))) } catch (e: Exception) {}
                    }
                )
            }
        }
    }
}

// =========================================================================================
// 🔽 LAYOUT 7: BHAKTI ACCORDION (भक्ति संगम - पूर्णतः ड्रॉपडाउन केंद्रित)
// Every single service is an interactive expandable accordion that slides down with smooth animations
// =========================================================================================

@Composable
fun BhaktiAccordionLayout(
    settings: AshramSettings,
    isHindi: Boolean,
    currentTheme: SacredTheme,
    activeSevadars: List<Admin>,
    dynamicEvents: List<AshramEvent>,
    onNavigateToToken: () -> Unit,
    onNavigateToFaceToken: () -> Unit,
    onNavigateToYatra: () -> Unit,
    onNavigateToInfo: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    onNavigateToParchas: () -> Unit = {},
    onNavigateToYatraExpenses: () -> Unit = {},
    onThemeChanged: (SacredTheme) -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Top Accordion Header Banner
        Surface(
            color = currentTheme.primaryColor.copy(alpha = 0.08f),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, currentTheme.primaryColor.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🔽", fontSize = 20.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (isHindi) "भक्ति संगम (स्मार्ट ड्रॉपडाउन स्वरूप)" else "Bhakti Sangam (Smart Accordions)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = currentTheme.primaryColor
                    )
                    Text(
                        text = if (isHindi) "किसी भी सेवा पर टैप करें, नीचे सभी विकल्प खुलेंगे" else "Tap any card to slide down all its options",
                        fontSize = 11.sp,
                        color = TextSecondaryDark
                    )
                }
            }
        }

        // Accordion 1: Token Services
        ScrollDownFunctionAccordion(
            title = if (isHindi) "1. टोकन सेवा एवं कतार" else "1. Token & Queue Services",
            subtitle = if (isHindi) "ऑनलाइन टोकन, बायोमेट्रिक, पर्चा व स्टेटस" else "Online token, face scan & status",
            icon = "🎫",
            badgeText = if (isHindi) "मुख्य सेवा" else "Core",
            primaryColor = currentTheme.primaryColor,
            isInitiallyExpanded = true
        ) {
            AccordionOptionRow(
                icon = "🎫",
                title = if (isHindi) "रविवार ऑनलाइन टोकन जारी करें" else "Issue Sunday Token",
                subtitle = if (isHindi) "100% निःशुल्क ऑनलाइन टोकन" else "100% Free Online Token",
                actionButtonText = if (isHindi) "टोकन लें" else "Get Token",
                accentColor = currentTheme.primaryColor,
                onClick = onNavigateToToken
            )
            AccordionOptionRow(
                icon = "📸",
                title = if (isHindi) "बायोमेट्रिक / फेस रिकग्निशन टोकन" else "Face Biometric Token",
                subtitle = if (isHindi) "चेहरे से 1-क्लिक त्वरित टोकन" else "1-click instant face token",
                actionButtonText = if (isHindi) "स्कैन करें" else "Scan",
                accentColor = Color(0xFF2E7D32),
                onClick = onNavigateToFaceToken
            )
            AccordionOptionRow(
                icon = "📜",
                title = if (isHindi) "टोकन पर्चा खोजें व डाउनलोड करें" else "Find & Download Token Parcha",
                subtitle = if (isHindi) "पुराने व नए टोकन पर्चे पीडीएफ में डाउनलोड करें" else "Download sacred token PDF",
                actionButtonText = if (isHindi) "खोजें" else "Search",
                accentColor = Color(0xFF00695C),
                onClick = onNavigateToParchas
            )
        }

        // Accordion 2: Darshan & Aarti
        ScrollDownFunctionAccordion(
            title = if (isHindi) "2. दर्शन, आरती व सत्संग" else "2. Darshan, Aarti & Satsang",
            subtitle = settings.darbarTimings.ifEmpty { "प्रातः एवं सांध्य आरती समय" },
            icon = "🪔",
            primaryColor = currentTheme.secondaryColor
        ) {
            AccordionOptionRow(
                icon = "🪔",
                title = if (isHindi) "आरती एवं दरबार समय सारणी" else "Aarti & Darbar Timings",
                subtitle = settings.darbarTimings.ifEmpty { "प्रातः 8:00 बजे व संध्या 7:00 बजे" },
                accentColor = currentTheme.secondaryColor,
                onClick = onNavigateToInfo
            )
            AccordionOptionRow(
                icon = "👑",
                title = if (isHindi) "पूज्य गुरुजी का सानिध्य व दर्शन" else "Revered Guruji Blessings",
                subtitle = settings.gurujiName.ifEmpty { "पूज्य गुरुजी" },
                accentColor = Color(0xFFC2185B),
                onClick = onNavigateToInfo
            )
            AccordionOptionRow(
                icon = "📜",
                title = if (isHindi) "दरबार नियम एवं सावधानियां" else "Darbar Rules & Guidelines",
                subtitle = if (isHindi) "रविवार दरबार के समस्त नियम व सावधानियां" else "Rules & regulations for visitors",
                actionButtonText = if (isHindi) "नियम" else "Rules",
                accentColor = Color(0xFF512DA8),
                onClick = onNavigateToInfo
            )
        }

        // Accordion 3: Yatra & Expenses
        ScrollDownFunctionAccordion(
            title = if (isHindi) "3. धाम यात्रा एवं धर्मशाला" else "3. Yatra & Accommodation",
            subtitle = if (isHindi) "बस यात्रा बुकिंग, धर्मशाला व खर्च विवरण" else "Yatra bus booking & stay",
            icon = "🚌",
            primaryColor = Color(0xFFE65100)
        ) {
            AccordionOptionRow(
                icon = "🚌",
                title = if (isHindi) "धाम यात्रा पंजीकरण" else "Yatra Registration",
                subtitle = if (isHindi) "आगामी दर्शन बस यात्रा में सीट सुरक्षित करें" else "Reserve seats for Dham visit",
                actionButtonText = if (isHindi) "रजिस्ट्रेशन" else "Register",
                accentColor = Color(0xFFE65100),
                onClick = onNavigateToYatra
            )
            if (settings.canDevoteeViewYatraDiary) {
                AccordionOptionRow(
                    icon = "💰",
                    title = if (isHindi) "यात्रा व्यय एवं हिसाब-किताब" else "Yatra Expense Ledger",
                    subtitle = if (isHindi) "पारदर्शी लेखा-जोखा व खर्च सूची" else "Transparent expense ledger",
                    actionButtonText = if (isHindi) "व्यय देखें" else "Ledger",
                    accentColor = Color(0xFF2E7D32),
                    onClick = onNavigateToYatraExpenses
                )
            }
            AccordionOptionRow(
                icon = "🏨",
                title = if (isHindi) "आश्रम धर्मशाला एवं ठहरने की व्यवस्था" else "Dharamshala Stay Facilities",
                subtitle = settings.address.ifEmpty { "आश्रम परिसर" },
                accentColor = Color(0xFF1565C0),
                onClick = onNavigateToInfo
            )
        }

        // Accordion 4: Sevadars & Contact
        ScrollDownFunctionAccordion(
            title = if (isHindi) "4. सेवादार एवं संपर्क केंद्र" else "4. Sevadars & Contact Hub",
            subtitle = if (isHindi) "हेल्पलाइन फोन, व्हाट्सएप व आश्रम मार्ग" else "Helpline phone, WhatsApp & route",
            icon = "👥",
            primaryColor = Color(0xFF1565C0)
        ) {
            AccordionOptionRow(
                icon = "📞",
                title = if (isHindi) "आश्रम फोन हेल्पलाइन" else "Ashram Helpline Phone",
                subtitle = settings.contactPhone.ifEmpty { "+91 98765 00000" },
                actionButtonText = if (isHindi) "कॉल" else "Call",
                accentColor = Color(0xFF2E7D32),
                onClick = {
                    val p = settings.contactPhone.ifEmpty { "+919876500000" }
                    try { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$p"))) } catch (e: Exception) {}
                }
            )
            AccordionOptionRow(
                icon = "💬",
                title = if (isHindi) "व्हाट्सएप सहायता" else "WhatsApp Support",
                subtitle = settings.whatsappNumber.ifEmpty { "+91 98765 43210" },
                actionButtonText = if (isHindi) "चैट" else "Chat",
                accentColor = Color(0xFF25D366),
                onClick = {
                    val wa = settings.whatsappNumber.replace(Regex("[^0-9]"), "")
                    openSocialMediaLink(context, "https://wa.me/$wa", "https://wa.me/$wa", isWhatsApp = true, errorMessage = "व्हाट्सएप खोलने में असमर्थ")
                }
            )
            AccordionOptionRow(
                icon = "📍",
                title = if (isHindi) "गूगल मैप्स नेविगेशन" else "Google Maps Navigation",
                subtitle = settings.address.ifEmpty { "श्री बालाजी कृपा धाम" },
                actionButtonText = if (isHindi) "रास्ता देखें" else "Map",
                accentColor = Color(0xFFD32F2F),
                onClick = {
                    openSocialMediaLink(context, "https://maps.google.com/?q=${Uri.encode(settings.address.ifEmpty { "Shri Balaji Kripa Dham" })}", "https://maps.google.com", errorMessage = "नक्शा खोलने में असमर्थ")
                }
            )
        }

        // Accordion 5: Admin & Management Portal
        ScrollDownFunctionAccordion(
            title = if (isHindi) "5. प्रबंधक व सेवादार लॉगिन" else "5. Sevadar & Admin Portal",
            subtitle = if (isHindi) "सेवादार एवं सुपर एडमिन पैनल प्रवेश" else "Login for Sevadars & Super Admin",
            icon = "🔐",
            primaryColor = Color(0xFF37474F)
        ) {
            AccordionOptionRow(
                icon = "🔐",
                title = if (isHindi) "एडमिन / सेवादार लॉगिन पोर्टल" else "Admin / Sevadar Login",
                subtitle = if (isHindi) "टोकन सत्यापन, डेस्क रजिस्ट्रेशन व आश्रम नियंत्रण" else "Access token verification & controls",
                actionButtonText = if (isHindi) "प्रवेश" else "Login",
                accentColor = Color(0xFF37474F),
                onClick = onNavigateToAdmin
            )
        }
    }
}

// =========================================================================================
// 🔄 LAYOUT 8: MANDIR PARIKRAMA FLOW (मंदिर परिक्रमा - 4 मुख्य धाम पड़ाव)
// Virtual Circumambulation with horizontal station tabs and expandable shrine details
// =========================================================================================

@Composable
fun MandirParikramaLayout(
    settings: AshramSettings,
    isHindi: Boolean,
    currentTheme: SacredTheme,
    activeSevadars: List<Admin>,
    dynamicEvents: List<AshramEvent>,
    onNavigateToToken: () -> Unit,
    onNavigateToFaceToken: () -> Unit,
    onNavigateToYatra: () -> Unit,
    onNavigateToInfo: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    onNavigateToParchas: () -> Unit = {},
    onNavigateToYatraExpenses: () -> Unit = {},
    onThemeChanged: (SacredTheme) -> Unit
) {
    var selectedStation by remember { mutableIntStateOf(0) }
    val stations = listOf(
        Pair("🎫", if (isHindi) "टोकन द्वार" else "Token Gate"),
        Pair("🪔", if (isHindi) "आरती दर्शन" else "Aarti Darshan"),
        Pair("🚌", if (isHindi) "यात्रा सेवा" else "Yatra Seva"),
        Pair("📜", if (isHindi) "पर्चा कृपा" else "Parcha Grace")
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Parikrama Header
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(2.dp),
            border = BorderStroke(1.dp, currentTheme.secondaryColor.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🔄", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isHindi) "पवित्र मंदिर परिक्रमा (चार मुख्य पड़ाव)" else "Sacred Parikrama (4 Holy Stations)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp,
                        color = currentTheme.primaryColor
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 4 Horizontal Stations Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    stations.forEachIndexed { index, pair ->
                        val isSelected = index == selectedStation
                        Surface(
                            onClick = { selectedStation = index },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) currentTheme.primaryColor else Color(0xFFF5F5F5),
                            border = BorderStroke(
                                if (isSelected) 1.5.dp else 0.8.dp,
                                if (isSelected) currentTheme.secondaryColor else Color(0xFFE0E0E0)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(pair.first, fontSize = 16.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = pair.second,
                                    fontSize = 10.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else TextPrimaryDark,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        // Active Station Content
        when (selectedStation) {
            0 -> {
                // Station 1: Token Gate
                ScrollDownFunctionAccordion(
                    title = if (isHindi) "पड़ाव १: पावन टोकन द्वार" else "Station 1: Holy Token Gate",
                    subtitle = if (isHindi) "ऑनलाइन टोकन, बायोमेट्रिक व पर्चा खोज" else "Online & biometric token options",
                    icon = "🎫",
                    primaryColor = currentTheme.primaryColor,
                    isInitiallyExpanded = true
                ) {
                    AccordionOptionRow(
                        icon = "🎫",
                        title = if (isHindi) "रविवार ऑनलाइन टोकन बुक करें" else "Book Sunday Token",
                        subtitle = if (isHindi) "दरबार में प्रवेश हेतु अनिवार्य" else "Mandatory for Sunday Darbar",
                        actionButtonText = if (isHindi) "टोकन लें" else "Book",
                        accentColor = currentTheme.primaryColor,
                        onClick = onNavigateToToken
                    )
                    AccordionOptionRow(
                        icon = "📸",
                        title = if (isHindi) "बायोमेट्रिक चेहरा टोकन" else "Biometric Face Token",
                        subtitle = if (isHindi) "चेहरा स्कैन करके टोकन लें" else "Face recognition entry",
                        actionButtonText = if (isHindi) "स्कैन" else "Scan",
                        accentColor = Color(0xFF2E7D32),
                        onClick = onNavigateToFaceToken
                    )
                    AccordionOptionRow(
                        icon = "📜",
                        title = if (isHindi) "अपना टोकन पर्चा देखें" else "Search Token Parcha",
                        subtitle = if (isHindi) "टोकन नंबर व स्थिति की जांच" else "Check token status & PDF",
                        actionButtonText = if (isHindi) "खोजें" else "Search",
                        accentColor = Color(0xFF00695C),
                        onClick = onNavigateToParchas
                    )
                }
            }
            1 -> {
                // Station 2: Aarti & Darshan
                ScrollDownFunctionAccordion(
                    title = if (isHindi) "पड़ाव २: आरती एवं दर्शन मंडप" else "Station 2: Aarti & Darshan Pavilion",
                    subtitle = settings.darbarTimings.ifEmpty { "प्रातः व सांध्य आरती" },
                    icon = "🪔",
                    primaryColor = currentTheme.secondaryColor,
                    isInitiallyExpanded = true
                ) {
                    AccordionOptionRow(
                        icon = "🪔",
                        title = if (isHindi) "आरती का पावन समय" else "Sacred Aarti Hours",
                        subtitle = settings.darbarTimings.ifEmpty { "प्रातः 8:00 बजे एवं संध्या 7:00 बजे" },
                        accentColor = currentTheme.secondaryColor,
                        onClick = onNavigateToInfo
                    )
                    AccordionOptionRow(
                        icon = "👑",
                        title = if (isHindi) "पूज्य गुरुजी का आशीर्वाद" else "Guruji Blessings",
                        subtitle = settings.gurujiName.ifEmpty { "पूज्य गुरुजी" },
                        accentColor = Color(0xFFC2185B),
                        onClick = onNavigateToInfo
                    )
                }
            }
            2 -> {
                // Station 3: Yatra & Seva
                ScrollDownFunctionAccordion(
                    title = if (isHindi) "पड़ाव ३: धाम यात्रा एवं धर्मशाला" else "Station 3: Yatra & Seva",
                    subtitle = if (isHindi) "बस यात्रा, खर्च ब्यौरा व धर्मशाला" else "Yatra bus, expense ledger & stay",
                    icon = "🚌",
                    primaryColor = Color(0xFFE65100),
                    isInitiallyExpanded = true
                ) {
                    AccordionOptionRow(
                        icon = "🚌",
                        title = if (isHindi) "धाम यात्रा पंजीकरण" else "Dham Yatra Registration",
                        subtitle = if (isHindi) "आगामी दर्शन यात्रा में सीट बुक करें" else "Reserve your yatra seat",
                        actionButtonText = if (isHindi) "पंजीकरण" else "Register",
                        accentColor = Color(0xFFE65100),
                        onClick = onNavigateToYatra
                    )
                    if (settings.canDevoteeViewYatraDiary) {
                        AccordionOptionRow(
                            icon = "💰",
                            title = if (isHindi) "यात्रा व्यय एवं खर्च" else "Yatra Expenses",
                            subtitle = if (isHindi) "यात्रा का पारदर्शी हिसाब-किताब" else "View trip expense ledger",
                            actionButtonText = if (isHindi) "हिसाब" else "Ledger",
                            accentColor = Color(0xFF2E7D32),
                            onClick = onNavigateToYatraExpenses
                        )
                    }
                }
            }
            3 -> {
                // Station 4: Parcha & Grace
                ScrollDownFunctionAccordion(
                    title = if (isHindi) "पड़ाव ४: पावन पर्चा एवं आश्रम नियम" else "Station 4: Parcha & Ashram Grace",
                    subtitle = if (isHindi) "पर्चे, मार्गदर्शन एवं सेवादार सहायता" else "Parchas, rules & sevadar support",
                    icon = "📜",
                    primaryColor = Color(0xFF00695C),
                    isInitiallyExpanded = true
                ) {
                    AccordionOptionRow(
                        icon = "📜",
                        title = if (isHindi) "टोकन पर्चा खोजें व डाउनलोड करें" else "Download Token Parcha",
                        subtitle = if (isHindi) "अपना पवित्र टोकन पर्चा देखें" else "View & print sacred PDF",
                        actionButtonText = if (isHindi) "पर्चा" else "Parcha",
                        accentColor = Color(0xFF00695C),
                        onClick = onNavigateToParchas
                    )
                    AccordionOptionRow(
                        icon = "👥",
                        title = if (isHindi) "सेवादार मंडल व सहायता" else "Sevadar Support",
                        subtitle = if (isHindi) "आश्रम सेवादारों से संपर्क करें" else "Get help from sevadars",
                        actionButtonText = if (isHindi) "संपर्क" else "Contact",
                        accentColor = Color(0xFF1565C0),
                        onClick = onNavigateToInfo
                    )
                }
            }
        }
    }
}

// =========================================================================================
// 🪷 LAYOUT 9: GOLDEN LOTUS (गोल्डन लोटस - श्वेत पदम)
// Serene pure white-and-gold aesthetic with minimalist sacred geometry
// =========================================================================================

@Composable
fun GoldenLotusLayout(
    settings: AshramSettings,
    isHindi: Boolean,
    currentTheme: SacredTheme,
    activeSevadars: List<Admin>,
    dynamicEvents: List<AshramEvent>,
    onNavigateToToken: () -> Unit,
    onNavigateToFaceToken: () -> Unit,
    onNavigateToYatra: () -> Unit,
    onNavigateToInfo: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    onNavigateToParchas: () -> Unit = {},
    onNavigateToYatraExpenses: () -> Unit = {},
    onThemeChanged: (SacredTheme) -> Unit
) {
    val context = LocalContext.current
    val goldAccent = Color(0xFFC59B27)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Serene Golden Lotus Emblem Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(2.dp),
            border = BorderStroke(1.2.dp, goldAccent.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFF9E6))
                        .border(1.5.dp, goldAccent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🪷", fontSize = 28.sp)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "॥ श्री बालाजी कृपा धाम ॥",
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp,
                    color = Color(0xFF3E2723),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = if (isHindi) "श्वेत पदम शांति एवं दिव्य आशीर्वाद" else "Peace, Serenity & Divine Blessings",
                    fontSize = 11.5.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Expandable Lotus Accordion for Token
        ScrollDownFunctionAccordion(
            title = if (isHindi) "पावन टोकन दर्शन सेवा" else "Sacred Token Darshan",
            subtitle = if (isHindi) "ऑनलाइन, बायोमेट्रिक व पर्चा सेवाएं" else "Online, biometric & parcha services",
            icon = "🎫",
            badgeText = "100% Free",
            primaryColor = goldAccent,
            isInitiallyExpanded = true
        ) {
            AccordionOptionRow(
                icon = "🎫",
                title = if (isHindi) "रविवार ऑनलाइन टोकन" else "Sunday Online Token",
                subtitle = if (isHindi) "दरबार में दर्शन हेतु टोकन प्राप्त करें" else "Book token for Sunday Darbar",
                actionButtonText = if (isHindi) "टोकन लें" else "Get Token",
                accentColor = goldAccent,
                onClick = onNavigateToToken
            )
            AccordionOptionRow(
                icon = "📸",
                title = if (isHindi) "बायोमेट्रिक फेस टोकन" else "Biometric Face Token",
                subtitle = if (isHindi) "चेहरे की पहचान द्वारा त्वरित टोकन" else "Face recognition token",
                actionButtonText = if (isHindi) "चेहरा स्कैन" else "Face Scan",
                accentColor = Color(0xFF2E7D32),
                onClick = onNavigateToFaceToken
            )
            AccordionOptionRow(
                icon = "📜",
                title = if (isHindi) "टोकन पर्चा स्थिति" else "Token Parcha Status",
                subtitle = if (isHindi) "पर्चा डाउनलोड व टोकन नंबर खोजें" else "Download sacred parcha PDF",
                actionButtonText = if (isHindi) "खोजें" else "Search",
                accentColor = Color(0xFF00695C),
                onClick = onNavigateToParchas
            )
        }

        // Expandable Lotus Accordion for Darshan & Yatra
        ScrollDownFunctionAccordion(
            title = if (isHindi) "आरती, उत्सव एवं धाम यात्रा" else "Aarti, Festivals & Yatra",
            subtitle = if (isHindi) "आरती समय, आगामी उत्सव व बस यात्रा" else "Aarti schedule & yatra bus",
            icon = "🪔",
            primaryColor = currentTheme.secondaryColor
        ) {
            AccordionOptionRow(
                icon = "🪔",
                title = if (isHindi) "पावन आरती समय" else "Aarti Timings",
                subtitle = settings.darbarTimings.ifEmpty { "प्रातः 8:00 बजे एवं संध्या 7:00 बजे" },
                accentColor = currentTheme.secondaryColor,
                onClick = onNavigateToInfo
            )
            AccordionOptionRow(
                icon = "🚌",
                title = if (isHindi) "धाम यात्रा पंजीकरण" else "Yatra Registration",
                subtitle = if (isHindi) "आगामी दर्शन यात्रा में सीट सुरक्षित करें" else "Reserve your yatra seat",
                actionButtonText = if (isHindi) "पंजीकरण" else "Register",
                accentColor = Color(0xFFE65100),
                onClick = onNavigateToYatra
            )
            if (settings.canDevoteeViewYatraDiary) {
                AccordionOptionRow(
                    icon = "💰",
                    title = if (isHindi) "यात्रा व्यय हिसाब" else "Yatra Ledger",
                    subtitle = if (isHindi) "पारदर्शी यात्रा व्यय विवरण" else "Trip expenses",
                    actionButtonText = if (isHindi) "हिसाब" else "Ledger",
                    accentColor = Color(0xFF2E7D32),
                    onClick = onNavigateToYatraExpenses
                )
            }
        }

        // Expandable Lotus Accordion for Sevadars & Help
        ScrollDownFunctionAccordion(
            title = if (isHindi) "सेवादार एवं आश्रम संपर्क" else "Sevadars & Contact",
            subtitle = if (isHindi) "हेल्पलाइन, व्हाट्सएप एवं सेवादार मंडल" else "Direct call & WhatsApp support",
            icon = "👥",
            primaryColor = Color(0xFF1565C0)
        ) {
            AccordionOptionRow(
                icon = "📞",
                title = if (isHindi) "आश्रम हेल्पलाइन" else "Ashram Helpline",
                subtitle = settings.contactPhone.ifEmpty { "+91 98765 00000" },
                actionButtonText = if (isHindi) "कॉल" else "Call",
                accentColor = Color(0xFF2E7D32),
                onClick = {
                    val p = settings.contactPhone.ifEmpty { "+919876500000" }
                    try { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$p"))) } catch (e: Exception) {}
                }
            )
            AccordionOptionRow(
                icon = "💬",
                title = if (isHindi) "व्हाट्सएप सेवा" else "WhatsApp Service",
                subtitle = settings.whatsappNumber.ifEmpty { "+91 98765 43210" },
                actionButtonText = if (isHindi) "चैट" else "Chat",
                accentColor = Color(0xFF25D366),
                onClick = {
                    val wa = settings.whatsappNumber.replace(Regex("[^0-9]"), "")
                    openSocialMediaLink(context, "https://wa.me/$wa", "https://wa.me/$wa", isWhatsApp = true, errorMessage = "व्हाट्सएप खोलने में असमर्थ")
                }
            )
        }
    }
}

// =========================================================================================
// ⛩️ LAYOUT 10: SIDDHA PEETH PORTAL (सिद्ध पीठ नेविगेटर)
// Top Segmented Category Bar with instant category filtering and expandable detail cards
// =========================================================================================

@Composable
fun SiddhaPeethPortalLayout(
    settings: AshramSettings,
    isHindi: Boolean,
    currentTheme: SacredTheme,
    activeSevadars: List<Admin>,
    dynamicEvents: List<AshramEvent>,
    onNavigateToToken: () -> Unit,
    onNavigateToFaceToken: () -> Unit,
    onNavigateToYatra: () -> Unit,
    onNavigateToInfo: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    onNavigateToParchas: () -> Unit = {},
    onNavigateToYatraExpenses: () -> Unit = {},
    onThemeChanged: (SacredTheme) -> Unit
) {
    val context = LocalContext.current
    var selectedCategory by remember { mutableIntStateOf(0) }
    val categories = listOf(
        Pair("🌟", if (isHindi) "समस्त" else "All"),
        Pair("🎫", if (isHindi) "टोकन" else "Tokens"),
        Pair("🪔", if (isHindi) "दर्शन" else "Darshan"),
        Pair("🚌", if (isHindi) "यात्रा" else "Yatra"),
        Pair("👥", if (isHindi) "सेवादार" else "Sevadars")
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top Category Bar
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(vertical = 2.dp)
        ) {
            items(categories.size) { index ->
                val cat = categories[index]
                val isSelected = index == selectedCategory
                Surface(
                    modifier = Modifier.clickable { selectedCategory = index },
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) currentTheme.primaryColor else Color.White,
                    border = BorderStroke(
                        if (isSelected) 1.5.dp else 0.8.dp,
                        if (isSelected) currentTheme.secondaryColor else Color(0xFFE0E0E0)
                    ),
                    shadowElevation = if (isSelected) 3.dp else 0.5.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(cat.first, fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = cat.second,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else TextPrimaryDark
                        )
                    }
                }
            }
        }

        // Filtered Function Views
        if (selectedCategory == 0 || selectedCategory == 1) {
            ScrollDownFunctionAccordion(
                title = if (isHindi) "टोकन सेवा केंद्र" else "Token Services",
                subtitle = if (isHindi) "ऑनलाइन, बायोमेट्रिक व पर्चा खोज" else "Online & face recognition tokens",
                icon = "🎫",
                badgeText = "Core",
                primaryColor = currentTheme.primaryColor,
                isInitiallyExpanded = true
            ) {
                AccordionOptionRow(
                    icon = "🎫",
                    title = if (isHindi) "रविवार ऑनलाइन टोकन जारी करें" else "Book Sunday Token",
                    subtitle = if (isHindi) "100% निःशुल्क ऑनलाइन टोकन" else "Online booking for Sunday Darbar",
                    actionButtonText = if (isHindi) "टोकन लें" else "Get Token",
                    accentColor = currentTheme.primaryColor,
                    onClick = onNavigateToToken
                )
                AccordionOptionRow(
                    icon = "📸",
                    title = if (isHindi) "बायोमेट्रिक / फेस टोकन" else "Biometric Face Token",
                    subtitle = if (isHindi) "कैमरे से तुरंत चेहरा स्कैन कर टोकन पाएं" else "Instant face scan entry",
                    actionButtonText = if (isHindi) "स्कैन" else "Scan",
                    accentColor = Color(0xFF2E7D32),
                    onClick = onNavigateToFaceToken
                )
                AccordionOptionRow(
                    icon = "📜",
                    title = if (isHindi) "टोकन पर्चा खोजें व डाउनलोड करें" else "Find & Download Token Parcha",
                    subtitle = if (isHindi) "टोकन स्थिति व पवित्र पर्चा पीडीएफ" else "Check status and download token PDF",
                    actionButtonText = if (isHindi) "पर्चा देखें" else "View",
                    accentColor = Color(0xFF00695C),
                    onClick = onNavigateToParchas
                )
            }
        }

        if (selectedCategory == 0 || selectedCategory == 2) {
            ScrollDownFunctionAccordion(
                title = if (isHindi) "दर्शन, आरती एवं सत्संग" else "Darshan, Aarti & Events",
                subtitle = settings.darbarTimings.ifEmpty { "प्रातः व सांध्य आरती समय" },
                icon = "🪔",
                primaryColor = currentTheme.secondaryColor,
                isInitiallyExpanded = selectedCategory == 2
            ) {
                AccordionOptionRow(
                    icon = "🪔",
                    title = if (isHindi) "आरती एवं दरबार समय सारणी" else "Aarti & Darbar Timings",
                    subtitle = settings.darbarTimings.ifEmpty { "प्रातः 8:00 बजे व संध्या 7:00 बजे" },
                    accentColor = currentTheme.secondaryColor,
                    onClick = onNavigateToInfo
                )
                AccordionOptionRow(
                    icon = "👑",
                    title = if (isHindi) "पूज्य गुरुजी का सानिध्य व दर्शन" else "Revered Guruji Blessings",
                    subtitle = settings.gurujiName.ifEmpty { "पूज्य गुरुजी" },
                    accentColor = Color(0xFFC2185B),
                    onClick = onNavigateToInfo
                )
            }
        }

        if (selectedCategory == 0 || selectedCategory == 3) {
            ScrollDownFunctionAccordion(
                title = if (isHindi) "धाम यात्रा एवं विश्राम व्यवस्था" else "Yatra & Stay",
                subtitle = if (isHindi) "बस यात्रा, खर्च व धर्मशाला" else "Bus booking, stay & expenses",
                icon = "🚌",
                primaryColor = Color(0xFFE65100),
                isInitiallyExpanded = selectedCategory == 3
            ) {
                AccordionOptionRow(
                    icon = "🚌",
                    title = if (isHindi) "धाम यात्रा पंजीकरण" else "Yatra Registration",
                    subtitle = if (isHindi) "आगामी दर्शन यात्रा में सीट सुरक्षित करें" else "Reserve your yatra seat",
                    actionButtonText = if (isHindi) "पंजीकरण" else "Register",
                    accentColor = Color(0xFFE65100),
                    onClick = onNavigateToYatra
                )
                if (settings.canDevoteeViewYatraDiary) {
                    AccordionOptionRow(
                        icon = "💰",
                        title = if (isHindi) "यात्रा व्यय हिसाब" else "Yatra Expense Ledger",
                        subtitle = if (isHindi) "पारदर्शी यात्रा व्यय विवरण" else "Trip expenses",
                        actionButtonText = if (isHindi) "हिसाब" else "Ledger",
                        accentColor = Color(0xFF2E7D32),
                        onClick = onNavigateToYatraExpenses
                    )
                }
            }
        }

        if (selectedCategory == 0 || selectedCategory == 4) {
            ScrollDownFunctionAccordion(
                title = if (isHindi) "सेवादार मंडल एवं सहायता" else "Sevadars & Support",
                subtitle = if (isHindi) "हेल्पलाइन, व्हाट्सएप एवं सेवादार सूची" else "Direct call & WhatsApp support",
                icon = "👥",
                primaryColor = Color(0xFF1565C0),
                isInitiallyExpanded = selectedCategory == 4
            ) {
                AccordionOptionRow(
                    icon = "📞",
                    title = if (isHindi) "आश्रम मुख्य हेल्पलाइन" else "Ashram Helpline Call",
                    subtitle = settings.contactPhone.ifEmpty { "+91 98765 00000" },
                    actionButtonText = if (isHindi) "कॉल करें" else "Call",
                    accentColor = Color(0xFF2E7D32),
                    onClick = {
                        val p = settings.contactPhone.ifEmpty { "+919876500000" }
                        try { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$p"))) } catch (e: Exception) {}
                    }
                )
                AccordionOptionRow(
                    icon = "💬",
                    title = if (isHindi) "व्हाट्सएप सहायता" else "WhatsApp Support",
                    subtitle = settings.whatsappNumber.ifEmpty { "+91 98765 43210" },
                    actionButtonText = if (isHindi) "चैट करें" else "Chat",
                    accentColor = Color(0xFF25D366),
                    onClick = {
                        val wa = settings.whatsappNumber.replace(Regex("[^0-9]"), "")
                        openSocialMediaLink(context, "https://wa.me/$wa", "https://wa.me/$wa", isWhatsApp = true, errorMessage = "व्हाट्सएप खोलने में असमर्थ")
                    }
                )
            }
        }
    }
}

// Reusable Quick Action Bubble for Mahabali Hero layout
@Composable
fun QuickActionBubble(
    icon: String,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.12f))
                .border(1.dp, color.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 20.sp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimaryDark,
            textAlign = TextAlign.Center
        )
    }
}

