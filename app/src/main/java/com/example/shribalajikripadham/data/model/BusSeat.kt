package com.example.shribalajikripadham.data.model

enum class PaymentStatus {
    PAID,
    UNPAID
}

data class BusSeat(
    val seatNumber: Int,
    val seatLabel: String,
    val row: Int,
    val column: Int,
    val isBooked: Boolean = false,
    val passengerName: String = "",
    val phoneNumber: String = "",
    val boardingPoint: String = "Gram Dungra Jaat Ashram",
    val paymentStatus: PaymentStatus = PaymentStatus.UNPAID,
    val paymentMode: String = "CASH",
    val fareAmount: Int = 1500,
    val yatraDate: String = "",
    val notes: String = ""
)
