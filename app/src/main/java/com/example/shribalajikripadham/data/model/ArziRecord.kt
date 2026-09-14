package com.example.shribalajikripadham.data.model

data class ArziDistributionRecord(
    val id: Long = 0,
    val devoteeName: String,
    val phoneNumber: String = "",
    val bigArziQty: Int = 0,
    val smallArziQty: Int = 0,
    val bigArziRate: Double = 100.0,
    val smallArziRate: Double = 50.0,
    val totalAmount: Double = (bigArziQty * bigArziRate) + (smallArziQty * smallArziRate),
    val isPaid: Boolean = false,
    val paymentMode: String = "CASH", // "CASH", "UPI_QR"
    val recordedBy: String = "SUPER_ADMIN",
    val darbarDate: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = ""
)

data class UnifiedLedgerEntry(
    val id: String,
    val date: String,
    val category: String, // "BUS_BOOKING", "ARZI_BOX", "DHARAMSHALA", "ASHRAM_EXPENSE", "UPI_QR_DONATION"
    val categoryTitleHindi: String,
    val devoteeOrPerson: String,
    val phone: String,
    val details: String,
    val amount: Double,
    val isInflow: Boolean, // true for income, false for expense
    val isPaid: Boolean,
    val paymentMode: String,
    val timestamp: Long,
    val recordedBy: String = "SUPER_ADMIN",
    val notes: String = ""
)

data class UnifiedMasterFinancialSummary(
    val totalInflow: Double = 0.0,
    val totalPaidInflow: Double = 0.0,
    val totalPendingInflow: Double = 0.0,
    val totalOutflow: Double = 0.0,
    val netBalance: Double = 0.0,
    // Service Breakdowns:
    val busTotalAmount: Double = 0.0,
    val busPaidAmount: Double = 0.0,
    val busPendingAmount: Double = 0.0,
    val busBookedSeatsCount: Int = 0,
    val arziTotalAmount: Double = 0.0,
    val arziPaidAmount: Double = 0.0,
    val arziPendingAmount: Double = 0.0,
    val arziBadiCount: Int = 0,
    val arziChhotiCount: Int = 0,
    val expenseTotalAmount: Double = 0.0,
    val expenseCount: Int = 0,
    val entries: List<UnifiedLedgerEntry> = emptyList()
)
