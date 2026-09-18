package com.example.shribalajikripadham.ui.granth

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shribalajikripadham.data.sacred.HanumanBahukData
import com.example.shribalajikripadham.data.sacred.HanumanashtakData
import com.example.shribalajikripadham.data.sacred.SundarkandData
import com.example.shribalajikripadham.theme.AmberGold
import com.example.shribalajikripadham.theme.MaroonAccent
import com.example.shribalajikripadham.theme.MaroonPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

enum class SacredReaderTheme(val label: String, val bgColor: Color, val cardColor: Color, val textColor: Color, val meaningColor: Color, val accentColor: Color) {
    PARCHMENT("📜 पीताम्बरी", Color(0xFFFFF9E6), Color(0xFFFFFDF5), Color(0xFF3E1208), Color(0xFF5D2E14), Color(0xFFB8860B)),
    LIGHT("☀️ श्वेत", Color(0xFFF5F5F5), Color(0xFFFFFFFF), Color(0xFF1E1E1E), Color(0xFF37474F), MaroonPrimary),
    DARK("🌙 रात्रि", Color(0xFF121212), Color(0xFF1E1E1E), Color(0xFFEEEEEE), Color(0xFFCFD8DC), AmberGold)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SacredGranthScreen(
    isHindi: Boolean = true,
    initialTab: Int = 0, // 0: Sundarkand, 1: Hanuman Bahuk, 2: Hanumanashtak
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("sbkd_sacred_reader_prefs", Context.MODE_PRIVATE) }

    var selectedTab by remember { mutableIntStateOf(prefs.getInt("saved_tab", initialTab)) }
    var fontSizeSp by remember { mutableIntStateOf(prefs.getInt("saved_font_size", 18)) }
    var selectedTheme by remember {
        val themeIdx = prefs.getInt("saved_theme_idx", 0)
        mutableStateOf(SacredReaderTheme.values().getOrElse(themeIdx) { SacredReaderTheme.PARCHMENT })
    }

    // Auto-Scroll states
    var isAutoScrolling by remember { mutableStateOf(false) }
    var scrollSpeedLevel by remember { mutableIntStateOf(1) } // 0: Slow, 1: Medium, 2: Fast
    val listState = rememberLazyListState()

    // Jump to Doha dialog state
    var showJumpDialog by remember { mutableStateOf(false) }
    var jumpTargetInput by remember { mutableStateOf("") }

    // Save preferences on change
    LaunchedEffect(selectedTab, fontSizeSp, selectedTheme) {
        prefs.edit()
            .putInt("saved_tab", selectedTab)
            .putInt("saved_font_size", fontSizeSp)
            .putInt("saved_theme_idx", selectedTheme.ordinal)
            .apply()
    }

    // Auto-scroll coroutine
    LaunchedEffect(isAutoScrolling, scrollSpeedLevel) {
        if (isAutoScrolling) {
            val delayMs = when (scrollSpeedLevel) {
                0 -> 120L // Slow
                1 -> 75L  // Medium
                else -> 40L // Fast
            }
            while (isActive && isAutoScrolling) {
                delay(delayMs)
                try {
                    listState.scrollBy(2f)
                } catch (e: Exception) {
                    isAutoScrolling = false
                    break
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = when (selectedTab) {
                                0 -> if (isHindi) "📖 सम्पूर्ण सुंदरकाण्ड" else "📖 Shri Sundarkand"
                                1 -> if (isHindi) "🛡️ सम्पूर्ण हनुमान बाहुक" else "🛡️ Shri Hanuman Bahuk"
                                else -> if (isHindi) "🚩 संकटमोचन हनुमानाष्टक" else "🚩 Sankatmochan Hanumanashtak"
                            },
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 17.sp
                        )
                        Text(
                            text = if (isHindi) "अर्थ सहित • १०८% ऑफ़लाइन पावन पाठ" else "With Meanings • 100% Offline Reader",
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
                    // Jump button for Sundarkand / Bahuk
                    if (selectedTab in 0..1) {
                        IconButton(onClick = { showJumpDialog = true }) {
                            Text("🔍", fontSize = 16.sp)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaroonAccent)
            )
        },
        bottomBar = {
            // SACRED READER CONTROL TOOLBAR
            Surface(
                color = selectedTheme.cardColor,
                shadowElevation = 8.dp,
                border = BorderStroke(0.5.dp, Color.Gray.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Font Size Adjuster
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { if (fontSizeSp > 14) fontSizeSp -= 2 },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Text("A-", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = selectedTheme.textColor)
                        }

                        Text(
                            text = "${fontSizeSp}sp",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = selectedTheme.accentColor,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )

                        IconButton(
                            onClick = { if (fontSizeSp < 28) fontSizeSp += 2 },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Text("A+", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = selectedTheme.textColor)
                        }
                    }

                    // Theme Cycler
                    TextButton(
                        onClick = {
                            val nextIdx = (selectedTheme.ordinal + 1) % SacredReaderTheme.values().size
                            selectedTheme = SacredReaderTheme.values()[nextIdx]
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(selectedTheme.label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Auto-Scroll Button
                    Button(
                        onClick = { isAutoScrolling = !isAutoScrolling },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isAutoScrolling) MaroonPrimary else selectedTheme.accentColor
                        ),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            text = if (isAutoScrolling) "⏸️ रोकें" else "▶️ ऑटो स्क्रॉल",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        },
        containerColor = selectedTheme.bgColor
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. GRANTH SELECTION TABS
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = selectedTheme.cardColor,
                contentColor = MaroonPrimary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                        isAutoScrolling = false
                    },
                    text = {
                        Text(
                            "सुंदरकाण्ड (६०)",
                            fontSize = 12.sp,
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        isAutoScrolling = false
                    },
                    text = {
                        Text(
                            "हनुमान बाहुक (४४)",
                            fontSize = 12.sp,
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = {
                        selectedTab = 2
                        isAutoScrolling = false
                    },
                    text = {
                        Text(
                            "हनुमानाष्टक",
                            fontSize = 12.sp,
                            fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }

            // Auto-scroll active status chip
            if (isAutoScrolling) {
                Surface(
                    color = MaroonPrimary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "⏳ स्वतः पाठ स्क्रॉल चालू है...",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            SpeedChip(label = "धीमी", isSelected = scrollSpeedLevel == 0) { scrollSpeedLevel = 0 }
                            SpeedChip(label = "मध्यम", isSelected = scrollSpeedLevel == 1) { scrollSpeedLevel = 1 }
                            SpeedChip(label = "तेज़", isSelected = scrollSpeedLevel == 2) { scrollSpeedLevel = 2 }
                        }
                    }
                }
            }

            // 2. SACRED TEXT CONTENT BY TAB
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item { Spacer(modifier = Modifier.height(4.dp)) }

                when (selectedTab) {
                    0 -> {
                        // TAB 0: SUNDARKAND (60 Dohas)
                        item {
                            MangalacharanCard(
                                title = "॥ श्री रामचरितमानस: सुंदरकाण्ड मंगलाचरण ॥",
                                devanagariText = SundarkandData.mangalacharan,
                                meaning = SundarkandData.mangalacharanMeaning,
                                theme = selectedTheme,
                                fontSize = fontSizeSp
                            )
                        }

                        itemsIndexed(SundarkandData.episodes) { index, episode ->
                            SundarkandEpisodeCard(
                                episode = episode,
                                theme = selectedTheme,
                                fontSize = fontSizeSp
                            )
                        }
                    }
                    1 -> {
                        // TAB 1: HANUMAN BAHUK (44 Pads)
                        item {
                            IntroductoryNoticeCard(
                                title = HanumanBahukData.title,
                                note = HanumanBahukData.introductoryNote,
                                theme = selectedTheme
                            )
                        }

                        itemsIndexed(HanumanBahukData.verses) { index, verse ->
                            SacredVerseCard(
                                verse = verse,
                                theme = selectedTheme,
                                fontSize = fontSizeSp
                            )
                        }
                    }
                    2 -> {
                        // TAB 2: SANKATMOCHAN HANUMANASHTAK (8 Stanzas + Mangalacharan + Doha)
                        item {
                            MangalacharanCard(
                                title = HanumanashtakData.mangalacharan.title,
                                devanagariText = HanumanashtakData.mangalacharan.devanagariText,
                                meaning = HanumanashtakData.mangalacharan.hindiMeaning,
                                theme = selectedTheme,
                                fontSize = fontSizeSp
                            )
                        }

                        itemsIndexed(HanumanashtakData.verses) { index, verse ->
                            SacredVerseCard(
                                verse = verse,
                                theme = selectedTheme,
                                fontSize = fontSizeSp
                            )
                        }

                        item {
                            SacredVerseCard(
                                verse = HanumanashtakData.concludingDoha,
                                theme = selectedTheme,
                                fontSize = fontSizeSp
                            )
                        }
                    }
                }

                item {
                    // Sacred concluding card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = selectedTheme.cardColor),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "॥ श्री सीताराम ॥\n॥ श्री हनुमते नमः ॥\n॥ श्री बालाजी कृपा धाम (डूँगरा जाट) ॥",
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                                color = selectedTheme.accentColor,
                                fontSize = (fontSizeSp - 2).coerceAtLeast(13).sp,
                                lineHeight = 22.sp
                            )
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(30.dp)) }
            }
        }
    }

    // JUMP TO DOHA / PAD DIALOG
    if (showJumpDialog) {
        val maxNumber = if (selectedTab == 0) 60 else 44
        val typeLabel = if (selectedTab == 0) "दोहा" else "पद"

        AlertDialog(
            onDismissRequest = { showJumpDialog = false },
            title = {
                Text(
                    text = "🔍 $typeLabel संख्या पर जाएँ (१ से $maxNumber)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "सीधे उस $typeLabel पर जाने के लिए संख्या दर्ज करें:",
                        fontSize = 13.sp,
                        color = Color.DarkGray
                    )
                    OutlinedTextField(
                        value = jumpTargetInput,
                        onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) jumpTargetInput = it },
                        label = { Text("$typeLabel संख्या") },
                        placeholder = { Text("उदा. 15") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = jumpTargetInput.toIntOrNull()
                        if (target != null && target in 1..maxNumber) {
                            showJumpDialog = false
                            jumpTargetInput = ""
                            // Jump list state
                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).run {
                                try {
                                    listState.requestScrollToItem(target)
                                } catch (e: Exception) {}
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary)
                ) {
                    Text("खोलें", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showJumpDialog = false }) {
                    Text("रद्द करें")
                }
            }
        )
    }
}

@Composable
fun SpeedChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (isSelected) AmberGold else Color.White.copy(alpha = 0.2f),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) MaroonPrimary else Color.White,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun MangalacharanCard(
    title: String,
    devanagariText: String,
    meaning: String,
    theme: SacredReaderTheme,
    fontSize: Int
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = theme.cardColor),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, theme.accentColor.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(3.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                color = theme.accentColor,
                fontSize = (fontSize - 2).coerceAtLeast(14).sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider(color = theme.accentColor.copy(alpha = 0.2f))

            Text(
                text = devanagariText,
                color = theme.textColor,
                fontSize = fontSize.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = (fontSize + 8).sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider(color = theme.accentColor.copy(alpha = 0.2f))

            Text(
                text = meaning,
                color = theme.meaningColor,
                fontSize = (fontSize - 3).coerceAtLeast(13).sp,
                lineHeight = (fontSize + 5).sp
            )
        }
    }
}

@Composable
fun IntroductoryNoticeCard(title: String, note: String, theme: SacredReaderTheme) {
    Card(
        colors = CardDefaults.cardColors(containerColor = theme.cardColor),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, theme.accentColor.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                color = theme.accentColor,
                fontSize = 15.sp
            )
            Text(
                text = note,
                color = theme.meaningColor,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
        }
    }
}

@Composable
fun SundarkandEpisodeCard(
    episode: com.example.shribalajikripadham.data.sacred.SundarkandDohaEpisode,
    theme: SacredReaderTheme,
    fontSize: Int
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = theme.cardColor),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(0.5.dp, theme.accentColor.copy(alpha = 0.25f)),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header with Doha Number Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = episode.title,
                    fontWeight = FontWeight.Bold,
                    color = theme.accentColor,
                    fontSize = (fontSize - 3).coerceAtLeast(13).sp
                )
                Surface(
                    color = theme.accentColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "दोहा #${episode.dohaNumber}",
                        fontWeight = FontWeight.Bold,
                        color = theme.accentColor,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            HorizontalDivider(color = theme.accentColor.copy(alpha = 0.15f))

            // Chaupai Text
            if (episode.shlokaOrChaupai.isNotBlank()) {
                Text(
                    text = episode.shlokaOrChaupai,
                    color = theme.textColor,
                    fontSize = fontSize.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = (fontSize + 8).sp
                )
            }

            // Doha Text Highlighted
            Surface(
                color = theme.accentColor.copy(alpha = 0.08f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(0.5.dp, theme.accentColor.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = episode.dohaText,
                    color = theme.textColor,
                    fontSize = fontSize.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = (fontSize + 8).sp,
                    modifier = Modifier.padding(10.dp)
                )
            }

            // Hindi Meaning (भावार्थ)
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "॥ भावार्थ ॥",
                    fontWeight = FontWeight.Bold,
                    color = theme.accentColor,
                    fontSize = 11.sp
                )
                Text(
                    text = episode.hindiMeaning,
                    color = theme.meaningColor,
                    fontSize = (fontSize - 3).coerceAtLeast(13).sp,
                    lineHeight = (fontSize + 6).sp
                )
            }
        }
    }
}

@Composable
fun SacredVerseCard(
    verse: com.example.shribalajikripadham.data.sacred.SacredVerse,
    theme: SacredReaderTheme,
    fontSize: Int
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = theme.cardColor),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(0.5.dp, theme.accentColor.copy(alpha = 0.25f)),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = verse.title,
                fontWeight = FontWeight.Bold,
                color = theme.accentColor,
                fontSize = (fontSize - 3).coerceAtLeast(13).sp
            )

            HorizontalDivider(color = theme.accentColor.copy(alpha = 0.15f))

            // Main Devanagari text
            Text(
                text = verse.devanagariText,
                color = theme.textColor,
                fontSize = fontSize.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = (fontSize + 8).sp
            )

            // Hindi Meaning
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "॥ सरल भावार्थ ॥",
                    fontWeight = FontWeight.Bold,
                    color = theme.accentColor,
                    fontSize = 11.sp
                )
                Text(
                    text = verse.hindiMeaning,
                    color = theme.meaningColor,
                    fontSize = (fontSize - 3).coerceAtLeast(13).sp,
                    lineHeight = (fontSize + 6).sp
                )
            }
        }
    }
}
