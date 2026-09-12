package com.example.shribalajikripadham.ui.yatra

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shribalajikripadham.data.model.BusSeat
import com.example.shribalajikripadham.data.model.PaymentStatus
import com.example.shribalajikripadham.data.repository.AshramRepository
import com.example.shribalajikripadham.theme.*
import kotlinx.coroutines.launch

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

    var seats by remember { mutableStateOf<List<BusSeat>>(emptyList()) }
    var selectedSeat by remember { mutableStateOf<BusSeat?>(null) }
    var showBookingDialog by remember { mutableStateOf(false) }

    // Dialog inputs
    var passengerName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var isPaid by remember { mutableStateOf(false) }

    fun refreshSeats() {
        scope.launch {
            try {
                seats = repository.getAllBusSeats()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshSeats()
    }

    val bookedCount = seats.count { it.isBooked }
    val availableCount = seats.size - bookedCount

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isHindi) "श्री बालाजी यात्रा बस व्यवस्था" else "Shri Balaji Bus Pilgrimage",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
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
                .padding(16.dp)
        ) {
            // 1. TRANSPARENCY NOTICE
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "🚌", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isHindi)
                            "हर 6 महीने में बस द्वारा बालाजी यात्रा जाती है, जिसमें केवल बस व धर्मशाला का वास्तविक किराया लिया जाता है।"
                        else
                            "Bus pilgrimage organized every 6 months. Devotees pay only the actual bus & accommodation fare.",
                        fontSize = 12.sp,
                        color = TextPrimaryDark,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. SUMMARY & EXPENSE BUTTON ROW
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row {
                    Surface(
                        color = Color(0xFFE8F5E9),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (isHindi) "खाली: $availableCount" else "Available: $availableCount",
                            color = StatusAvailable,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
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
                }

                Button(
                    onClick = onNavigateToExpenses,
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isHindi) "खर्च हिसाब  ➔" else "Trip Expenses ➔",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. COLOR LEGEND
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                LegendItem(color = StatusAvailable, label = if (isHindi) "खाली" else "Available")
                LegendItem(color = StatusPaid, label = if (isHindi) "बुक (Paid)" else "Paid")
                LegendItem(color = StatusUnpaid, label = if (isHindi) "बाकी (Unpaid)" else "Unpaid")
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. INTERACTIVE BUS LAYOUT (2x2 GRID WITH AISLE)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(3.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Front of bus (Driver + Gate)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Color(0xFFCFD8DC),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (isHindi) "🚪 प्रवेश द्वार" else "🚪 Entrance Gate",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }

                        Surface(
                            color = Color(0xFFECEFF1),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (isHindi) "👨‍✈️ ड्राइवर केबिन" else "👨‍✈️ Driver Cabin",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Divider(color = Color.LightGray.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // 10 Rows of Seats
                    val groupedRows = seats.groupBy { it.row }
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(groupedRows.keys.toList().sorted()) { rowIdx ->
                            val rowSeats = groupedRows[rowIdx] ?: emptyList()
                            val seatA = rowSeats.find { it.column == 1 }
                            val seatB = rowSeats.find { it.column == 2 }
                            val seatC = rowSeats.find { it.column == 3 }
                            val seatD = rowSeats.find { it.column == 4 }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Left Side (A & B)
                                SeatItem(seat = seatA, onClick = {
                                    selectedSeat = seatA
                                    passengerName = seatA?.passengerName ?: ""
                                    phoneNumber = seatA?.phoneNumber ?: ""
                                    isPaid = seatA?.paymentStatus == PaymentStatus.PAID
                                    showBookingDialog = true
                                })
                                SeatItem(seat = seatB, onClick = {
                                    selectedSeat = seatB
                                    passengerName = seatB?.passengerName ?: ""
                                    phoneNumber = seatB?.phoneNumber ?: ""
                                    isPaid = seatB?.paymentStatus == PaymentStatus.PAID
                                    showBookingDialog = true
                                })

                                // Aisle
                                Box(
                                    modifier = Modifier.width(36.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$rowIdx",
                                        fontSize = 11.sp,
                                        color = Color.Gray,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // Right Side (C & D)
                                SeatItem(seat = seatC, onClick = {
                                    selectedSeat = seatC
                                    passengerName = seatC?.passengerName ?: ""
                                    phoneNumber = seatC?.phoneNumber ?: ""
                                    isPaid = seatC?.paymentStatus == PaymentStatus.PAID
                                    showBookingDialog = true
                                })
                                SeatItem(seat = seatD, onClick = {
                                    selectedSeat = seatD
                                    passengerName = seatD?.passengerName ?: ""
                                    phoneNumber = seatD?.phoneNumber ?: ""
                                    isPaid = seatD?.paymentStatus == PaymentStatus.PAID
                                    showBookingDialog = true
                                })
                            }
                        }
                    }
                }
            }
        }

        // Booking / Seat Details Dialog
        if (showBookingDialog && selectedSeat != null) {
            val seat = selectedSeat!!
            AlertDialog(
                onDismissRequest = { showBookingDialog = false },
                title = {
                    Text(
                        text = if (isHindi) "बस सीट संख्या: ${seat.seatLabel}" else "Bus Seat: ${seat.seatLabel}",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column {
                        Text(
                            text = if (isHindi) "किराया: ₹${seat.fareAmount} (बस + धर्मशाला)" else "Fare: ₹${seat.fareAmount} (Bus + Dharamshala)",
                            fontSize = 13.sp,
                            color = SaffronDark,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = passengerName,
                            onValueChange = { passengerName = it },
                            label = { Text(text = if (isHindi) "यात्री का नाम" else "Passenger Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = { phoneNumber = it },
                            label = { Text(text = if (isHindi) "मोबाइल नंबर" else "Mobile Number") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = isPaid,
                                onCheckedChange = { isPaid = it }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isHindi) "किराया भुगतान प्राप्त (PAID)" else "Fare Payment Received (PAID)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val booked = passengerName.isNotBlank()
                            scope.launch {
                                repository.updateSeatBooking(
                                    seatNumber = seat.seatNumber,
                                    isBooked = booked,
                                    passengerName = passengerName.trim(),
                                    phoneNumber = phoneNumber.trim(),
                                    paymentStatus = if (isPaid) PaymentStatus.PAID else PaymentStatus.UNPAID,
                                    paymentMode = if (isPaid) "UPI/CASH" else "PENDING"
                                )
                                showBookingDialog = false
                                refreshSeats()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary)
                    ) {
                        Text(text = if (isHindi) "सेव करें" else "Save Details")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showBookingDialog = false }) {
                        Text(text = if (isHindi) "रद्द करें" else "Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun SeatItem(seat: BusSeat?, onClick: () -> Unit) {
    if (seat == null) {
        Box(modifier = Modifier.size(50.dp))
        return
    }

    val backgroundColor = when {
        !seat.isBooked -> StatusAvailable
        seat.paymentStatus == PaymentStatus.PAID -> Color(0xFFD32F2F) // Red (Booked Paid)
        else -> Color(0xFFEF6C00) // Orange (Booked Unpaid)
    }

    Box(
        modifier = Modifier
            .size(54.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = seat.seatLabel,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (!seat.isBooked)
                    "खाली"
                else if (seat.paymentStatus == PaymentStatus.PAID)
                    "Paid"
                else
                    "Due",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, fontSize = 11.sp, color = TextPrimaryDark)
    }
}
