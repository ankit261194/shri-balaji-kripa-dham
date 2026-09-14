package com.example.shribalajikripadham.ui.parcha

import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.shribalajikripadham.ai.SacredParchaEngine
import com.example.shribalajikripadham.data.model.ParchaCategory
import com.example.shribalajikripadham.data.model.SacredParcha
import com.example.shribalajikripadham.theme.sacredOutlinedTextFieldColors
import com.example.shribalajikripadham.theme.*
import com.example.shribalajikripadham.util.DevoteePhotoHelper
import com.example.shribalajikripadham.util.TakeAnyPicturePreview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditParchaDialog(
    isHindi: Boolean,
    initialParcha: SacredParcha? = null,
    onDismiss: () -> Unit,
    onSaveParcha: (SacredParcha) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(initialParcha?.title ?: "") }
    var selectedCategory by remember { mutableStateOf(initialParcha?.category ?: ParchaCategory.HAWAN) }
    var subtitle by remember { mutableStateOf(initialParcha?.subtitle ?: "") }
    var samagriText by remember { mutableStateOf(initialParcha?.samagriList?.joinToString("\n") ?: "") }
    var vidhiText by remember { mutableStateOf(initialParcha?.vidhiSteps?.joinToString("\n") ?: "") }
    var precautionsText by remember { mutableStateOf(initialParcha?.precautions?.joinToString("\n") ?: "") }
    var mantraText by remember { mutableStateOf(initialParcha?.mantraText ?: "") }
    var isHidden by remember { mutableStateOf(initialParcha?.isHidden ?: false) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var rawOcrInput by remember { mutableStateOf("") }
    var isScanMode by remember { mutableStateOf(false) }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = TakeAnyPicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            val safeBmp = DevoteePhotoHelper.toSoftwareBitmap(bitmap)
            capturedBitmap = safeBmp
            isScanMode = true
            Toast.makeText(context, if (isHindi) "📸 फोटो लोड हो गई! नीचे टेक्स्ट दर्ज/सत्यापित करें" else "Photo loaded! Verify details below", Toast.LENGTH_SHORT).show()
        }
    }

    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val bmp = DevoteePhotoHelper.loadBitmap(context, uri.toString())
            if (bmp != null) {
                val safeBmp = DevoteePhotoHelper.toSoftwareBitmap(bmp)
                capturedBitmap = safeBmp
                isScanMode = true
                Toast.makeText(context, if (isHindi) "📁 गैलरी से फोटो चुनी गई!" else "Photo picked from gallery!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.95f)
                .imePadding(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (initialParcha == null)
                                (if (isHindi) "📜 नया पर्चा जोड़ें / स्कैन करें" else "📜 Add / Scan New Parcha")
                            else
                                (if (isHindi) "✏️ पर्चा संपादित करें" else "✏️ Edit Parcha"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = MaroonPrimary
                        )
                        Text(
                            text = if (isHindi) "साफ-सुथरा A4 पेज बनाकर तुरंत ऐप पर लाइव करें" else "Format as clean A4 and publish live",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Text("✕", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = GoldSecondary.copy(alpha = 0.5f))

                // Scrollable Form Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Quick Template Pre-fill Section
                    if (initialParcha == null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFFF8E1),
                            border = BorderStroke(1.dp, GoldSecondary)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = if (isHindi) "⚡ त्वरित टेम्पलेट से भरें (1-क्लिक लोड):" else "⚡ Quick Load Canonical Preset:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaroonAccent
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val canonicals = remember { SacredParchaEngine.getCanonicalParchas() }
                                    canonicals.forEach { cp ->
                                        SuggestionChip(
                                            onClick = {
                                                title = cp.title
                                                selectedCategory = cp.category
                                                subtitle = cp.subtitle
                                                samagriText = cp.samagriList.joinToString("\n")
                                                vidhiText = cp.vidhiSteps.joinToString("\n")
                                                precautionsText = cp.precautions.joinToString("\n")
                                                mantraText = cp.mantraText
                                                Toast.makeText(context, if (isHindi) "${cp.category.displayNameHindi} लोड हो गया!" else "Loaded!", Toast.LENGTH_SHORT).show()
                                            },
                                            label = { Text("${cp.category.icon} ${cp.category.displayNameHindi}", fontSize = 11.sp) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Photo Scanning Row
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF3E5F5),
                        border = BorderStroke(1.dp, Color(0xFFCE93D8))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isHindi) "📷 कागज़ / रजिस्टर पर्चे की फोटो खींचें:" else "📷 Scan Paper Parcha Slip:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color(0xFF4A148C)
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    FilledTonalButton(
                                        onClick = { cameraLauncher.launch(null) },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("📸 कैमरा", fontSize = 11.sp)
                                    }
                                    FilledTonalButton(
                                        onClick = { galleryLauncher.launch("image/*") },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("📁 गैलरी", fontSize = 11.sp)
                                    }
                                }
                            }

                            if (capturedBitmap != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Image(
                                        bitmap = capturedBitmap!!.asImageBitmap(),
                                        contentDescription = "Parcha Photo",
                                        modifier = Modifier.size(50.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = if (isHindi) "✓ पर्चा फोटो संलग्न है।" else "Photo attached",
                                        fontSize = 11.sp,
                                        color = Color(0xFF2E7D32),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Raw text auto-parse button
                    if (isScanMode || rawOcrInput.isNotBlank()) {
                        OutlinedTextField(
                            value = rawOcrInput,
                            onValueChange = { rawOcrInput = it },
                            label = { Text(if (isHindi) "स्कैन किया गया कच्चा टेक्स्ट (यहाँ पेस्ट करें)" else "Raw Scanned OCR Text", fontWeight = FontWeight.SemiBold) },
                            textStyle = androidx.compose.ui.text.TextStyle(color = Color(0xFF111111), fontSize = 14.sp),
                            modifier = Modifier.fillMaxWidth().height(90.dp),
                            placeholder = { Text("उदा. हवन सामग्री: जौ, तिल, घी... विधि: 108 आहुति दें...", color = Color(0xFF757575)) },
                            colors = sacredOutlinedTextFieldColors()
                        )
                        Button(
                            onClick = {
                                if (rawOcrInput.isNotBlank()) {
                                    val parsed = SacredParchaEngine.parseScannedParchaText(rawOcrInput, selectedCategory)
                                    title = parsed.title
                                    selectedCategory = parsed.category
                                    if (parsed.subtitle.isNotBlank()) subtitle = parsed.subtitle
                                    if (parsed.samagriList.isNotEmpty()) samagriText = parsed.samagriList.joinToString("\n")
                                    if (parsed.vidhiSteps.isNotEmpty()) vidhiText = parsed.vidhiSteps.joinToString("\n")
                                    if (parsed.precautions.isNotEmpty()) precautionsText = parsed.precautions.joinToString("\n")
                                    if (parsed.mantraText.isNotBlank()) mantraText = parsed.mantraText
                                    Toast.makeText(context, if (isHindi) "टेक्स्ट को साफ-सुथरे अनुभागों में व्यवस्थित कर दिया गया!" else "Cleaned & Parsed into sections!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A1B9A))
                        ) {
                            Text(if (isHindi) "✨ टेक्स्ट को साफ-सुथरा व्यवस्थित करें" else "✨ Auto-Format into A4 Layout", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Category Selector
                    Text(if (isHindi) "पर्चा श्रेणी (Category):" else "Category:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaroonPrimary)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ParchaCategory.entries.forEach { cat ->
                            FilterChip(
                                selected = (selectedCategory == cat),
                                onClick = { selectedCategory = cat },
                                label = { Text("${cat.icon} ${cat.displayNameHindi}", fontSize = 11.sp) }
                            )
                        }
                    }

                    // Title
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(if (isHindi) "पर्चे का नाम / शीर्षक *" else "Parcha Title *", fontWeight = FontWeight.SemiBold) },
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color(0xFF111111), fontSize = 15.sp, fontWeight = FontWeight.Medium),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = sacredOutlinedTextFieldColors()
                    )

                    // Subtitle
                    OutlinedTextField(
                        value = subtitle,
                        onValueChange = { subtitle = it },
                        label = { Text(if (isHindi) "उप-शीर्षक / संक्षिप्त विवरण" else "Subtitle / Brief Summary", fontWeight = FontWeight.SemiBold) },
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color(0xFF111111), fontSize = 15.sp, fontWeight = FontWeight.Medium),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = sacredOutlinedTextFieldColors()
                    )

                    // Samagri List
                    OutlinedTextField(
                        value = samagriText,
                        onValueChange = { samagriText = it },
                        label = { Text(if (isHindi) "आवश्यक पूजा / हवन सामग्री (प्रत्येक वस्तु नई लाइन में लिखें)" else "Samagri List (One item per line)", fontWeight = FontWeight.SemiBold) },
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color(0xFF111111), fontSize = 14.sp),
                        modifier = Modifier.fillMaxWidth().height(110.dp),
                        colors = sacredOutlinedTextFieldColors()
                    )

                    // Vidhi Steps
                    OutlinedTextField(
                        value = vidhiText,
                        onValueChange = { vidhiText = it },
                        label = { Text(if (isHindi) "चरणबद्ध संपूर्ण विधि (प्रत्येक चरण नई लाइन में)" else "Step-by-Step Vidhi (One step per line)", fontWeight = FontWeight.SemiBold) },
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color(0xFF111111), fontSize = 14.sp),
                        modifier = Modifier.fillMaxWidth().height(110.dp),
                        colors = sacredOutlinedTextFieldColors()
                    )

                    // Precautions / Parhez
                    OutlinedTextField(
                        value = precautionsText,
                        onValueChange = { precautionsText = it },
                        label = { Text(if (isHindi) "महत्वपूर्ण सावधानियाँ व परहेज (नियम)" else "Precautions & Rules (One per line)", fontWeight = FontWeight.SemiBold) },
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color(0xFF111111), fontSize = 14.sp),
                        modifier = Modifier.fillMaxWidth().height(85.dp),
                        colors = sacredOutlinedTextFieldColors()
                    )

                    // Mantras
                    OutlinedTextField(
                        value = mantraText,
                        onValueChange = { mantraText = it },
                        label = { Text(if (isHindi) "सिद्ध मंत्र व स्तुति" else "Sacred Mantras", fontWeight = FontWeight.SemiBold) },
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color(0xFF111111), fontSize = 14.sp),
                        modifier = Modifier.fillMaxWidth().height(80.dp),
                        colors = sacredOutlinedTextFieldColors()
                    )

                    // Hide/Show Toggle
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isHidden) Color(0xFFFFEBEE) else Color(0xFFE8F5E9),
                        border = BorderStroke(1.dp, if (isHidden) Color(0xFFEF9A9A) else Color(0xFFA5D6A7))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isHidden) (if (isHindi) "🔒 पर्चा छिपा हुआ है (Hidden)" else "🔒 Hidden from Devotees")
                                           else (if (isHindi) "🌐 पर्चा सक्रिय व लाइव रहेगा (Public Live)" else "🌐 Active & Live to All"),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (isHidden) Color(0xFFC62828) else Color(0xFF1B5E20)
                                )
                                Text(
                                    text = if (isHidden)
                                        (if (isHindi) "केवल एडमिन को दिखेगा, सामान्य भक्तों से छिपा रहेगा।" else "Visible to Admins only.")
                                    else
                                        (if (isHindi) "सभी भक्तों को ऐप में दिखेगा और वे PDF डाउनलोड कर सकेंगे।" else "Visible to everyone, downloadable."),
                                    fontSize = 10.sp,
                                    color = Color.DarkGray
                                )
                            }
                            Switch(
                                checked = isHidden,
                                onCheckedChange = { isHidden = it }
                            )
                        }
                    }
                    // Bottom scroll clearance so keyboard never covers last inputs
                    Spacer(modifier = Modifier.height(48.dp))
                }

                // Action Buttons
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isHindi) "रद्द करें" else "Cancel")
                    }

                    Button(
                        onClick = {
                            if (title.isBlank()) {
                                Toast.makeText(context, if (isHindi) "कृपया पर्चे का शीर्षक दर्ज करें!" else "Please enter title!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            val samagriList = samagriText.lines().map { it.trim() }.filter { it.isNotBlank() }
                            val vidhiList = vidhiText.lines().map { it.trim() }.filter { it.isNotBlank() }
                            val preList = precautionsText.lines().map { it.trim() }.filter { it.isNotBlank() }

                            val updated = (initialParcha ?: SacredParcha(
                                parchaId = "PARCHA_" + System.currentTimeMillis(),
                                title = title,
                                createdAt = System.currentTimeMillis()
                            )).copy(
                                title = title,
                                category = selectedCategory,
                                subtitle = subtitle,
                                samagriList = samagriList,
                                vidhiSteps = vidhiList,
                                precautions = preList,
                                mantraText = mantraText,
                                isHidden = isHidden,
                                isPublished = true,
                                updatedAt = System.currentTimeMillis()
                            )

                            onSaveParcha(updated)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                        modifier = Modifier.weight(2f)
                    ) {
                        Text(
                            text = if (isHindi) "🚀 1-क्लिक में ऐप पर लाइव करें" else "🚀 Publish Live to App",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
