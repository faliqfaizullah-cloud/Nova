package com.nova.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private val VineDark = Color(0xFF0A3220)
private val LeafMid = Color(0xFF145A38)
private val LeafLight = Color(0xFF2A7A50)
private val FlowerColors = listOf(
    Color(0xFFF08A9A), // soft pink
    Color(0xFF6CC8F2), // pastel blue
    Color(0xFFF6D44C), // sunny yellow
    Color(0xFFA67BD8), // purple
    Color(0xFFEA6E3C), // warm orange
    Color(0xFF86D6C6)  // mint
)

/** Dark green backdrop with a faint leaf pattern and vines down both outer borders. */
@Composable
fun FloralBackdrop() {
    Canvas(Modifier.fillMaxSize()) {
        drawRect(NovaColors.Green)
        drawPattern()
        drawVines()
    }
}

/** Vines only (drawn over the cards' outer edges, like the mockup). Does not intercept touches. */
@Composable
fun FloralVines() {
    Canvas(Modifier.fillMaxSize()) { drawVines() }
}

private fun DrawScope.drawLeaf(x: Float, y: Float, deg: Float, len: Float, wid: Float, color: Color) {
    val p = Path().apply {
        moveTo(0f, 0f)
        quadraticBezierTo(len / 2, -wid, len, 0f)
        quadraticBezierTo(len / 2, wid, 0f, 0f)
        close()
    }
    withTransform({ translate(x, y); rotate(deg, Offset.Zero) }) { drawPath(p, color) }
}

private fun DrawScope.drawFlower(c: Offset, r: Float, color: Color, rot: Float) {
    val dp = 1.dp.toPx()
    for (k in 0 until 5) {
        val a = rot + k * (2f * PI.toFloat() / 5f)
        val pc = Offset(c.x + cos(a) * r * 0.55f, c.y + sin(a) * r * 0.55f)
        drawCircle(color, r * 0.48f, pc)
        drawCircle(VineDark, r * 0.48f, pc, style = Stroke(1.1f * dp))
    }
    drawCircle(if (color == FlowerColors[2]) Color(0xFFE8873A) else Color(0xFFF6D44C), r * 0.3f, c)
    drawCircle(VineDark, r * 0.3f, c, style = Stroke(1f * dp))
}

private fun DrawScope.drawCurl(c: Offset, r0: Float, start: Float, dir: Float) {
    val dp = 1.dp.toPx()
    val path = Path()
    val n = 36
    for (k in 0..n) {
        val t = k / n.toFloat()
        val r = r0 * (1f - 0.8f * t)
        val a = start + dir * t * (2.4f * PI.toFloat())
        val px = c.x + r * cos(a)
        val py = c.y + r * sin(a)
        if (k == 0) path.moveTo(px, py) else path.lineTo(px, py)
    }
    drawPath(path, VineDark, style = Stroke(1.8f * dp, cap = StrokeCap.Round))
}

private fun DrawScope.drawPattern() {
    val rnd = Random(3)
    val dp = 1.dp.toPx()
    repeat(46) {
        drawLeaf(
            rnd.nextFloat() * size.width, rnd.nextFloat() * size.height,
            rnd.nextFloat() * 360f, (16 + rnd.nextInt(14)) * dp, 6 * dp,
            LeafLight.copy(alpha = 0.22f)
        )
    }
    repeat(16) {
        drawCircle(LeafLight.copy(alpha = 0.18f), (2 + rnd.nextInt(3)) * dp,
            Offset(rnd.nextFloat() * size.width, rnd.nextFloat() * size.height))
    }
}

private fun DrawScope.drawVines() {
    val w = size.width
    val h = size.height
    val dp = 1.dp.toPx()

    for (side in 0..1) {
        val inward = if (side == 0) 1f else -1f
        val edge = if (side == 0) 0f else w
        val phase = side * 1.7f
        val rnd = Random(11 + side * 5)

        fun vx(y: Float) = edge + inward * (16 * dp + 14 * dp * sin(y / (110 * dp) + phase))
        fun slope(y: Float) = inward * 14 * dp * cos(y / (110 * dp) + phase) / (110 * dp)

        // main stem
        val stem = Path()
        var y = -20 * dp
        stem.moveTo(vx(y), y)
        while (y < h + 20 * dp) { y += 8 * dp; stem.lineTo(vx(y), y) }
        drawPath(stem, VineDark, style = Stroke(2.2f * dp, cap = StrokeCap.Round))

        // leaves
        var ly = 10 * dp
        var i = 0
        while (ly < h) {
            val base = Math.toDegrees(atan2(1f, slope(ly)).toDouble()).toFloat()
            val sgn = if (i % 2 == 0) 1 else -1
            drawLeaf(
                vx(ly), ly, base + sgn * (50 + rnd.nextInt(20)),
                (20 + rnd.nextInt(8)) * dp, 8 * dp,
                when (i % 3) { 0 -> VineDark; 1 -> LeafMid; else -> LeafLight }
            )
            ly += (30 + rnd.nextInt(16)) * dp
            i++
        }

        // curls
        listOf(0.22f, 0.52f, 0.82f).forEachIndexed { idx, f ->
            val cy = h * f
            val c = Offset(vx(cy) + inward * 14 * dp, cy)
            drawCurl(c, 13 * dp, if (side == 0) PI.toFloat() else 0f, if ((idx + side) % 2 == 0) 1f else -1f)
        }

        // flowers on short stems
        for (k in 0 until 8) {
            val fy = h * (0.06f + 0.115f * k) + rnd.nextInt(-14, 14) * dp
            val fx = vx(fy) + inward * (10 + rnd.nextInt(26)) * dp
            drawLine(VineDark, Offset(vx(fy), fy), Offset(fx, fy), 1.6f * dp)
            drawFlower(
                Offset(fx, fy), (9 + rnd.nextInt(7)) * dp,
                FlowerColors[(k + side * 2) % FlowerColors.size], rnd.nextFloat() * 6f
            )
        }
    }
}
