package com.nova.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import androidx.exifinterface.media.ExifInterface
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/** Removes the background of a photo on-device (ML Kit Subject Segmentation) and saves a transparent PNG. */
object CutOut {
    private val segmenter by lazy {
        SubjectSegmentation.getClient(
            SubjectSegmenterOptions.Builder().enableForegroundBitmap().build()
        )
    }

    /**
     * Returns the path of the new PNG, or null if segmentation failed.
     * outline = true adds a white sticker border and saves as "_sticker.png"; otherwise "_cut.png".
     */
    suspend fun run(srcPath: String, outline: Boolean = false): String? = withContext(Dispatchers.Default) {
        try {
            val bmp = decode(srcPath, 1536) ?: return@withContext null
            val fg: Bitmap? = suspendCancellableCoroutine { cont ->
                segmenter.process(InputImage.fromBitmap(bmp, 0))
                    .addOnSuccessListener { if (cont.isActive) cont.resume(it.foregroundBitmap) }
                    .addOnFailureListener { if (cont.isActive) cont.resume(null) }
            }
            if (fg == null) return@withContext null
            val result = if (outline) addOutline(fg) else fg
            val suffix = if (outline) "_sticker.png" else "_cut.png"
            val out = File(srcPath.substringBeforeLast('.') + suffix)
            FileOutputStream(out).use { result.compress(Bitmap.CompressFormat.PNG, 100, it) }
            out.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    /** White "sticker" border: draw a white silhouette at many offsets, then the subject on top. */
    private fun addOutline(fg: Bitmap): Bitmap {
        val r = (max(fg.width, fg.height) * 0.012f).coerceAtLeast(4f)
        val pad = (r + 3f).toInt()
        val out = Bitmap.createBitmap(fg.width + pad * 2, fg.height + pad * 2, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        val white = Paint(Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(
                ColorMatrix(floatArrayOf(0f,0f,0f,0f,255f, 0f,0f,0f,0f,255f, 0f,0f,0f,0f,255f, 0f,0f,0f,1f,0f))
            )
        }
        for (ring in listOf(r, r * 0.5f)) {
            for (k in 0 until 24) {
                val a = k * 2.0 * PI / 24.0
                c.drawBitmap(fg, pad + (cos(a) * ring).toFloat(), pad + (sin(a) * ring).toFloat(), white)
            }
        }
        c.drawBitmap(fg, pad.toFloat(), pad.toFloat(), null)
        return out
    }

    /** Decodes with downsampling and applies EXIF rotation. */
    fun decode(path: String, maxDim: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / sample > maxDim) sample *= 2
        val bmp = BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: return null
        val rot = try {
            when (ExifInterface(path).getAttributeInt(ExifInterface.TAG_ORIENTATION, 1)) {
                6 -> 90f; 3 -> 180f; 8 -> 270f; else -> 0f
            }
        } catch (e: Exception) { 0f }
        return if (rot == 0f) bmp
        else Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, Matrix().apply { postRotate(rot) }, true)
    }
}
