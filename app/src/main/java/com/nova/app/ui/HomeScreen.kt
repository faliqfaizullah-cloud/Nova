package com.nova.app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.nova.app.Entry
import com.nova.app.EntryType
import com.nova.app.isCutOut
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(entries: List<Entry>, onNew: (EntryType) -> Unit, onOpen: (Entry) -> Unit, onOpenBook: () -> Unit) {
    val view = LocalView.current
    val pager = rememberPagerState { 3 }
    var first by remember { mutableStateOf(true) }
    LaunchedEffect(pager.settledPage) { if (first) first = false else view.tick() }

    val notes = entries.filter { it.type == EntryType.NOTE }
    val journal = entries.filter { it.type == EntryType.JOURNAL }
    val photos = entries.flatMap { e -> e.photos.map { e to it } }
    val hero = photos.map { it.second }.firstOrNull { isCutOut(it) }

    Column(
        Modifier.fillMaxSize()
            .swipeVertical(up = true, onSwipe = onOpenBook)
            .statusBarsPadding().navigationBarsPadding().padding(vertical = 20.dp)
    ) {
        HorizontalPager(
            state = pager, modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 36.dp), pageSpacing = 16.dp
        ) { page ->
            BouncyPage(page, pager.currentPage) {
                when (page) {
                    0 -> NotesCard(notes, hero, onOpen) { onNew(EntryType.NOTE) }
                    1 -> JournalCard(journal, onOpen) { onNew(EntryType.JOURNAL) }
                    else -> PhotosCard(photos, onOpen) { onNew(EntryType.PHOTO) }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 14.dp), Arrangement.Center) {
            repeat(3) { i ->
                val w by animateDpAsState(
                    if (i == pager.currentPage) 28.dp else 8.dp,
                    spring(Spring.DampingRatioHighBouncy, Spring.StiffnessMedium), label = "dot"
                )
                Box(
                    Modifier.padding(horizontal = 4.dp).height(8.dp).width(w).clip(CircleShape)
                        .background(if (i == pager.currentPage) NovaColors.Yellow else NovaColors.Paper.copy(alpha = 0.35f))
                )
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 10.dp).bounceClick(onOpenBook),
            horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
        ) {
            BobbingChevron(NovaColors.Paper.copy(alpha = 0.8f))
            Spacer(Modifier.width(8.dp))
            Text("SWIPE UP · JOURNAL", style = bodyStyle(12.sp, FontWeight.SemiBold, NovaColors.Paper.copy(alpha = 0.8f)))
        }
    }
}

/** Cards pop in with a staggered bounce, and the focused card springs up slightly larger. */
@Composable
private fun BouncyPage(index: Int, current: Int, content: @Composable () -> Unit) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(index * 120L); shown = true }
    val s by animateFloatAsState(
        (if (shown) 1f else 0.6f) * (if (index == current) 1f else 0.92f),
        spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow), label = "page"
    )
    Box(Modifier.fillMaxSize().graphicsLayer { scaleX = s; scaleY = s; alpha = if (shown) 1f else 0f }) { content() }
}

@Composable
private fun NotesCard(items: List<Entry>, hero: String?, onOpen: (Entry) -> Unit, onNew: () -> Unit) {
    NovaCard(NovaColors.Paper) {
        Column(Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Wordmark(76.sp)
            if (hero != null) {
                Spacer(Modifier.height(8.dp))
                Showcase(hero, Modifier.fillMaxWidth().height(150.dp))
            }
            Spacer(Modifier.height(18.dp))
            EntryList(items, onOpen, "NO NOTES YET", Modifier.weight(1f))
            Spacer(Modifier.height(12.dp))
            Headline("MEET YOUR\nNOTES", 24.sp)
            Spacer(Modifier.height(14.dp))
            BobbingChevron()
            Spacer(Modifier.height(6.dp))
            PillButton("New Note", onClick = onNew)
        }
    }
}

@Composable
private fun JournalCard(items: List<Entry>, onOpen: (Entry) -> Unit, onNew: () -> Unit) {
    val t = rememberInfiniteTransition(label = "tag")
    // Tagline breathes from faint to bold (full strength when the journal is empty), wordmark floats.
    val tagAlpha by t.animateFloat(
        0.06f, if (items.isEmpty()) 1f else 0.2f,
        infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "a"
    )
    val wordY by t.animateFloat(
        0f, -12f,
        infiniteRepeatable(tween(1300, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "w"
    )
    NovaCard(NovaColors.Yellow) {
        Text(
            "LET THE\nDAY\nSETTLE\nINTO\nWORDS",
            Modifier.align(Alignment.Center),
            style = bodyStyle(68.sp, FontWeight.Bold, NovaColors.Ink.copy(alpha = tagAlpha))
                .copy(textAlign = TextAlign.Center, lineHeight = 70.sp)
        )
        Column(Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 36.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            NovaLogo(64.dp, NovaColors.Yellow)
            Spacer(Modifier.height(18.dp))
            EntryList(items, onOpen, "", Modifier.weight(1f))
            Spacer(Modifier.height(14.dp))
            BobbingChevron()
            Spacer(Modifier.height(6.dp))
            PillButton("New Entry", onClick = onNew)
            Spacer(Modifier.height(20.dp))
            Wordmark(46.sp, modifier = Modifier.graphicsLayer { translationY = wordY })
        }
    }
}

@Composable
private fun PhotosCard(photos: List<Pair<Entry, String>>, onOpen: (Entry) -> Unit, onNew: () -> Unit) {
    NovaCard(NovaColors.Orange) {
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Wordmark(38.sp)
            Spacer(Modifier.height(8.dp))
            Headline("EVERY *MOMENT*\nKEPT IN *FRAME*", 27.sp)
            Spacer(Modifier.height(14.dp))
            Box(
                Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(16.dp))
                    .background(NovaColors.Ink.copy(alpha = 0.12f))
            ) {
                if (photos.isEmpty()) {
                    Text("NO PHOTOS YET", Modifier.align(Alignment.Center), style = bodyStyle(15.sp, FontWeight.Medium))
                } else {
                    LazyVerticalGrid(
                        GridCells.Fixed(2), Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(photos) { (entry, path) ->
                            PhotoImage(path, Modifier.fillMaxWidth().aspectRatio(1f).bounceClick { onOpen(entry) })
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            BobbingChevron()
            Spacer(Modifier.height(6.dp))
            PillButton("Add Photo", onClick = onNew)
        }
    }
}

@Composable
private fun EntryList(items: List<Entry>, onOpen: (Entry) -> Unit, empty: String, modifier: Modifier) {
    if (items.isEmpty()) {
        Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(empty, style = bodyStyle(15.sp, FontWeight.Medium, NovaColors.Ink.copy(alpha = 0.5f)))
        }
        return
    }
    LazyColumn(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(items, key = { it.id }) { EntryRow(it, onOpen) }
    }
}

@Composable
private fun EntryRow(e: Entry, onOpen: (Entry) -> Unit) {
    val date = remember(e.createdAt) { SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(e.createdAt)) }
    Row(
        Modifier.fillMaxWidth().bounceClick { onOpen(e) }
            .clip(RoundedCornerShape(18.dp)).background(NovaColors.Ink.copy(alpha = 0.07f)).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (e.photos.isNotEmpty()) {
            PhotoImage(e.photos.first(), Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)))
            Spacer(Modifier.width(10.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(e.title.ifBlank { "Untitled" }, style = bodyStyle(16.sp, FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "$date · ${e.body.replace("\n", " ")}",
                style = bodyStyle(13.sp, color = NovaColors.Ink.copy(alpha = 0.7f)),
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
    }
}
