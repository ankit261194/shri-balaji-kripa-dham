package com.example.shribalajikripadham.ui.havan

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shribalajikripadham.data.repository.AshramRepository
import com.example.shribalajikripadham.theme.AmberGold
import com.example.shribalajikripadham.theme.MaroonPrimary
import com.example.shribalajikripadham.theme.SaffronPrimary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HavanApplicationScreen(
    isHindi: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { AshramRepository(context) }

    // Form Field States
    var devoteeName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var whatsappNumber by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var gotra by remember { mutableStateOf("") }
    var familyMembersCount by remember { mutableIntStateOf(4) }
    var problemDetails by remember { mutableStateOf("") }

    // Mandatory Terms Acknowledgements
    var costAcknowledged by remember { mutableStateOf(true) }
    var travelFareAcknowledged by remember { mutableStateOf(true) }

    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var submittedAppNo by remember { mutableStateOf<String?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isHindi) "🔥 हवन कराने हेतु आवेदन" else "🔥 Apply for Sacred Havan",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (isHindi) "श्री बालाजी कृपा धाम (डूँगरा जाट)" else "Shri Balaji Kripa Dham",
                            fontSize = 11.sp,
                            color = AmberGold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaroonPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF9F7F5))
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // =================================================================
            // 📢 TOP SACRED NOTICE CARD: EXPENSES & TERMS
            // "form ke upar ye sari cheeze likhi hogi kharcha pani jo bhi btaya thaa"
            // =================================================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                border = BorderStroke(1.5.dp, Color(0xFFFFA000)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF6F00)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🔥", fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isHindi) "पावन हवन अनुष्ठान सेवा" else "Sacred Havan Anushthan Rules",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFBF360C)
                            )
                            Text(
                                text = if (isHindi) "नियम एवं अनुमानित खर्च संबंधी आवश्यक सूचना" else "Important Expense & Travel Terms",
                                fontSize = 11.sp,
                                color = Color(0xFF795548),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFFFCC80))

                    // Rule 1: Estimated Cost ~₹14,000
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text("💰 ", fontSize = 16.sp)
                        Column {
                            Text(
                                text = if (isHindi) "हवन का अनुमानित खर्च: लगभग ₹14,000" else "Estimated Cost: Approx ₹14,000",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFB71C1C)
                            )
                            Text(
                                text = if (isHindi)
                                    "समस्त हवन समिधा, आहुति द्रव्य, पूजन सामग्री, सूखा गोला, देशी घी एवं वैदिक ब्राह्मण व्यवस्था हेतु।"
                                else
                                    "For complete sacred wood, puja samagri, desi ghee, and arrangements.",
                                fontSize = 12.sp,
                                color = Color(0xFF4E342E),
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Rule 2: Vehicle Travel Fare Borne by Devotee
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text("🚗 ", fontSize = 16.sp)
                        Column {
                            Text(
                                text = if (isHindi) "गाड़ी का आने-जाने का किराया: भगत को खुद देना होगा" else "Travel Fare: Devotee's Responsibility",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFB71C1C)
                            )
                            Text(
                                text = if (isHindi)
                                    "धाम से यजमान के घर तक आश्रम सेवा दल / पंडित जी के आने-जाने की गाड़ी व्यवस्था अथवा उसका समस्त किराया यजमान (भगत) को स्वयं देना होगा।"
                                else
                                    "Vehicle travel fare for arrival and departure must be borne entirely by the devotee.",
                                fontSize = 12.sp,
                                color = Color(0xFF4E342E),
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Rule 3: Free Guru Seva
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text("🚩 ", fontSize = 16.sp)
                        Column {
                            Text(
                                text = if (isHindi) "गुरु कृपा व आध्यात्मिक सेवा पूर्णतः 100% निःशुल्क है" else "Guru Seva is 100% Free",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                            Text(
                                text = if (isHindi)
                                    "पूज्य गुरुदेव जी का सानिध्य व कृपा सेवा पूर्णतः निःशुल्क (FREE) है, आश्रम द्वारा कोई दक्षिणा या निजी शुल्क नहीं लिया जाता।"
                                else
                                    "Guruji's divine guidance and blessings are completely free. No personal fee or dakshina.",
                                fontSize = 11.5.sp,
                                color = Color(0xFF33691E),
                                lineHeight = 15.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Rule 4: Date Decision by Ashram
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text("📅 ", fontSize = 16.sp)
                        Column {
                            Text(
                                text = if (isHindi) "हवन की पावन तिथि: आश्रम द्वारा निर्धारित होगी" else "Havan Date: Scheduled by Ashram",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE65100)
                            )
                            Text(
                                text = if (isHindi)
                                    "आवेदन प्राप्त होने के उपरांत आश्रम सेवा दल द्वारा आपसे संपर्क कर शुभ तिथि निर्धारित की जाएगी।"
                                else
                                    "After reviewing your application, the Ashram team will contact you to schedule an auspicious date.",
                                fontSize = 11.5.sp,
                                color = Color(0xFF5D4037),
                                lineHeight = 15.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (isHindi)
                            "⚠️ केवल पूर्ण सहमत एवं इच्छुक भगत ही यह पावन आवेदन भरें।"
                        else
                            "⚠️ Please apply only if you agree with the estimated cost and travel fare terms.",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD84315)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // =================================================================
            // DEVOTEE APPLICATION FORM (NO DATE SELECTION, NO HAVAN CATEGORY)
            // =================================================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isHindi) "यजमान विवरण एवं हवन प्रार्थना पत्र" else "Devotee Details & Prayer Form",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaroonPrimary
                    )
                    Text(
                        text = if (isHindi) "कृपया सभी आवश्यक जानकारी सही-सही भरें (* अनिवार्य)" else "Please fill required details carefully",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 1. Devotee Name
                    OutlinedTextField(
                        value = devoteeName,
                        onValueChange = { devoteeName = it },
                        label = { Text(if (isHindi) "भक्त / यजमान का पूरा नाम *" else "Devotee Full Name *") },
                        placeholder = { Text("उदा. रामकुमार शर्मा") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 2. Phone & WhatsApp in a Row
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = { if (it.length <= 10 && it.all { ch -> ch.isDigit() }) phoneNumber = it },
                            label = { Text(if (isHindi) "मोबाइल नंबर *" else "Mobile No *") },
                            placeholder = { Text("10 अंक") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = whatsappNumber,
                            onValueChange = { if (it.length <= 10 && it.all { ch -> ch.isDigit() }) whatsappNumber = it },
                            label = { Text(if (isHindi) "व्हाट्सएप" else "WhatsApp") },
                            placeholder = { Text("वैकल्पिक") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 3. Complete Address
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text(if (isHindi) "हवन स्थल का पूरा पता *" else "Complete Address *") },
                        placeholder = { Text("मकान नं, गाँव/शहर, तहसील, जिला व पिनकोड...") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 4. Gotra & Family Members
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = gotra,
                            onValueChange = { gotra = it },
                            label = { Text(if (isHindi) "गोत्र / कुलदेवता" else "Gotra") },
                            placeholder = { Text("उदा. कश्यप") },
                            singleLine = true,
                            modifier = Modifier.weight(1.2f),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = familyMembersCount.toString(),
                            onValueChange = { familyMembersCount = it.toIntOrNull() ?: 4 },
                            label = { Text(if (isHindi) "सदस्य संख्या" else "Members") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(0.8f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 5. Special Notes / Prayer
                    OutlinedTextField(
                        value = problemDetails,
                        onValueChange = { problemDetails = it },
                        label = { Text(if (isHindi) "विशेष प्रार्थना / टिप्पणी (वैकल्पिक)" else "Special Prayer / Notes (Optional)") },
                        placeholder = { Text("हवन से संबंधित कोई विशेष टिप्पणी या प्रार्थना लिखें...") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // =========================================================
                    // MANDATORY ACKNOWLEDGEMENT CHECKBOXES
                    // =========================================================
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFFFB74D))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // Consent 1: Cost ₹14,000
                            Row(
                                verticalAlignment = Alignment.Top,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { costAcknowledged = !costAcknowledged }
                            ) {
                                Checkbox(
                                    checked = costAcknowledged,
                                    onCheckedChange = { costAcknowledged = it },
                                    colors = CheckboxDefaults.colors(checkedColor = MaroonPrimary)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isHindi)
                                        "सहमति 1: मुझे ज्ञात है कि हवन सामग्री व पूजन विधि का अनुमानित खर्च लगभग ₹14,000 (चौदह हज़ार रुपये) होगा, जो मेरे द्वारा वहन किया जाएगा।"
                                    else
                                        "Consent 1: I acknowledge that the estimated cost of havan samagri is approx ₹14,000, which will be borne by me.",
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF4E342E)
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Consent 2: Devotee Bears Travel Fare
                            Row(
                                verticalAlignment = Alignment.Top,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { travelFareAcknowledged = !travelFareAcknowledged }
                            ) {
                                Checkbox(
                                    checked = travelFareAcknowledged,
                                    onCheckedChange = { travelFareAcknowledged = it },
                                    colors = CheckboxDefaults.colors(checkedColor = MaroonPrimary)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isHindi)
                                        "सहमति 2: धाम से आने-जाने का गाड़ी किराया व आवागमन व्यवस्था भगत/यजमान (मेरे) द्वारा स्वयं वहन की जाएगी।"
                                    else
                                        "Consent 2: Vehicle travel fare for arriving and departing must be borne by me (devotee).",
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF4E342E)
                                )
                            }
                        }
                    }

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = errorMessage ?: "",
                            color = Color(0xFFD32F2F),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Submit Button
                    Button(
                        onClick = {
                            errorMessage = null
                            val cleanName = devoteeName.trim()
                            val cleanPhone = phoneNumber.trim()
                            val cleanAddress = address.trim()

                            if (cleanName.length < 2) {
                                errorMessage = if (isHindi) "कृपया यजमान का पूरा नाम दर्ज करें।" else "Please enter devotee name."
                                return@Button
                            }
                            if (cleanPhone.length != 10) {
                                errorMessage = if (isHindi) "कृपया 10 अंकों का मान्य मोबाइल नंबर दर्ज करें।" else "Please enter 10-digit mobile number."
                                return@Button
                            }
                            if (cleanAddress.length < 6) {
                                errorMessage = if (isHindi) "कृपया हवन कराने का पूरा पता दर्ज करें।" else "Please enter complete address."
                                return@Button
                            }
                            if (!costAcknowledged || !travelFareAcknowledged) {
                                errorMessage = if (isHindi)
                                    "हवन सामग्री खर्च (~₹14,000) एवं गाड़ी किराया वहन करने की सहमति अनिवार्य है।"
                                else
                                    "Agreement to estimated cost (~₹14,000) and travel fare is mandatory."
                                return@Button
                            }

                            scope.launch {
                                isSubmitting = true
                                val (ok, appNo) = repository.submitDevoteeHavanApplication(
                                    devoteeName = cleanName,
                                    phoneNumber = cleanPhone,
                                    whatsappNumber = whatsappNumber.trim(),
                                    preferredDate = "आश्रम द्वारा तय होगी",
                                    address = cleanAddress,
                                    gotra = gotra.trim(),
                                    familyMembersCount = familyMembersCount,
                                    havanPurpose = "पावन हवन अनुष्ठान",
                                    problemDetails = problemDetails.trim(),
                                    costAcknowledged = costAcknowledged,
                                    travelFareAcknowledged = travelFareAcknowledged
                                )
                                isSubmitting = false
                                if (ok) {
                                    submittedAppNo = appNo
                                    showSuccessDialog = true
                                } else {
                                    errorMessage = if (isHindi)
                                        "आवेदन दर्ज करने में समस्या हुई। कृपया पुनः प्रयास करें।"
                                    else
                                        "Failed to submit application. Please retry."
                                }
                            }
                        },
                        enabled = !isSubmitting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text(
                                text = if (isHindi) "🔥 हवन आवेदन सबमिट करें" else "🔥 Submit Havan Application",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // =========================================================================
    // SUCCESS CONFIRMATION DIALOG
    // =========================================================================
    if (showSuccessDialog && submittedAppNo != null) {
        AlertDialog(
            onDismissRequest = {
                showSuccessDialog = false
                onBack()
            },
            title = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🚩", fontSize = 36.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isHindi) "जय श्री बालाजी महाराज!" else "Jai Shri Balaji Maharaj!",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32),
                        textAlign = TextAlign.Center
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isHindi)
                            "आपका पावन हवन आवेदन सफलतापूर्वक दर्ज हो गया है।"
                        else
                            "Your Havan application has been submitted successfully.",
                        fontSize = 13.sp,
                        color = Color(0xFF37474F),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                        border = BorderStroke(1.5.dp, Color(0xFFFFB300)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (isHindi) "आवेदन संदर्भ संख्या (Application No)" else "Application Reference No",
                                fontSize = 11.sp,
                                color = Color(0xFF795548),
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = submittedAppNo ?: "",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFB71C1C),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            Text(
                                text = "यजमान: $devoteeName",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF5D4037)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = if (isHindi)
                            "पूज्य गुरुदेव जी के सान्निध्य में आश्रम सेवा दल आपके आवेदन की समीक्षा कर तिथि निर्धारण हेतु जल्द ही आपसे संपर्क करेगा।"
                        else
                            "Ashram team will review your application and contact you soon to schedule the date.",
                        fontSize = 11.5.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        try {
                            val msg = "जय श्री बालाजी महाराज! 🙏\nमैंने श्री बालाजी कृपा धाम (डूँगरा जाट) के ऐप से हवन हेतु आवेदन किया है।\nआवेदन क्रमांक: $submittedAppNo\nयजमान: $devoteeName\nपता: $address"
                            val uri = Uri.parse("https://api.whatsapp.com/send?text=" + Uri.encode(msg))
                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                        } catch (e: Exception) {
                            Toast.makeText(context, "WhatsApp not available", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("💬 व्हाट्सएप पर सूचित करें", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showSuccessDialog = false
                        onBack()
                    }
                ) {
                    Text(if (isHindi) "मुख्य पृष्ठ पर लौटें" else "Back to Home", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
