package com.example.shribalajikripadham.data.model

import java.util.UUID

data class PaymentRecord(
    val id: Long = 0,
    val paymentId: String = UUID.randomUUID().toString(),
    val devoteeName: String = "",
    val devoteePhone: String = "",
    val paymentApp: String = "PhonePe", // PhonePe, Google Pay, Paytm, BHIM, Cred, Amazon Pay, Cash, Other
    val transactionId: String = "", // UTR / Reference number
    val amount: Double = 0.0,
    val purpose: String = "BUS_TICKET", // BUS_TICKET, HAWAN_SEVA, DHARAMSHALA, DONATION
    val seatNumbers: String = "", // e.g. 1A, 1B or 4, 5
    val timestamp: Long = System.currentTimeMillis(),
    val paymentStatus: String = "SUCCESS", // SUCCESS, VERIFIED, PENDING
    val paymentMode: String = "UPI_QR", // UPI_QR, OFFLINE_CASH
    val verifiedBy: String = "",
    val notes: String = ""
)
