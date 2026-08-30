package com.k.hosken.navipulse.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream

/**
 * Persists a user-picked dashboard background image. The source picture is downscaled and
 * center-cropped to the header panel's aspect ratio so it's never stored (or drawn) larger
 * than it needs to be.
 */
object BackgroundImageManager {
    private const val FILE_NAME = "dashboard_background.jpg"

    // Matches the wide, short shape of the dashboard's header panel.
    private const val TARGET_WIDTH = 1080
    private const val TARGET_HEIGHT = 480

    fun backgroundFile(context: Context): File = File(context.filesDir, FILE_NAME)

    /** Downscales, center-crops, and saves [uri] as the dashboard background. Returns the saved file path, or null on failure. */
    fun saveResizedBackground(context: Context, uri: Uri): String? {
        return try {
            val sampled = decodeSampledBitmap(context, uri, TARGET_WIDTH, TARGET_HEIGHT) ?: return null
            val cropped = centerCrop(sampled, TARGET_WIDTH, TARGET_HEIGHT)

            val file = backgroundFile(context)
            FileOutputStream(file).use { out ->
                cropped.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }

            if (cropped !== sampled) sampled.recycle()
            cropped.recycle()

            file.absolutePath
        } catch (e: Exception) {
            Log.e("BackgroundImageManager", "Failed to save background image from $uri", e)
            null
        }
    }

    fun deleteBackground(context: Context) {
        backgroundFile(context).delete()
    }

    // Read the picked image once into memory and decode from those bytes rather than opening the
    // content:// stream twice (once for bounds, once for the full decode) - some providers (cloud
    // photo backends, other picker sources) only support being read once per open, and would
    // otherwise fail the second read and silently produce no background image.
    private fun decodeSampledBitmap(context: Context, uri: Uri, reqWidth: Int, reqHeight: Int): Bitmap? {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(bounds, reqWidth, reqHeight)
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun centerCrop(source: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        val sourceRatio = source.width.toFloat() / source.height.toFloat()
        val targetRatio = targetWidth.toFloat() / targetHeight.toFloat()

        val scaledWidth: Int
        val scaledHeight: Int
        if (sourceRatio > targetRatio) {
            scaledHeight = targetHeight
            scaledWidth = (targetHeight * sourceRatio).toInt().coerceAtLeast(targetWidth)
        } else {
            scaledWidth = targetWidth
            scaledHeight = (targetWidth / sourceRatio).toInt().coerceAtLeast(targetHeight)
        }

        val scaled = Bitmap.createScaledBitmap(source, scaledWidth, scaledHeight, true)
        val x = ((scaledWidth - targetWidth) / 2).coerceAtLeast(0)
        val y = ((scaledHeight - targetHeight) / 2).coerceAtLeast(0)
        val cropped = Bitmap.createBitmap(scaled, x, y, targetWidth, targetHeight)

        if (scaled !== cropped && scaled !== source) scaled.recycle()
        return cropped
    }
}
