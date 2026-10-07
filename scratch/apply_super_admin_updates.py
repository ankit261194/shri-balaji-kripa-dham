import os
import sys

target_file = r"C:\Users\hp\.gemini\antigravity\scratch\shri_balaji_kripa_dham\app\src\main\java\com\example\shribalajikripadham\ui\admin\AdminDashboardScreen.kt"

with open(target_file, "r", encoding="utf-8") as f:
    content = f.read()

# 1. Check for old bulky cards
old_marker_start = "                        if (isSuper) {\n                            Card(\n                                modifier = Modifier\n                                    .fillMaxWidth()\n                                    .padding(horizontal = 4.dp, vertical = 6.dp),\n                                shape = RoundedCornerShape(14.dp),\n                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDF5)),"
old_marker_end = '                                                Text("भेजें 🚀", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)\n                                            }\n                                        }\n                                    }\n                                }\n                            }\n                            Spacer(modifier = Modifier.height(6.dp))\n                        }'

new_executive_bar = '''                        if (isSuper) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                border = BorderStroke(1.2.dp, Color(0xFFD4AF37)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp)
                                ) {
                                    // Row 1: Executive Deck Header & Status LED
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("👑", fontSize = 14.sp)
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(
                                                text = if (isHindi) "कार्यकारी नियंत्रण डेक" else "Executive Command Deck",
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 12.sp,
                                                color = Color(0xFFFFD700)
                                            )
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(7.dp)
                                                    .clip(CircleShape)
                                                    .background(if (settings.isTuesdayDarbarEnabled) Color(0xFF4CAF50) else Color(0xFFFF9800))
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (settings.isTuesdayDarbarEnabled) "मंगलवार: चालू" else "मंगलवार: बंद",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (settings.isTuesdayDarbarEnabled) Color(0xFF81C784) else Color(0xFFFFB74D)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "अंकित चौधरी",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFF94A3B8)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Row 2: High-Density Quick Controls in Horizontal Scroll
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // 1. Tuesday Toggle Pill
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (settings.isTuesdayDarbarEnabled) Color(0xFF1E3A2F) else Color(0xFF33202A),
                                            border = BorderStroke(1.dp, if (settings.isTuesdayDarbarEnabled) Color(0xFF2E7D32) else Color(0xFF8E24AA)),
                                            modifier = Modifier.height(34.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "🚩 मंगलवार",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
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
                                                            Toast.makeText(context, if (isChecked) "🚩 मंगलवार दरबार चालू" else "⏸️ मंगलवार दरबार बंद", Toast.LENGTH_SHORT).show()
                                                        }
                                                    },
                                                    modifier = Modifier.scale(0.68f),
                                                    colors = SwitchDefaults.colors(
                                                        checkedThumbColor = Color.White,
                                                        checkedTrackColor = Color(0xFF2E7D32),
                                                        uncheckedThumbColor = Color.White,
                                                        uncheckedTrackColor = Color.Gray
                                                    )
                                                )
                                            }
                                        }

                                        // 2. Hawan Cost Chip
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF2D1822),
                                            border = BorderStroke(1.dp, Color(0xFFEF5350)),
                                            modifier = Modifier
                                                .height(34.dp)
                                                .clickable {
                                                    havanCostInput = settings.havanEstimatedCost.toString()
                                                    havanRulesInput = settings.havanRulesNotice
                                                    showHavanCostDialog = true
                                                }
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 10.dp)
                                            ) {
                                                Text(
                                                    text = "🔥 हवन: ₹${settings.havanEstimatedCost} ✏️",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFFFCDD2)
                                                )
                                            }
                                        }

                                        // 3. Sevadar Chat Monitor Chip
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF201A38),
                                            border = BorderStroke(1.dp, Color(0xFF7E57C2)),
                                            modifier = Modifier
                                                .height(34.dp)
                                                .clickable {
                                                    val idx = allowedTabs.indexOfFirst { it.contains("चैट") || it.contains("Helpdesk") || it.contains("पूछताछ") }
                                                    if (idx >= 0) {
                                                        selectedTab = idx
                                                        activeScreenTitle = allowedTabs[idx]
                                                    }
                                                }
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 10.dp)
                                            ) {
                                                Text(
                                                    text = "💬 सेवादार चैट",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFD1C4E9)
                                                )
                                            }
                                        }

                                        // 4. Instant Notice Broadcast Chip
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF332014),
                                            border = BorderStroke(1.dp, Color(0xFFFFA726)),
                                            modifier = Modifier
                                                .height(34.dp)
                                                .clickable {
                                                    val idx = allowedTabs.indexOfFirst { it.contains("सूचना") || it.contains("Broadcast") }
                                                    if (idx >= 0) {
                                                        selectedTab = idx
                                                        activeScreenTitle = allowedTabs[idx]
                                                    }
                                                }
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 10.dp)
                                            ) {
                                                Text(
                                                    text = "📢 सूचना ➔",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFFFE0B2)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }'''

idx_start = content.find(old_marker_start)
if idx_start != -1:
    idx_end = content.find(old_marker_end, idx_start)
    if idx_end != -1:
        full_old = content[idx_start:idx_end + len(old_marker_end)]
        content = content[:idx_start] + new_executive_bar + content[idx_end + len(old_marker_end):]
        print("Successfully replaced Super Admin bulky cards with Executive Command Deck!")
    else:
        print("ERROR: old_marker_end not found")
else:
    print("ERROR: old_marker_start not found")

# 2. Add Section 2 (Bus Service Master On/Off Switch) in PublicServiceMatrixTab
old_section2_marker = "        // (Section 2 Bus & Section 2.5 Dharamshala removed)\n        // SECTION 3: ASHRAM UPI QR CODE & PAYMENT GATEWAY CONTROL (SUPER ADMIN DIRECT CONTROL)"

new_section2_code = '''        // SECTION 2: ASHRAM BUS SERVICE MASTER ON/OFF CONTROL (SUPER ADMIN DIRECT CONTROL)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, if (isBusBookingLive) Color(0xFF1976D2) else Color(0xFFBBDEFB))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Text("🚌", fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (isHindi) "आश्रम बस सेवा मास्टर ऑन/ऑफ (सुपर एडमिन नियंत्रण)" else "Ashram Bus Service Master Control",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaroonPrimary
                            )
                            Text(
                                text = if (isHindi) "बस बुकिंग, सीट चयन व लेजर का मास्टर स्विच (डेटा सुरक्षित रहता है)" else "Bus booking, seats & ledger master switch (data preserved)",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }
                    Switch(checked = isBusBookingLive, onCheckedChange = onBusBookingLiveChange)
                }

                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = if (isBusBookingLive) Color(0xFFE3F2FD) else Color(0xFFEEEEEE),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isBusBookingLive)
                            (if (isHindi) "🟢 सक्रिय: भक्तों के लिए बस बुकिंग व सीट चयन लाइव है। पिछला संपूर्ण लेजर व हिसाब सुरक्षित है।" else "🟢 LIVE: Bus booking & seat reservation are visible to devotees. Complete ledger is active.")
                        else
                            (if (isHindi) "🔒 निष्क्रीय (Hidden): बस बुकिंग सेवा भक्तों से छिपी हुई है। संपूर्ण लेजर, यात्री सूची व हिसाब सुरक्षित है और आवश्यकता पड़ने पर कभी भी चालू किया जा सकता है।" else "🔒 HIDDEN: Bus booking is hidden from devotees. Complete ledger and passenger records are preserved."),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isBusBookingLive) Color(0xFF0D47A1) else Color.DarkGray,
                        modifier = Modifier.padding(8.dp)
                    )
                }

                if (isBusBookingLive) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = busFareAmount,
                        onValueChange = onBusFareAmountChange,
                        label = { Text(if (isHindi) "बस किराया प्रति सीट (₹)" else "Bus Fare Per Seat (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // SECTION 3: ASHRAM UPI QR CODE & PAYMENT GATEWAY CONTROL (SUPER ADMIN DIRECT CONTROL)'''

if old_section2_marker in content:
    content = content.replace(old_section2_marker, new_section2_code, 1)
    print("Successfully added Section 2 Bus Master Switch to PublicServiceMatrixTab!")
else:
    print("WARNING: old_section2_marker not found directly, checking partial")
    pos = content.find("Section 2 Bus & Section 2.5 Dharamshala removed")
    print("Position of Section 2 Bus:", pos)

with open(target_file, "w", encoding="utf-8") as f:
    f.write(content)
print("Finished saving updates.")
