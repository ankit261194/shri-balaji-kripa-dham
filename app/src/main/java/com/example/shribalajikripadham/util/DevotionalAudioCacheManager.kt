package com.example.shribalajikripadham.util

import android.content.Context
import android.util.Log
import com.example.shribalajikripadham.data.sacred.SacredTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Devotional Audio Cache Manager (0.0s Instant Offline Audio Playback)
 *
 * Silently saves sacred tracks to device internal hidden storage (`.sacred_vani` with `.nomedia`)
 * so that devotees can listen to all authentic Aartis, Chalisas, and Bhajans with 0ms buffering.
 *
 * Hidden from phone gallery, Google Photos, and standard music players.
 * Self-healing: if files are cleared, they are automatically restored on next launch.
 */
object DevotionalAudioCacheManager {

    private const val TAG = "AudioCacheManager"
    private const val DIRECTORY_NAME = ".sacred_vani"
    private const val NOMEDIA_FILE = ".nomedia"
    private const val MIN_VALID_FILE_SIZE = 30000L // 30 KB minimum for valid audio

    fun getAudioDirectory(context: Context): File {
        val dir = File(context.filesDir, DIRECTORY_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        // Ensure .nomedia file exists to hide from Android MediaStore / Gallery
        try {
            val noMedia = File(dir, NOMEDIA_FILE)
            if (!noMedia.exists()) {
                noMedia.createNewFile()
            }
        } catch (ignored: Exception) {}
        return dir
    }

    fun getTrackFile(context: Context, trackKey: String): File {
        val safeKey = trackKey.lowercase().trim().replace("[^a-z0-9_]".toRegex(), "_")
        return File(getAudioDirectory(context), "sbkd_${safeKey}.mp3")
    }

    fun isTrackCached(context: Context, trackKey: String): Boolean {
        val file = getTrackFile(context, trackKey)
        return file.exists() && file.length() > MIN_VALID_FILE_SIZE
    }

    /**
     * Resolves the best playback source:
     * If cached offline in hidden storage -> returns local file path (0ms buffer).
     * Else -> returns remote stream URL.
     */
    fun getPlayableSource(context: Context, trackKey: String, remoteUrl: String): String {
        val file = getTrackFile(context, trackKey)
        if (file.exists() && file.length() > MIN_VALID_FILE_SIZE) {
            Log.d(TAG, "Playing track [$trackKey] from OFFLINE hidden storage: ${file.absolutePath}")
            return file.absolutePath
        }
        Log.d(TAG, "Playing track [$trackKey] from ONLINE stream: $remoteUrl")
        return remoteUrl
    }

    /**
     * Background self-healing sync: Silently verifies and downloads all sacred tracks
     * so that the user experiences zero audio buffering without filling external gallery.
     */
    suspend fun startSilentBackgroundSync(
        context: Context,
        tracks: List<SacredTrack> = emptyList()
    ) = withContext(Dispatchers.IO) {
        try {
            val audioDir = getAudioDirectory(context)
            Log.d(TAG, "Starting silent background audio pre-cache in: ${audioDir.absolutePath}")

            for (track in tracks) {
                if (track.audioUrl.isNotBlank() && !isTrackCached(context, track.trackKey)) {
                    Log.d(TAG, "Silently caching sacred track: ${track.trackKey} (${track.titleHindi})")
                    try {
                        downloadTrackForOffline(
                            context = context,
                            trackKey = track.trackKey,
                            audioUrl = track.audioUrl
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "Non-fatal: could not pre-cache ${track.trackKey}: ${e.message}")
                    }
                }
            }
            Log.d(TAG, "Silent audio pre-cache scan complete. Current cache size: ${getTotalCacheSizeFormatted(context)}")
        } catch (e: Exception) {
            Log.e(TAG, "Error in silent audio background sync: ${e.message}")
        }
    }

    /**
     * Downloads and permanently caches a track in the background with progress reporting.
     */
    suspend fun downloadTrackForOffline(
        context: Context,
        trackKey: String,
        audioUrl: String,
        onProgress: (Float) -> Unit = {}
    ): Boolean = withContext(Dispatchers.IO) {
        if (audioUrl.isBlank()) return@withContext false

        val targetFile = getTrackFile(context, trackKey)
        val tempFile = File(getAudioDirectory(context), "${trackKey}_temp.tmp")

        try {
            var currentUrl = audioUrl
            var connection: HttpURLConnection? = null
            var redirects = 0

            // Follow HTTP redirects safely (e.g. 301, 302 to CDN)
            while (redirects < 5) {
                val urlObj = URL(currentUrl)
                connection = (urlObj.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15000
                    readTimeout = 30000
                    instanceFollowRedirects = false
                    setRequestProperty("User-Agent", "ShriBalajiKripaDham-Android/2.54.0")
                }

                val status = connection.responseCode
                if (status == HttpURLConnection.HTTP_MOVED_TEMP ||
                    status == HttpURLConnection.HTTP_MOVED_PERM ||
                    status == 307 || status == 308) {
                    val newUrl = connection.getHeaderField("Location")
                    connection.disconnect()
                    if (newUrl != null && newUrl.isNotBlank()) {
                        currentUrl = newUrl
                        redirects++
                        continue
                    }
                }
                break
            }

            val finalConn = connection ?: return@withContext false
            if (finalConn.responseCode !in 200..299) {
                Log.e(TAG, "HTTP error downloading $trackKey: ${finalConn.responseCode}")
                finalConn.disconnect()
                return@withContext false
            }

            val totalBytes = finalConn.contentLengthLong
            val input = finalConn.inputStream
            val output = FileOutputStream(tempFile)

            val buffer = ByteArray(8192)
            var bytesRead: Int
            var downloadedBytes = 0L

            while (input.read(buffer).also { bytesRead = it } != -1) {
                output.write(buffer, 0, bytesRead)
                downloadedBytes += bytesRead
                if (totalBytes > 0) {
                    val progress = (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                    onProgress(progress)
                }
            }

            output.flush()
            output.close()
            input.close()
            finalConn.disconnect()

            if (tempFile.length() > MIN_VALID_FILE_SIZE) {
                if (targetFile.exists()) targetFile.delete()
                tempFile.renameTo(targetFile)
                Log.d(TAG, "Successfully cached $trackKey (${targetFile.length()} bytes)")
                onProgress(1f)
                return@withContext true
            } else {
                tempFile.delete()
                return@withContext false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed downloading track $trackKey: ${e.message}")
            try { tempFile.delete() } catch (ex: Exception) {}
            return@withContext false
        }
    }

    /**
     * Delete an offline cached track.
     */
    fun removeCachedTrack(context: Context, trackKey: String): Boolean {
        val file = getTrackFile(context, trackKey)
        return if (file.exists()) file.delete() else false
    }

    /**
     * Returns total size of all offline cached tracks formatted for display.
     */
    fun getTotalCacheSizeFormatted(context: Context): String {
        val files = getAudioDirectory(context).listFiles() ?: return "0 MB"
        val totalBytes = files.filter { it.name.endsWith(".mp3") }.sumOf { it.length() }
        val mb = totalBytes.toDouble() / (1024 * 1024)
        return String.format("%.1f MB", mb)
    }

    fun getCachedTrackCount(context: Context): Int {
        val files = getAudioDirectory(context).listFiles() ?: return 0
        return files.count { it.name.endsWith(".mp3") && it.length() > 1024 }
    }

    fun isFullyCached(context: Context, totalExpectedTracks: Int = 0): Boolean {
        if (totalExpectedTracks <= 0) return false
        return getCachedTrackCount(context) >= totalExpectedTracks
    }

    /**
     * Clear all cached tracks to free storage.
     */
    fun clearAllCache(context: Context) {
        val files = getAudioDirectory(context).listFiles() ?: return
        for (f in files) {
            if (f.name != NOMEDIA_FILE) {
                f.delete()
            }
        }
    }
}
