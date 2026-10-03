package com.example.shribalajikripadham.data.model

data class AppQuery(
    val id: Long = 0,
    val remoteId: Long = 0,
    val senderName: String,
    val senderPhone: String,
    val senderCity: String = "",
    val senderRole: String = "DEVOTEE", // "DEVOTEE" or "ADMIN"
    val category: String = "OTHER", // "BUG", "SUGGESTION", "TOKEN_ISSUE", "HAVAN_PARCHA", "OTHER"
    val subject: String = "",
    val message: String,
    val attachmentUrl: String = "",
    val status: String = "PENDING", // "PENDING", "IN_PROGRESS", "REPLIED", "RESOLVED"
    val adminReply: String = "",
    val repliedBy: String = "",
    val repliedAt: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun isReplied(): Boolean = adminReply.isNotBlank() || status == "REPLIED" || status == "RESOLVED"

    fun getCategoryHindi(): String = when (category.uppercase()) {
        "BUG" -> "🐛 ऐप में कमी / बग"
        "SUGGESTION" -> "💡 सुझाव / बदलाव"
        "TOKEN_ISSUE" -> "🎫 टोकन / दर्शन समस्या"
        "HAVAN_PARCHA" -> "🔥 हवन / पर्चा प्रश्न"
        else -> "❓ अन्य परेशानी / प्रश्न"
    }

    fun getStatusHindi(): String = when (status.uppercase()) {
        "REPLIED", "RESOLVED" -> "✅ उत्तर प्राप्त (समाधान)"
        "IN_PROGRESS" -> "⚙️ प्रक्रियाधीन"
        else -> "⏳ विचाराधीन (Pending)"
    }
}
