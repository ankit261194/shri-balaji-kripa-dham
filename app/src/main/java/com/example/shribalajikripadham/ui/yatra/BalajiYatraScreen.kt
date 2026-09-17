package com.example.shribalajikripadham.ui.yatra

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shribalajikripadham.data.model.AshramSettings
import com.example.shribalajikripadham.data.model.BusSeat
import com.example.shribalajikripadham.data.model.PaymentRecord
import com.example.shribalajikripadham.data.model.PaymentStatus
import com.example.shribalajikripadham.data.repository.AshramRepository
import com.example.shribalajikripadham.theme.*
import com.example.shribalajikripadham.util.BusTicketPdfGenerator
import com.example.shribalajikripadham.util.QrCodeGenerator
import kotlinx.coroutines.launch
import java.io.File
import java.net.URLEncoder

data class PassengerInput(
    val seatNumber: Int,
    val seatLabel: String,
    var name: String = "",
    var age: String = "",
    var gender: String = "पुरुष",
    var phone: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BalajiYatraScreen(
    isHindi: Boolean,
    onBack: () -> Unit,
    onNavigateToExpenses: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { AshramRepository(context) }
    val scope = rememberCoroutineScope()

    var settings by remember { mutableStateOf(AshramSettings()) }
    var seats by remember { mutableStateOf<List<BusSeat>>(emptyList()) }
    val selectedSeats = remember { mutableStateListOf<BusSeat>() }

    // Dialogs
    var showBookingSheet by remember { mutableStateOf(false) }
    var showSeatDetailsDialog by remember { mutableStateOf<BusSeat?>(null) }
    var downloadedTicketFile by remember { mutableStateOf<File?>(null) }
    var showTicketSuccessDialog by remember { mutableStateOf(false) }
    var isBookingInProgress by remember { mutableStateOf(false) }

    // Passenger inputs map
    val passengerInputs = remember { mutableStateMapOf<Int, PassengerInput>() }

    // Payment inputs
    var selectedPaymentApp by remember { mutableStateOf("PhonePe") }
    var transactionIdInput by remember { mutableStateOf("") }
    var payerNameInput by remember { mutableStateOf("") }
    var payerPhoneInput by remember { mutableStateOf("") }
    var bookingErrorMessage by remember { mutableStateOf<String?>(null) }
    val deviceId = remember {
        try {
            android.provider.Settings.Secure.getString(context.contentResolver, android.provider.Settings.Secure.ANDROID_ID) ?: "DEV_${System.currentTimeMillis()}"
        } catch (e: Exception) { "DEV_${System.currentTimeMillis()}" }
    }
    var holdSecondsRemaining by remember { mutableStateOf(300) }

    fun refreshData() {
        scope.launch {
            try {
                settings = repository.getSettings()
                seats = repository.getAllBusSeats()
                selectedSeats.clear()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshData()
    }

    LaunchedEffect(selectedSeats.size) {
        if (selectedSeats.isNotEmpty()) {
            holdSecondsRemaining = 300
            val sNums = selectedSeats.map { it.seatNumber }
            scope.launch {
                repository.holdBusSeats(sNums, deviceId)
            }
            while (holdSecondsRemaining > 0 && selectedSeats.isNotEmpty()) {
                kotlinx.coroutines.delay(1000L)
                holdSecondsRemaining--
            }
            if (holdSecondsRemaining <= 0 && selectedSeats.isNotEmpty()) {
                val toRelease = selectedSeats.map { it.seatNumber }
                selectedSeats.clear()
                passengerInputs.clear()
                showBookingSheet = false
                scope.launch {
                    repository.releaseBusSeatsHold(toRelease, deviceId)
                    refreshData()
                }
                bookingErrorMessage = if (isHindi) "समय समाप्त (5 मिनट)! सीटें पुनः सभी के लिए उपलब्ध कर दी गई हैं।" else "Time expired (5 mins)! Seats are released."
            }
        } else {
            holdSecondsRemaining = 300
        }
    }

    val bookedCount = seats.count { it.isBooked }
    val availableCount = seats.size - bookedCount
    val farePerSeat = if (settings.busSeatFareAmount > 0) settings.busSeatFareAmount else 1500
    val totalFare = selectedSeats.size * farePerSeat

    // Generate UPI QR Bitmap when payment sheet opens
    var upiQrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(showBookingSheet, totalFare, settings.ashramUpiId) {
        if (showBookingSheet && totalFare > 0) {
            val encodedName = try { URLEncoder.encode(settings.ashramUpiName, "UTF-8") } catch (e: Exception) { "ShriBalajiKripaDham" }
            val upiUri = "upi://pay?pa=${settings.ashramUpiId}&pn=$encodedName&am=$totalFare&cu=INR&tn=BalajiYatraTickets"
            upiQrBitmap = QrCodeGenerator.generateQrBitmap(upiUri, 380)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isHindi) "श्री बालाजी यात्रा (60-सीटर डीलक्स बस)" else "Shri Balaji Pilgrimage (60-Seater Bus)",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 17.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text(text = "⬅", color = Color.White, fontSize = 20.sp)
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToExpenses) {
                        Text(text = "💰", fontSize = 20.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaroonAccent)
            )
        },
        containerColor = SacredBackgroundLight
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(14.dp)
        ) {
            // Check Super Admin Live Status
            if (!settings.isBusBookingLive) {
                // Dignified "Inactive / Coming Soon" Notice under Super Admin Control
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEA)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, Color(0xFFE65100)),
                    elevation = CardDefaults.cardElevation(4.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🚌", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (isHindi) "श्री बालाजी यात्रा बस सेवा" else "Shri Balaji Bus Pilgrimage",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = MaroonPrimary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = Color(0xFFFFEBEE),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (isHindi) "🔒 सीट बुकिंग वर्तमान में अस्थायी रूप से बंद है" else "🔒 Seat Booking is currently inactive",
                                color = Color(0xFFC62828),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (isHindi)
                                "पूज्य गुरुजी एवं सुपर एडमिन द्वारा अगली बालाजी यात्रा (सालासर व मेंहदीपुर धाम) की तिथि घोषित होते ही ऑनलाइन 60-सीटर बस बुकिंग प्रारंभ कर दी जाएगी।\n\nकेवल वास्तविक बस व धर्मशाला का निर्धारित किराया (₹$farePerSeat प्रति सीट) लिया जाता है। यात्रा की अधिक जानकारी के लिए आश्रम से संपर्क करें।"
                            else
                                "Online bus booking for the 60-seater luxury coach will be activated once the upcoming pilgrimage dates are scheduled by Super Admin.\n\nOnly actual bus & accommodation fare (₹$farePerSeat per seat) is charged.",
                            fontSize = 13.sp,
                            color = Color.DarkGray,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { refreshData() },
                            colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(if (isHindi) "🔄 स्थिति पुनः जाँचें (Refresh)" else "🔄 Check Status", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // --- ACTIVE 60-SEATER BUS BOOKING UI ---
                // 1. Transparency Notice
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🚌", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isHindi)
                                "60-सीटर डीलक्स बस (3x2 बैठक): ₹$farePerSeat प्रति सीट वास्तविक किराया। सीटें चुनें व ऑनलाइन QR से भुगतान कर तुरंत A4 टिकट प्राप्त करें।"
                            else
                                "60-Seater Coach (3x2 layout): ₹$farePerSeat/seat. Select seats, pay via QR and get instant A4 ticket.",
                            fontSize = 12.sp,
                            color = TextPrimaryDark,
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 2. Summary Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            color = Color(0xFFE8F5E9),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (isHindi) "खाली: $availableCount" else "Vacant: $availableCount",
                                color = StatusAvailable,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Surface(
                            color = Color(0xFFFFEBEE),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (isHindi) "बुक: $bookedCount" else "Booked: $bookedCount",
                                color = StatusOutsideAshram,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        if (selectedSeats.isNotEmpty()) {
                            Surface(
                                color = Color(0xFFFFF3E0),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (isHindi) "चयनित: ${selectedSeats.size}" else "Selected: ${selectedSeats.size}",
                                    color = SaffronPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Button(
                        onClick = onNavigateToExpenses,
                        colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (isHindi) "खर्च हिसाब ➔" else "Expenses ➔",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3. Seat Legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    LegendIndicator(color = StatusAvailable, label = if (isHindi) "खाली" else "Vacant")
                    LegendIndicator(color = SaffronPrimary, label = if (isHindi) "चयनित" else "Selected")
                    LegendIndicator(color = Color(0xFFFF9800), label = if (isHindi) "होल्ड (5मि)" else "Held (5m)")
                    LegendIndicator(color = Color(0xFFFBC02D), label = if (isHindi) "सत्यापन बाकी" else "Pending")
                    LegendIndicator(color = StatusOutsideAshram, label = if (isHindi) "आरक्षित" else "Booked")
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 4. 60-SEATER BUS MAP (3x2 CONFIGURATION: 12 ROWS × 5 SEATS)
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(3.dp),
                    modifier = Modifier.fillMaxWidth().weight(1f)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Bus Front: Driver + Door
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = Color(0xFFCFD8DC),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = if (isHindi) "🚪 प्रवेश द्वार (Gate)" else "🚪 Entry",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            Surface(
                                color = Color(0xFFECEFF1),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = if (isHindi) "👨‍✈️ ड्राइवर केबिन" else "👨‍✈️ Driver",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        HorizontalDivider(color = Color(0xFFE0E0E0))
                        Spacer(modifier = Modifier.height(6.dp))

                        // Column Indicators: Left (A, B, C) | Aisle | Right (D, E)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("A", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
                                Text("B", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
                                Text("C", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
                            }
                            Text("रास्ता", fontSize = 9.sp, color = Color.LightGray, modifier = Modifier.width(28.dp), textAlign = TextAlign.Center)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("D", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
                                Text("E", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // 12 Rows of Seats (3x2 Layout)
                        val groupedRows = seats.groupBy { it.row }
                        val sortedRows = (1..12).toList()

                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(sortedRows) { rowIdx ->
                                val rowSeats = groupedRows[rowIdx] ?: emptyList()
                                val seatA = rowSeats.find { it.column == 1 }
                                val seatB = rowSeats.find { it.column == 2 }
                                val seatC = rowSeats.find { it.column == 3 }
                                val seatD = rowSeats.find { it.column == 4 }
                                val seatE = rowSeats.find { it.column == 5 }

                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Left Side (3 Seats: A, B, C)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        SixSeaterSeatItem(seatA, selectedSeats.contains(seatA), deviceId) { s ->
                                            handleSeatClick(s, selectedSeats, passengerInputs, deviceId, { showSeatDetailsDialog = it }) { bookingErrorMessage = it }
                                        }
                                        SixSeaterSeatItem(seatB, selectedSeats.contains(seatB), deviceId) { s ->
                                            handleSeatClick(s, selectedSeats, passengerInputs, deviceId, { showSeatDetailsDialog = it }) { bookingErrorMessage = it }
                                        }
                                        SixSeaterSeatItem(seatC, selectedSeats.contains(seatC), deviceId) { s ->
                                            handleSeatClick(s, selectedSeats, passengerInputs, deviceId, { showSeatDetailsDialog = it }) { bookingErrorMessage = it }
                                        }
                                    }

                                    // Central Aisle with Row Number
                                    Box(
                                        modifier = Modifier.width(28.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "$rowIdx",
                                            fontSize = 10.sp,
                                            color = Color.Gray,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    // Right Side (2 Seats: D, E)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        SixSeaterSeatItem(seatD, selectedSeats.contains(seatD), deviceId) { s ->
                                            handleSeatClick(s, selectedSeats, passengerInputs, deviceId, { showSeatDetailsDialog = it }) { bookingErrorMessage = it }
                                        }
                                        SixSeaterSeatItem(seatE, selectedSeats.contains(seatE), deviceId) { s ->
                                            handleSeatClick(s, selectedSeats, passengerInputs, deviceId, { showSeatDetailsDialog = it }) { bookingErrorMessage = it }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 5. Sticky Bottom Bar when seats are selected
                if (selectedSeats.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaroonPrimary),
                        shape = RoundedCornerShape(14.dp),
                        elevation = CardDefaults.cardElevation(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                val seatNames = selectedSeats.joinToString(", ") { it.seatLabel.ifEmpty { "#${it.seatNumber}" } }
                                val minStr = "%02d:%02d".format(holdSecondsRemaining / 60, holdSecondsRemaining % 60)
                                Text(
                                    text = if (isHindi) "सीटें: $seatNames (${selectedSeats.size}) • ⏱️ $minStr" else "Seats: $seatNames • ⏱️ $minStr",
                                    color = AmberGold,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = if (isHindi) "कुल किराया: ₹$totalFare" else "Total Fare: ₹$totalFare",
                                    color = Color.White,
                                    fontSize = 12.sp
                                )
                            }

                            Button(
                                onClick = {
                                    bookingErrorMessage = null
                                    showBookingSheet = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AmberGold),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = if (isHindi) "आगे बढ़ें ➔" else "Proceed ➔",
                                    color = MaroonPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // --- BOOKING SHEET / DIALOG (PASSENGER DETAILS + QR CODE PAYMENT) ---
    if (showBookingSheet && selectedSeats.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { if (!isBookingInProgress) showBookingSheet = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🚌", fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isHindi) "यात्री विवरण व QR भुगतान" else "Passengers & Payment",
                        fontWeight = FontWeight.Bold,
                        color = MaroonPrimary,
                        fontSize = 17.sp
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Summary Banner
                    Surface(
                        color = Color(0xFFFFF8E1),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            val labels = selectedSeats.joinToString(", ") { it.seatLabel.ifEmpty { "#${it.seatNumber}" } }
                            Text(
                                text = if (isHindi) "चयनित सीटें: $labels (${selectedSeats.size} सीटें)" else "Seats: $labels",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaroonPrimary
                            )
                            Text(
                                text = if (isHindi) "कुल देय राशि: ₹$totalFare (₹$farePerSeat x ${selectedSeats.size})" else "Total Amount: ₹$totalFare",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = Color(0xFFE65100)
                            )
                        }
                    }

                    // Payer Contact Details
                    Text(
                        text = if (isHindi) "संपर्क व्यक्ति (मुख्य भक्त का विवरण):" else "Primary Contact Details:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.DarkGray
                    )
                    OutlinedTextField(
                        value = payerNameInput,
                        onValueChange = { payerNameInput = it },
                        label = { Text(if (isHindi) "मुख्य भक्त का नाम *" else "Primary Name *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    OutlinedTextField(
                        value = payerPhoneInput,
                        onValueChange = { if (it.length <= 10) payerPhoneInput = it },
                        label = { Text(if (isHindi) "मोबाइल नंबर (10 अंक) *" else "Mobile Number *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    HorizontalDivider()

                    // Per-seat passenger inputs
                    Text(
                        text = if (isHindi) "प्रत्येक सीट के यात्री का नाम व आयु:" else "Passenger Details Per Seat:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaroonPrimary
                    )

                    selectedSeats.forEach { seat ->
                        val input = passengerInputs.getOrPut(seat.seatNumber) {
                            PassengerInput(seat.seatNumber, seat.seatLabel)
                        }
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "💺 सीट ${seat.seatLabel.ifEmpty { "#${seat.seatNumber}" }}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaroonPrimary
                                )
                                OutlinedTextField(
                                    value = input.name,
                                    onValueChange = { input.name = it },
                                    label = { Text(if (isHindi) "यात्री का पूरा नाम *" else "Passenger Name *", fontSize = 11.sp) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(6.dp)
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedTextField(
                                        value = input.age,
                                        onValueChange = { if (it.length <= 3) input.age = it },
                                        label = { Text(if (isHindi) "आयु *" else "Age *", fontSize = 11.sp) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    OutlinedTextField(
                                        value = input.gender,
                                        onValueChange = { input.gender = it },
                                        label = { Text(if (isHindi) "लिंग" else "Gender", fontSize = 11.sp) },
                                        singleLine = true,
                                        modifier = Modifier.weight(1.2f),
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider()

                    // UPI QR PAYMENT SECTION
                    Surface(
                        color = Color(0xFFE3F2FD),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF90CAF9)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (isHindi) "💰 आश्रम अधिकृत UPI QR कोड" else "💰 Ashram Official UPI QR",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF0D47A1)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isHindi) "किसी भी UPI ऐप से स्कैन करके ₹$totalFare का भुगतान करें:" else "Scan to pay ₹$totalFare with any UPI app:",
                                fontSize = 11.sp,
                                color = Color.DarkGray
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // QR Image Box
                            if (upiQrBitmap != null) {
                                Box(
                                    modifier = Modifier
                                        .size(170.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.White)
                                        .padding(6.dp)
                                ) {
                                    Image(
                                        bitmap = upiQrBitmap!!.asImageBitmap(),
                                        contentDescription = "Ashram UPI QR Code",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            } else {
                                CircularProgressIndicator(modifier = Modifier.size(32.dp))
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // UPI ID + Copy Button
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "UPI: ${settings.ashramUpiId}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF0D47A1)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                TextButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Ashram UPI", settings.ashramUpiId))
                                        Toast.makeText(context, if (isHindi) "UPI आईडी कॉपी हो गई!" else "UPI ID copied!", Toast.LENGTH_SHORT).show()
                                    },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("📋 Copy", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Payment App Selector
                    Text(
                        text = if (isHindi) "भुगतान किस ऐप से किया? *" else "Payment App Used: *",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    val paymentApps = listOf("PhonePe", "Google Pay", "Paytm", "BHIM", "Cred", "Other")
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        paymentApps.forEach { appName ->
                            FilterChip(
                                selected = selectedPaymentApp == appName,
                                onClick = { selectedPaymentApp = appName },
                                label = { Text(appName, fontSize = 11.sp) }
                            )
                        }
                    }

                    // Transaction ID / UTR Input
                    OutlinedTextField(
                        value = transactionIdInput,
                        onValueChange = { transactionIdInput = it },
                        label = { Text(if (isHindi) "UPI ट्रांजेक्शन आईडी / UTR (12 अंक) *" else "UTR / Txn ID (12 digits) *") },
                        placeholder = { Text("उदा. 423589123456", color = Color.Gray) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    if (bookingErrorMessage != null) {
                        Surface(
                            color = Color(0xFFFFEBEE),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = bookingErrorMessage!!,
                                color = Color.Red,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // Validation
                        val pName = payerNameInput.trim().ifEmpty {
                            passengerInputs.values.firstOrNull()?.name?.trim() ?: ""
                        }
                        val pPhone = payerPhoneInput.trim().ifEmpty {
                            passengerInputs.values.firstOrNull()?.phone?.trim() ?: ""
                        }
                        if (pName.isBlank()) {
                            bookingErrorMessage = if (isHindi) "कृपया मुख्य भक्त का नाम भरें" else "Please enter primary contact name"
                            return@Button
                        }
                        if (pPhone.length < 10) {
                            bookingErrorMessage = if (isHindi) "कृपया 10 अंकों का मान्य मोबाइल नंबर भरें" else "Please enter valid 10-digit mobile number"
                            return@Button
                        }
                        val missingNames = selectedSeats.any {
                            (passengerInputs[it.seatNumber]?.name?.trim() ?: "").isBlank()
                        }
                        if (missingNames) {
                            bookingErrorMessage = if (isHindi) "कृपया सभी सीटों के यात्रियों के नाम भरें" else "Please enter all passenger names"
                            return@Button
                        }
                        val utrClean = transactionIdInput.trim()
                        if (utrClean.length < 6) {
                            bookingErrorMessage = if (isHindi) "कृपया मान्य UPI ट्रांजेक्शन आईडी / UTR दर्ज करें" else "Please enter valid UTR/Transaction ID"
                            return@Button
                        }

                        isBookingInProgress = true
                        bookingErrorMessage = null

                        scope.launch {
                            try {
                                val isDuplicate = repository.isTransactionIdAlreadyUsed(utrClean)
                                if (isDuplicate) {
                                    bookingErrorMessage = if (isHindi)
                                        "⚠️ यह UTR / ट्रांजेक्शन नंबर पहले ही किसी अन्य बुकिंग में इस्तेमाल हो चुका है! कृपया अपनी नई वास्तविक पेमेंट रसीद का UTR दर्ज करें।"
                                    else
                                        "This UTR / Transaction ID has already been used. Please enter a valid unique UTR."
                                    isBookingInProgress = false
                                    return@launch
                                }

                                val bookedList = selectedSeats.map { seat ->
                                    val inp = passengerInputs[seat.seatNumber]
                                    seat.copy(
                                        isBooked = true,
                                        passengerName = inp?.name?.trim() ?: pName,
                                        passengerAge = inp?.age?.toIntOrNull() ?: 0,
                                        passengerGender = inp?.gender?.trim() ?: "पुरुष",
                                        phoneNumber = pPhone,
                                        paymentStatus = PaymentStatus.PENDING_VERIFICATION,
                                        paymentMode = "UPI_QR",
                                        transactionId = utrClean,
                                        fareAmount = farePerSeat,
                                        bookedAt = System.currentTimeMillis(),
                                        bookedBy = "DEVOTEE"
                                    )
                                }

                                val seatLabelsStr = bookedList.joinToString(", ") { it.seatLabel.ifEmpty { "#${it.seatNumber}" } }
                                val paymentRecord = PaymentRecord(
                                    devoteeName = pName,
                                    devoteePhone = pPhone,
                                    paymentApp = selectedPaymentApp,
                                    transactionId = utrClean,
                                    amount = totalFare.toDouble(),
                                    purpose = "BUS_TICKET",
                                    seatNumbers = seatLabelsStr,
                                    timestamp = System.currentTimeMillis(),
                                    paymentStatus = "PENDING_VERIFICATION",
                                    paymentMode = "UPI_QR"
                                )

                                val success = repository.bookMultipleBusSeats(bookedList, paymentRecord)
                                if (success) {
                                    // Generate and auto-save A4 PDF Ticket to phone Downloads
                                    val pdfFile = BusTicketPdfGenerator.generateA4BusTicket(
                                        context = context,
                                        seats = bookedList,
                                        paymentRecord = paymentRecord,
                                        yatraDate = "आगामी रविवार / पूर्णिमा",
                                        boardingPoint = "श्री बालाजी कृपा धाम, ग्राम डूँगरा जाट, बुलन्दशहर"
                                    )
                                    downloadedTicketFile = pdfFile
                                    showBookingSheet = false
                                    showTicketSuccessDialog = true
                                    refreshData()
                                } else {
                                    bookingErrorMessage = if (isHindi) "सीट बुकिंग में त्रुटि हुई! कृपया पुनः प्रयास करें।" else "Error booking seats. Please retry."
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                bookingErrorMessage = e.message ?: "Booking failed"
                            } finally {
                                isBookingInProgress = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                    enabled = !isBookingInProgress
                ) {
                    if (isBookingInProgress) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(if (isHindi) "पुष्टि करें व A4 टिकट डाउनलोड करें" else "Confirm & Download A4 Ticket", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showBookingSheet = false },
                    enabled = !isBookingInProgress
                ) {
                    Text(if (isHindi) "रद्द करें" else "Cancel")
                }
            }
        )
    }

    // --- TICKET DOWNLOADED SUCCESS DIALOG ---
    if (showTicketSuccessDialog && downloadedTicketFile != null) {
        AlertDialog(
            onDismissRequest = { showTicketSuccessDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("✅", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isHindi) "सीट बुक व A4 टिकट डाउनलोड हुआ!" else "Seats Booked & Ticket Saved!",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32),
                        fontSize = 16.sp
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isHindi)
                            "जय श्री बालाजी! आपकी सीटें आरक्षित हो गई हैं।\n\n📌 वर्तमान स्थिति: 'सत्यापन लंबित (Pending Bank Verification)'\nआश्रम व्यवस्थापक द्वारा बैंक में UTR की पुष्टि होते ही टिकट पूर्णतः सत्यापित हो जाएगा। A4 टिकट आपके Downloads फ़ोल्डर में सहेजा जा चुका है।"
                        else
                            "Seats reserved successfully!\n\nStatus: Pending Bank Verification.\nOnce Ashram verifies the UTR, your seat is fully confirmed. A4 ticket saved to Downloads.",
                        fontSize = 13.sp,
                        color = Color(0xFF111111)
                    )
                    Surface(
                        color = Color(0xFFF1F8E9),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "📄 ${downloadedTicketFile?.name}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF2E7D32),
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        downloadedTicketFile?.let {
                            BusTicketPdfGenerator.viewOrSharePdf(context, it, "श्री बालाजी यात्रा बस टिकट")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary)
                ) {
                    Text(if (isHindi) "📄 टिकट देखें / शेयर करें" else "📄 View / Share Ticket", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTicketSuccessDialog = false }) {
                    Text(if (isHindi) "बंद करें" else "Close")
                }
            }
        )
    }

    // --- VIEW ALREADY BOOKED SEAT DETAILS DIALOG ---
    if (showSeatDetailsDialog != null) {
        val seat = showSeatDetailsDialog!!
        AlertDialog(
            onDismissRequest = { showSeatDetailsDialog = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("💺", fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isHindi) "सीट विवरण: ${seat.seatLabel}" else "Seat Info: ${seat.seatLabel}",
                        fontWeight = FontWeight.Bold,
                        color = MaroonPrimary
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("यात्री का नाम: ${seat.passengerName.ifEmpty { "भक्त" }}", fontWeight = FontWeight.Bold)
                    if (seat.passengerAge > 0) Text("आयु: ${seat.passengerAge} वर्ष")
                    if (seat.passengerGender.isNotBlank()) Text("लिंग: ${seat.passengerGender}")
                    Text("मोबाइल: ${seat.phoneNumber.ifEmpty { "गोपनीय" }}")
                    Text("किराया: ₹${seat.fareAmount} (${seat.paymentStatus.name})")
                    if (seat.transactionId.isNotBlank()) Text("UTR / Txn ID: ${seat.transactionId}", color = Color(0xFF0D47A1), fontWeight = FontWeight.SemiBold)
                }
            },
            confirmButton = {
                TextButton(onClick = { showSeatDetailsDialog = null }) {
                    Text(if (isHindi) "समझ गया" else "OK")
                }
            }
        )
    }
}

private fun handleSeatClick(
    seat: BusSeat?,
    selectedSeats: MutableList<BusSeat>,
    passengerInputs: MutableMap<Int, PassengerInput>,
    deviceId: String,
    onShowDetails: (BusSeat) -> Unit,
    onShowError: (String) -> Unit
) {
    if (seat == null) return
    if (seat.isBooked) {
        onShowDetails(seat)
    } else if (seat.isHeld && seat.heldBy.isNotBlank() && seat.heldBy != deviceId) {
        val remSec = ((seat.holdExpiresAt - System.currentTimeMillis()) / 1000).coerceAtLeast(1)
        onShowError("सीट संख्या #${seat.seatLabel.ifEmpty { "${seat.seatNumber}" }} अन्य भक्त द्वारा 5 मिनट के होल्ड पर है ($remSec सेकंड शेष)। कृपया प्रतीक्षा करें या अन्य सीट चुनें।")
    } else {
        if (selectedSeats.contains(seat)) {
            selectedSeats.remove(seat)
            passengerInputs.remove(seat.seatNumber)
        } else {
            selectedSeats.add(seat)
            passengerInputs[seat.seatNumber] = PassengerInput(seat.seatNumber, seat.seatLabel)
        }
    }
}

@Composable
fun SixSeaterSeatItem(
    seat: BusSeat?,
    isSelected: Boolean,
    currentDeviceId: String = "",
    onClick: (BusSeat?) -> Unit
) {
    if (seat == null) {
        Box(modifier = Modifier.size(36.dp))
        return
    }

    val isHeldByOther = seat.isHeld && !isSelected && (seat.heldBy.isNotBlank() && seat.heldBy != currentDeviceId)

    val (bgColor, textColor, borderColor) = when {
        isSelected -> Triple(SaffronPrimary, Color.White, Color(0xFFE65100))
        seat.isBooked && seat.paymentStatus == PaymentStatus.PENDING_VERIFICATION ->
            Triple(Color(0xFFFFF9C4), Color(0xFFE65100), Color(0xFFFBC02D)) // Amber for Pending Admin Verification
        seat.isBooked -> Triple(Color(0xFFFFCDD2), Color(0xFFB71C1C), Color(0xFFE57373)) // Red for Confirmed Booked
        isHeldByOther -> Triple(Color(0xFFFFE0B2), Color(0xFFE65100), Color(0xFFFF9800)) // Amber/Orange for Held
        else -> Triple(Color(0xFFE8F5E9), Color(0xFF1B5E20), Color(0xFF81C784)) // Green for Available
    }

    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.2.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable { onClick(seat) },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (isHeldByOther) "⏳${seat.seatNumber}" else seat.seatLabel.ifEmpty { "${seat.seatNumber}" },
            fontSize = if (isHeldByOther) 9.sp else 10.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
private fun LegendIndicator(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, fontSize = 11.sp, color = TextPrimaryDark, fontWeight = FontWeight.Medium)
    }
}
