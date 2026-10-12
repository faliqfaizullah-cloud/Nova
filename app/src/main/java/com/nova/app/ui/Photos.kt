package com.nova.app.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.nova.app.photoPath
import com.nova.app.photoTint
import java.io.File

class Colorway(val name: String, val matrix: FloatArray)

/** Product-style colorways (like the bag cycling beige -> orange -> green in the reference video). */
val Colorways = listOf(
    Colorway("Original", floatArrayOf(1f,0f,0f,0f,0f, 0f,1f,0f,0f,0f, 0f,0f,1f,0f,0f, 0f,0f,0f,1f,0f)),
    Colorway("Sun",      floatArrayOf(1.25f,0f,0f,0f,12f, 0f,0.9f,0f,0f,0f, 0f,0f,0.4f,0f,0f, 0f,0f,0f,1f,0f)),
    Colorway("Forest",   floatArrayOf(0.5f,0f,0f,0f,0f, 0f,0.95f,0f,0f,6f, 0f,0f,0.55f,0f,0f, 0f,0f,0f,1f,0f)),
    Colorway("Berry",    floatArrayOf(1.05f,0f,0f,0f,0f, 0f,0.55f,0f,0f,0f, 0f,0f,0.85f,0f,12f, 0f,0f,0f,1f,0f))
)

fun tintFilter(ref: String): ColorFilter? {
    val i = photoTint(ref) % Colorways.size
    return if (i == 0) null else ColorFilter.colorMatrix(ColorMatrix(Colorways[i].matrix))
}

@Composable
fun PhotoImage(ref: String, modifier: Modifier = Modifier, scale: ContentScale = ContentScale.Crop) {
    AsyncImage(File(photoPath(ref)), null, modifier, contentScale = scale, colorFilter = tintFilter(ref))
}

/** Cut-out hero that smoothly cycles through the colorways forever (the white card in the video). */
@Composable
fun Showcase(ref: String, modifier: Modifier = Modifier) {
    val n = Colorways.size
    val t by rememberInfiniteTransition(label = "show").animateFloat(
        0f, n.toFloat(),
        infiniteRepeatable(tween(n * 1800, easing = LinearEasing)), label = "t"
    )
    val i = t.toInt().coerceAtMost(n - 1)
    val raw = ((t - i - 0.55f) / 0.45f).coerceIn(0f, 1f)
    val f = raw * raw * (3f - 2f * raw)
    val a = Colorways[i].matrix
    val b = Colorways[(i + 1) % n].matrix
    val m = FloatArray(20) { a[it] + (b[it] - a[it]) * f }
    AsyncImage(
        File(photoPath(ref)), null, modifier,
        contentScale = ContentScale.Fit, colorFilter = ColorFilter.colorMatrix(ColorMatrix(m))
    )
}
