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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
                        size = 58.dp,
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
                                size = 44.dp,
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
                        size = 54.dp,
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
                                size = 46.dp,
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
                        size = 46.dp,
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
                            size = 64.dp,
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
                                size = 42.dp,
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
