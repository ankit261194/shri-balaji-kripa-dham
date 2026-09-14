package com.example.shribalajikripadham.ui.admin

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shribalajikripadham.data.model.AshramSettings
import com.example.shribalajikripadham.data.model.UnifiedLedgerEntry
import com.example.shribalajikripadham.data.model.UnifiedMasterFinancialSummary
import com.example.shribalajikripadham.data.repository.AshramRepository
import com.example.shribalajikripadham.theme.AmberGold
import com.example.shribalajikripadham.theme.MaroonAccent
import com.example.shribalajikripadham.theme.MaroonPrimary
import com.example.shribalajikripadham.theme.SaffronPrimary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedMasterLedgerTab(
    isHindi: Boolean,
    repository: AshramRepository,
    settings: AshramSettings,
    isSuperAdmin: Boolean,
    scope: CoroutineScope,
    context: Context
) {
    val isSuper = isSuperAdmin

    var summary by remember { mutableStateOf(UnifiedMasterFinancialSummary()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedCategoryFilter by remember { mutableStateOf("ALL") } // ALL, ARZI_BOX, BUS_BOOKING, ASHRAM_EXPENSE
    var searchQuery by remember { mutableStateOf("") }

    fun loadData() {
        scope.launch {
            isLoading = true
            summary = repository.getUnifiedMasterFinancialSummary()
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    val filteredEntries = remember(summary.entries, selectedCategoryFilter, searchQuery) {
        summary.entries.filter { entry ->
            val matchesCategory = when (selectedCategoryFilter) {
                "ARZI_BOX" -> entry.category == "ARZI_BOX"
                "BUS_BOOKING" -> entry.category == "BUS_BOOKING"
                "ASHRAM_EXPENSE" -> entry.category == "ASHRAM_EXPENSE"
                else -> true
            }
            val matchesQuery = searchQuery.isBlank() ||
                    entry.devoteeOrPerson.contains(searchQuery, ignoreCase = true) ||
                    entry.phone.contains(searchQuery, ignoreCase = true) ||
                    entry.details.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 16.dp)
    ) {
        // 1. Grand Audit Overview Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaroonPrimary),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isHindi) "संपूर्ण ऐप महा-लेजर (ऑडिट रिकॉर्ड) 📊" else "Unified Master Financial Audit 📊",
                            color = AmberGold,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isHindi) "बस + अर्जी + धर्मशाला + आश्रम व्यय का सम्पूर्ण बहीखाता" else "Complete ledger across Bus, Arzi, Dharamshala & Expenses",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 11.sp
                        )
                    }

                    IconButton(
                        onClick = {
                            loadData()
                            Toast.makeText(context, if (isHindi) "महा-लेजर रीफ्रेश हुआ" else "Ledger refreshed", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Text("🔄", fontSize = 18.sp)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Big Net Balance Metric
                Surface(
                    color = Color.White.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isHindi) "शुद्ध आश्रम शेष राशि (Net Balance)" else "Net Ashram Balance",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                            Text(
                                text = "₹${summary.netBalance.toInt()}",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (summary.netBalance >= 0) AmberGold else Color(0xFFFF8A80)
                            )
                        }

                        Surface(
                            color = if (summary.netBalance >= 0) Color(0xFF2E7D32) else Color(0xFFC62828),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (summary.netBalance >= 0) "✅ बचत (Surplus)" else "⚠️ घाटा (Deficit)",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Breakdown: Inflow vs Outflow
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(if (isHindi) "कुल प्राप्त आवक" else "Paid Inflow", fontSize = 11.sp, color = Color.White.copy(alpha = 0.75f))
                        Text("₹${summary.totalPaidInflow.toInt()}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA5D6A7))
                        Text("बकाया: ₹${summary.totalPendingInflow.toInt()}", fontSize = 10.sp, color = Color(0xFFFFCC80))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(if (isHindi) "कुल आश्रम व्यय" else "Total Expenses", fontSize = 11.sp, color = Color.White.copy(alpha = 0.75f))
                        Text("₹${summary.totalOutflow.toInt()}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFAB91))
                        Text("${summary.expenseCount} खर्च प्रविष्टियां", fontSize = 10.sp, color = Color.White.copy(alpha = 0.75f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Service-Specific Quick Stats Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE0E0E0))
            ) {
                Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📦 अर्जी डिब्बे", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaroonPrimary)
                    Text("₹${summary.arziTotalAmount.toInt()}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20))
                    Text("चुकता: ₹${summary.arziPaidAmount.toInt()}", fontSize = 10.sp, color = Color.DarkGray)
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE0E0E0))
            ) {
                Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🚌 बस बुकिंग", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SaffronPrimary)
                    Text("₹${summary.busTotalAmount.toInt()}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20))
                    Text("${summary.busBookedSeatsCount} सीटें बुक", fontSize = 10.sp, color = Color.DarkGray)
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE0E0E0))
            ) {
                Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("💸 आश्रम खर्च", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                    Text("₹${summary.expenseTotalAmount.toInt()}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                    Text("${summary.expenseCount} खर्चे", fontSize = 10.sp, color = Color.DarkGray)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Search Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text(if (isHindi) "नाम / विवरण / फोन खोजें..." else "Search name/detail/phone...", fontSize = 13.sp) },
            leadingIcon = { Text("🔍", fontSize = 16.sp) },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Category Filter Chips
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(
                selected = selectedCategoryFilter == "ALL",
                onClick = { selectedCategoryFilter = "ALL" },
                label = { Text("सभी (${summary.entries.size})", fontSize = 11.sp) }
            )
            FilterChip(
                selected = selectedCategoryFilter == "ARZI_BOX",
                onClick = { selectedCategoryFilter = "ARZI_BOX" },
                label = { Text("📦 अर्जी (${summary.entries.count { it.category == "ARZI_BOX" }})", fontSize = 11.sp) }
            )
            FilterChip(
                selected = selectedCategoryFilter == "BUS_BOOKING",
                onClick = { selectedCategoryFilter = "BUS_BOOKING" },
                label = { Text("🚌 बस (${summary.entries.count { it.category == "BUS_BOOKING" }})", fontSize = 11.sp) }
            )
            FilterChip(
                selected = selectedCategoryFilter == "ASHRAM_EXPENSE",
                onClick = { selectedCategoryFilter = "ASHRAM_EXPENSE" },
                label = { Text("💸 खर्च (${summary.expenseCount})", fontSize = 11.sp) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 4. Ledger Entries List
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SaffronPrimary)
            }
        } else if (filteredEntries.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("📊", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (isHindi) "कोई महा-लेजर रिकॉर्ड उपलब्ध नहीं है" else "No Ledger Records",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.DarkGray
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredEntries, key = { it.id }) { entry ->
                    UnifiedLedgerEntryRow(entry = entry, isHindi = isHindi)
                }
            }
        }
    }
}

@Composable
private fun UnifiedLedgerEntryRow(
    entry: UnifiedLedgerEntry,
    isHindi: Boolean
) {
    val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    val dateStr = if (entry.timestamp > 0) sdf.format(Date(entry.timestamp)) else entry.date

    val categoryIcon = when (entry.category) {
        "ARZI_BOX" -> "📦"
        "BUS_BOOKING" -> "🚌"
        "ASHRAM_EXPENSE" -> "💸"
        else -> "💰"
    }

    val categoryBg = when (entry.category) {
        "ARZI_BOX" -> Color(0xFFFFF3E0)
        "BUS_BOOKING" -> Color(0xFFE3F2FD)
        "ASHRAM_EXPENSE" -> Color(0xFFFFEBEE)
        else -> Color(0xFFF3E5F5)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = categoryBg,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(categoryIcon, fontSize = 18.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = entry.categoryTitleHindi,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaroonPrimary
                        )
                        Text(
                            text = entry.devoteeOrPerson,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black
                        )
                    }
                }

                // Inflow or Outflow Amount
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (entry.isInflow) "+₹${entry.amount.toInt()}" else "-₹${entry.amount.toInt()}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (entry.isInflow) Color(0xFF1B5E20) else Color(0xFFC62828)
                    )
                    Surface(
                        color = if (entry.isPaid) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (entry.isPaid) "✓ चुकता" else "⏳ बकाया",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (entry.isPaid) Color(0xFF2E7D32) else Color(0xFFE65100),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFFF5F5F5))
            Spacer(modifier = Modifier.height(6.dp))

            // Details and Date Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = entry.details,
                    fontSize = 11.sp,
                    color = Color.DarkGray,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = dateStr,
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }
        }
    }
}
