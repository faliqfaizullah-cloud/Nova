package com.nova.app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NovaCard(color: Color, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.fillMaxSize().clip(RoundedCornerShape(48.dp)).background(color), content = content)
}

@Composable
fun Wordmark(size: TextUnit, color: Color = NovaColors.Ink, modifier: Modifier = Modifier) {
    Text("nova", modifier = modifier, style = displayStyle(size, color).copy(lineHeight = size))
}

@Composable
fun NovaLogo(diameter: Dp, bg: Color) {
    Canvas(Modifier.size(diameter)) {
        drawCircle(NovaColors.Ink)
        val w = this.size.width
        val p = Path().apply {
            moveTo(w * 0.20f, w * 0.28f)
            cubicTo(w * 0.60f, w * 0.38f, w * 0.40f, w * 0.62f, w * 0.80f, w * 0.72f)
        }
        drawPath(p, bg, style = Stroke(width = w * 0.09f, cap = StrokeCap.Round))
    }
}

/** Uppercase grotesque headline; words wrapped in *asterisks* become italic (like "STUDIO" / "COURT"). */
@Composable
fun Headline(text: String, size: TextUnit, modifier: Modifier = Modifier) {
    val styled = buildAnnotatedString {
        text.split("*").forEachIndexed { i, part ->
            if (i % 2 == 1) withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(part) } else append(part)
        }
    }
    Text(
        styled, modifier = modifier,
        style = bodyStyle(size).copy(textAlign = TextAlign.Center, lineHeight = (size.value * 1.05f).sp)
    )
}

@Composable
fun PillButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.bounceClick(onClick)
            .clip(CircleShape).background(NovaColors.Ink)
            .padding(horizontal = 26.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = bodyStyle(16.sp, FontWeight.SemiBold, NovaColors.Paper))
    }
}

/** The little "^" above the button — bobs up and down forever. */
@Composable
fun BobbingChevron(color: Color = NovaColors.Ink) {
    val t = rememberInfiniteTransition(label = "chev")
    val y by t.animateFloat(
        0f, -18f,
        infiniteRepeatable(tween(650, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "y"
    )
    Canvas(Modifier.size(26.dp, 14.dp).graphicsLayer { translationY = y }) {
        val p = Path().apply {
            moveTo(0f, size.height); lineTo(size.width / 2, 0f); lineTo(size.width, size.height)
        }
        drawPath(p, color, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}
