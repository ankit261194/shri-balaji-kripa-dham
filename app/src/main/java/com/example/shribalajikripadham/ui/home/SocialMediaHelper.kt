package com.example.shribalajikripadham.ui.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

fun openSocialMediaLink(
    context: Context,
    rawUrl: String?,
    defaultUrl: String,
    isWhatsApp: Boolean = false,
    errorMessage: String = "लिंक या ऐप खोलने में असमर्थ"
) {
    val trimmed = (rawUrl ?: "").trim()
    val finalUrl = when {
        trimmed.isEmpty() -> defaultUrl
        isWhatsApp -> {
            val cleanDigits = trimmed.replace("+", "").replace("-", "").replace(" ", "").replace("(", "").replace(")", "")
            if (cleanDigits.all { it.isDigit() } && cleanDigits.length in 10..15) {
                val fullNumber = if (cleanDigits.length == 10) "91$cleanDigits" else cleanDigits
                "https://wa.me/$fullNumber"
            } else if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
                "https://$trimmed"
            } else {
                trimmed
            }
        }
        else -> {
            if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
                "https://$trimmed"
            } else {
                trimmed
            }
        }
    }

    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        try {
            if (finalUrl != defaultUrl && defaultUrl.isNotBlank()) {
                val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(defaultUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
                return
            }
        } catch (ignored: Exception) {}
        Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
    }
}

fun shareAppContent(context: Context, shareMessage: String, title: String) {
    try {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, shareMessage)
        }
        val chooser = Intent.createChooser(shareIntent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    } catch (e: Exception) {
        Toast.makeText(context, "शेयर करने में असमर्थ", Toast.LENGTH_SHORT).show()
    }
}
