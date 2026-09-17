package com.example.shribalajikripadham.hardware

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.Build
import android.util.Log
import com.example.shribalajikripadham.data.model.Token
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*

/**
 * High-Performance Direct ESC/POS Bluetooth Thermal Printer Engine.
 * Supports standard 58mm and 80mm Bluetooth receipt printers (TVS, NGX, Everycom, RPP02, etc.).
 * Prints instantly via SPP RFCOMM socket without requiring Android Print Spooler dialog.
 */
object BluetoothThermalPrinterHelper {

    private const val TAG = "ThermalPrinterHelper"
    // Standard Bluetooth Serial Port Profile (SPP) UUID
    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    // ESC/POS Commands
    private val ESC_INIT = byteArrayOf(0x1B, 0x40) // Initialize printer
    private val ESC_ALIGN_LEFT = byteArrayOf(0x1B, 0x61, 0x00) // Left align
    private val ESC_ALIGN_CENTER = byteArrayOf(0x1B, 0x61, 0x01) // Center align
    private val ESC_ALIGN_RIGHT = byteArrayOf(0x1B, 0x61, 0x02) // Right align
    private val ESC_BOLD_ON = byteArrayOf(0x1B, 0x45, 0x01) // Bold ON
    private val ESC_BOLD_OFF = byteArrayOf(0x1B, 0x45, 0x00) // Bold OFF
    private val ESC_DOUBLE_SIZE = byteArrayOf(0x1D, 0x21, 0x11) // 2x Width & Height
    private val ESC_TRIPLE_SIZE = byteArrayOf(0x1D, 0x21, 0x22) // 3x Width & Height
    private val ESC_NORMAL_SIZE = byteArrayOf(0x1D, 0x21, 0x00) // Normal text size
    private val ESC_FEED_PAPER = byteArrayOf(0x1B, 0x64, 0x03) // Feed 3 lines
    private val ESC_CUT_PAPER = byteArrayOf(0x1D, 0x56, 0x41, 0x10) // Full cut

    @SuppressLint("MissingPermission")
    fun getPairedPrinters(context: Context): List<BluetoothDevice> {
        return try {
            val adapter = BluetoothAdapter.getDefaultAdapter() ?: return emptyList()
            if (!adapter.isEnabled) return emptyList()
            adapter.bondedDevices.toList()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting bonded devices: ${e.message}")
            emptyList()
        }
    }

    /**
     * Finds the best matching Bluetooth thermal printer among bonded devices.
     */
    @SuppressLint("MissingPermission")
    fun findTargetPrinter(context: Context): BluetoothDevice? {
        val paired = getPairedPrinters(context)
        if (paired.isEmpty()) return null

        // Priority matching for printer names
        val keywords = listOf("print", "pos", "rpp", "mpt", "bt", "receipt", "thermal", "58", "80")
        for (device in paired) {
            val name = (device.name ?: "").lowercase(Locale.ROOT)
            if (keywords.any { name.contains(it) }) {
                return device
            }
        }
        // Fallback: return the first paired Bluetooth device
        return paired.firstOrNull()
    }

    /**
     * Direct 1-Click Instant Print of a Token Receipt.
     * Connects, prints ESC/POS payload, and closes within ~1 second.
     */
    @SuppressLint("MissingPermission")
    suspend fun printTokenSlip(
        context: Context,
        token: Token,
        ashramName: String = "श्री बालाजी कृपा धाम",
        targetDevice: BluetoothDevice? = null
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        if (adapter == null || !adapter.isEnabled) {
            return@withContext Pair(false, "फ़ोन का ब्लूटूथ बंद है। कृपया ब्लूटूथ चालू करें।")
        }

        val device = targetDevice ?: findTargetPrinter(context)
        if (device == null) {
            return@withContext Pair(
                false,
                "कोई पेयर्ड ब्लूटूथ प्रिंटर नहीं मिला। कृपया फ़ोन की ब्लूटूथ सेटिंग्स में जाकर अपने प्रिंटर को पेयर करें।"
            )
        }

        var socket: BluetoothSocket? = null
        var os: OutputStream? = null

        try {
            socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            adapter.cancelDiscovery()
            socket.connect()
            os = socket.outputStream

            val charset = Charset.forName("CP437")

            // 1. Initialize Printer
            os.write(ESC_INIT)

            // 2. Header
            os.write(ESC_ALIGN_CENTER)
            os.write(ESC_BOLD_ON)
            os.write(ESC_DOUBLE_SIZE)
            os.write("SHRI BALAJI KRIPA DHAM\r\n".toByteArray(charset))
            os.write(ESC_NORMAL_SIZE)
            os.write("Gram Dungra Jaat, Bulandshahr\r\n".toByteArray(charset))
            os.write("Nishulk Ravivar Darbar\r\n".toByteArray(charset))
            os.write(ESC_BOLD_OFF)
            os.write("--------------------------------\r\n".toByteArray(charset))

            // 3. Huge Token Number
            os.write(ESC_BOLD_ON)
            os.write("TOKEN NUMBER\r\n".toByteArray(charset))
            os.write(ESC_TRIPLE_SIZE)
            os.write("# ${token.tokenNumber}\r\n".toByteArray(charset))
            os.write(ESC_NORMAL_SIZE)
            os.write(ESC_BOLD_OFF)
            os.write("--------------------------------\r\n".toByteArray(charset))

            // 4. Devotee Details (Left aligned)
            os.write(ESC_ALIGN_LEFT)
            os.write("Devotee : ${token.patientName}\r\n".toByteArray(charset))
            os.write("Phone   : ${token.phoneNumber}\r\n".toByteArray(charset))
            os.write("City    : ${token.city}\r\n".toByteArray(charset))
            os.write("Date    : ${token.darbarDate}\r\n".toByteArray(charset))
            os.write("Slot    : Morning 08:00 AM\r\n".toByteArray(charset))
            os.write("Status  : ${token.status.name}\r\n".toByteArray(charset))

            // 5. Footer & Blessings
            os.write(ESC_ALIGN_CENTER)
            os.write("--------------------------------\r\n".toByteArray(charset))
            os.write(ESC_BOLD_ON)
            os.write("Sankat Kate Mite Sab Peera\r\n".toByteArray(charset))
            os.write("Jo Sumire Hanumat Balbeera\r\n".toByteArray(charset))
            os.write(ESC_BOLD_OFF)
            os.write("--------------------------------\r\n".toByteArray(charset))
            val timeStr = SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.US).format(Date())
            os.write("Printed: $timeStr\r\n".toByteArray(charset))

            // 6. Feed & Cut
            os.write(ESC_FEED_PAPER)
            try {
                os.write(ESC_CUT_PAPER)
            } catch (ignored: Exception) {}

            os.flush()
            Pair(true, "✅ पर्ची सीधे ब्लूटूथ प्रिंटर (${device.name ?: "POS"}) पर प्रिंट हो गई!")
        } catch (e: Exception) {
            Log.e(TAG, "Bluetooth print failed: ${e.message}", e)
            Pair(false, "प्रिंटर से कनेक्ट नहीं हो सका: ${e.localizedMessage ?: "त्रुटि"}")
        } finally {
            try {
                os?.close()
                socket?.close()
            } catch (ignored: Exception) {}
        }
    }
}
