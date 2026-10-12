package com.nova.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

object BookExport {
    fun render(ctx: Context, s: Spread, w: Int = 1800): File {
        val h = (w / BookLayout.ASPECT).toInt()
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(BookLayout.Papers[s.paper % BookLayout.Papers.size].toInt())
        val p = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        if (s.title.isNotBlank()) {
            val paperC = BookLayout.Papers[s.paper % BookLayout.Papers.size].toInt()
            val tp = android.text.TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 0.06f * w
                color = if (androidx.core.graphics.ColorUtils.calculateLuminance(paperC) < 0.4) 0xFFFFFFFF.toInt() else 0xFF1A1A1A.toInt()
                typeface = Typeface.DEFAULT_BOLD
            }
            val t = android.text.TextUtils.ellipsize(s.title.replace("\n", " "), tp, w * 0.42f, android.text.TextUtils.TruncateAt.END)
            c.drawText(t.toString(), w * 0.05f, h * 0.05f + tp.textSize, tp)
        }

        s.elements.forEach { e ->
            c.save()
            c.translate(e.x * w, e.y * h)
            c.rotate(e.rot)
            when (e.type) {
                ElType.PHOTO -> drawPhoto(c, p, e, w)
                ElType.STICKER -> drawSticker(c, p, e, w)
                ElType.TEXT -> drawText(c, e, w)
            }
            c.restore()
        }

        // gutter shade + spiral coils
        p.color = 0x22000000
        c.drawRect(w / 2f - w * 0.02f, 0f, w / 2f + w * 0.02f, h.toFloat(), p)
        p.color = 0xFF202226.toInt()
        val n = 18
        for (i in 0 until n) {
            val y = (i + 0.5f) * h / n
            c.drawRoundRect(RectF(w / 2f - w * 0.034f, y - h * 0.011f, w / 2f + w * 0.034f, y + h * 0.011f), h * 0.011f, h * 0.011f, p)
        }

        val out = File(photosDir(ctx), "export_${s.id}.png")
        FileOutputStream(out).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return out
    }

    private fun drawCrop(c: Canvas, p: Paint, bmp: Bitmap, dst: RectF) {
        val sc = max(dst.width() / bmp.width, dst.height() / bmp.height)
        val sw = dst.width() / sc
        val sh = dst.height() / sc
        val src = Rect(
            ((bmp.width - sw) / 2).toInt(), ((bmp.height - sh) / 2).toInt(),
            ((bmp.width + sw) / 2).toInt(), ((bmp.height + sh) / 2).toInt()
        )
        c.drawBitmap(bmp, src, dst, p)
    }

    private fun drawPhoto(c: Canvas, p: Paint, e: Element, w: Int) {
        val bw = BookLayout.PHOTO_W * w * e.scale
        val bmp = CutOut.decode(photoPath(e.data), (bw * 1.5f).toInt().coerceAtLeast(200)) ?: return
        if (e.framed) {
            val bh = bw * 1.18f
            p.color = 0xFFFDFDFB.toInt()
            c.drawRect(-bw / 2, -bh / 2, bw / 2, bh / 2, p)
            val pad = bw * 0.07f
            drawCrop(c, p, bmp, RectF(-bw / 2 + pad, -bh / 2 + pad, bw / 2 - pad, bh / 2 - pad * 3.1f))
            c.save()
            c.rotate(-4f)
            p.color = 0xCCE2D3B0.toInt()
            c.drawRect(-bw * 0.2f, -bh / 2 - bw * 0.05f, bw * 0.2f, -bh / 2 + bw * 0.05f, p)
            c.restore()
        } else {
            val bh = bw * 0.85f
            drawCrop(c, p, bmp, RectF(-bw / 2, -bh / 2, bw / 2, bh / 2))
        }
    }

    private fun drawSticker(c: Canvas, p: Paint, e: Element, w: Int) {
        val bw = BookLayout.STICKER_W * w * e.scale
        val bmp = CutOut.decode(photoPath(e.data), (bw * 1.5f).toInt().coerceAtLeast(200)) ?: return
        val sc = min(bw / bmp.width, bw / bmp.height)
        val dw = bmp.width * sc
        val dh = bmp.height * sc
        c.drawBitmap(bmp, null, RectF(-dw / 2, -dh / 2, dw / 2, dh / 2), p)
    }

    private fun drawText(c: Canvas, e: Element, w: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = BookLayout.TEXT_SP_FRAC * w * e.scale
            color = BookLayout.InkColors[e.color % BookLayout.InkColors.size].toInt()
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        val lines = e.data.split("\n")
        val lineH = paint.textSize * 1.15f
        var y = -(lines.size - 1) * lineH / 2f + paint.textSize * 0.35f
        lines.forEach { c.drawText(it, 0f, y, paint); y += lineH }
    }
}
