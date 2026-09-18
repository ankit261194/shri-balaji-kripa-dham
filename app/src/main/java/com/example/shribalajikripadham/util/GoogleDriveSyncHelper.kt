package com.example.shribalajikripadham.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import com.example.shribalajikripadham.data.local.DatabaseHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * GoogleDriveSyncHelper:
 * 100% Zero-Touch Autonomous Backup & Google Drive Integration.
 * Allows instant headless background push to cloud vault and Google Drive.
 */
object GoogleDriveSyncHelper {
    private const val TAG = "GoogleDriveSyncHelper"
    private const val PREFS_VAULT = "sbkd_drive_vault_prefs"
    private const val KEY_SAF_URI = "google_drive_folder_saf_uri"
    private const val KEY_LAST_SYNC = "last_zero_touch_sync_timestamp"

    /**
     * Triggers zero-touch cloud auto-sync on server and local device.
     */
    suspend fun triggerZeroTouchSync(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            // 1. Ping server drive_autosync.php to auto-push to Google Drive / Cloud snapshot
            val serverUrl = URL("https://shribalajikripadham.online/api/drive_autosync.php?action=auto_push")
            val conn = (serverUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 8000
                readTimeout = 8000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("X-SBKD-API-KEY", "SBKD_SECURE_TOKEN_9100100251233433_V243")
            }
            conn.outputStream.use { os ->
                os.write("{\"trigger\":\"android_auto_sync\"}".toByteArray())
            }
            val responseCode = conn.responseCode
            conn.disconnect()

            // 2. Also write local backup snapshot to phone storage
            generateLocalVaultDump(context)

            val prefs = context.getSharedPreferences(PREFS_VAULT, Context.MODE_PRIVATE)
            prefs.edit().putLong(KEY_LAST_SYNC, System.currentTimeMillis()).apply()

            Log.d(TAG, "Zero-touch sync completed with responseCode: $responseCode")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Zero-touch sync error: ${e.message}")
            // Even if offline, ensure local vault is updated
            generateLocalVaultDump(context)
            false
        }
    }

    /**
     * Generates a complete JSON backup of all SQLite tables onto device storage.
     */
    fun generateLocalVaultDump(context: Context): File? {
        return try {
            val dbHelper = DatabaseHelper(context)
            val db = dbHelper.readableDatabase

            val root = JSONObject().apply {
                put("app", "श्री बालाजी कृपा धाम (ग्राम डूँगरा जाट)")
                put("version", "v2.50.0")
                put("vault_signature", "SBKD_IMMUTABLE_VAULT_V1")
                put("created_at", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
                put("timestamp", System.currentTimeMillis())
            }

            val tablesObj = JSONObject()
            val targetTables = listOf("tokens", "ashram_settings", "sevadars", "donors", "expenses", "bus_seats", "daily_darshan")

            for (tbl in targetTables) {
                val arr = JSONArray()
                try {
                    val cursor = db.rawQuery("SELECT * FROM $tbl", null)
                    try {
                        val colNames = cursor.columnNames
                        val colCount = cursor.columnCount
                        while (cursor.moveToNext()) {
                            val row = JSONObject()
                            for (i in 0 until colCount) {
                                val colName = colNames[i]
                                when (cursor.getType(i)) {
                                    android.database.Cursor.FIELD_TYPE_INTEGER -> row.put(colName, cursor.getLong(i))
                                    android.database.Cursor.FIELD_TYPE_FLOAT -> row.put(colName, cursor.getDouble(i))
                                    android.database.Cursor.FIELD_TYPE_STRING -> row.put(colName, cursor.getString(i))
                                    android.database.Cursor.FIELD_TYPE_BLOB -> row.put(colName, "[BLOB]")
                                    else -> row.put(colName, JSONObject.NULL)
                                }
                            }
                            arr.put(row)
                        }
                    } finally {
                        cursor.close()
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Table $tbl query warning: ${e.message}")
                }
                tablesObj.put(tbl, arr)
            }
            root.put("tables", tablesObj)

            // Save to internal filesDir & Downloads/ShriBalajiKripaDham_Backups
            val internalFile = File(context.filesDir, "sbkd_vault_latest.json")
            internalFile.writeText(root.toString(2), Charsets.UTF_8)

            val backupDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "ShriBalajiKripaDham_Backups")
            if (!backupDir.exists()) backupDir.mkdirs()

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val publicFile = File(backupDir, "SBKD_Backup_${timestamp}.json")
            publicFile.writeText(root.toString(2), Charsets.UTF_8)

            // Also keep a permanent 'SBKD_Latest_Backup.json'
            val latestPublic = File(backupDir, "SBKD_Latest_Backup.json")
            latestPublic.writeText(root.toString(2), Charsets.UTF_8)

            publicFile
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate local vault dump: ${e.message}")
            null
        }
    }

    /**
     * 1-Click share/upload to Google Drive via Android system intent.
     */
    fun shareBackupToGoogleDrive(context: Context) {
        val file = generateLocalVaultDump(context) ?: return
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "🚩 श्री बालाजी कृपा धाम - संपूर्ण डेटा बैकअप")
                putExtra(Intent.EXTRA_TEXT, "जय श्री बालाजी! यह श्री बालाजी कृपा धाम (ग्राम डूँगरा जाट) का आधिकारिक डेटाबेस बैकअप है।")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                // Set package to Google Drive if available, otherwise open system chooser
                `package` = "com.google.android.apps.docs"
            }

            try {
                context.startActivity(sendIntent)
            } catch (ex: Exception) {
                // Fallback to general chooser
                val chooser = Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "🚩 श्री बालाजी कृपा धाम - संपूर्ण डेटा बैकअप")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }, "Google Drive या सुरक्षित स्थान पर सेव करें")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to share backup to Google Drive: ${e.message}")
        }
    }
}
