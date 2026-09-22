package com.example.shribalajikripadham.util

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.shribalajikripadham.data.model.ArziDistributionRecord
import com.example.shribalajikripadham.data.model.AshramSettings
import com.example.shribalajikripadham.data.model.Token
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Universal Bluetooth Thermal Receipt Printer Engine (ESC/POS 58mm / 80mm).
 *
 * Engineered with 1-Bit Monochrome Canvas Rendering:
 * Standard POS thermal printers lack Hindi Devanagari font ROMs. This engine renders
 * all Hindi typography onto a crisp 384px / 576px Canvas and streams standard
 * ESC/POS raster bit image commands (GS v 0). This delivers 100% pristine, sharp Hindi
 * text, borders, and emblems on ANY standard Bluetooth thermal printer without special firmware!
 */
object BluetoothThermalPrinterHelper {

    private const val TAG = "ThermalPrinter"
    private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    const val WIDTH_58MM = 384
    const val WIDTH_80MM = 576

    /**
     * Returns true if required Bluetooth runtime permissions are granted.
     */
    fun hasBluetoothPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Lists all bonded / paired Bluetooth devices.
     */
    @SuppressLint("MissingPermission")
    fun getPairedDevices(context: Context): List<BluetoothDevice> {
        return try {
            if (!hasBluetoothPermission(context)) return emptyList()
            val adapter = BluetoothAdapter.getDefaultAdapter() ?: return emptyList()
            if (!adapter.isEnabled) return emptyList()
            adapter.bondedDevices?.toList() ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "getPairedDevices error: ${e.message}")
            emptyList()
        }
    }

    /**
     * Connects to Bluetooth device, prints ESC/POS raster bitmap, and cleanly disconnects.
     */
    @SuppressLint("MissingPermission")
    suspend fun printBitmap(device: BluetoothDevice, bitmap: Bitmap): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        var socket: BluetoothSocket? = null
        var outputStream: OutputStream? = null
        try {
            val adapter = BluetoothAdapter.getDefaultAdapter()
            adapter?.cancelDiscovery()

            socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            socket.connect()
            outputStream = socket.outputStream

            // 1. ESC @ (Initialize printer)
            outputStream.write(byteArrayOf(0x1B, 0x40))

            // 2. ESC a 1 (Center align)
            outputStream.write(byteArrayOf(0x1B, 0x61, 0x01))

            // 3. Convert Bitmap to ESC/POS Raster format (GS v 0)
            val rasterBytes = bitmapToEscPosRaster(bitmap)
            outputStream.write(rasterBytes)

            // 4. Feed 4 lines & cut
            outputStream.write(byteArrayOf(0x1B, 0x64, 0x04)) // Feed 4 lines
            outputStream.write(byteArrayOf(0x1D, 0x56, 0x42, 0x00)) // GS V 66 0 (Cut paper)

            outputStream.flush()
            Pair(true, "प्रिंट सफलतापूर्वक भेजा गया!")
        } catch (e: Exception) {
            Log.e(TAG, "printBitmap failed: ${e.message}")
            Pair(false, "प्रिंटर से संपर्क विफल: ${e.localizedMessage ?: e.message}")
        } finally {
            try { outputStream?.close() } catch (e: Exception) {}
            try { socket?.close() } catch (e: Exception) {}
        }
    }

    /**
     * Converts an Android Bitmap into ESC/POS GS v 0 raster bit image bytes.
     */
    fun bitmapToEscPosRaster(bitmap: Bitmap): ByteArray {
        val width = bitmap.width
        val height = bitmap.height
        val widthBytes = (width + 7) / 8

        val baos = ByteArrayOutputStream()

        // GS v 0 m xL xH yL yH
        // m = 0 (Normal mode)
        val xL = (widthBytes and 0xFF).toByte()
        val xH = ((widthBytes shr 8) and 0xFF).toByte()
        val yL = (height and 0xFF).toByte()
        val yH = ((height shr 8) and 0xFF).toByte()

        baos.write(byteArrayOf(0x1D, 0x76, 0x30, 0x00, xL, xH, yL, yH))

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        for (y in 0 until height) {
            for (xByte in 0 until widthBytes) {
                var b = 0
                for (bit in 0..7) {
                    val x = xByte * 8 + bit
                    if (x < width) {
                        val pixel = pixels[y * width + x]
                        // Convert to luminance (0.299R + 0.587G + 0.114B)
                        val r = (pixel shr 16) and 0xFF
                        val g = (pixel shr 8) and 0xFF
                        val bVal = pixel and 0xFF
                        val lum = (0.299 * r + 0.587 * g + 0.114 * bVal).toInt()
                        // Threshold: dark pixels (< 160) are printed (bit = 1)
                        if (lum < 160) {
                            b = b or (1 shl (7 - bit))
                        }
                    }
                }
                baos.write(b)
            }
        }
        return baos.toByteArray()
    }

    /**
     * Generates a beautifully formatted 58mm/80mm Token Slip Bitmap in Hindi.
     */
    fun generateTokenSlipBitmap(token: Token, is80mm: Boolean = false): Bitmap {
        val width = if (is80mm) WIDTH_80MM else WIDTH_58MM
        val padding = if (is80mm) 24 else 14
        val printWidth = width - (padding * 2)

        // Estimated height calculation
        val height = if (is80mm) 620 else 520
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        var y = 36f

        // 1. Header (Centered)
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = if (is80mm) 24f else 19f
        paint.isFakeBoldText = true
        canvas.drawText("॥ श्री हनुमते नमः ॥", width / 2f, y, paint)
        y += if (is80mm) 32f else 26f

        paint.textSize = if (is80mm) 26f else 21f
        canvas.drawText("श्री बालाजी कृपा धाम", width / 2f, y, paint)
        y += if (is80mm) 26f else 20f

        paint.textSize = if (is80mm) 16f else 13f
        paint.isFakeBoldText = false
        canvas.drawText("ग्राम डूँगरा जाट, शिकारपुर (बुलंदशहर)", width / 2f, y, paint)
        y += if (is80mm) 22f else 18f
        canvas.drawText("परम पूज्य गुरुजी श्री तेजवीर सिंह जी", width / 2f, y, paint)
        y += if (is80mm) 24f else 20f

        // Divider Line
        paint.strokeWidth = 2f
        canvas.drawLine(padding.toFloat(), y, (width - padding).toFloat(), y, paint)
        y += if (is80mm) 28f else 22f

        // 2. Token Number Big Box
        paint.textSize = if (is80mm) 18f else 15f
        paint.isFakeBoldText = true
        canvas.drawText("दिव्य दर्शन टोकन पर्ची", width / 2f, y, paint)
        y += if (is80mm) 48f else 40f

        paint.textSize = if (is80mm) 56f else 44f
        paint.isFakeBoldText = true
        canvas.drawText("टोकन #${token.tokenNumber}", width / 2f, y, paint)
        y += if (is80mm) 32f else 24f

        // Divider Line
        canvas.drawLine(padding.toFloat(), y, (width - padding).toFloat(), y, paint)
        y += if (is80mm) 26f else 20f

        // 3. Devotee Details (Left Aligned)
        paint.textAlign = Paint.Align.LEFT
        paint.isFakeBoldText = false
        paint.textSize = if (is80mm) 18f else 14f

        val leftX = padding.toFloat() + 6f
        val sdf = SimpleDateFormat("dd-MM-yyyy, hh:mm a", Locale.getDefault())
        val dateStr = if (token.createdAt > 0) sdf.format(Date(token.createdAt)) else sdf.format(Date())

        canvas.drawText("भक्त का नाम : ${token.patientName.ifBlank { "अज्ञात" }}", leftX, y, paint)
        y += if (is80mm) 26f else 21f

        if (token.phoneNumber.isNotBlank()) {
            canvas.drawText("मोबाइल नंबर  : ${token.phoneNumber}", leftX, y, paint)
            y += if (is80mm) 26f else 21f
        }

        if (token.city.isNotBlank()) {
            canvas.drawText("स्थान / शहर   : ${token.city}", leftX, y, paint)
            y += if (is80mm) 26f else 21f
        }

        canvas.drawText("तारीख व समय : $dateStr", leftX, y, paint)
        y += if (is80mm) 26f else 21f

        val regBy = if (token.registeredBy.equals("SUPER_ADMIN", true)) "आश्रम मुख्य काउंटर" else "सेवादार"
        canvas.drawText("पंजीकरणकर्ता : $regBy", leftX, y, paint)
        y += if (is80mm) 28f else 22f

        // Divider Line
        paint.strokeWidth = 1f
        canvas.drawLine(padding.toFloat(), y, (width - padding).toFloat(), y, paint)
        y += if (is80mm) 24f else 18f

        // 4. Sacred Blessing Footer
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = if (is80mm) 15f else 12f
        paint.isFakeBoldText = true
        canvas.drawText("संकट कटे मिटे सब पीरा। जो सुमिरै हनुमत बलबीरा॥", width / 2f, y, paint)
        y += if (is80mm) 20f else 16f

        paint.isFakeBoldText = false
        paint.textSize = if (is80mm) 14f else 11f
        canvas.drawText("कृपया अपनी बारी आने पर ही गुरुजी के समीप पधारें।", width / 2f, y, paint)

        return bitmap
    }

    /**
     * Generates an official 58mm/80mm Arzi Ledger Receipt Bitmap in Hindi.
     */
    fun generateArziReceiptBitmap(record: ArziDistributionRecord, settings: AshramSettings, is80mm: Boolean = false): Bitmap {
        val width = if (is80mm) WIDTH_80MM else WIDTH_58MM
        val padding = if (is80mm) 24 else 14
        val height = if (is80mm) 640 else 540

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
        }

        var y = 36f

        // Header
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = if (is80mm) 24f else 19f
        paint.isFakeBoldText = true
        canvas.drawText("॥ श्री बालाजी महाराज की जय ॥", width / 2f, y, paint)
        y += if (is80mm) 30f else 24f

        paint.textSize = if (is80mm) 26f else 21f
        canvas.drawText("श्री बालाजी कृपा धाम", width / 2f, y, paint)
        y += if (is80mm) 24f else 18f

        paint.textSize = if (is80mm) 16f else 13f
        paint.isFakeBoldText = false
        canvas.drawText("पवित्र अर्जी डिब्बा वितरण पावती", width / 2f, y, paint)
        y += if (is80mm) 24f else 18f

        // Divider
        paint.strokeWidth = 2f
        canvas.drawLine(padding.toFloat(), y, (width - padding).toFloat(), y, paint)
        y += if (is80mm) 26f else 20f

        // Details
        val leftX = padding.toFloat() + 6f
        val rightX = (width - padding).toFloat() - 6f

        paint.textAlign = Paint.Align.LEFT
        paint.textSize = if (is80mm) 17f else 13.5f

        val sdf = SimpleDateFormat("dd-MM-yyyy, hh:mm a", Locale.getDefault())
        val dateStr = sdf.format(Date(record.timestamp))

        canvas.drawText("भक्त का नाम: ${record.devoteeName}", leftX, y, paint)
        y += if (is80mm) 26f else 20f

        if (record.phoneNumber.isNotBlank()) {
            canvas.drawText("मोबाइल नं: ${record.phoneNumber}", leftX, y, paint)
            y += if (is80mm) 26f else 20f
        }

        canvas.drawText("तारीख: $dateStr", leftX, y, paint)
        y += if (is80mm) 26f else 20f

        // Divider
        paint.strokeWidth = 1f
        canvas.drawLine(padding.toFloat(), y, (width - padding).toFloat(), y, paint)
        y += if (is80mm) 24f else 18f

        // Table breakdown
        if (record.bigArziQty > 0) {
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText("बड़ी अर्जी x ${record.bigArziQty} (दर ₹${record.bigArziRate.toInt()})", leftX, y, paint)
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("₹${(record.bigArziQty * record.bigArziRate).toInt()}", rightX, y, paint)
            y += if (is80mm) 24f else 18f
        }

        if (record.smallArziQty > 0) {
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText("छोटी अर्जी x ${record.smallArziQty} (दर ₹${record.smallArziRate.toInt()})", leftX, y, paint)
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("₹${(record.smallArziQty * record.smallArziRate).toInt()}", rightX, y, paint)
            y += if (is80mm) 24f else 18f
        }

        // Total Divider
        paint.strokeWidth = 2f
        canvas.drawLine(padding.toFloat(), y, (width - padding).toFloat(), y, paint)
        y += if (is80mm) 28f else 22f

        paint.isFakeBoldText = true
        paint.textSize = if (is80mm) 22f else 17f
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("कुल देय राशि :", leftX, y, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("₹${record.totalAmount.toInt()}", rightX, y, paint)
        y += if (is80mm) 30f else 24f

        paint.textSize = if (is80mm) 18f else 14f
        paint.textAlign = Paint.Align.LEFT
        val statusText = if (record.isPaid) "✅ भुगतान: पूर्ण प्राप्त (${record.paymentMode})" else "⏳ भुगतान: बकाया (Pending)"
        canvas.drawText(statusText, leftX, y, paint)
        y += if (is80mm) 32f else 26f

        // Footer
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = if (is80mm) 14f else 11f
        paint.isFakeBoldText = false
        canvas.drawText("अर्जी डिब्बा आश्रम भंडार से प्राप्त करें।", width / 2f, y, paint)
        y += if (is80mm) 20f else 16f
        canvas.drawText("जय श्री राम | जय श्री बालाजी", width / 2f, y, paint)

        return bitmap
    }

    /**
     * Generates a 1-bit monochrome Bitmap receipt for Dharamshala / Room & Bed Booking.
     */
    fun generateDharamshalaSlipBitmap(
        bookingRef: String,
        devoteeName: String,
        phoneNumber: String,
        roomNumber: String,
        roomType: String,
        checkinDate: String,
        checkoutDate: String,
        totalDays: Int,
        totalAmount: Double,
        isPaid: Boolean,
        paymentMode: String,
        allocatedBy: String,
        is80mm: Boolean = false
    ): Bitmap {
        val width = if (is80mm) WIDTH_80MM else WIDTH_58MM
        val height = if (is80mm) 620 else 520
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            isFakeBoldText = true
        }

        val padding = if (is80mm) 20 else 14
        var y = if (is80mm) 36f else 28f

        // Header
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = if (is80mm) 23f else 18f
        canvas.drawText("श्री बालाजी कृपा धाम", width / 2f, y, paint)
        y += if (is80mm) 24f else 19f

        paint.textSize = if (is80mm) 15f else 12f
        paint.isFakeBoldText = false
        canvas.drawText("ग्राम डूँगरा जाट, बुलन्दशहर (उ.प्र.)", width / 2f, y, paint)
        y += if (is80mm) 20f else 16f

        paint.isFakeBoldText = true
        paint.textSize = if (is80mm) 17f else 13.5f
        canvas.drawText("🏨 धर्मशाला व कमरा आरक्षण रसीद 🏨", width / 2f, y, paint)
        y += if (is80mm) 22f else 17f

        // Outer Frame
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawRect(padding.toFloat(), 10f, (width - padding).toFloat(), (height - 10).toFloat(), paint)
        paint.style = Paint.Style.FILL

        // Divider
        canvas.drawLine(padding.toFloat(), y, (width - padding).toFloat(), y, paint)
        y += if (is80mm) 24f else 18f

        val leftX = padding.toFloat() + 6f
        val rightX = (width - padding).toFloat() - 6f

        paint.textAlign = Paint.Align.LEFT
        paint.textSize = if (is80mm) 16f else 13f

        canvas.drawText("बुकिंग संदर्भ: $bookingRef", leftX, y, paint)
        y += if (is80mm) 24f else 18f

        canvas.drawText("भक्त का नाम: $devoteeName", leftX, y, paint)
        y += if (is80mm) 24f else 18f

        if (phoneNumber.isNotBlank()) {
            canvas.drawText("मोबाइल नंबर: $phoneNumber", leftX, y, paint)
            y += if (is80mm) 24f else 18f
        }

        // Highlight Room Number
        paint.isFakeBoldText = true
        paint.textSize = if (is80mm) 20f else 16f
        val typeDesc = when (roomType) {
            "AC" -> "AC कक्ष"
            "NON_AC" -> "डीलक्स गैर-AC"
            else -> "सत्संग हॉल बेड"
        }
        canvas.drawText("आरक्षित कमरा: #$roomNumber ($typeDesc)", leftX, y, paint)
        y += if (is80mm) 28f else 22f

        paint.isFakeBoldText = false
        paint.textSize = if (is80mm) 15f else 12f
        canvas.drawText("चेक-इन: $checkinDate  |  चेक-आउट: $checkoutDate", leftX, y, paint)
        y += if (is80mm) 22f else 18f

        canvas.drawText("कुल दिवस: $totalDays दिन", leftX, y, paint)
        y += if (is80mm) 22f else 18f

        // Divider
        paint.strokeWidth = 1.5f
        canvas.drawLine(padding.toFloat(), y, (width - padding).toFloat(), y, paint)
        y += if (is80mm) 24f else 18f

        // Financials
        paint.isFakeBoldText = true
        paint.textSize = if (is80mm) 20f else 16f
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("कुल सेवा शुल्क:", leftX, y, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("₹${totalAmount.toInt()}", rightX, y, paint)
        y += if (is80mm) 28f else 22f

        paint.textSize = if (is80mm) 16f else 13f
        paint.textAlign = Paint.Align.LEFT
        val paymentStr = if (isPaid) "✅ भुगतान: प्राप्त ($paymentMode)" else "⏳ भुगतान: आश्रम काउंटर पर देय"
        canvas.drawText(paymentStr, leftX, y, paint)
        y += if (is80mm) 24f else 19f

        canvas.drawText("आरक्षणकर्ता: $allocatedBy", leftX, y, paint)
        y += if (is80mm) 26f else 20f

        // Footer rules
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = if (is80mm) 13f else 10.5f
        paint.isFakeBoldText = false
        canvas.drawText("कृपया आश्रम परिसर में स्वच्छता व मर्यादा बनाए रखें।", width / 2f, y, paint)
        y += if (is80mm) 18f else 14f
        canvas.drawText("जय श्री राम | जय श्री बालाजी महाराज", width / 2f, y, paint)

        return bitmap
    }
}
