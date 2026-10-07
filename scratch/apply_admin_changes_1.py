import re

file_path = r'app\src\main\java\com\example\shribalajikripadham\ui\admin\AdminDashboardScreen.kt'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Update refreshData to populate havan inputs
old_refresh = """            customSundayTokenBannerTitle = s.sundayTokenBannerTitle
            customSundayTokenBannerText = s.sundayTokenBannerText
            customSundayTokenCustomNotice = s.sundayTokenCustomNotice"""
new_refresh = """            customSundayTokenBannerTitle = s.sundayTokenBannerTitle
            customSundayTokenBannerText = s.sundayTokenBannerText
            customSundayTokenCustomNotice = s.sundayTokenCustomNotice
            havanCostInput = s.havanEstimatedCost.toString()
            havanRulesInput = s.havanRulesAndExpenses"""

if old_refresh in content:
    content = content.replace(old_refresh, new_refresh, 1)
    print("1. refreshData havan inputs added.")
else:
    print("1. WARNING: refreshData target not found.")

# 2. Clean up allowedTabs
old_bus_tab = """            if (isSuper || (admin.canManageYatra && settings.isBusBookingLive)) {
                allowedTabs.add(if (isHindi) "बस बुकिंग लेजर" else "Bus Ledger")
            }"""
if old_bus_tab in content:
    content = content.replace(old_bus_tab, "// Bus booking ledger removed (feature not in main app)", 1)
    print("2a. Bus ledger tab removed.")
else:
    print("2a. WARNING: Bus ledger tab target not found.")

old_ui_website = """            if (isSuper || admin.canManageUiControl) {
                allowedTabs.add(if (isHindi) "UI बॉक्स कंट्रोल" else "UI Control")
            }
            if (isSuper || admin.canManageWebsite) {
                allowedTabs.add(if (isHindi) "🌐 वेबसाइट लाइव एडिटर" else "Website Live Editor")
                allowedTabs.add(if (isHindi) "🌐 वेबसाइट व CMS" else "Website & CMS")
            }"""
if old_ui_website in content:
    content = content.replace(old_ui_website, "// UI Control and Website CMS tabs removed (features not in main app)", 1)
    print("2b. UI Control and Website CMS tabs removed.")
else:
    print("2b. WARNING: UI Control and Website CMS tabs target not found.")

old_distances = """            if (isSuper || admin.canManageDistances) {
                allowedTabs.add(if (isHindi) "कस्टम दूरियाँ" else "Distances")
            }"""
if old_distances in content:
    content = content.replace(old_distances, "// Distances tab removed (feature not in main app)", 1)
    print("2c. Distances tab removed.")
else:
    print("2c. WARNING: Distances tab target not found.")

# 3. Replace the Super Admin Top Deck with the Executive Pro Command Deck
old_deck_start = "if (isSuper) {\n                        // COMPACT 1-LINE ADMIN STATUS BAR (LEAVING 95% SCREEN FOR GRID/LIST)"
old_deck_end = "AdminHubDashboardView(\n                            isHindi = isHindi,"

# Let's verify start and end positions
idx_start = content.find(old_deck_start)
idx_end = content.find(old_deck_end, idx_start)

if idx_start != -1 and idx_end != -1:
    new_deck = """if (isSuper) {
                        // =========================================================================
                        // 👑 EXECUTIVE PRO-TIER SUPER ADMIN COMMAND DECK (सर्वोच्च व्यवस्थापक कंसोल)
                        // =========================================================================
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.5.dp, AmberGold),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                // --- 1. ROYAL INSIGNIA HEADER ---
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFFFF9EE),
                                    border = BorderStroke(1.dp, Color(0xFFFFD54F)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                Text("🚩", fontSize = 24.sp)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(
                                                        text = "श्री बालाजी कृपा धाम • सुपर एडमिन सुप्रीम कंसोल",
                                                        fontWeight = FontWeight.ExtraBold,
                                                        fontSize = 14.5.sp,
                                                        color = MaroonPrimary
                                                    )
                                                    Text(
                                                        text = "सर्वोच्च व्यवस्थापक: श्री अंकित चौधरी (पूर्ण प्रशासनिक अधिकार)",
                                                        fontSize = 11.sp,
                                                        color = Color(0xFF5D4037),
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            }

                                            // Logout Exit Button
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color(0xFFFFEBEE),
                                                border = BorderStroke(0.8.dp, Color(0xFFEF9A9A)),
                                                modifier = Modifier.clickable { showLogoutExitDialog = true }
                                            ) {
                                                Text(
                                                    text = "🚪 बाहर निकलें",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFC62828),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Real-time telemetry & Quick action chips
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            // Telemetry Indicators
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFE8F5E9)) {
                                                    Text(
                                                        text = "📱 $telemetryTotalDevices डिवाइस",
                                                        fontSize = 10.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF2E7D32),
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                                Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFE3F2FD)) {
                                                    Text(
                                                        text = "☁️ Hostinger Live",
                                                        fontSize = 10.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF1565C0),
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }

                                            // Toolbar actions
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = Color(0xFFEDE7F6),
                                                    border = BorderStroke(0.7.dp, Color(0xFFB39DDB)),
                                                    modifier = Modifier.clickable {
                                                        val file = com.example.shribalajikripadham.util.AshramManualPdfGenerator.generateAdminGuidePdf(context)
                                                        if (file != null) {
                                                            com.example.shribalajikripadham.util.AshramManualPdfGenerator.openOrSharePdf(
                                                                context, file,
                                                                if (isHindi) "श्री बालाजी कृपा धाम - व्यवस्थापक मार्गदर्शिका" else "Shri Balaji Kripa Dham - Admin Manual"
                                                            )
                                                        } else {
                                                            Toast.makeText(context, if (isHindi) "PDF तैयार करने में असमर्थ" else "Failed to generate PDF", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                ) {
                                                    Text(
                                                        text = "📘 गाइड",
                                                        fontSize = 10.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF4A148C),
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                                    )
                                                }

                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = Color(0xFFFFF3E0),
                                                    border = BorderStroke(0.7.dp, Color(0xFFFFB74D)),
                                                    modifier = Modifier.clickable {
                                                        refreshData()
                                                        Toast.makeText(context, "🔄 डेटा रिफ्रेश हुआ", Toast.LENGTH_SHORT).show()
                                                    }
                                                ) {
                                                    Text(
                                                        text = "🔄 रिफ्रेश",
                                                        fontSize = 10.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFFE65100),
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                                    )
                                                }

                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = Color(0xFFE8F5E9),
                                                    border = BorderStroke(0.7.dp, Color(0xFF81C784)),
                                                    modifier = Modifier.clickable {
                                                        ownNewPassword = ""
                                                        ownNewPin = ""
                                                        ownCredentialsErrorMsg = null
                                                        showChangeOwnCredentialsDialog = true
                                                    }
                                                ) {
                                                    Text(
                                                        text = "🔐 पिन",
                                                        fontSize = 10.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF1B5E20),
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // --- 2. FOUR HIGH-IMPACT MASTER CARDS ---
                                // CARD 1: 🚩 TUESDAY DARBAR MASTER SWITCH
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (settings.isTuesdayDarbarEnabled) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                                    border = BorderStroke(1.2.dp, if (settings.isTuesdayDarbarEnabled) Color(0xFF2E7D32) else Color(0xFFFF9800)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = if (settings.isTuesdayDarbarEnabled) "🚩 मंगलवार विशेष दरबार: ● सक्रिय (चालू)" else "🛑 मंगलवार विशेष दरबार: ○ निष्क्रिय (बंद)",
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 13.5.sp,
                                                    color = if (settings.isTuesdayDarbarEnabled) Color(0xFF1B5E20) else Color(0xFFBF360C)
                                                )
                                            }
                                            Text(
                                                text = if (settings.isTuesdayDarbarEnabled) "भक्तों के फोन में मंगलवार दरबार की लाइव बुकिंग व टोकन सेवा चालू है" else "दरबार बंद है (1-क्लिक में यहीं से चालू अथवा बंद करें)",
                                                fontSize = 11.sp,
                                                color = Color.DarkGray
                                            )
                                        }

                                        Switch(
                                            checked = settings.isTuesdayDarbarEnabled,
                                            onCheckedChange = { isChecked ->
                                                scope.launch {
                                                    repository.updateTuesdayDarbarSettings(
                                                        isEnabled = isChecked,
                                                        name = settings.tuesdayDarbarName,
                                                        address = settings.tuesdayDarbarAddress,
                                                        latitude = settings.tuesdayLatitude,
                                                        longitude = settings.tuesdayLongitude,
                                                        allowedRadiusMeters = settings.tuesdayAllowedRadiusMeters,
                                                        outstationMinDistanceKm = settings.tuesdayOutstationMinDistanceKm,
                                                        timings = settings.tuesdayDarbarTimings,
                                                        tokenServiceMode = settings.tuesdayTokenServiceMode,
                                                        tokenNotice = settings.tuesdayTokenNotice
                                                    )
                                                    settings = repository.getSettings()
                                                    Toast.makeText(context, if (isChecked) "🚩 मंगलवार दरबार चालू हो गया!" else "🛑 मंगलवार दरबार बंद कर दिया गया", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color.White,
                                                checkedTrackColor = Color(0xFF2E7D32),
                                                uncheckedThumbColor = Color.White,
                                                uncheckedTrackColor = Color.Gray
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // CARD 2: 🔥 HAWAN COST (₹14,000) & RULES EDITOR
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFFFF8E1),
                                    border = BorderStroke(1.2.dp, Color(0xFFFFB300)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "🔥 महा-हवन सेवा दक्षिणा:",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = MaroonPrimary
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = SaffronPrimary
                                                ) {
                                                    Text(
                                                        text = "₹${settings.havanEstimatedCost}",
                                                        fontWeight = FontWeight.ExtraBold,
                                                        fontSize = 12.sp,
                                                        color = Color.White,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "यजमान संकल्प, आहुति नियम व सामग्री खर्च (संपादित करने पर लाइव सिंक)",
                                                fontSize = 11.sp,
                                                color = Color.DarkGray
                                            )
                                        }

                                        Button(
                                            onClick = {
                                                havanCostInput = settings.havanEstimatedCost.toString()
                                                havanRulesInput = settings.havanRulesAndExpenses
                                                showHavanCostDialog = true
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                            modifier = Modifier.height(36.dp)
                                        ) {
                                            Text("✏️ राशि बदलें", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // CARD 3: 💬 MASTER CHATS OVERSIGHT (ALL SEVADARS & DEVOTEES)
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFEDE7F6),
                                    border = BorderStroke(1.2.dp, Color(0xFF7E57C2)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "💬 समस्त सेवादार लाइव चैट्स",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = Color(0xFF4A148C)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = Color(0xFFD1C4E9)
                                                ) {
                                                    Text(
                                                        text = "100% पारदर्शी",
                                                        fontSize = 9.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF311B92),
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "समस्त सेवादारों व भक्तों के सभी संदेशों की लाइव सुप्रीम निगरानी",
                                                fontSize = 11.sp,
                                                color = Color.DarkGray
                                            )
                                        }

                                        Button(
                                            onClick = {
                                                val idx = allowedTabs.indexOfFirst { it.contains("चैट") || it.contains("Helpdesk") || it.contains("सुझाव") }
                                                if (idx >= 0) {
                                                    selectedTab = idx
                                                    activeScreenTitle = allowedTabs[idx]
                                                }
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF512DA8)),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                            modifier = Modifier.height(36.dp)
                                        ) {
                                            Text("चैट्स देखें ➜", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // CARD 4: 📢 INSTANT BROADCAST PUSH NOTIFICATION
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFE0F2FE),
                                    border = BorderStroke(1.2.dp, Color(0xFF0284C7)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "📢 तात्कालिक महा-प्रसारण (Push Alert)",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = Color(0xFF0369A1)
                                            )
                                            Text(
                                                text = "सभी जुड़े हुए $telemetryTotalDevices भक्तों के फोन पर तत्काल सूचना या अलर्ट भेजें",
                                                fontSize = 11.sp,
                                                color = Color.DarkGray
                                            )
                                        }

                                        Button(
                                            onClick = {
                                                val idx = allowedTabs.indexOfFirst { it.contains("सूचना") || it.contains("Broadcast") }
                                                if (idx >= 0) {
                                                    selectedTab = idx
                                                    activeScreenTitle = allowedTabs[idx]
                                                }
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                            modifier = Modifier.height(36.dp)
                                        ) {
                                            Text("अलर्ट भेजें ➜", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    """
    content = content[:idx_start] + new_deck + content[idx_end:]
    print("3. Executive Pro Super Admin Command Deck installed.")
else:
    print(f"3. WARNING: Could not locate Deck start ({idx_start}) or end ({idx_end}).")

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)
print("File written successfully.")
