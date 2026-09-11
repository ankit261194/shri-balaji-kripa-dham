package com.example.shribalajikripadham.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.shribalajikripadham.data.model.Token
import com.example.shribalajikripadham.data.model.TokenStatus
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Utility to export devotee tokens as Excel-compatible CSV files.
 * Uses UTF-8 BOM (\uFEFF) to ensure Microsoft Excel and Google Sheets
 * render Hindi Devanagari text flawlessly without encoding issues.
 */
object TokenCsvExporter {

    fun exportTokensToCsv(context: Context, tokens: List<Token>, darbarDate: String): File {
        val fileName = "ShriBalajiKripaDham_Tokens_${darbarDate.replace("-", "")}.csv"
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) exportDir.mkdirs()

        val file = File(exportDir, fileName)
        val fos = FileOutputStream(file)

        // UTF-8 Byte Order Mark (BOM) so Excel recognizes Hindi/UTF-8 automatically
        fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))

        val writer = fos.bufferedWriter(Charsets.UTF_8)

        // Header row
        writer.appendLine("टोकन #,भक्त का नाम,मोबाइल नंबर,निवासी स्थान (Origin),गंतव्य,सड़क दूरी (KM),दर्शन स्थिति,पंजीकरण स्रोत,तारीख व समय")

        val sdf = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())

        for (token in tokens) {
            val statusStr = when {
                token.status == TokenStatus.CANCELLED -> "रद्द (CANCELLED)"
                token.isDarshanCompleted -> "दर्शन संपन्न (COMPLETED)"
                else -> "प्रतीक्षारत (WAITING)"
            }
            val distStr = if (token.distanceKm >= 0) "${token.distanceKm} KM" else "उपलब्ध नहीं"
            val timeStr = sdf.format(Date(token.createdAt))

            val row = listOf(
                escapeCsv(token.tokenNumber.toString()),
                escapeCsv(token.patientName),
                escapeCsv(token.phoneNumber),
                escapeCsv(token.originAddress.ifBlank { token.city }),
                escapeCsv(token.destinationAddress),
                escapeCsv(distStr),
                escapeCsv(statusStr),
                escapeCsv(token.registeredBy),
                escapeCsv(timeStr)
            ).joinToString(",")

            writer.appendLine(row)
        }

        writer.flush()
        writer.close()
        fos.close()

        return file
    }

    private fun escapeCsv(text: String): String {
        val containsSpecial = text.contains(',') || text.contains('"') || text.contains('\n')
        val result = text.replace("\"", "\"\"")
        return if (containsSpecial) "\"$result\"" else result
    }

    fun shareTokensCsv(context: Context, tokens: List<Token>, darbarDate: String) {
        try {
            if (tokens.isEmpty()) {
                Toast.makeText(context, "एक्सपोर्ट हेतु कोई टोकन उपलब्ध नहीं है", Toast.LENGTH_SHORT).show()
                return
            }
            val file = exportTokensToCsv(context, tokens, darbarDate)
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "श्री बालाजी कृपा धाम टोकन सूची CSV - $darbarDate")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "श्री बालाजी कृपा धाम, डुंगरा जाट (बुलंदशहर)\nआज के टोकनों की एक्सेल/CSV सूची संलग्न है।"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "एक्सेल / CSV रिपोर्ट शेयर करें"))
        } catch (e: Exception) {
            Toast.makeText(context, "CSV एक्सपोर्ट त्रुटि: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}
