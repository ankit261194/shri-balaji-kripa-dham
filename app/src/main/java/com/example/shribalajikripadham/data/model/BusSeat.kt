package com.example.shribalajikripadham.data.model

enum class PaymentStatus {
    PAID,
    UNPAID,
    PENDING_VERIFICATION
}

data class BusSeat(
    val seatNumber: Int,
    val seatLabel: String,
    val row: Int,
    val column: Int,
    val isBooked: Boolean = false,
    val passengerName: String = "",
    val passengerAge: Int = 0,
    val passengerGender: String = "",
    val phoneNumber: String = "",
    val boardingPoint: String = "Gram Dungra Jaat Ashram",
    val paymentStatus: PaymentStatus = PaymentStatus.UNPAID,
    val paymentMode: String = "UPI_QR",
    val transactionId: String = "",
    val fareAmount: Int = 1500,
    val yatraDate: String = "",
    val bookedAt: Long = 0L,
    val bookedBy: String = "DEVOTEE",
    val notes: String = ""
)
