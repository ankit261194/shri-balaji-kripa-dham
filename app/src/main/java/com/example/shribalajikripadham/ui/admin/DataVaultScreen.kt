package com.example.shribalajikripadham.ui.admin

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shribalajikripadham.data.local.DatabaseHelper
import com.example.shribalajikripadham.theme.*
import com.example.shribalajikripadham.util.GoogleDriveSyncHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataVaultScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var totalTokens by remember { mutableStateOf(0) }
    var totalDonors by remember { mutableStateOf(0) }
    var totalSevadars by remember { mutableStateOf(0) }
    var totalExpenses by remember { mutableStateOf(0) }
    var isInspecting by remember { mutableStateOf(false) }
    var isSyncing by remember { mutableStateOf(false) }
    var previewData by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }

    // Load initial counts
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val db = DatabaseHelper(context).readableDatabase
            try {
                val c1 = db.rawQuery("SELECT COUNT(*) FROM tokens", null)
                if (c1.moveToFirst()) totalTokens = c1.getInt(0)
                c1.close()

                val c2 = db.rawQuery("SELECT COUNT(*) FROM donors", null)
                if (c2.moveToFirst()) totalDonors = c2.getInt(0)
                c2.close()

                val c3 = db.rawQuery("SELECT COUNT(*) FROM sevadars", null)
                if (c3.moveToFirst()) totalSevadars = c3.getInt(0)
                c3.close()

                val c4 = db.rawQuery("SELECT COUNT(*) FROM expenses", null)
                if (c4.moveToFirst()) totalExpenses = c4.getInt(0)
                c4.close()
            } catch (e: Exception) {
                // Table might not exist yet in local sqlite
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "अखंड डेटा वॉल्ट व ऑटो-बैकअप",
                        color = GoldSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Text(text = "⬅", color = GoldSecondary, fontSize = 20.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaroonAccent)
            )
        },
        containerColor = SacredBackgroundLight
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Autonomous Live Status Banner
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(2.dp, GoldSecondary),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(MaroonAccent, MaroonPrimary)
                            )
                        )
                        .padding(16.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF00E676))
                            )
                            Text(
                                text = "🟢 100% ज़ीरो-टच ऑटो-बैकअप सक्रिय",
                                color = GoldSecondary,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "आपको कोई बटन दबाने की ज़रूरत नहीं है। हर नया टोकन, पर्चा, दानदाता और खर्च स्वतः ही सर्वर, लोकल स्टोरेज और बैकअप लेज़र में हर सेकंड सुरक्षित हो रहा है।",
                            color = Color(0xFFFFF3E0),
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // 2. Data Statistics Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFFFE082)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "📊 सुरक्षित डेटा सांख्यिकी (Live Records)",
                        fontWeight = FontWeight.Bold,
                        color = MaroonPrimary,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatItem(title = "कुल टोकन", count = totalTokens.toString(), icon = "🎟️")
                        StatItem(title = "दानदाता", count = totalDonors.toString(), icon = "💰")
                        StatItem(title = "सेवादार", count = totalSevadars.toString(), icon = "👥")
                        StatItem(title = "खर्च खाते", count = totalExpenses.toString(), icon = "📑")
                    }
                }
            }

            // 3. Action Buttons
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFFFE082)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "⚡ सुपरएडमिन त्वरित नियंत्रण (Data Actions)",
                        fontWeight = FontWeight.Bold,
                        color = MaroonPrimary,
                        fontSize = 15.sp
                    )

                    // Button: Google Drive Save
                    Button(
                        onClick = {
                            GoogleDriveSyncHelper.shareBackupToGoogleDrive(context)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("☁️ Google Drive में बैकअप भेजें / सेव करें", fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    // Button: Phone Download (JSON)
                    Button(
                        onClick = {
                            val file = GoogleDriveSyncHelper.generateLocalVaultDump(context)
                            if (file != null) {
                                Toast.makeText(
                                    context,
                                    "✅ बैकअप डाउनलोड हुआ:\nDownloads/ShriBalajiKripaDham_Backups/${file.name}",
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                Toast.makeText(context, "बैकअप बनाने में त्रुटि", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("📥 फ़ोन में फ़ाइल डाउनलोड करें (JSON Vault)", fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    // Button: Inspect Data
                    OutlinedButton(
                        onClick = {
                            isInspecting = !isInspecting
                            if (isInspecting) {
                                scope.launch(Dispatchers.IO) {
                                    val list = mutableListOf<Pair<String, String>>()
                                    try {
                                        val db = DatabaseHelper(context).readableDatabase
                                        val cur = db.rawQuery("SELECT token_number, patient_name, phone_number, city FROM tokens ORDER BY id DESC LIMIT 15", null)
                                        try {
                                            while (cur.moveToNext()) {
                                                val num = cur.getInt(0)
                                                val name = cur.getString(1)
                                                val city = cur.getString(3) ?: ""
                                                list.add("टोकन #$num: $name" to city)
                                            }
                                        } finally {
                                            cur.close()
                                        }
                                    } catch (e: Exception) {}
                                    previewData = list
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaroonPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (isInspecting) "डेटा प्रिव्यू छिपाएं" else "👁️ बैकअप डेटा रिकॉर्ड्स देखें",
                            color = MaroonPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Data Preview Section
                    AnimatedVisibility(visible = isInspecting) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFFFDF5), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFFFFD54F), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                "ताज़ा 15 सुरक्षित रिकॉर्ड्स:",
                                fontWeight = FontWeight.Bold,
                                color = MaroonPrimary,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            if (previewData.isEmpty()) {
                                Text("कोई रिकॉर्ड नहीं मिला या टेबल अभी खाली है।", fontSize = 12.sp, color = Color.Gray)
                            } else {
                                previewData.forEach { (title, sub) ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        Text(sub, fontSize = 11.sp, color = Color.DarkGray)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. New Hosting Migration Guide
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                border = BorderStroke(1.dp, Color(0xFFFFCA28)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "💡 नई होस्टिंग पर शिफ्ट कैसे करें?",
                            fontWeight = FontWeight.Bold,
                            color = SaffronPrimary,
                            fontSize = 15.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "1. नई होस्टिंग में shribalajikripadham.online डोमेन जोड़ें।\n" +
                               "2. नई होस्टिंग के public_html फोल्डर में install.php अपलोड करें।\n" +
                               "3. ब्राउज़र में shribalajikripadham.online/install.php खोलें (सारा कोड व टेबल्स 1 सेकंड में बन जाएंगे)।\n" +
                               "4. अपना बैकअप अपलोड कर दें — सारे भक्तों के मोबाइल ऐप बिना किसी अपडेट के तुरंत चालू हो जाएंगे!",
                        color = Color(0xFF37474F),
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun StatItem(title: String, count: String, icon: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Text(text = icon, fontSize = 20.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = count,
            fontWeight = FontWeight.ExtraBold,
            color = MaroonPrimary,
            fontSize = 16.sp
        )
        Text(
            text = title,
            fontSize = 11.sp,
            color = Color.Gray
        )
    }
}
