package com.example.shribalajikripadham.ui.dharamshala

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.shribalajikripadham.theme.AmberGold
import com.example.shribalajikripadham.theme.MaroonAccent
import com.example.shribalajikripadham.theme.MaroonPrimary
import com.example.shribalajikripadham.theme.SaffronPrimary
import com.example.shribalajikripadham.util.BluetoothThermalPrinterHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

data class DharamshalaRoom(
    val id: Int,
    val roomNumber: String,
    val roomType: String, // AC, NON_AC, HALL_BED
    val titleHindi: String,
    val floor: String,
    val capacity: Int,
    val dailySevaRate: Double,
    val status: String // AVAILABLE, OCCUPIED, MAINTENANCE
)

data class DharamshalaBooking(
    val id: Long,
    val bookingRef: String,
    val devoteeName: String,
    val phoneNumber: String,
    val roomId: Int,
    val roomNumber: String,
    val devoteeCount: Int,
    val checkinDate: String,
    val checkoutDate: String,
    val totalDays: Int,
    val dailyRate: Double,
    val totalAmount: Double,
    val isPaid: Boolean,
    val paymentMode: String,
    val utrRef: String,
    val status: String,
    val allocatedBy: String,
    val createdAt: Long
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DharamshalaBookingScreen(
    isHindi: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var activeTab by remember { mutableIntStateOf(0) } // 0: Rooms, 1: Book Form, 2: Active Bookings
    var rooms by remember { mutableStateOf<List<DharamshalaRoom>>(emptyList()) }
    var bookings by remember { mutableStateOf<List<DharamshalaBooking>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Form fields
    var devoteeName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var selectedRoom by remember { mutableStateOf<DharamshalaRoom?>(null) }
    var devoteeCount by remember { mutableIntStateOf(2) }
    var totalDays by remember { mutableIntStateOf(1) }
    var paymentMode by remember { mutableStateOf("CASH") }
    var isPaid by remember { mutableStateOf(true) }
    var isSubmitting by remember { mutableStateOf(false) }

    // Thermal Printer Dialog
    var showPrinterDialog by remember { mutableStateOf(false) }
    var pairedPrinters by remember { mutableStateOf<List<BluetoothDevice>>(emptyList()) }
    var bookingToPrint by remember { mutableStateOf<DharamshalaBooking?>(null) }
    var isPrinting by remember { mutableStateOf(false) }

    // Network Functions
    val fetchRoomsAndBookings: () -> Unit = {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val (fetchedRooms, fetchedBookings) = withContext(Dispatchers.IO) {
                    val rList = mutableListOf<DharamshalaRoom>()
                    val bList = mutableListOf<DharamshalaBooking>()

                    // 1. Fetch rooms
                    val roomsUrl = URL("https://shribalajikripadham.online/api/dharamshala.php?action=list_rooms")
                    val conn = (roomsUrl.openConnection() as HttpURLConnection).apply {
                        connectTimeout = 8000
                        readTimeout = 8000
                        requestMethod = "GET"
                        setRequestProperty("User-Agent", "SBKD-Android/2.53")
                    }
                    if (conn.responseCode in 200..299) {
                        val resp = conn.inputStream.bufferedReader().use { it.readText() }
                        val jsonObj = JSONObject(resp)
                        if (jsonObj.optBoolean("success", false)) {
                            val arr = jsonObj.optJSONArray("rooms") ?: JSONArray()
                            for (i in 0 until arr.length()) {
                                val item = arr.getJSONObject(i)
                                rList.add(
                                    DharamshalaRoom(
                                        id = item.optInt("id"),
                                        roomNumber = item.optString("room_number"),
                                        roomType = item.optString("room_type", "NON_AC"),
                                        titleHindi = item.optString("title_hindi"),
                                        floor = item.optString("floor", "Ground"),
                                        capacity = item.optInt("capacity", 4),
                                        dailySevaRate = item.optDouble("daily_seva_rate", 250.0),
                                        status = item.optString("status", "AVAILABLE")
                                    )
                                )
                            }
                        }
                    }
                    conn.disconnect()

                    // 2. Fetch bookings
                    val bookUrl = URL("https://shribalajikripadham.online/api/dharamshala.php?action=list_bookings")
                    val bConn = (bookUrl.openConnection() as HttpURLConnection).apply {
                        connectTimeout = 8000
                        readTimeout = 8000
                        requestMethod = "GET"
                        setRequestProperty("User-Agent", "SBKD-Android/2.53")
                    }
                    if (bConn.responseCode in 200..299) {
                        val resp = bConn.inputStream.bufferedReader().use { it.readText() }
                        val jsonObj = JSONObject(resp)
                        if (jsonObj.optBoolean("success", false)) {
                            val arr = jsonObj.optJSONArray("bookings") ?: JSONArray()
                            for (i in 0 until arr.length()) {
                                val item = arr.getJSONObject(i)
                                bList.add(
                                    DharamshalaBooking(
                                        id = item.optLong("id"),
                                        bookingRef = item.optString("booking_ref"),
                                        devoteeName = item.optString("devotee_name"),
                                        phoneNumber = item.optString("phone_number"),
                                        roomId = item.optInt("room_id"),
                                        roomNumber = item.optString("room_number"),
                                        devoteeCount = item.optInt("devotee_count", 1),
                                        checkinDate = item.optString("checkin_date"),
                                        checkoutDate = item.optString("checkout_date"),
                                        totalDays = item.optInt("total_days", 1),
                                        dailyRate = item.optDouble("daily_rate", 0.0),
                                        totalAmount = item.optDouble("total_amount", 0.0),
                                        isPaid = item.optInt("is_paid", 0) == 1,
                                        paymentMode = item.optString("payment_mode", "CASH"),
                                        utrRef = item.optString("utr_ref", ""),
                                        status = item.optString("status", "CONFIRMED"),
                                        allocatedBy = item.optString("allocated_by", "SEVADAR"),
                                        createdAt = item.optLong("created_at", 0L)
                                    )
                                )
                            }
                        }
                    }
                    bConn.disconnect()

                    Pair(rList, bList)
                }

                rooms = fetchedRooms
                bookings = fetchedBookings
                if (selectedRoom == null && fetchedRooms.isNotEmpty()) {
                    selectedRoom = fetchedRooms.firstOrNull { it.status == "AVAILABLE" } ?: fetchedRooms.first()
                }
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "डेटा लोड करने में असमर्थ"
            } finally {
                isLoading = false
            }
        }
    }

    val submitBooking: () -> Unit = {
        val room = selectedRoom
        if (room == null) {
            Toast.makeText(context, if (isHindi) "कृपया कमरा चुनें!" else "Please select a room!", Toast.LENGTH_SHORT).show()
        } else if (devoteeName.isBlank()) {
            Toast.makeText(context, if (isHindi) "कृपया भक्त का नाम लिखें!" else "Please enter devotee name!", Toast.LENGTH_SHORT).show()
        } else if (phoneNumber.length < 10) {
            Toast.makeText(context, if (isHindi) "कृपया 10 अंकों का मान्य मोबाइल नंबर लिखें!" else "Please enter valid 10-digit phone!", Toast.LENGTH_SHORT).show()
        } else {
            scope.launch {
                isSubmitting = true
                try {
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val cal = Calendar.getInstance()
                    val checkin = sdf.format(cal.time)
                    cal.add(Calendar.DAY_OF_YEAR, totalDays)
                    val checkout = sdf.format(cal.time)

                    val payload = JSONObject().apply {
                        put("action", "book_room")
                        put("devotee_name", devoteeName.trim())
                        put("phone_number", phoneNumber.trim())
                        put("room_id", room.id)
                        put("devotee_count", devoteeCount)
                        put("checkin_date", checkin)
                        put("checkout_date", checkout)
                        put("total_days", totalDays)
                        put("is_paid", if (isPaid) 1 else 0)
                        put("payment_mode", paymentMode)
                        put("allocated_by", "आश्रम सेवादार काउंटर")
                    }

                    val result = withContext(Dispatchers.IO) {
                        val url = URL("https://shribalajikripadham.online/api/dharamshala.php?action=book_room")
                        val conn = (url.openConnection() as HttpURLConnection).apply {
                            connectTimeout = 8000
                            readTimeout = 8000
                            requestMethod = "POST"
                            doOutput = true
                            setRequestProperty("Content-Type", "application/json; charset=utf-8")
                            setRequestProperty("User-Agent", "SBKD-Android/2.53")
                        }
                        OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }
                        if (conn.responseCode in 200..299) {
                            val resp = conn.inputStream.bufferedReader().use { it.readText() }
                            JSONObject(resp)
                        } else {
                            val err = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                            JSONObject().apply { put("success", false); put("error", err) }
                        }
                    }

                    if (result.optBoolean("success", false)) {
                        val ref = result.optString("booking_ref", "CONFIRMED")
                        Toast.makeText(context, "✅ कमरा #${room.roomNumber} आरक्षित! संदर्भ: $ref", Toast.LENGTH_LONG).show()
                        devoteeName = ""
                        phoneNumber = ""
                        fetchRoomsAndBookings()
                        activeTab = 2 // Switch to active bookings
                    } else {
                        Toast.makeText(context, "त्रुटि: ${result.optString("error", "आरक्षण विफल")}", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "त्रुटि: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    isSubmitting = false
                }
            }
        }
    }

    val checkOutBooking: (DharamshalaBooking) -> Unit = { booking ->
        scope.launch {
            try {
                val payload = JSONObject().apply {
                    put("action", "check_out")
                    put("booking_id", booking.id)
                }
                val success = withContext(Dispatchers.IO) {
                    val url = URL("https://shribalajikripadham.online/api/dharamshala.php?action=check_out")
                    val conn = (url.openConnection() as HttpURLConnection).apply {
                        connectTimeout = 8000
                        readTimeout = 8000
                        requestMethod = "POST"
                        doOutput = true
                        setRequestProperty("Content-Type", "application/json; charset=utf-8")
                        setRequestProperty("User-Agent", "SBKD-Android/2.53")
                    }
                    OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }
                    conn.responseCode in 200..299
                }
                if (success) {
                    Toast.makeText(context, "✅ कमरा #${booking.roomNumber} का चेक-आउट सफल!", Toast.LENGTH_SHORT).show()
                    fetchRoomsAndBookings()
                } else {
                    Toast.makeText(context, "चेक-आउट विफल", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "त्रुटि: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        fetchRoomsAndBookings()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isHindi) "🏨 धर्मशाला व कमरा आरक्षण" else "🏨 Dharamshala Booking",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = AmberGold
                        )
                        Text(
                            text = if (isHindi) "श्री बालाजी कृपा धाम • ग्राम डूँगरा जाट" else "Shri Balaji Kripa Dham",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("⬅️", fontSize = 18.sp)
                    }
                },
                actions = {
                    IconButton(onClick = { fetchRoomsAndBookings() }) {
                        Text("🔄", fontSize = 18.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaroonPrimary)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF9F9F9))
        ) {
            // Segmented Tab Selector
            Surface(
                color = Color.White,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                TabRow(
                    selectedTabIndex = activeTab,
                    containerColor = Color.White,
                    contentColor = MaroonAccent
                ) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = {
                            Text(
                                text = if (isHindi) "🛏️ कमरे (${rooms.size})" else "🛏️ Rooms (${rooms.size})",
                                fontWeight = if (activeTab == 0) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.5.sp
                            )
                        }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = {
                            Text(
                                text = if (isHindi) "✍️ नया आरक्षण" else "✍️ New Booking",
                                fontWeight = if (activeTab == 1) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.5.sp
                            )
                        }
                    )
                    Tab(
                        selected = activeTab == 2,
                        onClick = { activeTab = 2 },
                        text = {
                            val activeCount = bookings.count { it.status != "CHECKED_OUT" && it.status != "CANCELLED" }
                            Text(
                                text = if (isHindi) "📋 सक्रिय ($activeCount)" else "📋 Active ($activeCount)",
                                fontWeight = if (activeTab == 2) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.5.sp
                            )
                        }
                    )
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaroonAccent)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (isHindi) "आश्रम धर्मशाला डेटा लोड हो रहा है..." else "Loading Dharamshala data...",
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                    }
                }
            } else if (errorMessage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("⚠️", fontSize = 36.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = errorMessage ?: "", color = Color.Red, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { fetchRoomsAndBookings() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaroonAccent)
                        ) {
                            Text(if (isHindi) "पुनः प्रयास करें" else "Retry")
                        }
                    }
                }
            } else {
                when (activeTab) {
                    0 -> RoomsListView(
                        isHindi = isHindi,
                        rooms = rooms,
                        onSelectForBooking = { room ->
                            selectedRoom = room
                            activeTab = 1
                        }
                    )
                    1 -> BookingFormView(
                        isHindi = isHindi,
                        rooms = rooms,
                        selectedRoom = selectedRoom,
                        onRoomSelected = { selectedRoom = it },
                        devoteeName = devoteeName,
                        onDevoteeNameChange = { devoteeName = it },
                        phoneNumber = phoneNumber,
                        onPhoneNumberChange = { phoneNumber = it },
                        devoteeCount = devoteeCount,
                        onDevoteeCountChange = { devoteeCount = it },
                        totalDays = totalDays,
                        onTotalDaysChange = { totalDays = it },
                        paymentMode = paymentMode,
                        onPaymentModeChange = { paymentMode = it },
                        isPaid = isPaid,
                        onIsPaidChange = { isPaid = it },
                        isSubmitting = isSubmitting,
                        onSubmit = submitBooking
                    )
                    2 -> BookingsListView(
                        isHindi = isHindi,
                        bookings = bookings,
                        onCheckOut = checkOutBooking,
                        onPrintSlip = { b ->
                            bookingToPrint = b
                            pairedPrinters = BluetoothThermalPrinterHelper.getPairedDevices(context)
                            showPrinterDialog = true
                        }
                    )
                }
            }
        }
    }

    // Thermal Printer Modal Dialog
    if (showPrinterDialog && bookingToPrint != null) {
        val b = bookingToPrint!!
        Dialog(onDismissRequest = { showPrinterDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.5.dp, AmberGold),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🖨️", fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (isHindi) "थर्मल रसीद प्रिंटर चुनें" else "Select Thermal Printer",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaroonPrimary
                            )
                            Text(
                                text = "कमरा #${b.roomNumber} (${b.devoteeName})",
                                fontSize = 11.5.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (pairedPrinters.isEmpty()) {
                        Surface(
                            color = Color(0xFFFFF3E0),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (isHindi)
                                    "⚠️ कोई पेयर किया हुआ ब्लूटूथ प्रिंटर नहीं मिला। कृपया फोन की ब्लूटूथ सेटिंग्स में जाकर 58mm/80mm थर्मल प्रिंटर पेयर करें।"
                                else
                                    "No paired Bluetooth printers found. Pair in phone settings.",
                                fontSize = 11.5.sp,
                                color = Color(0xFFE65100),
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    } else {
                        Text(
                            text = if (isHindi) "पेयर किए हुए प्रिंटर:" else "Paired Printers:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.DarkGray
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        pairedPrinters.forEach { device ->
                            @SuppressLint("MissingPermission")
                            val devName = try { device.name ?: "POS Thermal" } catch (e: Exception) { "POS Thermal" }
                            @SuppressLint("MissingPermission")
                            val devAddress = try { device.address } catch (e: Exception) { "" }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF5F5F5),
                                border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .clickable(enabled = !isPrinting) {
                                        scope.launch {
                                            isPrinting = true
                                            try {
                                                val slipBitmap = BluetoothThermalPrinterHelper.generateDharamshalaSlipBitmap(
                                                    bookingRef = b.bookingRef,
                                                    devoteeName = b.devoteeName,
                                                    phoneNumber = b.phoneNumber,
                                                    roomNumber = b.roomNumber,
                                                    roomType = "AC",
                                                    checkinDate = b.checkinDate,
                                                    checkoutDate = b.checkoutDate,
                                                    totalDays = b.totalDays,
                                                    totalAmount = b.totalAmount,
                                                    isPaid = b.isPaid,
                                                    paymentMode = b.paymentMode,
                                                    allocatedBy = b.allocatedBy,
                                                    is80mm = false
                                                )
                                                val (ok, msg) = BluetoothThermalPrinterHelper.printBitmap(device, slipBitmap)
                                                Toast.makeText(context, if (ok) "✅ $msg" else "❌ $msg", Toast.LENGTH_SHORT).show()
                                                if (ok) showPrinterDialog = false
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "त्रुटि: ${e.message}", Toast.LENGTH_SHORT).show()
                                            } finally {
                                                isPrinting = false
                                            }
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(text = devName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(text = devAddress, fontSize = 10.sp, color = Color.Gray)
                                    }
                                    Text(
                                        text = if (isPrinting) "प्रिंट हो रहा..." else "प्रिंट ➔",
                                        color = MaroonAccent,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(
                        onClick = { showPrinterDialog = false },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(if (isHindi) "बंद करें" else "Cancel", color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
fun RoomsListView(
    isHindi: Boolean,
    rooms: List<DharamshalaRoom>,
    onSelectForBooking: (DharamshalaRoom) -> Unit
) {
    var selectedFilter by remember { mutableStateOf("ALL") }

    val filteredRooms = remember(rooms, selectedFilter) {
        when (selectedFilter) {
            "AC" -> rooms.filter { it.roomType == "AC" }
            "NON_AC" -> rooms.filter { it.roomType == "NON_AC" }
            "HALL_BED" -> rooms.filter { it.roomType == "HALL_BED" }
            else -> rooms
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            // Filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val filters = listOf(
                    Triple("ALL", if (isHindi) "सभी कक्ष" else "All", rooms.size),
                    Triple("AC", if (isHindi) "❄️ AC कक्ष" else "AC", rooms.count { it.roomType == "AC" }),
                    Triple("NON_AC", if (isHindi) "🌬️ गैर-AC" else "Non-AC", rooms.count { it.roomType == "NON_AC" }),
                    Triple("HALL_BED", if (isHindi) "🛏️ हॉल बेड" else "Hall Bed", rooms.count { it.roomType == "HALL_BED" })
                )
                filters.forEach { (key, label, count) ->
                    val isSelected = selectedFilter == key
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaroonAccent else Color.White,
                        border = BorderStroke(1.dp, if (isSelected) AmberGold else Color(0xFFE0E0E0)),
                        modifier = Modifier.clickable { selectedFilter = key }
                    ) {
                        Text(
                            text = "$label ($count)",
                            fontSize = 11.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) AmberGold else Color.DarkGray,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }

        items(filteredRooms) { room ->
            val isAvailable = room.status == "AVAILABLE"
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, if (isAvailable) Color(0xFFC8E6C9) else Color(0xFFFFCDD2)),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = when (room.roomType) {
                                    "AC" -> Color(0xFFE3F2FD)
                                    "NON_AC" -> Color(0xFFFFF3E0)
                                    else -> Color(0xFFF3E5F5)
                                }
                            ) {
                                Text(
                                    text = when (room.roomType) {
                                        "AC" -> "❄️ AC"
                                        "NON_AC" -> "🌬️ Non-AC"
                                        else -> "🛏️ Hall"
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = when (room.roomType) {
                                        "AC" -> Color(0xFF1565C0)
                                        "NON_AC" -> Color(0xFFE65100)
                                        else -> Color(0xFF7B1FA2)
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "कमरा #${room.roomNumber}",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = MaroonPrimary
                            )
                        }

                        // Availability Badge
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isAvailable) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                            border = BorderStroke(0.5.dp, if (isAvailable) Color(0xFF4CAF50) else Color(0xFFF44336))
                        ) {
                            Text(
                                text = if (isAvailable)
                                    (if (isHindi) "✅ उपलब्ध" else "Available")
                                else
                                    (if (isHindi) "🔒 आरक्षित" else "Occupied"),
                                color = if (isAvailable) Color(0xFF2E7D32) else Color(0xFFC62828),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = room.titleHindi,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.DarkGray
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "मंजिल: ${room.floor} • क्षमता: ${room.capacity} व्यक्ति",
                            fontSize = 11.5.sp,
                            color = Color.Gray
                        )

                        Text(
                            text = "₹${room.dailySevaRate.toInt()} / दिन",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = SaffronPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { onSelectForBooking(room) },
                        enabled = isAvailable,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isAvailable) MaroonAccent else Color.LightGray,
                            contentColor = AmberGold
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isAvailable)
                                (if (isHindi) "🏨 यह कमरा बुक करें" else "Book This Room")
                            else
                                (if (isHindi) "यह कमरा अभी व्यस्त है" else "Room Currently Occupied"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BookingFormView(
    isHindi: Boolean,
    rooms: List<DharamshalaRoom>,
    selectedRoom: DharamshalaRoom?,
    onRoomSelected: (DharamshalaRoom) -> Unit,
    devoteeName: String,
    onDevoteeNameChange: (String) -> Unit,
    phoneNumber: String,
    onPhoneNumberChange: (String) -> Unit,
    devoteeCount: Int,
    onDevoteeCountChange: (Int) -> Unit,
    totalDays: Int,
    onTotalDaysChange: (Int) -> Unit,
    paymentMode: String,
    onPaymentModeChange: (String) -> Unit,
    isPaid: Boolean,
    onIsPaidChange: (Boolean) -> Unit,
    isSubmitting: Boolean,
    onSubmit: () -> Unit
) {
    val dailyRate = selectedRoom?.dailySevaRate ?: 250.0
    val totalAmount = dailyRate * totalDays

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE0E0E0))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = if (isHindi) "1. कमरा व आवास प्रकार चुनें:" else "1. Select Room:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaroonPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Horizontal room cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rooms.take(4).forEach { room ->
                            val isSel = selectedRoom?.id == room.id
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSel) MaroonAccent else Color(0xFFF5F5F5),
                                border = BorderStroke(1.dp, if (isSel) AmberGold else Color(0xFFE0E0E0)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onRoomSelected(room) }
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "#${room.roomNumber}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (isSel) AmberGold else Color.Black
                                    )
                                    Text(
                                        text = "₹${room.dailySevaRate.toInt()}",
                                        fontSize = 10.sp,
                                        color = if (isSel) Color.White else Color.Gray
                                    )
                                }
                            }
                        }
                    }

                    if (selectedRoom != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "चयनित: ${selectedRoom.titleHindi} (क्षमता: ${selectedRoom.capacity} व्यक्ति)",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE0E0E0))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = if (isHindi) "2. भक्त का विवरण:" else "2. Devotee Details:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaroonPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = devoteeName,
                        onValueChange = onDevoteeNameChange,
                        label = { Text(if (isHindi) "भक्त / मुख्य यात्री का नाम *" else "Devotee Name *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = onPhoneNumberChange,
                        label = { Text(if (isHindi) "10-अंकीय मोबाइल नंबर *" else "Mobile Number *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Devotee count
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isHindi) "यात्रियों की संख्या:" else "Devotees:",
                                fontSize = 11.sp,
                                color = Color.DarkGray
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(
                                    onClick = { if (devoteeCount > 1) onDevoteeCountChange(devoteeCount - 1) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Text("➖", fontSize = 14.sp)
                                }
                                Text(text = "$devoteeCount व्यक्ति", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                IconButton(
                                    onClick = { if (devoteeCount < 10) onDevoteeCountChange(devoteeCount + 1) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Text("➕", fontSize = 14.sp)
                                }
                            }
                        }

                        // Total Days
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isHindi) "ठहरने के दिन:" else "Days of Stay:",
                                fontSize = 11.sp,
                                color = Color.DarkGray
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(
                                    onClick = { if (totalDays > 1) onTotalDaysChange(totalDays - 1) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Text("➖", fontSize = 14.sp)
                                }
                                Text(text = "$totalDays दिन", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                IconButton(
                                    onClick = { if (totalDays < 15) onTotalDaysChange(totalDays + 1) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Text("➕", fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            // Payment breakdown card
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                border = BorderStroke(1.dp, AmberGold)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isHindi) "कुल सेवा शुल्क (गणित):" else "Total Seva Amount:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaroonPrimary
                        )
                        Text(
                            text = "₹${totalAmount.toInt()}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaroonAccent
                        )
                    }

                    Text(
                        text = "दर ₹${dailyRate.toInt()} x $totalDays दिन",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onPaymentModeChange("CASH") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (paymentMode == "CASH") MaroonAccent else Color.White,
                                contentColor = if (paymentMode == "CASH") AmberGold else MaroonPrimary
                            ),
                            border = BorderStroke(1.dp, MaroonAccent),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("💵 नकद (Cash)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { onPaymentModeChange("UPI_QR") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (paymentMode == "UPI_QR") MaroonAccent else Color.White,
                                contentColor = if (paymentMode == "UPI_QR") AmberGold else MaroonPrimary
                            ),
                            border = BorderStroke(1.dp, MaroonAccent),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("📱 UPI QR", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onIsPaidChange(!isPaid) }
                    ) {
                        Checkbox(
                            checked = isPaid,
                            onCheckedChange = onIsPaidChange,
                            colors = CheckboxDefaults.colors(checkedColor = MaroonAccent)
                        )
                        Text(
                            text = if (isHindi) "भुगतान प्राप्त हुआ (आश्रम खाते में दर्ज करें)" else "Payment Received",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        item {
            Button(
                onClick = onSubmit,
                enabled = !isSubmitting,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = AmberGold, modifier = Modifier.size(24.dp))
                } else {
                    Text(
                        text = if (isHindi) "🏨 आरक्षण सुरक्षित करें व पर्ची बनाएं" else "Confirm Booking & Generate Slip",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.5.sp,
                        color = AmberGold
                    )
                }
            }
        }
    }
}

@Composable
fun BookingsListView(
    isHindi: Boolean,
    bookings: List<DharamshalaBooking>,
    onCheckOut: (DharamshalaBooking) -> Unit,
    onPrintSlip: (DharamshalaBooking) -> Unit
) {
    val context = LocalContext.current
    val activeBookings = remember(bookings) {
        bookings.filter { it.status != "CHECKED_OUT" && it.status != "CANCELLED" }
    }

    if (activeBookings.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🏨", fontSize = 40.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isHindi) "वर्तमान में कोई सक्रिय कमरा आरक्षण नहीं है।" else "No active room bookings right now.",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(activeBookings) { b ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaroonAccent
                            ) {
                                Text(
                                    text = "कमरा #${b.roomNumber}",
                                    color = AmberGold,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }

                            Text(
                                text = b.bookingRef,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.Gray
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = b.devoteeName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaroonPrimary
                        )

                        Text(
                            text = "मोबाइल: ${b.phoneNumber} • यात्री: ${b.devoteeCount} व्यक्ति",
                            fontSize = 12.sp,
                            color = Color.DarkGray
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "चेक-इन: ${b.checkinDate} ➔ चेक-आउट: ${b.checkoutDate} (${b.totalDays} दिन)",
                            fontSize = 11.5.sp,
                            color = Color.Gray
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "कुल सेवा शुल्क: ₹${b.totalAmount.toInt()}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = SaffronPrimary
                            )

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (b.isPaid) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                            ) {
                                Text(
                                    text = if (b.isPaid) "✅ पूर्ण प्राप्त (${b.paymentMode})" else "⏳ आश्रम काउंटर पर देय",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (b.isPaid) Color(0xFF2E7D32) else Color(0xFFE65100),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Action buttons: Thermal Print, WhatsApp, Check Out
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onPrintSlip(b) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("🖨️ पर्ची", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    val msg = "श्री बालाजी कृपा धाम, ग्राम डूँगरा जाट\n🏨 धर्मशाला कमरा आरक्षण रसीद\nसंदर्भ: ${b.bookingRef}\nभक्त: ${b.devoteeName}\nकमरा: #${b.roomNumber}\nचेक-इन: ${b.checkinDate} | चेक-आउट: ${b.checkoutDate}\nकुल शुल्क: ₹${b.totalAmount.toInt()}\nजय श्री राम | जय श्री बालाजी"
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        data = Uri.parse("https://api.whatsapp.com/send?phone=+91${b.phoneNumber}&text=" + Uri.encode(msg))
                                    }
                                    try { context.startActivity(intent) } catch (e: Exception) {}
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00897B)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("💬 WhatsApp", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { onCheckOut(b) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaroonAccent),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("🚪 चेक-आउट", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = AmberGold)
                            }
                        }
                    }
                }
            }
        }
    }
}
