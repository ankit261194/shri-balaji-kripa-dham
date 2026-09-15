package com.example.shribalajikripadham.data.model

data class DevoteeDirectoryEntry(
    val id: Long = 0,
    val devoteeId: String,
    val patientName: String,
    val phoneNumber: String,
    val city: String = "",
    val age: Int = 0,
    val gender: String = "",
    val photoUri: String = "",
    val lastVisitDate: String = "",
    val visitCount: Int = 1,
    val sourceModule: String = "TOKEN",
    val updatedAt: Long = System.currentTimeMillis()
) {
    val displaySubtitle: String
        get() {
            val parts = mutableListOf<String>()
            if (city.isNotBlank()) parts.add(city)
            if (phoneNumber.isNotBlank()) parts.add("📞 $phoneNumber")
            if (visitCount > 1) parts.add("दर्शन: $visitCount बार")
            return parts.joinToString(" • ")
        }
}
