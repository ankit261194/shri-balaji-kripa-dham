package com.example.shribalajikripadham.ui.admin

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.shribalajikripadham.data.model.AshramSettings
import com.example.shribalajikripadham.data.model.BusSeat
import com.example.shribalajikripadham.data.model.PaymentRecord
import com.example.shribalajikripadham.data.model.PaymentStatus
import com.example.shribalajikripadham.data.repository.AshramRepository
import com.example.shribalajikripadham.theme.AmberGold
import com.example.shribalajikripadham.theme.MaroonAccent
import com.example.shribalajikripadham.theme.MaroonPrimary
import com.example.shribalajikripadham.theme.SaffronPrimary
import com.example.shribalajikripadham.util.BusTicketPdfGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusLedgerTab(
    isHindi: Boolean,
    repository: AshramRepository,
    settings: AshramSettings,
    isSuperAdmin: Boolean,
    scope: CoroutineScope,
    context: Context
) {
    var seats by remember { mutableStateOf<List<BusSeat>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) } // 0 = 60-Seat Map, 1 = Passenger Ledger

    // Dialog States
    var seatDetailTarget by remember { mutableStateOf<BusSeat?>(null) }
    var showCounterBookingDialog by remember { mutableStateOf(false) }
    var preselectedSeatForCounter by remember { mutableStateOf<BusSeat?>(null) }
    var cancelConfirmationSeat by remember { mutableStateOf<BusSeat?>(null) }

    fun refreshSeats() {
        scope.launch {
            isLoading = true
            try {
                seats = repository.getAllBusSeats()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshSeats()
    }

    val bookedSeats = seats.filter { it.isBooked }
    val availableSeats = seats.filter { !it.isBooked }
    val farePerSeat = if (settings.busSeatFareAmount > 0) settings.busSeatFareAmount else 1500
    val totalRevenue = bookedSeats.size * farePerSeat

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // TOP SUMMARY CARD
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(14.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🚌", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isHindi) "60-सीटर डीलक्स बस लेजर" else "60-Seater Deluxe Bus Ledger",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaroonPrimary
                            )
                        }
                        Text(
                            text = if (isHindi) "12 पंक्तियाँ × 5 सीटें (3+2 व्यवस्था) • ₹$farePerSeat/सीट" else "12 Rows × 5 Seats (3+2 Layout) • ₹$farePerSeat/Seat",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(
                            onClick = { refreshSeats() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Text("🔄", fontSize = 16.sp)
                        }
                        IconButton(
                            onClick = {
                                scope.launch {
                                    Toast.makeText(context, if (isHindi) "क्लाउड से सिंक हो रहा है..." else "Syncing with cloud...", Toast.LENGTH_SHORT).show()
                                    val (success, _) = repository.syncLiveBusSeatsFromGitHub()
                                    refreshSeats()
                                    if (success) {
                                        Toast.makeText(context, if (isHindi) "सीट डेटा सिंक सफल!" else "Synced!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, if (isHindi) "स्थानीय डेटा लोड है" else "Local data active", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Text("☁️", fontSize = 16.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Stats row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = Color(0xFFE8F5E9),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${availableSeats.size}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                            Text(
                                text = if (isHindi) "खाली सीटें" else "Available",
                                fontSize = 10.sp,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }

                    Surface(
                        color = Color(0xFFFFEBEE),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${bookedSeats.size}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFC62828)
                            )
                            Text(
                                text = if (isHindi) "बुक सीटें" else "Booked",
                                fontSize = 10.sp,
                                color = Color(0xFFC62828)
                            )
                        }
                    }

                    Surface(
                        color = Color(0xFFFFF8E1),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "₹$totalRevenue",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE65100)
                            )
                            Text(
                                text = if (isHindi) "कुल संग्रह" else "Total Revenue",
                                fontSize = 10.sp,
                                color = Color(0xFFE65100)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            preselectedSeatForCounter = null
                            showCounterBookingDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isHindi) "➕ ऑफलाइन काउंटर बुकिंग" else "➕ Counter Booking", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                val (ok, msg) = repository.publishBusSeatsToGitHub()
                                Toast.makeText(context, if (ok) (if (isHindi) "सीटें क्लाउड पर सुरक्षित हुईं!" else "Saved to cloud!") else msg, Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(if (isHindi) "☁️ बैकअप" else "☁️ Backup", fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // VIEW TABS (Seat Map vs Passenger Ledger)
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color(0xFFFFF3E0),
            contentColor = MaroonPrimary
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text(if (isHindi) "🚌 60-सीट नक्शा" else "🚌 60-Seat Map", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text(if (isHindi) "📋 यात्री सूची (${bookedSeats.size})" else "📋 Passengers (${bookedSeats.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaroonPrimary)
            }
        } else if (selectedTab == 0) {
            // 60-SEAT VISUAL MAP (12 Rows x 5 Columns: 3 Left + Aisle + 2 Right)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Bus Windshield / Front Banner
                Surface(
                    color = Color(0xFF37474F),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🚍", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isHindi) "बस का अगला भाग (चालक केबिन)" else "Front / Driver Cabin",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "🚪 " + (if (isHindi) "प्रवेश द्वार" else "Entry Door"),
                            color = AmberGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Legend
                Surface(
                    color = Color(0xFFFAFAFA),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(14.dp).background(Color(0xFF4CAF50), RoundedCornerShape(3.dp)))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isHindi) "उपलब्ध (खाली)" else "Available", fontSize = 11.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(14.dp).background(Color(0xFFC62828), RoundedCornerShape(3.dp)))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isHindi) "आरक्षित (बुक)" else "Booked", fontSize = 11.sp)
                        }
                    }
                }

                // 12 Rows Layout
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
                    border = BorderStroke(1.dp, Color(0xFFCFD8DC)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (row in 1..12) {
                            val rowSeats = seats.filter { it.row == row }.sortedBy { it.column }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Row Number Badge
                                Text(
                                    text = "R$row",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray,
                                    modifier = Modifier.width(28.dp)
                                )

                                // Left 3 Seats (Cols 1, 2, 3)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    for (c in 1..3) {
                                        val s = rowSeats.find { it.column == c }
                                        AdminSeatBox(
                                            seat = s,
                                            label = if (s != null) s.seatLabel else "$row${listOf("A","B","C")[c-1]}",
                                            onClick = {
                                                if (s != null) {
                                                    if (s.isBooked) {
                                                        seatDetailTarget = s
                                                    } else {
                                                        preselectedSeatForCounter = s
                                                        showCounterBookingDialog = true
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }

                                // Aisle (walking space)
                                Column(
                                    modifier = Modifier.width(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("🚶", fontSize = 10.sp)
                                }

                                // Right 2 Seats (Cols 4, 5)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    for (c in 4..5) {
                                        val s = rowSeats.find { it.column == c }
                                        AdminSeatBox(
                                            seat = s,
                                            label = if (s != null) s.seatLabel else "$row${listOf("D","E")[c-4]}",
                                            onClick = {
                                                if (s != null) {
                                                    if (s.isBooked) {
                                                        seatDetailTarget = s
                                                    } else {
                                                        preselectedSeatForCounter = s
                                                        showCounterBookingDialog = true
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        } else {
            // PASSENGER LEDGER (LIST VIEW WITH SEARCH)
            Column(modifier = Modifier.fillMaxSize()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text(if (isHindi) "यात्री का नाम / फोन / सीट नंबर खोजें" else "Search Name / Phone / Seat #") },
                    leadingIcon = { Text("🔍", fontSize = 14.sp) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Text("✕", fontSize = 14.sp, color = Color.Gray)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                val filteredBooked = bookedSeats.filter {
                    val q = searchQuery.trim().lowercase()
                    q.isEmpty() ||
                            it.passengerName.lowercase().contains(q) ||
                            it.phoneNumber.contains(q) ||
                            it.seatLabel.lowercase().contains(q) ||
                            it.seatNumber.toString() == q ||
                            it.transactionId.lowercase().contains(q)
                }

                if (filteredBooked.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isNotEmpty())
                                (if (isHindi) "कोई यात्री नहीं मिला।" else "No matching passenger found.")
                            else
                                (if (isHindi) "अभी कोई सीट बुक नहीं है।" else "No seats booked yet."),
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredBooked) { seat ->
                            PassengerCard(
                                isHindi = isHindi,
                                seat = seat,
                                onPrintTicket = {
                                    scope.launch(Dispatchers.IO) {
                                        val dummyPay = PaymentRecord(
                                            devoteeName = seat.passengerName,
                                            devoteePhone = seat.phoneNumber,
                                            paymentApp = if (seat.paymentMode.contains("CASH", ignoreCase = true)) "Cash" else "UPI QR",
                                            transactionId = seat.transactionId,
                                            amount = seat.fareAmount.toDouble(),
                                            seatNumbers = seat.seatLabel
                                        )
                                        val pdf = BusTicketPdfGenerator.generateA4BusTicket(context, listOf(seat), dummyPay)
                                        withContext(Dispatchers.Main) {
                                            if (pdf != null) {
                                                BusTicketPdfGenerator.viewOrSharePdf(context, pdf, "बस टिकट - सीट ${seat.seatLabel}")
                                            } else {
                                                Toast.makeText(context, if (isHindi) "टिकट बनाने में विफल" else "Failed to generate ticket", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                },
                                onCancelSeat = {
                                    cancelConfirmationSeat = seat
                                },
                                onViewDetail = {
                                    seatDetailTarget = seat
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // DETAIL DIALOG FOR BOOKED SEAT
    if (seatDetailTarget != null) {
        val s = seatDetailTarget!!
        Dialog(onDismissRequest = { seatDetailTarget = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = MaroonPrimary,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = s.seatLabel,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isHindi) "सीट विवरण" else "Seat Details",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaroonPrimary
                            )
                        }

                        IconButton(onClick = { seatDetailTarget = null }) {
                            Text("✕", fontSize = 14.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = Color(0xFFEEEEEE))
                    Spacer(modifier = Modifier.height(10.dp))

                    DetailItem(if (isHindi) "यात्री का नाम" else "Passenger Name", s.passengerName)
                    DetailItem(if (isHindi) "उम्र व लिंग" else "Age & Gender", "${if (s.passengerAge > 0) "${s.passengerAge} वर्ष" else "-"}, ${s.passengerGender.ifBlank { "-" }}")
                    DetailItem(if (isHindi) "मोबाइल नंबर" else "Phone Number", s.phoneNumber)
                    DetailItem(if (isHindi) "भुगतान माध्यम" else "Payment Mode", s.paymentMode)
                    DetailItem(if (isHindi) "UTR / ट्रांजेक्शन ID" else "Transaction UTR", s.transactionId.ifBlank { "नकद (Cash)" })
                    DetailItem(if (isHindi) "किराया राशि" else "Fare Amount", "₹${s.fareAmount}")
                    DetailItem(if (isHindi) "बोर्डिंग पॉइंट" else "Boarding Point", s.boardingPoint)
                    if (s.bookedAt > 0) {
                        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                        DetailItem(if (isHindi) "बुकिंग समय" else "Booked At", sdf.format(Date(s.bookedAt)))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Buttons: Print A4 Ticket & Cancel Seat
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                scope.launch(Dispatchers.IO) {
                                    val dummyPay = PaymentRecord(
                                        devoteeName = s.passengerName,
                                        devoteePhone = s.phoneNumber,
                                        paymentApp = if (s.paymentMode.contains("CASH", ignoreCase = true)) "Cash" else "UPI QR",
                                        transactionId = s.transactionId,
                                        amount = s.fareAmount.toDouble(),
                                        seatNumbers = s.seatLabel
                                    )
                                    val pdf = BusTicketPdfGenerator.generateA4BusTicket(context, listOf(s), dummyPay)
                                    withContext(Dispatchers.Main) {
                                        if (pdf != null) {
                                            BusTicketPdfGenerator.viewOrSharePdf(context, pdf, "बस टिकट - सीट ${s.seatLabel}")
                                        } else {
                                            Toast.makeText(context, if (isHindi) "टिकट बनाने में त्रुटि" else "Error generating ticket", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(if (isHindi) "📄 A4 टिकट" else "📄 A4 Ticket", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                cancelConfirmationSeat = s
                                seatDetailTarget = null
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(if (isHindi) "❌ सीट रद्द करें" else "Cancel Seat", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    // CANCEL CONFIRMATION DIALOG
    if (cancelConfirmationSeat != null) {
        val s = cancelConfirmationSeat!!
        AlertDialog(
            onDismissRequest = { cancelConfirmationSeat = null },
            title = { Text(if (isHindi) "सीट बुकिंग रद्द करें?" else "Cancel Seat Booking?") },
            text = {
                Text(
                    if (isHindi)
                        "क्या आप निश्चित रूप से सीट ${s.seatLabel} (यात्री: ${s.passengerName}) की बुकिंग रद्द करना चाहते हैं? यह सीट पुनः अन्य भक्तों के लिए खाली हो जाएगी।"
                    else
                        "Are you sure you want to cancel booking for Seat ${s.seatLabel} (${s.passengerName})? This seat will become available again."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val ok = repository.cancelBusSeatBooking(s.seatNumber)
                            if (ok) {
                                Toast.makeText(context, if (isHindi) "सीट ${s.seatLabel} सफलतापूर्वक रद्द हुई!" else "Seat cancelled!", Toast.LENGTH_SHORT).show()
                                refreshSeats()
                            } else {
                                Toast.makeText(context, if (isHindi) "रद्द करने में विफल" else "Failed", Toast.LENGTH_SHORT).show()
                            }
                            cancelConfirmationSeat = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text(if (isHindi) "हाँ, रद्द करें" else "Yes, Cancel", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { cancelConfirmationSeat = null }) {
                    Text(if (isHindi) "नहीं" else "No")
                }
            }
        )
    }

    // OFFLINE COUNTER BOOKING DIALOG
    if (showCounterBookingDialog) {
        var passengerName by remember { mutableStateOf("") }
        var passengerAge by remember { mutableStateOf("") }
        var passengerGender by remember { mutableStateOf("पुरुष") }
        var passengerPhone by remember { mutableStateOf("") }
        var paymentMode by remember { mutableStateOf("काउंटर नकद (Cash)") }
        var transactionId by remember { mutableStateOf("") }
        val selectedSeatsList = remember {
            mutableStateListOf<Int>().apply {
                if (preselectedSeatForCounter != null && !preselectedSeatForCounter!!.isBooked) {
                    add(preselectedSeatForCounter!!.seatNumber)
                }
            }
        }
        var isSubmitting by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { if (!isSubmitting) showCounterBookingDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isHindi) "ऑफलाइन काउंटर बस बुकिंग" else "Counter Bus Booking",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaroonPrimary
                        )
                        IconButton(onClick = { if (!isSubmitting) showCounterBookingDialog = false }) {
                            Text("✕", fontSize = 14.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (isHindi) "उपलब्ध सीटें चुनें:" else "Select Available Seat(s):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.DarkGray
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Chips of available seats
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        availableSeats.forEach { s ->
                            val isSel = selectedSeatsList.contains(s.seatNumber)
                            FilterChip(
                                selected = isSel,
                                onClick = {
                                    if (isSel) selectedSeatsList.remove(s.seatNumber)
                                    else selectedSeatsList.add(s.seatNumber)
                                },
                                label = { Text(s.seatLabel, fontSize = 12.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = passengerName,
                        onValueChange = { passengerName = it },
                        label = { Text(if (isHindi) "मुख्य यात्री का नाम *" else "Passenger Name *") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = passengerAge,
                            onValueChange = { passengerAge = it },
                            label = { Text(if (isHindi) "उम्र (वर्ष)" else "Age") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )

                        // Gender dropdown / options
                        Column(modifier = Modifier.weight(1.2f)) {
                            Text(
                                text = if (isHindi) "लिंग" else "Gender",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf("पुरुष", "महिला").forEach { g ->
                                    FilterChip(
                                        selected = passengerGender.startsWith(g),
                                        onClick = { passengerGender = g },
                                        label = { Text(g, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = passengerPhone,
                        onValueChange = { passengerPhone = it },
                        label = { Text(if (isHindi) "मोबाइल नंबर *" else "Phone Number *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (isHindi) "भुगतान माध्यम:" else "Payment Mode:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.DarkGray
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                        listOf("काउंटर नकद (Cash)", "UPI QR कोड").forEach { mode ->
                            FilterChip(
                                selected = paymentMode == mode,
                                onClick = { paymentMode = mode },
                                label = { Text(mode, fontSize = 11.sp) }
                            )
                        }
                    }

                    if (paymentMode == "UPI QR कोड") {
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = transactionId,
                            onValueChange = { transactionId = it },
                            label = { Text(if (isHindi) "UTR / ट्रांजेक्शन ID" else "UTR / Transaction ID") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val totalAmount = selectedSeatsList.size * farePerSeat
                    Surface(
                        color = Color(0xFFFFF3E0),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isHindi) "चयनित सीटें: ${selectedSeatsList.size}" else "Selected Seats: ${selectedSeatsList.size}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "कुल किराया: ₹$totalAmount",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaroonPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (selectedSeatsList.isEmpty()) {
                                Toast.makeText(context, if (isHindi) "कृपया कम से कम एक सीट चुनें" else "Please select at least 1 seat", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (passengerName.isBlank()) {
                                Toast.makeText(context, if (isHindi) "कृपया यात्री का नाम दर्ज करें" else "Please enter passenger name", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (passengerPhone.isBlank()) {
                                Toast.makeText(context, if (isHindi) "कृपया मोबाइल नंबर दर्ज करें" else "Please enter phone number", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            isSubmitting = true
                            scope.launch {
                                val seatLabels = seats.filter { selectedSeatsList.contains(it.seatNumber) }.joinToString(", ") { it.seatLabel }
                                val payRec = PaymentRecord(
                                    devoteeName = passengerName.trim(),
                                    devoteePhone = passengerPhone.trim(),
                                    paymentApp = if (paymentMode.contains("Cash")) "Cash" else "UPI QR",
                                    transactionId = transactionId.trim().ifBlank { "CASH-COUNTER-${System.currentTimeMillis() % 10000}" },
                                    amount = totalAmount.toDouble(),
                                    purpose = "BUS_TICKET",
                                    seatNumbers = seatLabels,
                                    paymentStatus = "VERIFIED",
                                    paymentMode = if (paymentMode.contains("Cash")) "OFFLINE_CASH" else "UPI_QR",
                                    verifiedBy = "COUNTER_DESK"
                                )

                                val seatsToBook = selectedSeatsList.map { seatNum ->
                                    val existing = seats.find { it.seatNumber == seatNum }
                                    BusSeat(
                                        seatNumber = seatNum,
                                        seatLabel = existing?.seatLabel ?: "$seatNum",
                                        row = existing?.row ?: 1,
                                        column = existing?.column ?: 1,
                                        isBooked = true,
                                        passengerName = passengerName.trim(),
                                        passengerAge = passengerAge.toIntOrNull() ?: 0,
                                        passengerGender = passengerGender,
                                        phoneNumber = passengerPhone.trim(),
                                        boardingPoint = "श्री बालाजी कृपा धाम, ग्राम डूँगरा जाट, बुलन्दशहर",
                                        paymentStatus = PaymentStatus.PAID,
                                        paymentMode = if (paymentMode.contains("Cash")) "OFFLINE_CASH" else "UPI_QR",
                                        transactionId = transactionId.trim(),
                                        fareAmount = farePerSeat,
                                        bookedAt = System.currentTimeMillis(),
                                        bookedBy = "ADMIN_COUNTER"
                                    )
                                }

                                val ok = repository.bookMultipleBusSeats(seatsToBook, payRec)

                                if (ok) {
                                    // Generate A4 ticket immediately
                                    val bookedSeatsList = repository.getAllBusSeats().filter { selectedSeatsList.contains(it.seatNumber) }
                                    val pdf = BusTicketPdfGenerator.generateA4BusTicket(context, bookedSeatsList, payRec)

                                    Toast.makeText(context, if (isHindi) "सीटें बुक हुईं व A4 टिकट तैयार हुआ!" else "Booked & A4 Ticket generated!", Toast.LENGTH_LONG).show()
                                    refreshSeats()
                                    showCounterBookingDialog = false

                                    if (pdf != null) {
                                        BusTicketPdfGenerator.viewOrSharePdf(context, pdf, "श्री बालाजी यात्रा - बस टिकट")
                                    }
                                } else {
                                    Toast.makeText(context, if (isHindi) "बुकिंग में त्रुटि हुई।" else "Booking failed", Toast.LENGTH_SHORT).show()
                                }
                                isSubmitting = false
                            }
                        },
                        enabled = !isSubmitting,
                        colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                        } else {
                            Text(if (isHindi) "सीट कन्फर्म करें व A4 टिकट निकालें" else "Confirm & Generate A4 Ticket", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminSeatBox(
    seat: BusSeat?,
    label: String,
    onClick: () -> Unit
) {
    val isBooked = seat?.isBooked == true
    val bgColor = if (isBooked) Color(0xFFC62828) else Color(0xFF4CAF50)
    val textColor = Color.White

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(6.dp),
        shadowElevation = 2.dp,
        modifier = Modifier
            .size(width = 38.dp, height = 44.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Text(
                text = if (isBooked) "बुक" else "खाली",
                fontSize = 8.sp,
                color = textColor.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
private fun PassengerCard(
    isHindi: Boolean,
    seat: BusSeat,
    onPrintTicket: () -> Unit,
    onCancelSeat: () -> Unit,
    onViewDetail: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Color(0xFFEEEEEE)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaroonPrimary,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = seat.seatLabel,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = seat.passengerName.ifBlank { if (isHindi) "यात्री" else "Passenger" },
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "📞 ${seat.phoneNumber}",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }

                Surface(
                    color = Color(0xFFFFF3E0),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "₹${seat.fareAmount}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE65100),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            if (seat.transactionId.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "UTR: ${seat.transactionId}",
                    fontSize = 11.sp,
                    color = Color.DarkGray
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = Color(0xFFF5F5F5))
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onViewDetail) {
                    Text(if (isHindi) "विवरण" else "Details", fontSize = 11.sp)
                }
                Spacer(modifier = Modifier.width(4.dp))
                OutlinedButton(
                    onClick = onPrintTicket,
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(if (isHindi) "📄 A4 टिकट" else "📄 Ticket", fontSize = 11.sp)
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = onCancelSeat,
                    modifier = Modifier.size(32.dp)
                ) {
                    Text("🗑️", fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun DetailItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = Color.Gray)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
    }
}
