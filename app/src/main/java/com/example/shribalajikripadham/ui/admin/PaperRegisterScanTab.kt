package com.example.shribalajikripadham.ui.admin

import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.shribalajikripadham.ai.PaperRegisterScannerEngine
import com.example.shribalajikripadham.data.model.Admin
import com.example.shribalajikripadham.data.model.AdminRole
import com.example.shribalajikripadham.data.model.RegisterEntry
import com.example.shribalajikripadham.data.model.Token
import com.example.shribalajikripadham.data.repository.AshramRepository
import com.example.shribalajikripadham.theme.AmberGold
import com.example.shribalajikripadham.theme.MaroonAccent
import com.example.shribalajikripadham.theme.MaroonPrimary
import com.example.shribalajikripadham.theme.SaffronPrimary
import com.example.shribalajikripadham.util.DevoteePhotoHelper
import com.example.shribalajikripadham.util.TakeAnyPicturePreview
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaperRegisterScanTab(
    isHindi: Boolean,
    admin: Admin,
    repository: AshramRepository,
    onTokensGenerated: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var parsedEntries by remember { mutableStateOf<List<RegisterEntry>>(emptyList()) }
    var rawTextInput by remember { mutableStateOf("") }
    var activeMode by remember { mutableIntStateOf(0) } // 0 = Camera Photo, 1 = Direct Text Paste
    var isProcessing by remember { mutableStateOf(false) }
    var currentMaxTokenToday by remember { mutableIntStateOf(0) }
    var generatedTokensResult by remember { mutableStateOf<List<Token>?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Refresh current maximum token number today to give live sequence visibility
    val refreshSequenceInfo: () -> Unit = {
        scope.launch {
            try {
                val tokens = repository.getAllTokensToday()
                currentMaxTokenToday = tokens.maxOfOrNull { it.tokenNumber } ?: 0
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshSequenceInfo()
    }

    // Camera Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = TakeAnyPicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            val safeBmp = DevoteePhotoHelper.toSoftwareBitmap(bitmap)
            capturedBitmap = safeBmp
            // Automatically pre-populate default sequential parsing lines for instant review
            if (rawTextInput.isBlank()) {
                rawTextInput = "1. \n2. \n3. "
            }
            Toast.makeText(context, if (isHindi) "📸 फोटो खींची गई! नीचे नाम दर्ज/सत्यापित करें" else "📸 Photo captured! Verify names below", Toast.LENGTH_SHORT).show()
        }
    }

    // Gallery Picker Launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val bmp = DevoteePhotoHelper.loadBitmap(context, uri.toString())
            if (bmp != null) {
                val safeBmp = DevoteePhotoHelper.toSoftwareBitmap(bmp)
                capturedBitmap = safeBmp
                Toast.makeText(context, if (isHindi) "📁 फोटो लोड हो गई!" else "📁 Photo loaded!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header & Strict Sequence Explainer Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9EE)),
                border = BorderStroke(1.5.dp, AmberGold),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📷", fontSize = 28.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isHindi) "रजिस्टर / कॉपी स्कैन टोकन निर्माण" else "Paper Register Sequential Token Issuance",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaroonPrimary
                            )
                            Text(
                                text = if (isHindi) "कॉपी पर लिखे क्रम में ही स्वतः टोकन बनेंगे" else "Tokens will be issued in exact written sequence",
                                fontSize = 11.sp,
                                color = Color.DarkGray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Live Sequence Indicator Box
                    Surface(
                        color = Color(0xFFE8F5E9),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFF81C784)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = if (isHindi) "आज ऐप से पहले बने टोकन:" else "Tokens already created today:",
                                    fontSize = 11.sp,
                                    color = Color.DarkGray
                                )
                                Text(
                                    text = if (currentMaxTokenToday > 0)
                                        "#1 से #${currentMaxTokenToday} तक"
                                    else
                                        (if (isHindi) "आज अभी कोई टोकन नहीं बना" else "None yet today"),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1B5E20)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaroonAccent
                            ) {
                                Text(
                                    text = "अगला टोकन: #${currentMaxTokenToday + 1}",
                                    color = AmberGold,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Mode Switcher (Camera Photo vs Direct Text Paste)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFEDE7F6))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = { activeMode = 0 },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activeMode == 0) MaroonAccent else Color.Transparent,
                        contentColor = if (activeMode == 0) AmberGold else MaroonPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (isHindi) "📸 कॉपी की फोटो लें" else "📸 Take Register Photo",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Button(
                    onClick = { activeMode = 1 },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activeMode == 1) MaroonAccent else Color.Transparent,
                        contentColor = if (activeMode == 1) AmberGold else MaroonPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (isHindi) "✏️ सीधे लिस्ट लिखें / पेस्ट करें" else "✏️ Paste / Type List",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // MODE 0: Camera / Gallery
        if (activeMode == 0) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE0E0E0))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (capturedBitmap != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Black)
                            ) {
                                Image(
                                    bitmap = capturedBitmap!!.asImageBitmap(),
                                    contentDescription = "Register Photo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { cameraLauncher.launch(null) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaroonAccent),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = if (capturedBitmap == null)
                                        (if (isHindi) "📸 फोटो खींचें" else "📸 Take Photo")
                                    else
                                        (if (isHindi) "🔄 पुनः फोटो लें" else "🔄 Retake"),
                                    fontWeight = FontWeight.Bold,
                                    color = AmberGold
                                )
                            }

                            OutlinedButton(
                                onClick = { galleryLauncher.launch("image/*") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(if (isHindi) "📁 गैलरी से चुनें" else "📁 Choose Gallery")
                            }
                        }
                    }
                }
            }
        }

        // Multi-line Text Parser & Quick Entry Area
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE0E0E0))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = if (isHindi) "📝 नाम व मोबाइल नंबर लिस्ट (प्रति पंक्ति एक नाम):" else "📝 Name & Phone List (One entry per line):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaroonPrimary
                    )
                    Text(
                        text = if (isHindi) "उदाहरण: 1. रमेश कुमार 9876543210 मेरठ (देवनागरी १. २. भी मान्य)" else "Example: 1. Ramesh Kumar 9876543210 Meerut",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = rawTextInput,
                        onValueChange = { rawTextInput = it },
                        placeholder = {
                            Text(
                                "1. रमेश कुमार 9876543210 मेरठ\n2. सुरेश शर्मा 9812345678 दिल्ली\n3. अनिता देवी 9998887776 बुलंदशहर",
                                fontSize = 12.sp,
                                color = Color.LightGray
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaroonAccent,
                            unfocusedBorderColor = Color.LightGray
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            val parsed = PaperRegisterScannerEngine.parseRegisterText(rawTextInput)
                            if (parsed.isEmpty()) {
                                Toast.makeText(context, if (isHindi) "कोई वैध नाम नहीं मिला! कृपया लिस्ट लिखें।" else "No valid names found! Please write a list.", Toast.LENGTH_SHORT).show()
                            } else {
                                parsedEntries = parsed
                                Toast.makeText(context, if (isHindi) "✅ ${parsed.size} नाम क्रमबद्ध पहचान लिए गए!" else "✅ ${parsed.size} names parsed in order!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = if (isHindi) "🔍 नाम पार्स व क्रमबद्ध करें (Parse in Sequence)" else "🔍 Parse Names in Sequence",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Interactive Editable Review Table
        if (parsedEntries.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isHindi) "📋 क्रमबद्ध टोकन समीक्षा (${parsedEntries.size} नाम)" else "📋 Sequential Token Review (${parsedEntries.size} entries)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaroonPrimary
                    )

                    TextButton(onClick = {
                        val updated = parsedEntries.toMutableList()
                        val nextSerial = (updated.maxOfOrNull { it.serialNumber } ?: 0) + 1
                        updated.add(
                            RegisterEntry(
                                serialNumber = nextSerial,
                                patientName = "",
                                phoneNumber = "",
                                city = "डूँगरा जाट (स्थानीय)"
                            )
                        )
                        parsedEntries = updated
                    }) {
                        Text(if (isHindi) "+ नया नाम जोड़ें" else "+ Add Row", fontWeight = FontWeight.Bold, color = MaroonAccent)
                    }
                }
            }

            itemsIndexed(parsedEntries) { index, entry ->
                val proposedTokenNum = currentMaxTokenToday + 1 + index
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE0E0E0))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaroonAccent
                            ) {
                                Text(
                                    text = "टोकन #$proposedTokenNum",
                                    color = AmberGold,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }

                            Text(
                                text = "पंक्ति #${index + 1}",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )

                            IconButton(
                                onClick = {
                                    val updated = parsedEntries.toMutableList()
                                    updated.removeAt(index)
                                    parsedEntries = updated
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Text("🗑️", fontSize = 14.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Devotee Name Field
                        OutlinedTextField(
                            value = entry.patientName,
                            onValueChange = { newName ->
                                val updated = parsedEntries.toMutableList()
                                updated[index] = entry.copy(patientName = newName)
                                parsedEntries = updated
                            },
                            label = { Text(if (isHindi) "भक्त / मरीज का नाम" else "Devotee Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = entry.phoneNumber,
                                onValueChange = { newPhone ->
                                    val updated = parsedEntries.toMutableList()
                                    updated[index] = entry.copy(phoneNumber = newPhone)
                                    parsedEntries = updated
                                },
                                label = { Text(if (isHindi) "मोबाइल" else "Phone") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp)
                            )

                            OutlinedTextField(
                                value = entry.city,
                                onValueChange = { newCity ->
                                    val updated = parsedEntries.toMutableList()
                                    updated[index] = entry.copy(city = newCity)
                                    parsedEntries = updated
                                },
                                label = { Text(if (isHindi) "शहर / गाँव" else "City") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }
            }

            // Prominent Sequential Token Generation Action Button
            item {
                Spacer(modifier = Modifier.height(6.dp))

                Button(
                    onClick = {
                        val validEntries = parsedEntries.filter { it.patientName.isNotBlank() }
                        if (validEntries.isEmpty()) {
                            Toast.makeText(context, if (isHindi) "कृपया कम से कम एक नाम भरें!" else "Please provide at least one valid name!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        scope.launch {
                            isProcessing = true
                            errorMessage = null
                            try {
                                val registeredBy = if (admin.role == AdminRole.SUPER_ADMIN) {
                                    "SUPER_ADMIN (${admin.name})"
                                } else {
                                    "SEVADAR_DESK (${admin.name})"
                                }

                                val created = repository.registerBatchTokens(
                                    entries = validEntries,
                                    registeredBy = registeredBy
                                )

                                generatedTokensResult = created
                                showSuccessDialog = true
                                parsedEntries = emptyList()
                                rawTextInput = ""
                                capturedBitmap = null
                                refreshSequenceInfo()
                                onTokensGenerated()
                            } catch (e: Exception) {
                                errorMessage = e.localizedMessage ?: "त्रुटि उत्पन्न हुई"
                            } finally {
                                isProcessing = false
                            }
                        }
                    },
                    enabled = !isProcessing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary)
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(color = AmberGold, modifier = Modifier.size(24.dp))
                    } else {
                        Text(
                            text = if (isHindi)
                                "⚡ सभी ${parsedEntries.size} टोकन क्रमबद्ध जारी करें (#${currentMaxTokenToday + 1} से)"
                            else
                                "⚡ Issue All ${parsedEntries.size} Tokens (#${currentMaxTokenToday + 1} Onwards)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = AmberGold
                        )
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "⚠️ $errorMessage",
                        color = Color.Red,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Success Confirmation Dialog
    if (showSuccessDialog && generatedTokensResult != null) {
        val tokens = generatedTokensResult!!
        val firstNum = tokens.firstOrNull()?.tokenNumber ?: 0
        val lastNum = tokens.lastOrNull()?.tokenNumber ?: 0

        Dialog(onDismissRequest = { showSuccessDialog = false }) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(2.dp, AmberGold),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE8F5E9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🎉", fontSize = 32.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (isHindi) "क्रमबद्ध टोकन सफलतापूर्वक जारी!" else "Sequential Tokens Issued Successfully!",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = MaroonPrimary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "कुल ${tokens.size} टोकन: #${firstNum} से #${lastNum} तक",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1B5E20)
                    )

                    Text(
                        text = if (isHindi) "Google Sheets में भी लाइव सिंक पूर्ण हो गया है।" else "Live synced with central Google Sheet.",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action buttons: Share list or dismiss
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val shareText = buildString {
                                    append("🚩 श्री बालाजी कृपा धाम, डुंगरा जाट 🚩\n")
                                    append("रविवार दरबार टोकन सूची (रजिस्टर स्कैन):\n\n")
                                    for (t in tokens) {
                                        append("टोकन #${t.tokenNumber}: ${t.patientName} (${t.city})\n")
                                    }
                                    append("\nपूर्णतः निःशुल्क सेवा। जय श्री बालाजी महाराज!")
                                }
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "टोकन सूची शेयर करें"))
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(if (isHindi) "📤 शेयर करें" else "📤 Share", fontSize = 12.sp)
                        }

                        Button(
                            onClick = { showSuccessDialog = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaroonAccent)
                        ) {
                            Text(if (isHindi) "पूर्ण (Done)" else "Done", color = AmberGold, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
