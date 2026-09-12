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
        // Standard Android & OEM camera intent extras for FRONT camera
        intent.putExtra("android.intent.extras.CAMERA_FACING", 1) // 1 = Front
        intent.putExtra("android.intent.extras.LENS_FACING_FRONT", 1)
        intent.putExtra("android.intent.extra.USE_FRONT_CAMERA", true)
        intent.putExtra("android.intent.extras.FRONT_CAMERA", true)
        intent.putExtra("camerafacing", "front")
        intent.putExtra("facing", "front")
        intent.putExtra("android.intent.extra.LENS_FACING", 1)
        intent.putExtra("front_camera", true)
        intent.putExtra("camerasensortype", 2) // 2 = Front on Xiaomi/MIUI
        intent.putExtra("oppo_camera_facing", 1) // 1 = Front on Oppo/Realme
        intent.putExtra("com.google.assistant.extra.USE_FRONT_CAMERA", true)
        intent.putExtra("com.android.camera.extra.facing", 1)
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
     * Loads a Bitmap from file path or content URI and ensures it is a software bitmap.
     */
    fun loadBitmap(context: Context, photoUri: String): Bitmap? {
        if (photoUri.isBlank()) return null
        return try {
            val loaded = if (photoUri.startsWith("content://") || photoUri.startsWith("android.resource://")) {
                val uri = Uri.parse(photoUri)
                val input: InputStream? = context.contentResolver.openInputStream(uri)
                input?.use { BitmapFactory.decodeStream(it) }
            } else {
                val path = if (photoUri.startsWith("file://")) photoUri.removePrefix("file://") else photoUri
                val file = File(path)
                if (file.exists()) {
                    BitmapFactory.decodeFile(file.absolutePath)
                } else null
            }
            if (loaded != null) toSoftwareBitmap(loaded) else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
