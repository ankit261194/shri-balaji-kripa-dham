file_path = r'app\src\main\java\com\example\shribalajikripadham\ui\admin\AdminDashboardScreen.kt'
with open(file_path, 'r', encoding='utf-8') as f:
    lines = f.readlines()

print("Original lines:", len(lines))

# 1. Update refreshData (around line 440)
idx_refresh = -1
for i, l in enumerate(lines[:600]):
    if 'customSundayTokenCustomNotice = s.sundayTokenCustomNotice' in l:
        idx_refresh = i
        break

if idx_refresh != -1:
    lines.insert(idx_refresh + 1, "            havanCostInput = s.havanEstimatedCost.toString()\n")
    lines.insert(idx_refresh + 2, "            havanRulesInput = s.havanRulesAndExpenses\n")
    print("1. havan inputs added to refreshData.")
else:
    print("1. ERROR: refreshData target not found.")

# 2. Clean up allowedTabs (remove bus, ui, website, distances)
for i in range(1200, 1400):
    if i < len(lines):
        l = lines[i]
        if 'allowedTabs.add(if (isHindi) "बस बुकिंग लेजर" else "Bus Ledger")' in l:
            lines[i] = "            // Bus ledger removed\n"
            print("2a. Bus ledger removed.")
        elif 'allowedTabs.add(if (isHindi) "UI बॉक्स कंट्रोल" else "UI Control")' in l:
            lines[i] = "            // UI control removed\n"
            print("2b. UI control removed.")
        elif 'allowedTabs.add(if (isHindi) "🌐 वेबसाइट लाइव एडिटर" else "Website Live Editor")' in l:
            lines[i] = "            // Website editor removed\n"
            print("2c. Website editor removed.")
        elif 'allowedTabs.add(if (isHindi) "🌐 वेबसाइट व CMS" else "Website & CMS")' in l:
            lines[i] = "            // Website CMS removed\n"
            print("2d. Website CMS removed.")
        elif 'allowedTabs.add(if (isHindi) "कस्टम दूरियाँ" else "Distances")' in l:
            lines[i] = "            // Distances removed\n"
            print("2e. Distances removed.")

# 3. Replace lines 1367 to 1694 (inside `if (isSuper) {`) with the Executive Pro Deck
start_deck = -1
end_deck = -1
for i, l in enumerate(lines):
    if '// COMPACT 1-LINE ADMIN STATUS BAR (LEAVING 95% SCREEN FOR GRID/LIST)' in l:
        start_deck = i
    if '// UNIFIED ADMIN CONTROL HUB: LIST & GRID VIEW OF ALL MODULES' in l and start_deck != -1:
        end_deck = i
        break

print(f"Deck range: {start_deck+1} to {end_deck+1}")
if start_deck != -1 and end_deck != -1:
    new_deck_lines = [
        "                        // =========================================================================\n",
        "                        // 👑 EXECUTIVE PRO-TIER SUPER ADMIN COMMAND DECK (सर्वोच्च व्यवस्थापक कंसोल)\n",
        "                        // =========================================================================\n",
        "                        Card(\n",
        "                            modifier = Modifier\n",
        "                                .fillMaxWidth()\n",
        "                                .padding(horizontal = 8.dp, vertical = 6.dp),\n",
        "                            shape = RoundedCornerShape(16.dp),\n",
        "                            colors = CardDefaults.cardColors(containerColor = Color.White),\n",
        "                            border = BorderStroke(1.5.dp, AmberGold),\n",
        "                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)\n",
        "                        ) {\n",
        "                            Column(modifier = Modifier.padding(12.dp)) {\n",
        "                                // --- 1. ROYAL INSIGNIA HEADER ---\n",
        "                                Surface(\n",
        "                                    shape = RoundedCornerShape(12.dp),\n",
        "                                    color = Color(0xFFFFF9EE),\n",
        "                                    border = BorderStroke(1.dp, Color(0xFFFFD54F)),\n",
        "                                    modifier = Modifier.fillMaxWidth()\n",
        "                                ) {\n",
        "                                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {\n",
        "                                        Row(\n",
        "                                            modifier = Modifier.fillMaxWidth(),\n",
        "                                            verticalAlignment = Alignment.CenterVertically,\n",
        "                                            horizontalArrangement = Arrangement.SpaceBetween\n",
        "                                        ) {\n",
        "                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {\n",
        "                                                Text(\"🚩\", fontSize = 24.sp)\n",
        "                                                Spacer(modifier = Modifier.width(8.dp))\n",
        "                                                Column {\n",
        "                                                    Text(\n",
        "                                                        text = \"श्री बालाजी कृपा धाम • सुपर एडमिन सुप्रीम कंसोल\",\n",
        "                                                        fontWeight = FontWeight.ExtraBold,\n",
        "                                                        fontSize = 14.5.sp,\n",
        "                                                        color = MaroonPrimary\n",
        "                                                    )\n",
        "                                                    Text(\n",
        "                                                        text = \"सर्वोच्च व्यवस्थापक: श्री अंकित चौधरी (पूर्ण प्रशासनिक अधिकार)\",\n",
        "                                                        fontSize = 11.sp,\n",
        "                                                        color = Color(0xFF5D4037),\n",
        "                                                        fontWeight = FontWeight.SemiBold\n",
        "                                                    )\n",
        "                                                }\n",
        "                                            }\n",
        "                                            Surface(\n",
        "                                                shape = RoundedCornerShape(8.dp),\n",
        "                                                color = Color(0xFFFFEBEE),\n",
        "                                                border = BorderStroke(0.8.dp, Color(0xFFEF9A9A)),\n",
        "                                                modifier = Modifier.clickable { showLogoutExitDialog = true }\n",
        "                                            ) {\n",
        "                                                Text(\n",
        "                                                    text = \"🚪 बाहर निकलें\",\n",
        "                                                    fontSize = 11.sp,\n",
        "                                                    fontWeight = FontWeight.Bold,\n",
        "                                                    color = Color(0xFFC62828),\n",
        "                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)\n",
        "                                                )\n",
        "                                            }\n",
        "                                        }\n",
        "                                        Spacer(modifier = Modifier.height(8.dp))\n",
        "                                        Row(\n",
        "                                            modifier = Modifier.fillMaxWidth(),\n",
        "                                            verticalAlignment = Alignment.CenterVertically,\n",
        "                                            horizontalArrangement = Arrangement.SpaceBetween\n",
        "                                        ) {\n",
        "                                            Row(\n",
        "                                                verticalAlignment = Alignment.CenterVertically,\n",
        "                                                horizontalArrangement = Arrangement.spacedBy(6.dp)\n",
        "                                            ) {\n",
        "                                                Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFE8F5E9)) {\n",
        "                                                    Text(\n",
        "                                                        text = \"📱 $telemetryTotalDevices डिवाइस\",\n",
        "                                                        fontSize = 10.5.sp,\n",
        "                                                        fontWeight = FontWeight.Bold,\n",
        "                                                        color = Color(0xFF2E7D32),\n",
        "                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)\n",
        "                                                    )\n",
        "                                                }\n",
        "                                                Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFE3F2FD)) {\n",
        "                                                    Text(\n",
        "                                                        text = \"☁️ Hostinger Live\",\n",
        "                                                        fontSize = 10.5.sp,\n",
        "                                                        fontWeight = FontWeight.Bold,\n",
        "                                                        color = Color(0xFF1565C0),\n",
        "                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)\n",
        "                                                    )\n",
        "                                                }\n",
        "                                            }\n",
        "                                            Row(\n",
        "                                                verticalAlignment = Alignment.CenterVertically,\n",
        "                                                horizontalArrangement = Arrangement.spacedBy(4.dp)\n",
        "                                            ) {\n",
        "                                                Surface(\n",
        "                                                    shape = RoundedCornerShape(6.dp),\n",
        "                                                    color = Color(0xFFEDE7F6),\n",
        "                                                    border = BorderStroke(0.7.dp, Color(0xFFB39DDB)),\n",
        "                                                    modifier = Modifier.clickable {\n",
        "                                                        val file = com.example.shribalajikripadham.util.AshramManualPdfGenerator.generateAdminGuidePdf(context)\n",
        "                                                        if (file != null) {\n",
        "                                                            com.example.shribalajikripadham.util.AshramManualPdfGenerator.openOrSharePdf(\n",
        "                                                                context, file,\n",
        "                                                                if (isHindi) \"श्री बालाजी कृपा धाम - व्यवस्थापक मार्गदर्शिका\" else \"Shri Balaji Kripa Dham - Admin Manual\"\n",
        "                                                            )\n",
        "                                                        } else {\n",
        "                                                            Toast.makeText(context, if (isHindi) \"PDF तैयार करने में असमर्थ\" else \"Failed to generate PDF\", Toast.LENGTH_SHORT).show()\n",
        "                                                        }\n",
        "                                                    }\n",
        "                                                ) {\n",
        "                                                    Text(\n",
        "                                                        text = \"📘 गाइड\",\n",
        "                                                        fontSize = 10.5.sp,\n",
        "                                                        fontWeight = FontWeight.Bold,\n",
        "                                                        color = Color(0xFF4A148C),\n",
        "                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)\n",
        "                                                    )\n",
        "                                                }\n",
        "                                                Surface(\n",
        "                                                    shape = RoundedCornerShape(6.dp),\n",
        "                                                    color = Color(0xFFFFF3E0),\n",
        "                                                    border = BorderStroke(0.7.dp, Color(0xFFFFB74D)),\n",
        "                                                    modifier = Modifier.clickable {\n",
        "                                                        refreshData()\n",
        "                                                        Toast.makeText(context, \"🔄 डेटा रिफ्रेश हुआ\", Toast.LENGTH_SHORT).show()\n",
        "                                                    }\n",
        "                                                ) {\n",
        "                                                    Text(\n",
        "                                                        text = \"🔄 रिफ्रेश\",\n",
        "                                                        fontSize = 10.5.sp,\n",
        "                                                        fontWeight = FontWeight.Bold,\n",
        "                                                        color = Color(0xFFE65100),\n",
        "                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)\n",
        "                                                    )\n",
        "                                                }\n",
        "                                                Surface(\n",
        "                                                    shape = RoundedCornerShape(6.dp),\n",
        "                                                    color = Color(0xFFE8F5E9),\n",
        "                                                    border = BorderStroke(0.7.dp, Color(0xFF81C784)),\n",
        "                                                    modifier = Modifier.clickable {\n",
        "                                                        ownNewPassword = \"\"\n",
        "                                                        ownNewPin = \"\"\n",
        "                                                        ownCredentialsErrorMsg = null\n",
        "                                                        showChangeOwnCredentialsDialog = true\n",
        "                                                    }\n",
        "                                                ) {\n",
        "                                                    Text(\n",
        "                                                        text = \"🔐 पिन\",\n",
        "                                                        fontSize = 10.5.sp,\n",
        "                                                        fontWeight = FontWeight.Bold,\n",
        "                                                        color = Color(0xFF1B5E20),\n",
        "                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)\n",
        "                                                    )\n",
        "                                                }\n",
        "                                            }\n",
        "                                        }\n",
        "                                    }\n",
        "                                }\n",
        "                                Spacer(modifier = Modifier.height(10.dp))\n",
        "                                // CARD 1: TUESDAY DARBAR MASTER SWITCH\n",
        "                                Surface(\n",
        "                                    shape = RoundedCornerShape(12.dp),\n",
        "                                    color = if (settings.isTuesdayDarbarEnabled) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),\n",
        "                                    border = BorderStroke(1.2.dp, if (settings.isTuesdayDarbarEnabled) Color(0xFF2E7D32) else Color(0xFFFF9800)),\n",
        "                                    modifier = Modifier.fillMaxWidth()\n",
        "                                ) {\n",
        "                                    Row(\n",
        "                                        modifier = Modifier\n",
        "                                            .fillMaxWidth()\n",
        "                                            .padding(horizontal = 12.dp, vertical = 10.dp),\n",
        "                                        verticalAlignment = Alignment.CenterVertically,\n",
        "                                        horizontalArrangement = Arrangement.SpaceBetween\n",
        "                                    ) {\n",
        "                                        Column(modifier = Modifier.weight(1f)) {\n",
        "                                            Text(\n",
        "                                                text = if (settings.isTuesdayDarbarEnabled) \"🚩 मंगलवार विशेष दरबार: ● सक्रिय (चालू)\" else \"🛑 मंगलवार विशेष दरबार: ○ निष्क्रिय (बंद)\",\n",
        "                                                fontWeight = FontWeight.ExtraBold,\n",
        "                                                fontSize = 13.5.sp,\n",
        "                                                color = if (settings.isTuesdayDarbarEnabled) Color(0xFF1B5E20) else Color(0xFFBF360C)\n",
        "                                            )\n",
        "                                            Text(\n",
        "                                                text = if (settings.isTuesdayDarbarEnabled) \"भक्तों के फोन में मंगलवार दरबार की लाइव बुकिंग व टोकन सेवा चालू है\" else \"दरबार बंद है (1-क्लिक में यहीं से चालू अथवा बंद करें)\",\n",
        "                                                fontSize = 11.sp,\n",
        "                                                color = Color.DarkGray\n",
        "                                            )\n",
        "                                        }\n",
        "                                        Switch(\n",
        "                                            checked = settings.isTuesdayDarbarEnabled,\n",
        "                                            onCheckedChange = { isChecked ->\n",
        "                                                scope.launch {\n",
        "                                                    repository.updateTuesdayDarbarSettings(\n",
        "                                                        isEnabled = isChecked,\n",
        "                                                        name = settings.tuesdayDarbarName,\n",
        "                                                        address = settings.tuesdayDarbarAddress,\n",
        "                                                        latitude = settings.tuesdayLatitude,\n",
        "                                                        longitude = settings.tuesdayLongitude,\n",
        "                                                        allowedRadiusMeters = settings.tuesdayAllowedRadiusMeters,\n",
        "                                                        outstationMinDistanceKm = settings.tuesdayOutstationMinDistanceKm,\n",
        "                                                        timings = settings.tuesdayDarbarTimings,\n",
        "                                                        tokenServiceMode = settings.tuesdayTokenServiceMode,\n",
        "                                                        tokenNotice = settings.tuesdayTokenNotice\n",
        "                                                    )\n",
        "                                                    settings = repository.getSettings()\n",
        "                                                    Toast.makeText(context, if (isChecked) \"🚩 मंगलवार दरबार चालू हो गया!\" else \"🛑 मंगलवार दरबार बंद कर दिया गया\", Toast.LENGTH_SHORT).show()\n",
        "                                                }\n",
        "                                            },\n",
        "                                            colors = SwitchDefaults.colors(\n",
        "                                                checkedThumbColor = Color.White,\n",
        "                                                checkedTrackColor = Color(0xFF2E7D32),\n",
        "                                                uncheckedThumbColor = Color.White,\n",
        "                                                uncheckedTrackColor = Color.Gray\n",
        "                                            )\n",
        "                                        )\n",
        "                                    }\n",
        "                                }\n",
        "                                Spacer(modifier = Modifier.height(8.dp))\n",
        "                                // CARD 2: HAWAN COST (₹14,000) & RULES EDITOR\n",
        "                                Surface(\n",
        "                                    shape = RoundedCornerShape(12.dp),\n",
        "                                    color = Color(0xFFFFF8E1),\n",
        "                                    border = BorderStroke(1.2.dp, Color(0xFFFFB300)),\n",
        "                                    modifier = Modifier.fillMaxWidth()\n",
        "                                ) {\n",
        "                                    Row(\n",
        "                                        modifier = Modifier\n",
        "                                            .fillMaxWidth()\n",
        "                                            .padding(horizontal = 12.dp, vertical = 10.dp),\n",
        "                                        verticalAlignment = Alignment.CenterVertically,\n",
        "                                        horizontalArrangement = Arrangement.SpaceBetween\n",
        "                                    ) {\n",
        "                                        Column(modifier = Modifier.weight(1f)) {\n",
        "                                            Row(verticalAlignment = Alignment.CenterVertically) {\n",
        "                                                Text(\n",
        "                                                    text = \"🔥 महा-हवन सेवा दक्षिणा:\",\n",
        "                                                    fontWeight = FontWeight.Bold,\n",
        "                                                    fontSize = 13.sp,\n",
        "                                                    color = MaroonPrimary\n",
        "                                                )\n",
        "                                                Spacer(modifier = Modifier.width(6.dp))\n",
        "                                                Surface(\n",
        "                                                    shape = RoundedCornerShape(6.dp),\n",
        "                                                    color = SaffronPrimary\n",
        "                                                ) {\n",
        "                                                    Text(\n",
        "                                                        text = \"₹${settings.havanEstimatedCost}\",\n",
        "                                                        fontWeight = FontWeight.ExtraBold,\n",
        "                                                        fontSize = 12.sp,\n",
        "                                                        color = Color.White,\n",
        "                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)\n",
        "                                                    )\n",
        "                                                }\n",
        "                                            }\n",
        "                                            Text(\n",
        "                                                text = \"यजमान संकल्प, आहुति नियम व सामग्री खर्च (संपादित करने पर लाइव सिंक)\",\n",
        "                                                fontSize = 11.sp,\n",
        "                                                color = Color.DarkGray\n",
        "                                            )\n",
        "                                        }\n",
        "                                        Button(\n",
        "                                            onClick = {\n",
        "                                                havanCostInput = settings.havanEstimatedCost.toString()\n",
        "                                                havanRulesInput = settings.havanRulesAndExpenses\n",
        "                                                showHavanCostDialog = true\n",
        "                                            },\n",
        "                                            shape = RoundedCornerShape(8.dp),\n",
        "                                            colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),\n",
        "                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),\n",
        "                                            modifier = Modifier.height(36.dp)\n",
        "                                        ) {\n",
        "                                            Text(\"✏️ राशि बदलें\", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)\n",
        "                                        }\n",
        "                                    }\n",
        "                                }\n",
        "                                Spacer(modifier = Modifier.height(8.dp))\n",
        "                                // CARD 3: MASTER CHATS OVERSIGHT (ALL SEVADARS & DEVOTEES)\n",
        "                                Surface(\n",
        "                                    shape = RoundedCornerShape(12.dp),\n",
        "                                    color = Color(0xFFEDE7F6),\n",
        "                                    border = BorderStroke(1.2.dp, Color(0xFF7E57C2)),\n",
        "                                    modifier = Modifier.fillMaxWidth()\n",
        "                                ) {\n",
        "                                    Row(\n",
        "                                        modifier = Modifier\n",
        "                                            .fillMaxWidth()\n",
        "                                            .padding(horizontal = 12.dp, vertical = 10.dp),\n",
        "                                        verticalAlignment = Alignment.CenterVertically,\n",
        "                                        horizontalArrangement = Arrangement.SpaceBetween\n",
        "                                    ) {\n",
        "                                        Column(modifier = Modifier.weight(1f)) {\n",
        "                                            Row(verticalAlignment = Alignment.CenterVertically) {\n",
        "                                                Text(\n",
        "                                                    text = \"💬 समस्त सेवादार लाइव चैट्स\",\n",
        "                                                    fontWeight = FontWeight.Bold,\n",
        "                                                    fontSize = 13.sp,\n",
        "                                                    color = Color(0xFF4A148C)\n",
        "                                                )\n",
        "                                                Spacer(modifier = Modifier.width(6.dp))\n",
        "                                                Surface(\n",
        "                                                    shape = RoundedCornerShape(4.dp),\n",
        "                                                    color = Color(0xFFD1C4E9)\n",
        "                                                ) {\n",
        "                                                    Text(\n",
        "                                                        text = \"100% पारदर्शी\",\n",
        "                                                        fontSize = 9.5.sp,\n",
        "                                                        fontWeight = FontWeight.Bold,\n",
        "                                                        color = Color(0xFF311B92),\n",
        "                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)\n",
        "                                                    )\n",
        "                                                }\n",
        "                                            }\n",
        "                                            Text(\n",
        "                                                text = \"समस्त सेवादारों व भक्तों के सभी संदेशों की लाइव सुप्रीम निगरानी\",\n",
        "                                                fontSize = 11.sp,\n",
        "                                                color = Color.DarkGray\n",
        "                                            )\n",
        "                                        }\n",
        "                                        Button(\n",
        "                                            onClick = {\n",
        "                                                val idx = allowedTabs.indexOfFirst { it.contains(\"चैट\") || it.contains(\"Helpdesk\") || it.contains(\"सुझाव\") }\n",
        "                                                if (idx >= 0) {\n",
        "                                                    selectedTab = idx\n",
        "                                                    activeScreenTitle = allowedTabs[idx]\n",
        "                                                }\n",
        "                                            },\n",
        "                                            shape = RoundedCornerShape(8.dp),\n",
        "                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF512DA8)),\n",
        "                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),\n",
        "                                            modifier = Modifier.height(36.dp)\n",
        "                                        ) {\n",
        "                                            Text(\"चैट्स देखें ➜\", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)\n",
        "                                        }\n",
        "                                    }\n",
        "                                }\n",
        "                                Spacer(modifier = Modifier.height(8.dp))\n",
        "                                // CARD 4: INSTANT BROADCAST PUSH NOTIFICATION\n",
        "                                Surface(\n",
        "                                    shape = RoundedCornerShape(12.dp),\n",
        "                                    color = Color(0xFFE0F2FE),\n",
        "                                    border = BorderStroke(1.2.dp, Color(0xFF0284C7)),\n",
        "                                    modifier = Modifier.fillMaxWidth()\n",
        "                                ) {\n",
        "                                    Row(\n",
        "                                        modifier = Modifier\n",
        "                                            .fillMaxWidth()\n",
        "                                            .padding(horizontal = 12.dp, vertical = 10.dp),\n",
        "                                        verticalAlignment = Alignment.CenterVertically,\n",
        "                                        horizontalArrangement = Arrangement.SpaceBetween\n",
        "                                    ) {\n",
        "                                        Column(modifier = Modifier.weight(1f)) {\n",
        "                                            Text(\n",
        "                                                text = \"📢 तात्कालिक महा-प्रसारण (Push Alert)\",\n",
        "                                                fontWeight = FontWeight.Bold,\n",
        "                                                fontSize = 13.sp,\n",
        "                                                color = Color(0xFF0369A1)\n",
        "                                            )\n",
        "                                            Text(\n",
        "                                                text = \"सभी जुड़े हुए $telemetryTotalDevices भक्तों के फोन पर तत्काल सूचना या अलर्ट भेजें\",\n",
        "                                                fontSize = 11.sp,\n",
        "                                                color = Color.DarkGray\n",
        "                                            )\n",
        "                                        }\n",
        "                                        Button(\n",
        "                                            onClick = {\n",
        "                                                val idx = allowedTabs.indexOfFirst { it.contains(\"सूचना\") || it.contains(\"Broadcast\") }\n",
        "                                                if (idx >= 0) {\n",
        "                                                    selectedTab = idx\n",
        "                                                    activeScreenTitle = allowedTabs[idx]\n",
        "                                                }\n",
        "                                            },\n",
        "                                            shape = RoundedCornerShape(8.dp),\n",
        "                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),\n",
        "                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),\n",
        "                                            modifier = Modifier.height(36.dp)\n",
        "                                        ) {\n",
        "                                            Text(\"अलर्ट भेजें ➜\", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)\n",
        "                                        }\n",
        "                                    }\n",
        "                                }\n",
        "                            }\n",
        "                        }\n",
        "                        Spacer(modifier = Modifier.height(6.dp))\n"
    ]
    lines = lines[:start_deck] + new_deck_lines + lines[end_deck:]
    print("3. Super Admin Command Deck replaced accurately.")

# 4. Clean up PublicServiceMatrixTab
# 4a. showRoomManagementDialog variable
for i in range(8800, 9100):
    if i < len(lines):
        if 'var showRoomManagementDialog by remember' in lines[i]:
            lines[i] = "    // showRoomManagementDialog removed\n"
            print("4a. showRoomManagementDialog variable removed.")
            break

# 4b. Remove bus and dharamshala sections (from SECTION 2 to before SECTION 3)
idx_sec2 = -1
idx_sec3 = -1
for i, l in enumerate(lines):
    if '// SECTION 2: 60-SEATER LUXURY BUS CONTROL' in l:
        idx_sec2 = i
    if '// SECTION 3: ASHRAM UPI QR CODE & PAYMENT GATEWAY CONTROL' in l:
        idx_sec3 = i
        break

if idx_sec2 != -1 and idx_sec3 != -1:
    del lines[idx_sec2:idx_sec3]
    lines.insert(idx_sec2, "        // (Bus and Dharamshala controls removed: features not present in main app)\n\n")
    print(f"4b. Bus & Dharamshala sections removed (lines {idx_sec2} to {idx_sec3}).")

# 4c. Remove Aarti timings switch row
for i, l in enumerate(lines):
    if 'onAartiTimingsChange)' in l and 'ServiceSwitchRow' in l:
        lines[i] = "                // (Aarti timings toggle removed: aarti timings disabled in main app)\n"
        print(f"4c. Aarti timings toggle removed at line {i+1}.")
        break

# 4d. Remove Section 5 (Yatra diary) and room dialogs (before the closing brace of Column at line 10237)
idx_sec5 = -1
idx_close_col = -1
for i, l in enumerate(lines):
    if '// SECTION 5: YATRA EXPENSE DIARY PRIVACY' in l:
        idx_sec5 = i
    if 'if (showSaveConfirmationDialog) {' in l and idx_sec5 != -1:
        # The closing brace } of the Column is just before if (showSaveConfirmationDialog)
        # Search backwards from i for the } line
        for k in range(i-1, idx_sec5, -1):
            if lines[k].strip() == '}':
                idx_close_col = k
                break
        break

print(f"Section 5 index: {idx_sec5}, Column close brace index: {idx_close_col}")
if idx_sec5 != -1 and idx_close_col != -1:
    del lines[idx_sec5:idx_close_col]
    lines.insert(idx_sec5, "        // (Yatra diary privacy & room management dialogs removed)\n")
    print("4d. Yatra diary & room dialogs removed, Column closing brace preserved.")

with open(file_path, 'w', encoding='utf-8') as f:
    f.writelines(lines)

print("Finished cleanly! Total lines now:", len(lines))
