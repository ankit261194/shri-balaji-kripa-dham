package com.example.shribalajikripadham.ui.admin

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.shribalajikripadham.data.model.Admin
import com.example.shribalajikripadham.data.repository.AshramRepository
import com.example.shribalajikripadham.theme.AmberGold
import com.example.shribalajikripadham.theme.MaroonPrimary
import com.example.shribalajikripadham.theme.SaffronPrimary
import com.example.shribalajikripadham.util.DevoteePhotoHelper
import com.example.shribalajikripadham.util.IdCardTemplate
import com.example.shribalajikripadham.util.IdCardTemplateLibrary
import com.example.shribalajikripadham.util.PhotoshopPsdGenerator
import com.example.shribalajikripadham.util.SevadarIdCardData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperIdCardStudioTab(
    isHindi: Boolean,
    repository: AshramRepository,
    adminsList: List<Admin>
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedCategory by remember { mutableStateOf("सभी (All 50+)") }
    var selectedTemplate by remember { mutableStateOf(IdCardTemplateLibrary.TEMPLATES[0]) }
    var previewSideTab by remember { mutableStateOf(0) } // 0 = Front, 1 = Back

    // Card Details Input States
    var sevadarName by remember { mutableStateOf("") }
    var sevadarIdNo by remember { mutableStateOf("SBKD-2026-001") }
    var sevadarRole by remember { mutableStateOf("अधिकृत सेवादार") }
    var dutyArea by remember { mutableStateOf("कतार व टोकन व्यवस्था") }
    var phoneNumber by remember { mutableStateOf("+91 98765 43210") }
    var bloodGroup by remember { mutableStateOf("O+") }
    var validityYear by remember { mutableStateOf("2026 - 2027") }
    var photoUri by remember { mutableStateOf("") }

    var isGenerating by remember { mutableStateOf(false) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var successDialogMsg by remember { mutableStateOf<String?>(null) }
    var exportedFileUri by remember { mutableStateOf<Uri?>(null) }

    // Dropdown to pick from existing Sevadars
    var isSevadarDropdownExpanded by remember { mutableStateOf(false) }

    // Photo picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val bmp = withContext(Dispatchers.IO) {
                    DevoteePhotoHelper.loadBitmap(context, uri.toString())
                }
                if (bmp != null) {
                    val saved = DevoteePhotoHelper.saveDevoteePhoto(context, bmp, "idcard_sevadar")
                    if (saved.isNotBlank()) {
                        photoUri = saved
                    }
                }
            }
        }
    }

    // Refresh live preview bitmap
    fun refreshPreview() {
        val cardData = SevadarIdCardData(
            sevadarId = sevadarIdNo,
            name = sevadarName,
            role = sevadarRole,
            dutyArea = dutyArea,
            phoneNumber = phoneNumber,
            bloodGroup = bloodGroup,
            validityYear = validityYear,
            photoUri = photoUri,
            templateId = selectedTemplate.id
        )
        previewBitmap = PhotoshopPsdGenerator.generateIdCardBitmap(
            context = context,
            data = cardData,
            template = selectedTemplate,
            isBackSide = (previewSideTab == 1)
        )
    }

    LaunchedEffect(selectedTemplate, previewSideTab, sevadarName, sevadarIdNo, sevadarRole, dutyArea, phoneNumber, bloodGroup, validityYear, photoUri) {
        refreshPreview()
    }

    val categories = listOf("सभी (All 50+)") + IdCardTemplateLibrary.getAllCategories()
    val filteredTemplates = if (selectedCategory == "सभी (All 50+)") {
        IdCardTemplateLibrary.TEMPLATES
    } else {
        IdCardTemplateLibrary.getByCategory(selectedCategory)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 40.dp)
    ) {
        // HEADER TITLE CARD
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(3.dp),
                border = BorderStroke(1.2.dp, SaffronPrimary)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🪪", fontSize = 28.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isHindi) "सुपर एडमिन 50+ ID कार्ड स्टूडियो" else "Super Admin 50+ ID Card Studio",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaroonPrimary
                            )
                            Text(
                                text = if (isHindi) "50+ भव्य टेम्पलेट्स से 1-क्लिक में Photoshop (.PSD), PDF व इमेज बनाएं" else "Generate Adobe Photoshop (.PSD), PDF & PNG in 1 click",
                                fontSize = 12.sp,
                                color = Color.DarkGray
                            )
                        }
                    }
                }
            }
        }

        // STEP 1: CATEGORY & TEMPLATE SELECTOR
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = if (isHindi) "🎨 चरण 1: ID कार्ड टेम्पलेट चुनें (50+ उपलब्ध)" else "🎨 Step 1: Select ID Card Template (50+ Available)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaroonPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Category Filter Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.forEach { cat ->
                            val isSelected = (selectedCategory == cat)
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SaffronPrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Horizontal Scrolling Template Cards
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(filteredTemplates) { tmpl ->
                            val isSelected = (selectedTemplate.id == tmpl.id)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) Color(0xFFFFF3E0) else Color(0xFFFBFBFB),
                                border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) SaffronPrimary else Color(0xFFE0E0E0)),
                                modifier = Modifier
                                    .width(135.dp)
                                    .clickable { selectedTemplate = tmpl }
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // Mini color badge preview
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(28.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(tmpl.primaryColor),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text(tmpl.emblem, fontSize = 14.sp)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = tmpl.titleHindi,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        color = if (isSelected) MaroonPrimary else Color.Black
                                    )
                                    Text(
                                        text = tmpl.category,
                                        fontSize = 9.5.sp,
                                        color = Color.Gray
                                    )
                                    if (isSelected) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("✓ चयनित", color = SaffronPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // STEP 2: SEVADAR / ADMIN DETAILS FORM
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = if (isHindi) "👤 चरण 2: सेवादार विवरण भरें" else "👤 Step 2: Fill Sevadar Details",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaroonPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Quick Sevadar Dropdown Selector
                    if (adminsList.isNotEmpty()) {
                        ExposedDropdownMenuBox(
                            expanded = isSevadarDropdownExpanded,
                            onExpandedChange = { isSevadarDropdownExpanded = !isSevadarDropdownExpanded }
                        ) {
                            OutlinedTextField(
                                value = if (sevadarName.isNotBlank()) "चयनित: $sevadarName" else "सूची से सेवादार चुनें...",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("सूचीबद्ध सेवादारों में से चुनें") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isSevadarDropdownExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = isSevadarDropdownExpanded,
                                onDismissRequest = { isSevadarDropdownExpanded = false }
                            ) {
                                adminsList.forEach { admin ->
                                    DropdownMenuItem(
                                        text = { Text("${admin.name} (${admin.phoneNumber})") },
                                        onClick = {
                                            sevadarName = admin.name
                                            phoneNumber = admin.phoneNumber
                                            photoUri = admin.photoUri
                                            sevadarIdNo = "SBKD-${admin.id.toString().padStart(3, '0')}"
                                            isSevadarDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    OutlinedTextField(
                        value = sevadarName,
                        onValueChange = { sevadarName = it },
                        label = { Text("सेवादार का पूरा नाम *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = sevadarIdNo,
                            onValueChange = { sevadarIdNo = it },
                            label = { Text("आईडी नंबर") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = bloodGroup,
                            onValueChange = { bloodGroup = it },
                            label = { Text("रक्त समूह (Blood Group)") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = sevadarRole,
                            onValueChange = { sevadarRole = it },
                            label = { Text("पद / उपाधि") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = dutyArea,
                            onValueChange = { dutyArea = it },
                            label = { Text("सेवा क्षेत्र / विभाग") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = { phoneNumber = it },
                            label = { Text("मोबाइल नंबर") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = validityYear,
                            onValueChange = { validityYear = it },
                            label = { Text("सत्र / सत्र वैधता") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Photo Selection & 90 deg Rotate
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            onClick = { photoPickerLauncher.launch("image/*") },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (photoUri.isNotBlank()) "📷 फोटो बदलें" else "📷 सेवादार फोटो चुनें", fontWeight = FontWeight.Bold)
                        }

                        if (photoUri.isNotBlank()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        val rotated = DevoteePhotoHelper.rotateSavedPhoto(context, photoUri, 90f)
                                        if (rotated != null) {
                                            photoUri = rotated
                                            Toast.makeText(context, "🔄 फोटो 90° घुमाई गई", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(Color(0xFFFFF3E0), CircleShape)
                            ) {
                                Text("🔄", fontSize = 18.sp)
                            }
                        }
                    }
                }
            }
        }

        // STEP 3: LIVE PREVIEW OF FRONT & BACK SIDE
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isHindi) "👁️ चरण 3: लाइव ID कार्ड प्रिव्यू" else "👁️ Step 3: Live ID Card Preview",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaroonPrimary
                        )
                        Text(
                            text = "300 DPI High-Res",
                            fontSize = 11.sp,
                            color = SaffronPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Front / Back Tab Switcher
                    TabRow(
                        selectedTabIndex = previewSideTab,
                        containerColor = Color(0xFFF5F5F5),
                        contentColor = MaroonPrimary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                    ) {
                        Tab(
                            selected = previewSideTab == 0,
                            onClick = { previewSideTab = 0 },
                            text = { Text("🪪 मुख्य भाग (Front Side)", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                        )
                        Tab(
                            selected = previewSideTab == 1,
                            onClick = { previewSideTab = 1 },
                            text = { Text("📜 पिछला भाग (Back Side)", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Render Preview Image
                    if (previewBitmap != null) {
                        Box(
                            modifier = Modifier
                                .width(220.dp)
                                .height(385.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .border(1.5.dp, AmberGold, RoundedCornerShape(14.dp))
                        ) {
                            Image(
                                bitmap = previewBitmap!!.asImageBitmap(),
                                contentDescription = "ID Card Preview",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .width(220.dp)
                                .height(385.dp)
                                .background(Color(0xFFEEEEEE), RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = SaffronPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "टेम्पलेट: ${selectedTemplate.titleHindi} (${selectedTemplate.category})",
                        fontSize = 12.sp,
                        color = Color.DarkGray,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // STEP 4: EXPORT ACTIONS (PHOTOSHOP .PSD, PRINTABLE PDF, HD PNG)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9EE)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.2.dp, SaffronPrimary),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isHindi) "📥 चरण 4: डाउनलोड व एक्सपोर्ट विकल्प" else "📥 Step 4: Download & Export Options",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaroonPrimary
                    )
                    Text(
                        text = if (isHindi) "वास्तविक Adobe Photoshop (.PSD) लेयर्ड फ़ाइल, प्रिंटेबल 300 DPI PDF अथवा HD इमेज तुरंत डाउनलोड करें।"
                        else "Download genuine Adobe Photoshop (.PSD) file, 300 DPI printable PVC card PDF or HD PNG image.",
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 1. PHOTOSHOP .PSD BUTTON
                    Button(
                        onClick = {
                            if (sevadarName.isBlank()) {
                                Toast.makeText(context, "कृपया पहले सेवादार का नाम भरें!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            isGenerating = true
                            scope.launch {
                                try {
                                    val cardData = SevadarIdCardData(
                                        sevadarId = sevadarIdNo,
                                        name = sevadarName,
                                        role = sevadarRole,
                                        dutyArea = dutyArea,
                                        phoneNumber = phoneNumber,
                                        bloodGroup = bloodGroup,
                                        validityYear = validityYear,
                                        photoUri = photoUri,
                                        templateId = selectedTemplate.id
                                    )
                                    val psdFile = PhotoshopPsdGenerator.generatePhotoshopPsd(context, cardData, selectedTemplate)
                                    isGenerating = false
                                    successDialogMsg = """✅ Adobe Photoshop (.PSD) फ़ाइल सफलतापूर्वक डाउनलोड हुई!

फ़ाइल का नाम: ${psdFile.name}
लोकेशन: फोन का Downloads फ़ोल्डर

आप इस फ़ाइल को Adobe Photoshop, Photopea या GIMP में खोलकर लेयर्स के साथ एडिट व प्रिंट कर सकते हैं।"""
                                    exportedFileUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", psdFile)
                                } catch (e: Exception) {
                                    isGenerating = false
                                    Toast.makeText(context, "त्रुटि: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(if (isHindi) "💾 Photoshop (.PSD) फाइल डाउनलोड करें" else "💾 Download Photoshop (.PSD) File", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 2. PRINTABLE 300 DPI PDF BUTTON
                    Button(
                        onClick = {
                            if (sevadarName.isBlank()) {
                                Toast.makeText(context, "कृपया पहले सेवादार का नाम भरें!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            isGenerating = true
                            scope.launch {
                                try {
                                    val cardData = SevadarIdCardData(
                                        sevadarId = sevadarIdNo,
                                        name = sevadarName,
                                        role = sevadarRole,
                                        dutyArea = dutyArea,
                                        phoneNumber = phoneNumber,
                                        bloodGroup = bloodGroup,
                                        validityYear = validityYear,
                                        photoUri = photoUri,
                                        templateId = selectedTemplate.id
                                    )
                                    val pdfFile = PhotoshopPsdGenerator.generatePrintablePdf(context, cardData, selectedTemplate)
                                    isGenerating = false
                                    successDialogMsg = """✅ प्रिंटेबल PVC Card (300 DPI) PDF डाउनलोड हुई!

फ़ाइल: ${pdfFile.name}
स्थान: Downloads फोल्डर

दोनों साइड (Front व Back) उच्च गुणवत्ता में तैयार हैं।"""
                                    exportedFileUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
                                } catch (e: Exception) {
                                    isGenerating = false
                                    Toast.makeText(context, "त्रुटि: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Text(if (isHindi) "📄 प्रिंटेबल PVC PDF (300 DPI) डाउनलोड करें" else "📄 Download Printable PVC PDF (300 DPI)", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 3. HD PNG IMAGES BUTTON
                    OutlinedButton(
                        onClick = {
                            if (sevadarName.isBlank()) {
                                Toast.makeText(context, "कृपया पहले सेवादार का नाम भरें!", Toast.LENGTH_SHORT).show()
                                return@OutlinedButton
                            }
                            isGenerating = true
                            scope.launch {
                                try {
                                    val cardData = SevadarIdCardData(
                                        sevadarId = sevadarIdNo,
                                        name = sevadarName,
                                        role = sevadarRole,
                                        dutyArea = dutyArea,
                                        phoneNumber = phoneNumber,
                                        bloodGroup = bloodGroup,
                                        validityYear = validityYear,
                                        photoUri = photoUri,
                                        templateId = selectedTemplate.id
                                    )
                                    val (front, back) = PhotoshopPsdGenerator.saveIdCardPng(context, cardData, selectedTemplate)
                                    isGenerating = false
                                    successDialogMsg = """✅ Front व Back दोनों HD इमेज डाउनलोड फोल्डर में सेव हो गईं!

- ${front.name}
- ${back.name}"""
                                    exportedFileUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", front)
                                } catch (e: Exception) {
                                    isGenerating = false
                                    Toast.makeText(context, "त्रुटि: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Text(if (isHindi) "🖼️ Front & Back HD इमेज (PNG) सेव करें" else "🖼️ Save Front & Back HD PNG Images", fontWeight = FontWeight.Bold)
                    }

                    if (isGenerating) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = SaffronPrimary, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("कृपया प्रतीक्षा करें, 300 DPI फ़ाइल जनरेट हो रही है...", fontSize = 12.sp, color = Color.DarkGray)
                        }
                    }
                }
            }
        }
    }

    // SUCCESS & SHARE DIALOG
    if (successDialogMsg != null) {
        AlertDialog(
            onDismissRequest = { successDialogMsg = null },
            title = { Text("🚩 ID कार्ड सफलतापूर्वक तैयार!", fontWeight = FontWeight.Bold, color = MaroonPrimary) },
            text = { Text(successDialogMsg!!, fontSize = 13.5.sp, lineHeight = 19.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        exportedFileUri?.let { uri ->
                            try {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "*/*"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "ID कार्ड फ़ाइल शेयर करें"))
                            } catch (e: Exception) {
                                Toast.makeText(context, "शेयर करने में त्रुटि: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                        }
                        successDialogMsg = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary)
                ) {
                    Text("📤 फ़ाइल शेयर करें / भेजें")
                }
            },
            dismissButton = {
                TextButton(onClick = { successDialogMsg = null }) {
                    Text("ठीक है")
                }
            }
        )
    }
}
