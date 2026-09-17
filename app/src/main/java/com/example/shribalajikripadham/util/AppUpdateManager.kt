package com.example.shribalajikripadham.util

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import org.json.JSONObject

object AppUpdateManager {

    const val DEFAULT_APK_URL = "https://github.com/ankit261194/shri-balaji-kripa-dham/releases/download/v2.43.0/ShriBalajiKripaDham-v2.43.0.apk"
    const val DEFAULT_VERSION_JSON_URL = "https://raw.githubusercontent.com/ankit261194/shri-balaji-kripa-dham/main/version.json"

    data class OnlineUpdateInfo(
        val versionCode: Int,
        val versionName: String,
        val updateNotesHindi: String,
        val updateNotesEnglish: String,
        val apkUrl: String,
        val isForce: Boolean,
        val webhookUrl: String = ""
    )

    suspend fun fetchLatestUpdateFromOnline(urlStr: String = DEFAULT_VERSION_JSON_URL): OnlineUpdateInfo? {
        return withContext(Dispatchers.IO) {
            val fromVersionJson = fetchFromVersionJson(urlStr)
            val fromAppUpdateJson = fetchFromVersionJson("https://raw.githubusercontent.com/ankit261194/shri-balaji-kripa-dham/main/app_update.json")
            val fromGitHub = fetchFromGitHubReleasesApi()
            listOfNotNull(fromVersionJson, fromAppUpdateJson, fromGitHub).maxByOrNull { it.versionCode }
        }
    }

    private fun fetchFromVersionJson(baseUrl: String): OnlineUpdateInfo? {
        return try {
            val delimiter = if (baseUrl.contains("?")) "&" else "?"
            val cacheBusterUrl = "$baseUrl${delimiter}nocache=${System.currentTimeMillis()}&rand=${(1000..9999).random()}"
            val url = URL(cacheBusterUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            conn.useCaches = false
            conn.defaultUseCaches = false
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "BalajiApp-UpdateCheck/2.28")
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate")
            conn.setRequestProperty("Pragma", "no-cache")
            conn.setRequestProperty("Expires", "0")

            if (conn.responseCode in 200..299) {
                val response = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                conn.disconnect()
                val json = JSONObject(response)
                val parsedApkUrl = when {
                    json.has("apk_url") && json.optString("apk_url").isNotBlank() -> json.optString("apk_url")
                    json.has("apk_download_url") && json.optString("apk_download_url").isNotBlank() -> json.optString("apk_download_url")
                    else -> DEFAULT_APK_URL
                }
                val parsedCode = when {
                    json.has("latest_version_code") -> json.optInt("latest_version_code", 1)
                    json.has("version_code") -> json.optInt("version_code", 1)
                    else -> 1
                }
                val parsedName = when {
                    json.has("latest_version_name") -> json.optString("latest_version_name", "1.0")
                    json.has("version_name") -> json.optString("version_name", "1.0")
                    else -> "1.0"
                }
                val parsedForce = when {
                    json.has("is_force_update") -> json.optBoolean("is_force_update", false)
                    json.has("is_force") -> json.optBoolean("is_force", false)
                    else -> false
                }
                OnlineUpdateInfo(
                    versionCode = parsedCode,
                    versionName = parsedName,
                    updateNotesHindi = json.optString("update_notes_hindi", ""),
                    updateNotesEnglish = json.optString("update_notes_english", ""),
                    apkUrl = parsedApkUrl,
                    isForce = parsedForce,
                    webhookUrl = json.optString("webhook_url", "")
                )
            } else {
                conn.disconnect()
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun fetchFromGitHubReleasesApi(): OnlineUpdateInfo? {
        return try {
            val url = URL("https://api.github.com/repos/ankit261194/shri-balaji-kripa-dham/releases/latest?nocache=${System.currentTimeMillis()}")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            conn.useCaches = false
            conn.defaultUseCaches = false
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "BalajiApp-UpdateCheck/2.28")
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
            conn.setRequestProperty("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate")
            conn.setRequestProperty("Pragma", "no-cache")

            if (conn.responseCode in 200..299) {
                val response = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                conn.disconnect()
                val json = JSONObject(response)
                val tagName = json.optString("tag_name", "")
                val name = json.optString("name", "")
                val body = json.optString("body", "")

                var apkDownloadUrl = ""
                val assets = json.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val assetName = asset.optString("name", "")
                        if (assetName.endsWith(".apk", ignoreCase = true)) {
                            apkDownloadUrl = asset.optString("browser_download_url", "")
                            if (assetName.contains("release", ignoreCase = true) || assetName.contains("v2.", ignoreCase = true)) {
                                break
                            }
                        }
                    }
                }
                if (apkDownloadUrl.isBlank()) {
                    apkDownloadUrl = DEFAULT_APK_URL
                }

                // Extract build number from release title or body e.g. "Build #35" or "(Build #35)"
                val combinedText = "$name $body $tagName"
                val buildMatch = Regex("""Build\s*#?(\d+)""", RegexOption.IGNORE_CASE).find(combinedText)
                val parsedCode = buildMatch?.groupValues?.get(1)?.toIntOrNull() ?: 1
                val verClean = tagName.removePrefix("v").trim()

                // Keep release notes concise as a clean security patch notice so the update popup never overflows
                val conciseHindiNotes = if (body.isNotBlank() && body.length <= 120 && !body.contains("\n\n")) {
                    body.trim()
                } else {
                    "सुरक्षा पैच एवं सिस्टम स्थिरता सुधार (Security Patch Update)"
                }
                val conciseEnglishNotes = "Security patch & critical stability improvements"

                OnlineUpdateInfo(
                    versionCode = parsedCode,
                    versionName = verClean.ifEmpty { "2.34.0" },
                    updateNotesHindi = conciseHindiNotes,
                    updateNotesEnglish = conciseEnglishNotes,
                    apkUrl = apkDownloadUrl,
                    isForce = true,
                    webhookUrl = ""
                )
            } else {
                conn.disconnect()
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun getCurrentVersionCode(context: Context): Int {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode
            }
        } catch (e: Exception) {
            1
        }
    }

    fun getCurrentVersionName(context: Context): String {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            packageInfo.versionName ?: "1.0"
        } catch (e: Exception) {
            "1.0"
        }
    }

    fun isUpdateAvailable(currentVersionCode: Int, latestVersionCode: Int): Boolean {
        return latestVersionCode > currentVersionCode
    }

    /**
     * Checks if Unknown Sources install permission is granted on Android 8.0+
     */
    fun hasInstallPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    /**
     * Prompts user to enable "Install Unknown Apps" permission for this app.
     */
    fun requestInstallPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val intent = Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                ).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "अनुमति सेटिंग खोलने में असमर्थ", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Directly triggers the Android Package Installer for the given APK file.
     */
    fun triggerApkInstall(context: Context, file: File) {
        try {
            if (!file.exists()) {
                Toast.makeText(context, "APK फ़ाइल नहीं मिली (APK file not found)", Toast.LENGTH_SHORT).show()
                return
            }

            // Ensure the file is readable by the Android OS Package Installer
            file.setReadable(true, false)

            // Validate that the file is a complete, uncorrupted APK archive before launching installer
            val packageInfo = try {
                context.packageManager.getPackageArchiveInfo(file.absolutePath, 0)
            } catch (e: Exception) {
                null
            }

            if (packageInfo == null) {
                Toast.makeText(
                    context,
                    "APK फ़ाइल अमान्य या अधूरी है, कृपया पुनः डाउनलोड करें (Incomplete or corrupt APK file)",
                    Toast.LENGTH_LONG
                ).show()
                try { file.delete() } catch (_: Exception) {}
                return
            }

            if (!hasInstallPermission(context)) {
                requestInstallPermission(context)
                Toast.makeText(
                    context,
                    "कृपया ऐप अपडेट करने के लिए 'Allow from this source' अनुमति चालू करें",
                    Toast.LENGTH_LONG
                ).show()
                return
            }

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
            }

            // Explicitly grant URI read permissions to all intent handlers and system package installers
            val resolveInfoList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.queryIntentActivities(
                    intent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            }

            for (resolveInfo in resolveInfoList) {
                val targetPkg = resolveInfo.activityInfo.packageName
                try {
                    context.grantUriPermission(targetPkg, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (_: Exception) {}
            }

            listOf("com.google.android.packageinstaller", "com.android.packageinstaller").forEach { pkg ->
                try {
                    context.grantUriPermission(pkg, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (_: Exception) {}
            }

            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "इन्स्टॉल त्रुटि: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Downloads APK directly inside the app with real-time percentage progress callback,
     * and automatically triggers the package installer upon completion.
     */
    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0.0 MB"
        val mb = bytes.toDouble() / (1024.0 * 1024.0)
        return String.format(Locale.getDefault(), "%.1f MB", mb)
    }

    /**
     * Downloads APK directly inside the app with real-time percentage and byte progress,
     * following redirects securely, without any external browser redirection.
     */
    suspend fun startInAppUpdateDetailed(
        context: Context,
        downloadUrl: String,
        onProgress: (percent: Int, downloadedBytes: Long, totalBytes: Long) -> Unit,
        onSuccess: (File) -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        val finalUrl = if (downloadUrl.isNotBlank()) downloadUrl.trim() else DEFAULT_APK_URL

        // Choose accessible external files directory so system Package Installer can read the file
        val targetDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: context.externalCacheDir
            ?: context.cacheDir

        // Check if pre-existing release APK is available locally in Downloads folder
        try {
            val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val localCandidate = File(publicDownloads, "ShriBalajiKripaDham-release.apk")
            if (localCandidate.exists() && localCandidate.length() > 5000000L && downloadUrl.isBlank()) {
                val valid = context.packageManager.getPackageArchiveInfo(localCandidate.absolutePath, 0) != null
                if (valid) {
                    withContext(Dispatchers.Main) {
                        onProgress(100, localCandidate.length(), localCandidate.length())
                        onSuccess(localCandidate)
                    }
                    return@withContext
                }
            }
        } catch (_: Exception) {}

        try {
            // Clean up any old cached APK files
            try {
                targetDir.listFiles()?.forEach { f ->
                    if (f.name.endsWith(".apk", ignoreCase = true)) {
                        f.delete()
                    }
                }
                context.cacheDir.listFiles()?.forEach { f ->
                    if (f.name.endsWith(".apk", ignoreCase = true)) {
                        f.delete()
                    }
                }
            } catch (_: Exception) {}

            val targetFile = File(targetDir, "ShriBalajiKripaDham_update.apk")
            val candidateUrls = mutableListOf<String>()
            if (finalUrl.isNotBlank()) candidateUrls.add(finalUrl.trim())
            val fallbacks = listOf(
                "https://github.com/ankit261194/shri-balaji-kripa-dham/releases/download/v2.43.0/ShriBalajiKripaDham-v2.43.0.apk",
                "https://github.com/ankit261194/shri-balaji-kripa-dham/releases/download/v2.43.0/ShriBalajiKripaDham-release.apk",
                "https://github.com/ankit261194/shri-balaji-kripa-dham/releases/download/v2.42.1/ShriBalajiKripaDham-v2.42.1.apk",
                "https://shribalajikripadham.online/download.php",
                DEFAULT_APK_URL
            )
            for (fb in fallbacks) {
                if (!candidateUrls.contains(fb)) {
                    candidateUrls.add(fb)
                }
            }

            var lastError: Exception? = null
            var downloadSuccess = false

            for (tryUrl in candidateUrls) {
                try {
                    if (targetFile.exists()) {
                        targetFile.delete()
                    }

                    var currentUrl = tryUrl
                    var connection: HttpURLConnection? = null
                    var redirects = 0
                    val maxRedirects = 6

                    while (redirects < maxRedirects) {
                        val urlObj = URL(currentUrl)
                        connection = (urlObj.openConnection() as HttpURLConnection).apply {
                            connectTimeout = 15000
                            readTimeout = 30000
                            useCaches = false
                            defaultUseCaches = false
                            instanceFollowRedirects = true
                            requestMethod = "GET"
                            setRequestProperty("User-Agent", "ShriBalajiKripaDham-Updater/2.43.0")
                            setRequestProperty("Accept-Encoding", "identity")
                            setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
                        }
                        connection.connect()

                        val code = connection.responseCode
                        if (code == HttpURLConnection.HTTP_MOVED_PERM ||
                            code == HttpURLConnection.HTTP_MOVED_TEMP ||
                            code == HttpURLConnection.HTTP_SEE_OTHER ||
                            code == 307 || code == 308
                        ) {
                            val location = connection.getHeaderField("Location")
                            connection.disconnect()
                            if (location != null) {
                                currentUrl = location
                                redirects++
                                continue
                            }
                        }
                        break
                    }

                    val conn = connection ?: throw Exception("कनेक्शन स्थापित करने में असमर्थ (Connection failed)")
                    val responseCode = conn.responseCode
                    if (responseCode !in 200..299) {
                        conn.disconnect()
                        throw Exception("HTTP $responseCode")
                    }

                    val totalBytes = conn.contentLength.toLong()
                    val inputStream = BufferedInputStream(conn.inputStream)
                    val outputStream = FileOutputStream(targetFile)

                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var downloadedBytes: Long = 0
                    var lastReportedPercent = -1

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        if (totalBytes > 0) {
                            val percent = ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100)
                            if (percent != lastReportedPercent || bytesRead == -1) {
                                lastReportedPercent = percent
                                withContext(Dispatchers.Main) {
                                    onProgress(percent, downloadedBytes, totalBytes)
                                }
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                onProgress(50, downloadedBytes, 0L)
                            }
                        }
                    }

                    outputStream.flush()
                    outputStream.close()
                    inputStream.close()
                    conn.disconnect()

                    // Verify download completeness and validity
                    if (totalBytes > 0 && downloadedBytes < totalBytes) {
                        targetFile.delete()
                        throw Exception("डाउनलोड अधूरा रह गया (${formatFileSize(downloadedBytes)} / ${formatFileSize(totalBytes)})")
                    }

                    if (targetFile.length() < 5000000L) {
                        targetFile.delete()
                        throw Exception("अमान्य APK फ़ाइल आकार (${formatFileSize(targetFile.length())})")
                    }

                    targetFile.setReadable(true, false)

                    // Validate that the package is intact
                    val packageArchive = context.packageManager.getPackageArchiveInfo(targetFile.absolutePath, 0)
                    if (packageArchive == null) {
                        targetFile.delete()
                        throw Exception("डाउनलोड की गई APK पैकेज अमान्य या दूषित है (Invalid APK package)")
                    }

                    withContext(Dispatchers.Main) {
                        onProgress(100, targetFile.length(), targetFile.length())
                        onSuccess(targetFile)
                    }
                    downloadSuccess = true
                    break
                } catch (e: Exception) {
                    lastError = e
                }
            }

            if (!downloadSuccess) {
                throw (lastError ?: Exception("अपडेट डाउनलोड में असमर्थ"))
            }
        } catch (e: Exception) {
            // Local fallback check
            var resolvedLocal = false
            try {
                val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val localCandidate = File(publicDownloads, "ShriBalajiKripaDham-release.apk")
                if (localCandidate.exists() && localCandidate.length() > 5000000L) {
                    val valid = context.packageManager.getPackageArchiveInfo(localCandidate.absolutePath, 0) != null
                    if (valid) {
                        resolvedLocal = true
                        withContext(Dispatchers.Main) {
                            onProgress(100, localCandidate.length(), localCandidate.length())
                            onSuccess(localCandidate)
                        }
                    }
                }
            } catch (_: Exception) {}

            if (!resolvedLocal) {
                withContext(Dispatchers.Main) {
                    onError(e.localizedMessage ?: "नेटवर्क डाउनलोड त्रुटि (Network Download Error)")
                }
            }
        }
    }

    /**
     * Opens the direct APK download link in external system browser (Chrome/default browser)
     * as a 100% reliable alternative when phone settings block in-app package installations.
     */
    fun openInBrowser(context: Context, downloadUrl: String) {
        try {
            val finalUrl = if (downloadUrl.isNotBlank()) downloadUrl.trim() else DEFAULT_APK_URL
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "ब्राउज़र खोलने में असमर्थ: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Backward-compatible overload for startInAppUpdate.
     */
    suspend fun startInAppUpdate(
        context: Context,
        downloadUrl: String,
        onProgress: (Int) -> Unit,
        onSuccess: (File) -> Unit,
        onError: (String) -> Unit
    ) {
        startInAppUpdateDetailed(
            context = context,
            downloadUrl = downloadUrl,
            onProgress = { percent, _, _ -> onProgress(percent) },
            onSuccess = onSuccess,
            onError = onError
        )
    }

    /**
     * Fallback download via Android DownloadManager or Web Browser.
     */
    fun downloadAndInstallUpdate(context: Context, downloadUrl: String) {
        val finalUrl = if (downloadUrl.isNotBlank()) downloadUrl.trim() else DEFAULT_APK_URL

        try {
            val uri = Uri.parse(finalUrl)
            if (finalUrl.endsWith(".apk", ignoreCase = true)) {
                val request = DownloadManager.Request(uri).apply {
                    setTitle("Shri Balaji Kripa Dham Update")
                    setDescription("Downloading latest version...")
                    setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "ShriBalajiKripaDham_update.apk")
                    setAllowedOverMetered(true)
                    setAllowedOverRoaming(true)
                }

                val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                val downloadId = downloadManager.enqueue(request)

                val onComplete = object : BroadcastReceiver() {
                    override fun onReceive(ctxt: Context, intent: Intent) {
                        val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                        if (id == downloadId) {
                            val downloadedFile = File(
                                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                                "ShriBalajiKripaDham_update.apk"
                            )
                            if (downloadedFile.exists()) {
                                triggerApkInstall(context, downloadedFile)
                            }
                            try {
                                context.unregisterReceiver(this)
                            } catch (_: Exception) {}
                        }
                    }
                }
                val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.registerReceiver(onComplete, filter, Context.RECEIVER_EXPORTED)
                } else {
                    context.registerReceiver(onComplete, filter)
                }
                Toast.makeText(context, "डाउनलोड प्रारंभ हो गया है... (Download started)", Toast.LENGTH_SHORT).show()
            } else {
                val browserIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(browserIntent)
            }
        } catch (e: Exception) {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(browserIntent)
        }
    }
}
