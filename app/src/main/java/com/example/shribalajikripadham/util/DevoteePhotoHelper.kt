package com.example.shribalajikripadham.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * Camera Contract to explicitly request the front-facing selfie camera.
 */
class TakeFrontPicturePreview : ActivityResultContracts.TakePicturePreview() {
    override fun createIntent(context: Context, input: Void?): Intent {
        val intent = super.createIntent(context, input)
        intent.putExtra("android.intent.extras.CAMERA_FACING", 1) // 1 = Front
        intent.putExtra("android.intent.extras.LENS_FACING_FRONT", 1)
        intent.putExtra("android.intent.extra.USE_FRONT_CAMERA", true)
        intent.putExtra("android.intent.extras.FRONT_CAMERA", true)
        intent.putExtra("com.google.assistant.extra.USE_FRONT_CAMERA", true)
        intent.putExtra("default_camera", "1")
        intent.putExtra("camerafacing", "front")
        intent.putExtra("camerasensortype", 2)
        return intent
    }
}

/**
 * Camera Contract for standard rear or front camera (e.g. for desk sevadars/admins).
 */
class TakeAnyPicturePreview : ActivityResultContracts.TakePicturePreview()

object DevoteePhotoHelper {

    /**
     * Saves captured camera bitmap securely in app's internal storage.
     * Returns the absolute file path (e.g. /data/user/0/.../devotee_photos/photo_12345.jpg).
     */
    fun saveDevoteePhoto(context: Context, bitmap: Bitmap, prefix: String = "token_photo"): String {
        return try {
            val photosDir = File(context.filesDir, "devotee_photos")
            if (!photosDir.exists()) {
                photosDir.mkdirs()
            }
            val fileName = "${prefix}_${System.currentTimeMillis()}_${(100..999).random()}.jpg"
            val file = File(photosDir, fileName)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    /**
     * Loads a Bitmap from file path or content URI.
     */
    fun loadBitmap(context: Context, photoUri: String): Bitmap? {
        if (photoUri.isBlank()) return null
        return try {
            if (photoUri.startsWith("content://") || photoUri.startsWith("android.resource://")) {
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
        } catch (e: Exception) {
            null
        }
    }
}
