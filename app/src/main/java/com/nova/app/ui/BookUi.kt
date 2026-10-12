package com.nova.app.ui

import android.content.Intent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.style.TextOverflow
import com.nova.app.PaperSound
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sin
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.nova.app.BookExport
import com.nova.app.BookLayout
import com.nova.app.BookState
import com.nova.app.ElType
import com.nova.app.Element
import com.nova.app.Spread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.roundToInt

// ---------------------------------------------------------------- gestures
/** Fires when the user swipes vertically on area that no child (list/pager) consumed. */
fun Modifier.swipeVertical(up: Boolean, onSwipe: () -> Unit): Modifier = pointerInput(up) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Final)
        var dx = 0f
        var dy = 0f
        do {
            val ev = awaitPointerEvent(PointerEventPass.Final)
            ev.changes.forEach { c ->
                if (!c.isConsumed) { val d = c.positionChange(); dx += d.x; dy += d.y }
            }
        } while (ev.changes.any { it.pressed })
        if (abs(dy) > 90.dp.toPx() && abs(dy) > abs(dx) * 1.5f && (dy < 0f) == up) onSwipe()
    }
}

// ---------------------------------------------------------------- icons
enum class Ico { BOOK, GRID, SEARCH, MENU, DOTS, UPLOAD, TRASH, PLUS }

@Composable
fun NovaIcon(kind: Ico, tint: Color, modifier: Modifier = Modifier.size(24.dp)) {
    Canvas(modifier) {
        val u = size.minDimension / 24f
        val sw = 2.1f * u
        val st = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
        fun o(x: Float, y: Float) = Offset(x * u, y * u)
        fun line(a: Offset, b: Offset) = drawLine(tint, a, b, sw, StrokeCap.Round)
        when (kind) {
            Ico.PLUS -> { line(o(12f, 5f), o(12f, 19f)); line(o(5f, 12f), o(19f, 12f)) }
            Ico.MENU -> { line(o(5f, 7f), o(19f, 7f)); line(o(5f, 12f), o(19f, 12f)); line(o(5f, 17f), o(19f, 17f)) }
            Ico.DOTS -> listOf(5f, 12f, 19f).forEach { drawCircle(tint, 1.9f * u, o(it, 12f)) }
            Ico.SEARCH -> { drawCircle(tint, 6f * u, o(10.5f, 10.5f), style = st); line(o(15f, 15f), o(20f, 20f)) }
            Ico.GRID -> listOf(4f to 4f, 14f to 4f, 4f to 14f, 14f to 14f).forEach { (x, y) ->
                drawRoundRect(tint, o(x, y), Size(6f * u, 6f * u), CornerRadius(1.6f * u), style = st)
            }
            Ico.BOOK -> {
                val p = Path().apply {
                    moveTo(12f * u, 6.5f * u)
                    cubicTo(9f * u, 4.8f * u, 6f * u, 4.6f * u, 3.5f * u, 5.6f * u)
                    lineTo(3.5f * u, 18f * u)
                    cubicTo(6f * u, 17f * u, 9f * u, 17.2f * u, 12f * u, 19f * u)
                    cubicTo(15f * u, 17.2f * u, 18f * u, 17f * u, 20.5f * u, 18f * u)
                    lineTo(20.5f * u, 5.6f * u)
                    cubicTo(18f * u, 4.6f * u, 15f * u, 4.8f * u, 12f * u, 6.5f * u)
                    lineTo(12f * u, 19f * u)
                }
                drawPath(p, tint, style = st)
            }
            Ico.UPLOAD -> {
                val p = Path().apply {
                    moveTo(8f * u, 10f * u); lineTo(5f * u, 10f * u); lineTo(5f * u, 20f * u)
                    lineTo(19f * u, 20f * u); lineTo(19f * u, 10f * u); lineTo(16f * u, 10f * u)
                }
                drawPath(p, tint, style = st)
                line(o(12f, 15f), o(12f, 4f)); line(o(8.2f, 7.6f), o(12f, 4f)); line(o(15.8f, 7.6f), o(12f, 4f))
            }
            Ico.TRASH -> {
                line(o(4.5f, 7f), o(19.5f, 7f))
                val h = Path().apply { moveTo(9f * u, 7f * u); lineTo(9f * u, 4.5f * u); lineTo(15f * u, 4.5f * u); lineTo(15f * u, 7f * u) }
                drawPath(h, tint, style = st)
                val b = Path().apply { moveTo(6.5f * u, 7f * u); lineTo(7.5f * u, 19.5f * u); lineTo(16.5f * u, 19.5f * u); lineTo(17.5f * u, 7f * u) }
                drawPath(b, tint, style = st)
                line(o(10.2f, 11f), o(10.2f, 16f)); line(o(13.8f, 11f), o(13.8f, 16f))
            }
        }
    }
}

@Composable
fun CircleBtn(
    icon: Ico, bg: Color, tint: Color, size: Dp = 56.dp,
    outlined: Boolean = false, selected: Boolean = false, onClick: () -> Unit
) {
    Box(
        Modifier.bounceClick(onClick).size(size)
            .then(if (!outlined) Modifier.shadow(8.dp, CircleShape) else Modifier)
            .clip(CircleShape).background(bg)
            .then(
                if (outlined) Modifier.border(
                    if (selected) 2.dp else 1.dp,
                    Color.White.copy(alpha = if (selected) 1f else 0.35f), CircleShape
                ) else Modifier
            ),
        contentAlignment = Alignment.Center
    ) { NovaIcon(icon, tint, Modifier.size(size * 0.42f)) }
}

@Composable
fun ChipPill(text: String, onClick: () -> Unit) {
    Box(
        Modifier.bounceClick(onClick).clip(CircleShape)
            .background(Color.White.copy(alpha = 0.14f))
            .border(1.5.dp, Color.White.copy(alpha = 0.6f), CircleShape)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) { Text(text, style = bodyStyle(14.sp, FontWeight.SemiBold, Color.White)) }
}

@Composable
fun BookBackdrop(modifier: Modifier = Modifier) {
    Box(
        modifier.background(
            Brush.radialGradient(listOf(Color(0xFF4A5680), Color(0xFF3C4770), Color(0xFF2C3556)), radius = 1300f)
        ).pointerInput(Unit) { detectTapGestures { } } // blocks touches reaching the screen underneath
    )
}

@Composable
fun TextPrompt(title: String, initial: String, onDone: (String?) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f))
            .pointerInput(Unit) { detectTapGestures { onDone(null) } }.imePadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.padding(24.dp).clip(RoundedCornerShape(28.dp)).background(NovaColors.Paper)
                .pointerInput(Unit) { detectTapGestures { } }.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, style = bodyStyle(14.sp, FontWeight.SemiBold, NovaColors.Ink.copy(alpha = 0.6f)))
            Spacer(Modifier.height(10.dp))
            BasicTextField(
                text, { text = it }, textStyle = bodyStyle(22.sp), cursorBrush = SolidColor(NovaColors.Ink),
                modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp, max = 200.dp).focusRequester(focus)
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PillButton("Cancel") { onDone(null) }
                PillButton("OK") { onDone(text) }
            }
        }
    }
}

// ---------------------------------------------------------------- spread canvas
private fun Modifier.centerAt(cx: Float, cy: Float): Modifier = layout { measurable, constraints ->
    val p = measurable.measure(Constraints())
    layout(constraints.maxWidth, constraints.maxHeight) {
        p.placeRelative((cx - p.width / 2f).roundToInt(), (cy - p.height / 2f).roundToInt())
    }
}

/** Renders one spread (two pages + spiral). All content is user-made. */
@Composable
fun SpreadView(
    spread: Spread, modifier: Modifier = Modifier,
    selectedId: Long? = null, interactive: Boolean = false,
    onSelect: (Long) -> Unit = {}, onChange: (Element) -> Unit = {}, onBackgroundTap: () -> Unit = {}
) {
    val paper = Color(BookLayout.Papers[spread.paper % BookLayout.Papers.size])
    BoxWithConstraints(
        modifier.aspectRatio(BookLayout.ASPECT).clip(RoundedCornerShape(18.dp)).background(paper)
            .then(if (interactive) Modifier.pointerInput(Unit) { detectTapGestures { onBackgroundTap() } } else Modifier)
    ) {
        val wPx = constraints.maxWidth.toFloat()
        val hPx = constraints.maxHeight.toFloat()
        if (spread.title.isNotBlank()) {
            val dens = LocalDensity.current
            Text(
                spread.title,
                Modifier.align(Alignment.TopStart)
                    .padding(start = with(dens) { (wPx * 0.05f).toDp() }, top = with(dens) { (hPx * 0.05f).toDp() })
                    .widthIn(max = with(dens) { (wPx * 0.42f).toDp() }),
                maxLines = 2, overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    fontFamily = NovaFonts.display, fontWeight = NovaFonts.displayWeight,
                    fontSize = with(dens) { (wPx * 0.06f).toSp() },
                    color = if (paper.luminance() < 0.4f) Color.White else NovaColors.Ink
                )
            )
        }
        spread.elements.forEach { e ->
            key(e.id) { ElementView(e, wPx, hPx, e.id == selectedId, interactive, onSelect, onChange) }
        }
        Canvas(Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val gw = size.width * 0.03f
            drawRect(
                Brush.horizontalGradient(
                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.16f), Color.Transparent),
                    startX = cx - gw, endX = cx + gw
                ),
                topLeft = Offset(cx - gw, 0f), size = Size(gw * 2, size.height)
            )
            val n = 18
            for (i in 0 until n) {
                val y = (i + 0.5f) * size.height / n
                val cw = size.width * 0.068f
                val ch = size.height * 0.022f
                drawRoundRect(Color(0xFF202226), Offset(cx - cw / 2, y - ch / 2), Size(cw, ch), CornerRadius(ch / 2))
                drawRoundRect(Color.White.copy(alpha = 0.25f), Offset(cx - cw / 2 + 2f, y - ch / 2 + 1f), Size(cw * 0.5f, ch * 0.3f), CornerRadius(ch / 4))
            }
        }
    }
}

@Composable
private fun ElementView(
    e: Element, wPx: Float, hPx: Float, selected: Boolean, interactive: Boolean,
    onSelect: (Long) -> Unit, onChange: (Element) -> Unit
) {
    val density = LocalDensity.current
    val cur by rememberUpdatedState(e)
    val changeS by rememberUpdatedState(onChange)
    val selectS by rememberUpdatedState(onSelect)
    val baseW = when (e.type) {
        ElType.PHOTO -> BookLayout.PHOTO_W
        ElType.STICKER -> BookLayout.STICKER_W
        ElType.TEXT -> BookLayout.PHOTO_W
    } * wPx
    val wDp = with(density) { (baseW * e.scale).toDp() }

    var m: Modifier = Modifier.centerAt(e.x * wPx, e.y * hPx)
    if (interactive) {
        m = m
            .pointerInput(Unit) { detectTapGestures(onPress = { selectS(cur.id); tryAwaitRelease() }) }
            .pointerInput(wPx, hPx) {
                detectTransformGestures { _, pan, zoom, rotation ->
                    val c = cur
                    changeS(
                        c.copy(
                            x = (c.x + pan.x / wPx).coerceIn(0.02f, 0.98f),
                            y = (c.y + pan.y / hPx).coerceIn(0.02f, 0.98f),
                            scale = (c.scale * zoom).coerceIn(0.3f, 4f),
                            rot = c.rot + rotation
                        )
                    )
                }
            }
    }
    m = m.graphicsLayer { rotationZ = e.rot }

    Box(
        m.then(
            if (selected && interactive) Modifier.border(2.dp, Color(0xFF6C8CFF), RoundedCornerShape(6.dp)) else Modifier
        )
    ) {
        when (e.type) {
            ElType.PHOTO -> if (e.framed) {
                Box(Modifier.width(wDp).aspectRatio(1f / 1.18f)) {
                    val pad = wDp * 0.07f
                    Box(
                        Modifier.fillMaxSize().shadow(6.dp, RoundedCornerShape(3.dp))
                            .background(Color(0xFFFDFDFB)).padding(start = pad, top = pad, end = pad, bottom = pad * 3.1f)
                    ) { PhotoImage(e.data, Modifier.fillMaxSize()) }
                    Box(
                        Modifier.align(Alignment.TopCenter).offset(y = -wDp * 0.04f)
                            .width(wDp * 0.4f).height(wDp * 0.1f)
                            .graphicsLayer { rotationZ = -4f }.background(Color(0xCCE2D3B0))
                    )
                }
            } else {
                PhotoImage(e.data, Modifier.width(wDp).aspectRatio(1f / 0.85f).clip(RoundedCornerShape(10.dp)))
            }
            ElType.STICKER -> PhotoImage(e.data, Modifier.size(wDp), ContentScale.Fit)
            ElType.TEXT -> Text(
                e.data,
                Modifier.widthIn(max = wDp * 1.8f),
                style = TextStyle(
                    fontFamily = NovaFonts.display, fontWeight = NovaFonts.displayWeight,
                    fontSize = with(density) { (BookLayout.TEXT_SP_FRAC * wPx * e.scale).toSp() },
                    color = Color(BookLayout.InkColors[e.color % BookLayout.InkColors.size]),
                    textAlign = TextAlign.Center
                )
            )
        }
    }
}

// ---------------------------------------------------------------- real page-turning book
@Composable
private fun Half(s: Spread, right: Boolean, halfW: Dp, fullH: Dp, modifier: Modifier = Modifier) {
    Box(modifier.size(halfW, fullH).clipToBounds()) {
        SpreadView(s, Modifier.requiredSize(halfW * 2, fullH).offset(x = if (right) -halfW else 0.dp))
    }
}

/**
 * A book you turn like a real one: drag a page across the spine, it lifts in 3D, shades, flips and
 * lands with a bounce while a paper-slide sound plays. Tap the left/right edge to turn, centre to edit.
 */
@Composable
fun BookFlip(
    spreads: List<Spread>, index: Int,
    onIndexChange: (Int) -> Unit, onOpen: (Spread) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val n = spreads.size
    val maxP = max(0, n - 1).toFloat()
    val p = remember { Animatable(index.coerceIn(0, max(0, n - 1)).toFloat()) }
    var dragging by remember { mutableStateOf(false) }

    LaunchedEffect(n) { p.updateBounds(0f, maxP); if (p.value > maxP) p.snapTo(maxP) }
    LaunchedEffect(index, n) {
        val t = index.coerceIn(0, max(0, n - 1)).toFloat()
        if (!dragging && kotlin.math.abs(p.value - t) > 0.01f) {
            PaperSound.play(); view.tick()
            p.animateTo(t, spring(0.75f, Spring.StiffnessLow))
        }
    }

    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val a = maxWidth - 32.dp
        val b = (maxHeight - 12.dp) * BookLayout.ASPECT
        val fullW = if (a < b) a else b
        val fullH = fullW / BookLayout.ASPECT
        val halfW = fullW / 2
        val halfPx = with(LocalDensity.current) { halfW.toPx() }

        val base by remember(n) { derivedStateOf { floor(p.value).toInt().coerceIn(0, max(0, n - 1)) } }
        val flipping by remember(n) { derivedStateOf { (p.value - base) > 0.002f && base < n - 1 } }

        fun settle(lastDx: Float, startIdx: Int, played: Boolean) {
            val cur = p.value
            val bs = floor(cur).toInt()
            val f = cur - bs
            val target = when {
                lastDx < -2f -> if (f > 0.18f) bs + 1 else bs
                lastDx > 2f -> if (f < 0.82f) bs else bs + 1
                else -> kotlin.math.round(cur).toInt()
            }.coerceIn(0, max(0, n - 1))
            if (target != startIdx && !played) { PaperSound.play() }
            scope.launch {
                p.animateTo(target.toFloat(), spring(0.75f, Spring.StiffnessLow))
                dragging = false
                if (target != startIdx) view.confirm()
                onIndexChange(target)
            }
        }

        fun turn(delta: Int) {
            val cur = kotlin.math.round(p.value).toInt()
            val t = (cur + delta).coerceIn(0, max(0, n - 1))
            if (t == cur) return
            PaperSound.play(); view.tick()
            scope.launch {
                dragging = true
                p.animateTo(t.toFloat(), spring(0.75f, Spring.StiffnessLow))
                dragging = false
                view.confirm()
                onIndexChange(t)
            }
        }

        Box(
            Modifier.size(fullW, fullH)
                .pointerInput(n) {
                    detectTapGestures { off ->
                        val x = off.x / size.width
                        when {
                            x < 0.16f -> turn(-1)
                            x > 0.84f -> turn(+1)
                            else -> spreads.getOrNull(kotlin.math.round(p.value).toInt())?.let(onOpen)
                        }
                    }
                }
                .pointerInput(n, halfPx) {
                    var lastDx = 0f
                    var startIdx = 0
                    var startVal = 0f
                    var played = false
                    detectHorizontalDragGestures(
                        onDragStart = {
                            dragging = true; played = false; lastDx = 0f
                            startVal = p.value; startIdx = kotlin.math.round(p.value).toInt()
                            scope.launch { p.stop() }
                        },
                        onHorizontalDrag = { change, dx ->
                            change.consume()
                            lastDx = dx
                            val np = (p.value - dx / (halfPx * 1.4f)).coerceIn(0f, maxP)
                            scope.launch { p.snapTo(np) }
                            if (!played && kotlin.math.abs(np - startVal) > 0.06f) {
                                played = true; PaperSound.play(); view.tick()
                            }
                        },
                        onDragEnd = { settle(lastDx, startIdx, played) },
                        onDragCancel = { settle(lastDx, startIdx, played) }
                    )
                }
        ) {
            // paper stack under the book + soft shadow
            Box(Modifier.size(fullW, fullH).shadow(14.dp, RoundedCornerShape(18.dp)))
            Box(Modifier.offset(y = 7.dp).size(fullW, fullH).clip(RoundedCornerShape(18.dp)).background(Color(0xFFCFC6B4)))
            Box(Modifier.offset(y = 3.5.dp).size(fullW, fullH).clip(RoundedCornerShape(18.dp)).background(Color(0xFFE6DECD)))

            if (!flipping) {
                spreads.getOrNull(base)?.let { SpreadView(it, Modifier.size(fullW, fullH)) }
            } else {
                val cur = spreads[base]
                val nxt = spreads[base + 1]
                // page revealed under the turning leaf (right half of next spread) + staying left page
                Half(cur, false, halfW, fullH, Modifier.align(Alignment.TopStart))
                Box(Modifier.align(Alignment.TopEnd)) {
                    Half(nxt, true, halfW, fullH)
                    Box(
                        Modifier.matchParentSize().graphicsLayer {
                            val f = (p.value - base).coerceIn(0f, 1f)
                            alpha = 0.30f * sin(PI.toFloat() * f)
                        }.background(Color.Black)
                    )
                }
                // the leaf: front = right half of current, back = left half of next; hinged on the spine
                Box(
                    Modifier.align(Alignment.TopEnd).graphicsLayer {
                        val f = (p.value - base).coerceIn(0f, 1f)
                        transformOrigin = TransformOrigin(0f, 0.5f)
                        rotationY = 180f * f
                        cameraDistance = 18f * density
                    }
                ) {
                    Half(
                        cur, true, halfW, fullH,
                        Modifier.graphicsLayer { alpha = if ((p.value - base) < 0.5f) 1f else 0f }
                    )
                    Half(
                        nxt, false, halfW, fullH,
                        Modifier.graphicsLayer { scaleX = -1f; alpha = if ((p.value - base) >= 0.5f) 1f else 0f }
                    )
                    Box(
                        Modifier.matchParentSize().graphicsLayer {
                            val f = (p.value - base).coerceIn(0f, 1f)
                            alpha = 0.38f * sin(PI.toFloat() * f)
                        }.background(Color.Black)
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Journal screen
@Composable
fun BookScreen(book: BookState, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    var editing by remember { mutableStateOf<Long?>(null) }
    var grid by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf<String?>(null) }
    var showMore by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var titleFor by remember { mutableStateOf<Long?>(null) }
    var pageIndex by remember { mutableIntStateOf(0) }

    BackHandler(enabled = editing == null) { onClose() }

    val q = query
    val visible = if (q.isNullOrBlank()) book.spreads.toList() else book.spreads.filter { s ->
        s.title.contains(q, ignoreCase = true) ||
            s.elements.any { it.type == ElType.TEXT && it.data.contains(q, ignoreCase = true) }
    }
    val idx = pageIndex.coerceIn(0, max(0, visible.size - 1))
    val cur = visible.getOrNull(idx)

    Box(Modifier.fillMaxSize()) {
        BookBackdrop(Modifier.fillMaxSize())
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
                .swipeVertical(up = false) { if (editing == null) onClose() }
        ) {
            Box(Modifier.fillMaxWidth().padding(top = 6.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.bounceClick(onClose).padding(10.dp)) { NovaIcon(Ico.DOTS, Color.White.copy(alpha = 0.7f), Modifier.size(20.dp)) }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CircleBtn(Ico.BOOK, Color.Transparent, Color.White, 48.dp, outlined = true, selected = !grid) { grid = false }
                    CircleBtn(Ico.GRID, Color.Transparent, Color.White, 48.dp, outlined = true, selected = grid) { grid = true }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CircleBtn(Ico.SEARCH, Color.Transparent, Color.White, 48.dp, outlined = true, selected = query != null) {
                        query = if (query == null) "" else null
                    }
                    CircleBtn(Ico.MENU, Color.Transparent, Color.White, 48.dp, outlined = true) { renaming = true }
                }
            }
            if (query != null) {
                BasicTextField(
                    query ?: "", { query = it },
                    textStyle = bodyStyle(16.sp, color = Color.White), cursorBrush = SolidColor(Color.White),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp).fillMaxWidth()
                        .clip(CircleShape).background(Color.White.copy(alpha = 0.14f)).padding(horizontal = 18.dp, vertical = 12.dp),
                    decorationBox = { inner ->
                        Box {
                            if (query.isNullOrEmpty()) Text("Search titles and text", style = bodyStyle(16.sp, color = Color.White.copy(alpha = 0.5f)))
                            inner()
                        }
                    }
                )
            }
            Spacer(Modifier.height(8.dp))
            // tap the title to give your journal its own name
            Text(
                book.title,
                Modifier.fillMaxWidth().bounceClick { renaming = true },
                style = bodyStyle(34.sp, FontWeight.Bold, Color.White).copy(textAlign = TextAlign.Center),
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(
                if (visible.isEmpty()) "0 Pages" else "Page ${idx + 1} of ${visible.size}",
                Modifier.fillMaxWidth().padding(top = 2.dp),
                style = bodyStyle(15.sp, FontWeight.SemiBold, Color.White.copy(alpha = 0.7f)).copy(textAlign = TextAlign.Center)
            )

            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (visible.isEmpty()) {
                    Text("No pages match", style = bodyStyle(16.sp, color = Color.White.copy(alpha = 0.6f)))
                } else if (grid) {
                    LazyVerticalGrid(
                        GridCells.Fixed(2), Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        itemsIndexed(visible, key = { _, s -> s.id }) { i, s ->
                            SpreadView(s, Modifier.bounceClick { pageIndex = i; grid = false })
                        }
                    }
                } else {
                    BookFlip(
                        spreads = visible, index = idx,
                        onIndexChange = { pageIndex = it },
                        onOpen = { editing = it.id },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // page title: each page can have its own title, written on the page itself
            if (cur != null && !grid) {
                Text(
                    if (cur.title.isBlank()) "＋ Add a title to this page" else cur.title,
                    Modifier.fillMaxWidth().bounceClick { titleFor = cur.id }.padding(horizontal = 24.dp, vertical = 10.dp),
                    style = bodyStyle(16.sp, FontWeight.SemiBold, Color.White.copy(alpha = if (cur.title.isBlank()) 0.6f else 1f))
                        .copy(textAlign = TextAlign.Center),
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }

            if (showMore && cur != null) {
                Column(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChipPill("Duplicate page") { book.duplicate(cur.id); pageIndex = idx + 1; showMore = false; view.confirm() }
                    ChipPill("Change paper") { book.replace(cur.copy(paper = (cur.paper + 1) % BookLayout.Papers.size)); book.save(); showMore = false }
                    ChipPill("Clear page") { book.replace(cur.copy(elements = emptyList())); book.save(); showMore = false; view.confirm() }
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 22.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
            ) {
                CircleBtn(Ico.DOTS, Color.White, NovaColors.Ink) { showMore = !showMore }
                CircleBtn(Ico.UPLOAD, Color.White, NovaColors.Ink) {
                    val s = cur ?: return@CircleBtn
                    scope.launch {
                        try {
                            val f = withContext(Dispatchers.IO) { BookExport.render(ctx, s) }
                            val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", f)
                            val i = Intent(Intent.ACTION_SEND).apply {
                                type = "image/png"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            ctx.startActivity(Intent.createChooser(i, "Share page"))
                        } catch (e: Exception) {
                            Toast.makeText(ctx, "Couldn't export this page", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                CircleBtn(Ico.TRASH, Color.White, NovaColors.Ink) {
                    cur?.let { book.remove(it.id); view.confirm() }
                }
                CircleBtn(Ico.PLUS, Color.White, NovaColors.Ink) {
                    val s = book.add(); view.confirm()
                    query = null; grid = false
                    pageIndex = book.spreads.size - 1
                    editing = s.id
                }
            }
        }

        editing?.let { id -> BookEditor(book, id) { editing = null } }
        if (renaming) TextPrompt("Name your journal", book.title) { t -> renaming = false; if (t != null) book.rename(t) }
        titleFor?.let { id ->
            val s = book.spreads.firstOrNull { it.id == id }
            if (s == null) titleFor = null
            else TextPrompt("Page title", s.title) { t ->
                titleFor = null
                if (t != null) { book.replace(s.copy(title = t.trim())); book.save() }
            }
        }
    }
}
