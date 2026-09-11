package com.example.shribalajikripadham.ui.yatra

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shribalajikripadham.data.model.ExpenseCategory
import com.example.shribalajikripadham.data.model.YatraExpense
import com.example.shribalajikripadham.data.repository.AshramRepository
import com.example.shribalajikripadham.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YatraExpenseScreen(
    isHindi: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { AshramRepository(context) }
    val scope = rememberCoroutineScope()

    var expenses by remember { mutableStateOf<List<YatraExpense>>(emptyList()) }
    var financialSummary by remember { mutableStateOf(Triple(0.0, 0.0, 0.0)) }
    var showAddDialog by remember { mutableStateOf(false) }

    // Dialog inputs
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ExpenseCategory.DIESEL) }

    fun refreshData() {
        scope.launch {
            expenses = repository.getAllYatraExpenses()
            financialSummary = repository.getYatraFinancialSummary()
        }
    }

    LaunchedEffect(Unit) {
        refreshData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isHindi) "यात्रा खर्च हिसाब-किताब" else "Yatra Expense Ledger",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text(text = "⬅", color = Color.White, fontSize = 20.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaroonAccent)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = SaffronPrimary,
                contentColor = Color.White
            ) {
                Text(text = "＋", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = SacredBackgroundLight
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // 1. FINANCIAL SUMMARY CARD
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isHindi) "वित्तीय सारांश (बस व धर्मशाला)" else "Financial Audit Summary",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaroonAccent
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = if (isHindi) "कुल किराया संग्रह" else "Total Collected",
                                fontSize = 11.sp,
                                color = TextSecondaryDark
                            )
                            Text(
                                text = "₹${"%.0f".format(financialSummary.first)}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = StatusPaid
                            )
                        }
                        Column {
                            Text(
                                text = if (isHindi) "कुल वास्तविक खर्च" else "Total Expenses",
                                fontSize = 11.sp,
                                color = TextSecondaryDark
                            )
                            Text(
                                text = "₹${"%.0f".format(financialSummary.second)}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = StatusOutsideAshram
                            )
                        }
                        Column {
                            Text(
                                text = if (isHindi) "अवशेष बैलेंस" else "Net Balance",
                                fontSize = 11.sp,
                                color = TextSecondaryDark
                            )
                            Text(
                                text = "₹${"%.0f".format(financialSummary.third)}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = SaffronDark
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (isHindi) "खर्च विवरण सूची" else "Itemized Expense Records",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaroonAccent
            )
            Spacer(modifier = Modifier.height(10.dp))

            // 2. EXPENSE LIST
            if (expenses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isHindi) "कोई खर्च प्रविष्टि दर्ज नहीं है।" else "No expenses recorded yet.",
                        color = TextSecondaryDark
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(expenses) { exp ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = exp.title,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimaryDark
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = "${if (isHindi) exp.category.displayNameHindi else exp.category.displayNameEnglish} • ${exp.addedByAdminName} • ${exp.expenseDate}",
                                        fontSize = 11.sp,
                                        color = TextSecondaryDark
                                    )
                                }
                                Text(
                                    text = "₹${"%.0f".format(exp.amount)}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SaffronDark
                                )
                            }
                        }
                    }
                }
            }
        }

        // Add Expense Dialog
        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = {
                    Text(
                        text = if (isHindi) "नया यात्रा खर्च जोड़ें" else "Add New Expense",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text(text = if (isHindi) "खर्च का विवरण *" else "Expense Title *") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = amountText,
                            onValueChange = { amountText = it },
                            label = { Text(text = if (isHindi) "रकम (₹) *" else "Amount (₹) *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = if (isHindi) "खर्च श्रेणी:" else "Category:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            ExpenseCategory.values().take(3).forEach { cat ->
                                FilterChip(
                                    selected = selectedCategory == cat,
                                    onClick = { selectedCategory = cat },
                                    label = {
                                        Text(
                                            text = if (isHindi) cat.displayNameHindi else cat.displayNameEnglish,
                                            fontSize = 11.sp
                                        )
                                    },
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val amt = amountText.toDoubleOrNull() ?: 0.0
                            if (title.isNotBlank() && amt > 0) {
                                scope.launch {
                                    repository.addYatraExpense(
                                        title = title.trim(),
                                        category = selectedCategory,
                                        amount = amt,
                                        addedBy = "सेवादार"
                                    )
                                    showAddDialog = false
                                    title = ""
                                    amountText = ""
                                    refreshData()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary)
                    ) {
                        Text(text = if (isHindi) "जोड़ें" else "Add")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) {
                        Text(text = if (isHindi) "रद्द करें" else "Cancel")
                    }
                }
            )
        }
    }
}
