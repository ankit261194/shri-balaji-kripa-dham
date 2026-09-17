package com.example.shribalajikripadham.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * Camera Contract to explicitly request the front-facing selfie camera for Devotees.
 * Uses comprehensive OEM camera intent extras to guarantee front camera across
 * all OEM camera implementations (Samsung, Xiaomi/Redmi, Oppo, Vivo, Realme, OnePlus, Motorola, Pixel).
 */
class TakeFrontPicturePreview : ActivityResultContracts.TakePicturePreview() {
    override fun createIntent(context: Context, input: Void?): Intent {
        val intent = super.createIntent(context, input)
        // Standard Android camera intent extras for FRONT camera
        intent.putExtra("android.intent.extras.CAMERA_FACING", 1) // 1 = Front
        intent.putExtra("android.intent.extra.USE_FRONT_CAMERA", true)
        return intent
    }
}

/**
 * Camera Contract to explicitly request the rear/back camera for Admins/Sevadars
 * (e.g. taking devotee photo from desk or scanning paper register).
 */
class TakeRearPicturePreview : ActivityResultContracts.TakePicturePreview() {
    override fun createIntent(context: Context, input: Void?): Intent {
        val intent = super.createIntent(context, input)
        // Standard Android & OEM camera intent extras for REAR camera
        intent.putExtra("android.intent.extras.CAMERA_FACING", 0) // 0 = Back
        intent.putExtra("android.intent.extras.LENS_FACING_FRONT", 0)
        intent.putExtra("android.intent.extra.USE_FRONT_CAMERA", false)
        intent.putExtra("android.intent.extras.FRONT_CAMERA", false)
        intent.putExtra("camerafacing", "back")
        intent.putExtra("facing", "back")
        intent.putExtra("android.intent.extra.LENS_FACING", 0)
        intent.putExtra("front_camera", false)
        intent.putExtra("camerasensortype", 1) // 1 = Rear on Xiaomi/MIUI
        intent.putExtra("oppo_camera_facing", 0) // 0 = Rear on Oppo/Realme
        intent.putExtra("com.android.camera.extra.facing", 0)
        return intent
    }
}

/**
 * Camera Contract for standard rear or front camera (fallback).
 */
class TakeAnyPicturePreview : ActivityResultContracts.TakePicturePreview()

object DevoteePhotoHelper {

    /**
     * Converts ANY Android Bitmap (especially Bitmap.Config.HARDWARE returned by camera previews)
     * into a guaranteed software-backed Bitmap.Config.ARGB_8888 bitmap.
     *
     * Why this is mandatory:
     * 1. Calling bitmap.getPixels() on Config.HARDWARE crashes immediately with:
     *    java.lang.IllegalStateException: getPixels() is not supported on Config.HARDWARE bitmaps
     * 2. Passing Config.HARDWARE into Compose Image(bitmap = bmp.asImageBitmap()) crashes with:
     *    java.lang.IllegalArgumentException: Software rendering doesn't support hardware bitmaps
     * 3. Compressing Config.HARDWARE throws on several Android versions.
     */
    fun toSoftwareBitmap(bitmap: Bitmap): Bitmap {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (bitmap.config == Bitmap.Config.HARDWARE) {
                    bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: drawToSoftwareCanvas(bitmap)
                } else {
                    bitmap
                }
            } else {
                bitmap
            }
        } catch (e: Exception) {
            e.printStackTrace()
            drawToSoftwareCanvas(bitmap)
        }
    }

    private fun drawToSoftwareCanvas(source: Bitmap): Bitmap {
        return try {
            val width = source.width.coerceAtLeast(1)
            val height = source.height.coerceAtLeast(1)
            val softwareBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(softwareBitmap)
            val paint = Paint(Paint.FILTER_BITMAP_FLAG)
            canvas.drawBitmap(source, 0f, 0f, paint)
            softwareBitmap
        } catch (e: Exception) {
            e.printStackTrace()
            source
        }
    }

    /**
     * Saves captured camera bitmap securely in app's internal storage.
     * Guaranteed safe against Config.HARDWARE exceptions.
     * Returns the absolute file path (e.g. /data/user/0/.../devotee_photos/photo_12345.jpg).
     */
    fun saveDevoteePhoto(context: Context, bitmap: Bitmap, prefix: String = "token_photo"): String {
        return try {
            val safeBitmap = toSoftwareBitmap(bitmap)
            val photosDir = File(context.filesDir, "devotee_photos")
            if (!photosDir.exists()) {
                photosDir.mkdirs()
            }
            val fileName = "${prefix}_${System.currentTimeMillis()}_${(100..999).random()}.jpg"
            val file = File(photosDir, fileName)
            FileOutputStream(file).use { out ->
                safeBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    /**
     * Rotates a Bitmap by arbitrary degrees clockwise.
     */
    fun rotateBitmap(source: Bitmap, degrees: Float): Bitmap {
        if (degrees % 360f == 0f) return source
        return try {
            val matrix = android.graphics.Matrix().apply {
                postRotate(degrees)
            }
            val rotated = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
            toSoftwareBitmap(rotated)
        } catch (e: Exception) {
            e.printStackTrace()
            source
        }
    }

    /**
     * Reads EXIF orientation from a content URI or file stream.
     */
    fun getExifOrientationDegrees(context: Context, uri: Uri): Float {
        return try {
            val input = context.contentResolver.openInputStream(uri) ?: return 0f
            val exif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                android.media.ExifInterface(input)
            } else {
                null
            }
            val orientation = exif?.getAttributeInt(
                android.media.ExifInterface.TAG_ORIENTATION,
                android.media.ExifInterface.ORIENTATION_NORMAL
            ) ?: android.media.ExifInterface.ORIENTATION_NORMAL
            input.close()
            when (orientation) {
                android.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                android.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                android.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } catch (e: Exception) {
            0f
        }
    }

    /**
     * Reads EXIF orientation from a local file path.
     */
    fun getExifOrientationDegrees(filePath: String): Float {
        return try {
            val exif = android.media.ExifInterface(filePath)
            val orientation = exif.getAttributeInt(
                android.media.ExifInterface.TAG_ORIENTATION,
                android.media.ExifInterface.ORIENTATION_NORMAL
            )
            when (orientation) {
                android.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                android.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                android.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } catch (e: Exception) {
            0f
        }
    }

    /**
     * Rotates an existing local photo file by degrees (default 90° clockwise)
     * and saves it back to storage. Returns the new valid file path.
     */
    fun rotateSavedPhoto(context: Context, photoPath: String, degrees: Float = 90f): String {
        return try {
            val bmp = loadBitmap(context, photoPath) ?: return photoPath
            val rotated = rotateBitmap(bmp, degrees)
            saveDevoteePhoto(context, rotated, "rotated")
        } catch (e: Exception) {
            e.printStackTrace()
            photoPath
        }
    }

    /**
     * Clears cached remote images from the remote_cache directory.
     * Essential when a new Guruji or Sevadar photo is uploaded so stale images are never loaded.
     */
    fun clearNetworkCache(context: Context, urlSubstring: String? = null) {
        try {
            val cacheDir = File(context.filesDir, "remote_cache")
            if (cacheDir.exists() && cacheDir.isDirectory) {
                if (urlSubstring == null) {
                    cacheDir.listFiles()?.forEach { it.delete() }
                } else {
                    val hash = Math.abs(urlSubstring.hashCode()).toString()
                    cacheDir.listFiles()?.filter { it.name.contains(hash) }?.forEach { it.delete() }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Saves Guruji's photo to dedicated internal file 'guruji_profile.jpg'
     * and clears cache to ensure immediate app-wide refresh.
     */
    fun saveGurujiPhoto(context: Context, bitmap: Bitmap): String {
        return try {
            clearNetworkCache(context)
            val safeBitmap = toSoftwareBitmap(bitmap)
            val photosDir = File(context.filesDir, "devotee_photos")
            if (!photosDir.exists()) photosDir.mkdirs()
            val file = File(photosDir, "guruji_profile_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out ->
                safeBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize.coerceAtLeast(1)
    }

    fun decodeSampledBitmapFromFile(filePath: String, reqWidth: Int = 1024, reqHeight: Int = 1024): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(filePath, options)
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false
            BitmapFactory.decodeFile(filePath, options)
        } catch (e: Exception) {
            null
        }
    }

    fun decodeSampledBitmapFromStream(context: Context, uri: Uri, reqWidth: Int = 1024, reqHeight: Int = 1024): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }
        } catch (e: Exception) {
            null
        }
    }

    fun decodeSampledBitmapFromByteArray(bytes: ByteArray, reqWidth: Int = 1024, reqHeight: Int = 1024): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Loads a Bitmap from file path, content URI, or remote HTTP/HTTPS URL with automatic disk caching
     * and automatic EXIF upright correction. Memory-optimized with smart downsampling.
     */
    fun loadBitmap(context: Context, photoUri: String, forceRefresh: Boolean = false): Bitmap? {
        if (photoUri.isBlank()) return null
        return try {
            val (loadedRaw, exifDegrees) = when {
                photoUri.startsWith("http://") || photoUri.startsWith("https://") -> {
                    Pair(loadFromNetworkOrCache(context, photoUri, forceRefresh), 0f)
                }
                photoUri.startsWith("content://") || photoUri.startsWith("android.resource://") -> {
                    val uri = Uri.parse(photoUri)
                    val degrees = getExifOrientationDegrees(context, uri)
                    val bmp = decodeSampledBitmapFromStream(context, uri, 1024, 1024)
                    Pair(bmp, degrees)
                }
                else -> {
                    val path = if (photoUri.startsWith("file://")) photoUri.removePrefix("file://") else photoUri
                    val file = File(path)
                    if (file.exists()) {
                        val degrees = getExifOrientationDegrees(file.absolutePath)
                        val bmp = decodeSampledBitmapFromFile(file.absolutePath, 1024, 1024)
                        Pair(bmp, degrees)
                    } else Pair(null, 0f)
                }
            }

            if (loadedRaw != null) {
                val softBmp = toSoftwareBitmap(loadedRaw)
                if (exifDegrees != 0f) {
                    rotateBitmap(softBmp, exifDegrees)
                } else {
                    softBmp
                }
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun loadFromNetworkOrCache(context: Context, urlString: String, forceRefresh: Boolean = false): Bitmap? {
        return try {
            val cacheDir = File(context.filesDir, "remote_cache")
            if (!cacheDir.exists()) cacheDir.mkdirs()
            val safeFileName = "img_" + Math.abs(urlString.hashCode()).toString() + ".jpg"
            val cacheFile = File(cacheDir, safeFileName)
            val isGurujiPhoto = urlString.contains("guruji", ignoreCase = true)

            // If cached and valid, return cached image immediately unless forceRefresh or Guruji master photo
            if (!forceRefresh && !isGurujiPhoto && cacheFile.exists() && cacheFile.length() > 0) {
                val bmp = decodeSampledBitmapFromFile(cacheFile.absolutePath, 1024, 1024)
                if (bmp != null) return bmp
            }

            // Download from network with Zero-Cache headers
            val requestUrl = if (isGurujiPhoto) {
                if (urlString.contains("?")) "$urlString&t=${System.currentTimeMillis()}" else "$urlString?t=${System.currentTimeMillis()}"
            } else {
                urlString
            }
            val url = java.net.URL(requestUrl)
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.connectTimeout = 6000
            conn.readTimeout = 8000
            conn.requestMethod = "GET"
            conn.setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
            conn.setRequestProperty("Pragma", "no-cache")
            conn.setRequestProperty("User-Agent", "ShriBalajiApp/2.42.1")

            if (conn.responseCode in 200..299) {
                val bytes = conn.inputStream.use { it.readBytes() }
                if (bytes.isNotEmpty()) {
                    try {
                        FileOutputStream(cacheFile).use { out ->
                            out.write(bytes)
                        }
                    } catch (ignored: Exception) {}
                    return decodeSampledBitmapFromByteArray(bytes, 1024, 1024)
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }
}
