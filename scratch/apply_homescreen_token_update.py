import os
import sys

target = r"C:\Users\hp\.gemini\antigravity\scratch\shri_balaji_kripa_dham\app\src\main\java\com\example\shribalajikripadham\ui\home\HomeScreen.kt"

with open(target, "r", encoding="utf-8") as f:
    code = f.read()

# Add imports
imports_anchor = "import com.example.shribalajikripadham.data.model.AshramSettings\n"
new_imports = """import com.example.shribalajikripadham.data.model.AshramSettings
import com.example.shribalajikripadham.data.model.Token
import com.example.shribalajikripadham.ui.token.PremiumRoyalTokenCard
import com.example.shribalajikripadham.hardware.DeviceFingerprintManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
"""
if "import com.example.shribalajikripadham.data.model.Token" not in code:
    code = code.replace(imports_anchor, new_imports, 1)
    print("Added imports to HomeScreen.kt")

# Add myActiveToken state & polling
state_anchor = "    var settings by remember { mutableStateOf(AshramSettings()) }\n"
new_state = """    var settings by remember { mutableStateOf(AshramSettings()) }
    var myActiveToken by remember { mutableStateOf<Token?>(null) }
"""
if "var myActiveToken" not in code:
    code = code.replace(state_anchor, new_state, 1)
    print("Added myActiveToken state")

# Add token loading LaunchedEffect
sync_anchor = "    // Adaptive Background Telemetry Sync\n"
token_sync_code = """    // Devotee Personal Token Auto-Recovery & Active Tracker
    LaunchedEffect(Unit) {
        while (isActive) {
            try {
                val devId = com.example.shribalajikripadham.hardware.DeviceFingerprintManager.getDeviceId(context)
                val targetDate = if (settings.darbarDate.isNotBlank()) settings.darbarDate else com.example.shribalajikripadham.data.local.DatabaseHelper.getTodayDateString()
                var tok = repository.checkDeviceRegisteredToday(devId, targetDate)
                if (tok == null) {
                    val savedPhone = try {
                        context.getSharedPreferences("sbkd_devotee_my_token_prefs", Context.MODE_PRIVATE).getString("my_phone_number", "") ?: ""
                    } catch (_: Exception) { "" }
                    tok = com.example.shribalajikripadham.data.network.HostingerCentralSyncManager.checkDeviceRegisteredOnServer(devId, targetDate, savedPhone)
                }
                if (tok == null) {
                    tok = com.example.shribalajikripadham.hardware.PersistentTokenReceiptHelper.readPersistentReceipt(devId, targetDate)
                }
                myActiveToken = tok
            } catch (e: Exception) {}
            delay(5_000L)
        }
    }

    // Adaptive Background Telemetry Sync
"""
if "Devotee Personal Token Auto-Recovery" not in code:
    code = code.replace(sync_anchor, token_sync_code, 1)
    print("Added token loading LaunchedEffect")

# In ProHomeTab.DARSHAN: Add active token card right below DailyDarshanHeroCard
hero_anchor = """                            // 1. Daily Consecrated Darshan Card (Hero)
                            DailyDarshanHeroCard(
                                isHindi = isHindi,
                                currentTheme = currentTheme
                            )

                            // 2. Real-Time Darbar & Token Status Card"""

hero_replacement = """                            // 1. Daily Consecrated Darshan Card (Hero)
                            DailyDarshanHeroCard(
                                isHindi = isHindi,
                                currentTheme = currentTheme
                            )

                            // 1b. Devotee's Active Personal Token Card (Hero Prominence)
                            myActiveToken?.let { tok ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onNavigateToToken() },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9E6)),
                                    border = BorderStroke(1.5.dp, Color(0xFFD4AF37)),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            Surface(
                                                shape = CircleShape,
                                                color = MaroonPrimary,
                                                modifier = Modifier.size(46.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = "#${tok.tokenNumber}",
                                                        color = Color(0xFFFFD700),
                                                        fontSize = 18.sp,
                                                        fontWeight = FontWeight.Black
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = "🚩 आपका सक्रिय टोकन: #${tok.tokenNumber}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = MaroonPrimary
                                                )
                                                Text(
                                                    text = "${tok.patientName} • दिनांक: ${tok.darbarDate}",
                                                    fontSize = 11.sp,
                                                    color = Color.DarkGray
                                                )
                                                Text(
                                                    text = if (settings.runningTokenNumber > 0) "वर्तमान में सेवारत: #${settings.runningTokenNumber}" else "कतार में प्रतीक्षारत",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (settings.runningTokenNumber == tok.tokenNumber) Color(0xFF2E7D32) else Color(0xFFE65100)
                                                )
                                            }
                                        }
                                        Surface(
                                            color = MaroonPrimary,
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = "रसीद देखें ➔",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // 2. Real-Time Darbar & Token Status Card"""

if hero_anchor in code:
    code = code.replace(hero_anchor, hero_replacement, 1)
    print("Added active token card to ProHomeTab.DARSHAN")

# In ProHomeTab.TOKEN: Render PremiumRoyalTokenCard when myActiveToken != null
old_token_tab = """                        ProHomeTab.TOKEN -> {
                            SundayTokenActionCard(
                                isHindi = isHindi,
                                currentTheme = currentTheme,
                                isTuesdayEnabled = settings.isTuesdayDarbarEnabled,
                                onNavigateToToken = onNavigateToToken,
                                onNavigateToFaceToken = onNavigateToFaceToken,
                                onNavigateToTuesdayToken = onNavigateToTuesdayToken
                            )

                            LiveDarbarStatusCard(
                                isHindi = isHindi,
                                settings = settings,
                                scheduleState = scheduleState,
                                currentTheme = currentTheme,
                                onNavigateToLiveDarbar = onNavigateToLiveDarbar,
                                onNavigateToToken = onNavigateToToken
                            )
                        }"""

new_token_tab = """                        ProHomeTab.TOKEN -> {
                            if (myActiveToken != null) {
                                PremiumRoyalTokenCard(
                                    token = myActiveToken!!,
                                    settings = settings,
                                    isHindi = isHindi,
                                    onBackToHome = { selectedTab = ProHomeTab.DARSHAN }
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedButton(
                                    onClick = onNavigateToToken,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.2.dp, MaroonPrimary)
                                ) {
                                    Text(
                                        text = if (isHindi) "➕ अन्य परिजन हेतु नया टोकन बनाएं" else "➕ Issue Another Token for Family",
                                        fontWeight = FontWeight.Bold,
                                        color = MaroonPrimary,
                                        fontSize = 13.sp
                                    )
                                }
                            } else {
                                SundayTokenActionCard(
                                    isHindi = isHindi,
                                    currentTheme = currentTheme,
                                    isTuesdayEnabled = settings.isTuesdayDarbarEnabled,
                                    onNavigateToToken = onNavigateToToken,
                                    onNavigateToFaceToken = onNavigateToFaceToken,
                                    onNavigateToTuesdayToken = onNavigateToTuesdayToken
                                )

                                LiveDarbarStatusCard(
                                    isHindi = isHindi,
                                    settings = settings,
                                    scheduleState = scheduleState,
                                    currentTheme = currentTheme,
                                    onNavigateToLiveDarbar = onNavigateToLiveDarbar,
                                    onNavigateToToken = onNavigateToToken
                                )
                            }
                        }"""

if old_token_tab in code:
    code = code.replace(old_token_tab, new_token_tab, 1)
    print("Enhanced ProHomeTab.TOKEN with Royal Token Card view!")

with open(target, "w", encoding="utf-8") as f:
    f.write(code)

print("Saved HomeScreen.kt successfully!")
