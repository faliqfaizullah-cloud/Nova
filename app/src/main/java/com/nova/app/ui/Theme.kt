package com.nova.app.ui

import android.content.Context
import android.graphics.Typeface
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

object NovaColors {
    val Green = Color(0xFF0D4A2F)
    val Paper = Color(0xFFFAFAFA)
    val Yellow = Color(0xFFFDC544)
    val Orange = Color(0xFFC4551B)
    val Ink = Color(0xFF1A1A1A)
}

/** Chunky Cooper-style wordmark + Helvetica-style grotesque. Loaded from assets/fonts, with fallbacks. */
object NovaFonts {
    var display: FontFamily = FontFamily.Serif
    var displayWeight: FontWeight = FontWeight.Black
    var body: FontFamily = FontFamily.SansSerif

    fun init(ctx: Context) {
        load(ctx, "fonts/caprasimo.ttf")?.let { display = it; displayWeight = FontWeight.Normal }
        load(ctx, "fonts/intertight.ttf")?.let { body = it }
    }

    private fun load(ctx: Context, path: String): FontFamily? =
        try { FontFamily(Typeface.createFromAsset(ctx.assets, path)) } catch (e: Exception) { null }
}

fun displayStyle(size: TextUnit, color: Color = NovaColors.Ink) = TextStyle(
    fontFamily = NovaFonts.display, fontWeight = NovaFonts.displayWeight,
    fontSize = size, color = color, letterSpacing = (-1).sp
)

fun bodyStyle(size: TextUnit, weight: FontWeight = FontWeight.Normal, color: Color = NovaColors.Ink) = TextStyle(
    fontFamily = NovaFonts.body, fontWeight = weight, fontSize = size, color = color
)

// ---------- Haptics ----------
fun View.tick() { performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY) }
fun View.confirm() {
    performHapticFeedback(
        if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.LONG_PRESS
    )
}

// ---------- Bouncy press: springs down on touch, overshoots back on release, with a haptic tick ----------
fun Modifier.bounceClick(onClick: () -> Unit): Modifier = composed {
    val view = LocalView.current
    var pressed by remember { mutableStateOf(false) }
    val s by animateFloatAsState(
        targetValue = if (pressed) 0.86f else 1f,
        animationSpec = spring(Spring.DampingRatioHighBouncy, Spring.StiffnessMedium),
        label = "bounce"
    )
    this
        .graphicsLayer { scaleX = s; scaleY = s }
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = { pressed = true; view.tick(); tryAwaitRelease(); pressed = false },
                onTap = { onClick() }
            )
        }
}
