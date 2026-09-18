package com.example.shribalajikripadham.ui.panchang

import androidx.compose.animation.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shribalajikripadham.panchang.ChoghadiyaNature
import com.example.shribalajikripadham.panchang.ChoghadiyaSlot
import com.example.shribalajikripadham.panchang.VedicPanchangData
import com.example.shribalajikripadham.panchang.VedicPanchangEngine
import com.example.shribalajikripadham.theme.AmberGold
import com.example.shribalajikripadham.theme.MaroonAccent
import com.example.shribalajikripadham.theme.MaroonPrimary
import com.example.shribalajikripadham.theme.SacredBackgroundLight
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanchangScreen(
    isHindi: Boolean = true,
    onBack: () -> Unit
) {
    var selectedCalendar by remember { mutableStateOf(Calendar.getInstance()) }
    var panchangData by remember { mutableStateOf(VedicPanchangEngine.calculatePanchang(selectedCalendar)) }
    var selectedChoghadiyaTab by remember { mutableIntStateOf(0) } // 0: Day, 1: Night

    // Refresh calculations when calendar changes
    LaunchedEffect(selectedCalendar) {
        panchangData = VedicPanchangEngine.calculatePanchang(selectedCalendar)
    }

    val isToday = remember(selectedCalendar) {
        val today = Calendar.getInstance()
        selectedCalendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                selectedCalendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isHindi) "🚩 श्री बालाजी दैनिक पंचांग" else "🚩 Shri Balaji Daily Panchang",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 17.sp
                        )
                        Text(
                            text = if (isHindi) "डूँगरा जाट • शुभ मुहूर्त व चौघड़िया" else "Dungra Jat • Auspicious Timings & Choghadiya",
                            color = AmberGold,
                            fontSize = 11.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("⬅", color = Color.White, fontSize = 20.sp)
                    }
                },
                actions = {
                    if (!isToday) {
                        TextButton(
                            onClick = { selectedCalendar = Calendar.getInstance() }
                        ) {
                            Text(if (isHindi) "आज" else "Today", color = AmberGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaroonAccent)
            )
        },
        containerColor = SacredBackgroundLight
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { Spacer(modifier = Modifier.height(2.dp)) }

            // 1. DATE NAVIGATION BAR
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(3.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = {
                                val c = selectedCalendar.clone() as Calendar
                                c.add(Calendar.DAY_OF_YEAR, -1)
                                selectedCalendar = c
                            }
                        ) {
                            Text("◀", fontSize = 16.sp, color = MaroonPrimary, fontWeight = FontWeight.Bold)
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = panchangData.dateString,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaroonPrimary
                            )
                            Text(
                                text = "${panchangData.dayOfWeekHindi} • विक्रम संवत् ${panchangData.vikramSamvat}",
                                fontSize = 12.sp,
                                color = Color(0xFF6A1B9A),
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        IconButton(
                            onClick = {
                                val c = selectedCalendar.clone() as Calendar
                                c.add(Calendar.DAY_OF_YEAR, 1)
                                selectedCalendar = c
                            }
                        ) {
                            Text("▶", fontSize = 16.sp, color = MaroonPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 2. ACTIVE CHOGHADIYA BANNER (Only on today)
            if (isToday && panchangData.currentChoghadiya != null) {
                val current = panchangData.currentChoghadiya!!
                item {
                    val bgGradient = if (current.nature.isAuspicious) {
                        Brush.horizontalGradient(listOf(Color(0xFF1B5E20), Color(0xFF388E3C)))
                    } else {
                        Brush.horizontalGradient(listOf(Color(0xFFB71C1C), Color(0xFFE53935)))
                    }

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        elevation = CardDefaults.cardElevation(5.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .background(bgGradient)
                                .padding(14.dp)
                                .fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(Color.White)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isHindi) "वर्तमान में सक्रिय चौघड़िया" else "CURRENT ACTIVE CHOGHADIYA",
                                            color = Color.White.copy(alpha = 0.9f),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${current.nature.labelHindi}",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                    Text(
                                        text = "समय: ${current.startTime} से ${current.endTime}",
                                        color = AmberGold,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Text(
                                    text = if (current.nature.isAuspicious) "🟢 शुभ" else "🔴 त्याज्य",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier
                                        .background(Color.Black.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 3. PANCHANG PRIMARY 5 ANGAS (पंचांग के पांच अंग)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(3.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "🕉️ पंचांग के मुख्य अंग (Vedic Angas)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaroonPrimary
                        )

                        HorizontalDivider(color = Color(0xFFEEEEEE))

                        PanchangDetailRow(label = "तिथि (Tithi)", value = panchangData.tithiHindi, isHighlight = true)
                        PanchangDetailRow(label = "वार (Day)", value = panchangData.dayOfWeekHindi)
                        PanchangDetailRow(label = "नक्षत्र (Nakshatra)", value = panchangData.nakshatraHindi)
                        PanchangDetailRow(label = "योग (Yoga)", value = panchangData.yogaHindi)
                        PanchangDetailRow(label = "करण (Karana)", value = panchangData.karanaHindi)
                        PanchangDetailRow(label = "मास (Month)", value = "${panchangData.maasHindi} (${panchangData.pakshaHindi})")
                        PanchangDetailRow(label = "शक संवत् (Shak)", value = "${panchangData.shakSamvat}")
                    }
                }
            }

            // 4. SUNRISE, SUNSET & AUSPICIOUS / INAUSPICIOUS TIMINGS
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Sunrise & Sunset Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFFFE082)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("🌅 सूर्योदय व सूर्यास्त", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFE65100))
                            Text("सूर्योदय: ${panchangData.sunriseTime}", fontSize = 11.sp, color = Color.DarkGray, fontWeight = FontWeight.Medium)
                            Text("सूर्यास्त: ${panchangData.sunsetTime}", fontSize = 11.sp, color = Color.DarkGray, fontWeight = FontWeight.Medium)
                        }
                    }

                    // Abhijit Muhurat Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFA5D6A7)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("✨ अभिजित मुहूर्त", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF2E7D32))
                            Text(panchangData.abhijitMuhuratTime, fontSize = 11.sp, color = Color.DarkGray, fontWeight = FontWeight.SemiBold)
                            Text("सर्वश्रेष्ठ शुभ समय", fontSize = 10.sp, color = Color(0xFF388E3C))
                        }
                    }
                }
            }

            // 5. RAHU KAAL & INAUSPICIOUS PERIODS
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFFFFCDD2)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⚠️", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "राहुकाल एवं अशुभ समय (त्याज्य समय)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFFC62828)
                            )
                        }
                        Text(
                            text = "• राहुकाल: ${panchangData.rahuKaalTime} (इस समय में नया कार्य न करें)",
                            fontSize = 12.sp,
                            color = Color(0xFFB71C1C),
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "• यमगण्ड काल: ${panchangData.yamagandaTime}",
                            fontSize = 11.sp,
                            color = Color.DarkGray
                        )
                        Text(
                            text = "• गुलिक काल: ${panchangData.gulikaTime}",
                            fontSize = 11.sp,
                            color = Color.DarkGray
                        )
                    }
                }
            }

            // 6. SHRI BALAJI SPECIAL PUJA GUIDANCE
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEDE7F6)),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFFD1C4E9)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🚩", fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "श्री बालाजी कृपा धाम पावन निर्देश",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF4A148C)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = panchangData.balajiSpecialPujaTime,
                                fontSize = 12.sp,
                                color = Color(0xFF311B92),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // 7. CHOGHADIYA TABLE SECTION
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⏰ 24-घंटे सम्पूर्ण चौघड़िया",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaroonPrimary
                        )

                        // Day / Night Toggle
                        TabRow(
                            selectedTabIndex = selectedChoghadiyaTab,
                            modifier = Modifier
                                .width(170.dp)
                                .height(36.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            containerColor = Color(0xFFE0E0E0),
                            indicator = {}
                        ) {
                            Tab(
                                selected = selectedChoghadiyaTab == 0,
                                onClick = { selectedChoghadiyaTab = 0 },
                                text = {
                                    Text(
                                        "☀️ दिन",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedChoghadiyaTab == 0) MaroonPrimary else Color.DarkGray
                                    )
                                }
                            )
                            Tab(
                                selected = selectedChoghadiyaTab == 1,
                                onClick = { selectedChoghadiyaTab = 1 },
                                text = {
                                    Text(
                                        "🌙 रात",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedChoghadiyaTab == 1) MaroonPrimary else Color.DarkGray
                                    )
                                }
                            )
                        }
                    }

                    // Choghadiya legend chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        LegendChip(text = "अमृत / शुभ / लाभ: उत्तम 🟢", color = Color(0xFF2E7D32))
                        LegendChip(text = "चर: सामान्य 🔵", color = Color(0xFF1976D2))
                        LegendChip(text = "काल / रोग / उद्वेग: त्याज्य 🔴", color = Color(0xFFC62828))
                    }
                }
            }

            // 8. CHOGHADIYA SLOTS LIST
            val slotsToShow = if (selectedChoghadiyaTab == 0) panchangData.dayChoghadiya else panchangData.nightChoghadiya
            items(slotsToShow) { slot ->
                ChoghadiyaSlotCard(slot = slot, isToday = isToday)
            }

            // Footer note
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "॥ श्री हनुमते नमः ॥\nयह पंचांग एवं चौघड़िया वैदिक सूर्य सिद्धान्त व द्रिक खगोलीय कलन-विधि द्वारा श्री बालाजी कृपा धाम (डूँगरा जाट) के स्थानीय समय अनुसार 100% स्वतः अद्यतन (Auto-Updated) होता है।",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(12.dp).fillMaxWidth()
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

@Composable
fun PanchangDetailRow(label: String, value: String, isHighlight: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = Color.DarkGray,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            fontSize = if (isHighlight) 14.sp else 13.sp,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.SemiBold,
            color = if (isHighlight) MaroonPrimary else Color.Black
        )
    }
}

@Composable
fun LegendChip(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(0.5.dp, color.copy(alpha = 0.3f))
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            color = color,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun ChoghadiyaSlotCard(slot: ChoghadiyaSlot, isToday: Boolean) {
    val isActive = isToday && slot.isCurrentlyActive
    val borderColor = if (isActive) AmberGold else Color(0xFFE0E0E0)
    val borderWidth = if (isActive) 2.dp else 1.dp

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(borderWidth, borderColor),
        elevation = CardDefaults.cardElevation(if (isActive) 4.dp else 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(slot.nature.colorHex))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = slot.nature.labelHindi,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(slot.nature.colorHex)
                    )
                    if (isActive) {
                        Text(
                            text = "⚡ वर्तमान में सक्रिय",
                            fontSize = 10.sp,
                            color = MaroonPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Text(
                text = "${slot.startTime} - ${slot.endTime}",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.DarkGray
            )
        }
    }
}
