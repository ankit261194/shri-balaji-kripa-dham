package com.example.shribalajikripadham.data.model

enum class ExpenseCategory(val displayNameHindi: String, val displayNameEnglish: String) {
    DIESEL("डीजल / ईंधन", "Fuel / Diesel"),
    DHARAMSHALA("धर्मशाला बुकिंग", "Dharamshala Booking"),
    FOOD("खान-पान व जलपान", "Food & Meals"),
    TOLL("टोल टैक्स", "Toll Taxes"),
    MISC("अन्य खर्च", "Miscellaneous")
}

data class YatraExpense(
    val id: Long = 0,
    val title: String,
    val category: ExpenseCategory,
    val amount: Double,
    val receiptUri: String = "",
    val addedByAdminName: String = "Sevadar",
    val expenseDate: String,
    val createdAt: Long = System.currentTimeMillis()
)
