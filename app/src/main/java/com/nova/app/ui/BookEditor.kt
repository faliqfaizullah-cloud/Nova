package com.nova.app.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.nova.app.BookLayout
import com.nova.app.BookState
import com.nova.app.CutOut
import com.nova.app.ElType
import com.nova.app.Element
import com.nova.app.copyIntoApp
import com.nova.app.newPhotoFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.random.Random

@Composable
fun BookEditor(book: BookState, spreadId: Long, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    val spread = book.spreads.firstOrNull { it.id == spreadId }
    if (spread == null) { LaunchedEffect(Unit) { onClose() }; return }

    var selId by remember { mutableStateOf<Long?>(null) }
    var busy by remember { mutableStateOf(false) }
    var prompt by remember { mutableStateOf<Long?>(null) } // -1 = new text, otherwise element id
    var pending by remember { mutableStateOf<File?>(null) }
    var asSticker by remember { mutableStateOf(false) }

    fun mutate(f: (List<Element>) -> List<Element>) {
        val s = book.spreads.firstOrNull { it.id == spreadId } ?: return
        book.replace(s.copy(elements = f(s.elements)))
    }
    fun add(e: Element) { mutate { it + e }; selId = e.id }
    fun update(e: Element) { mutate { l -> l.map { if (it.id == e.id) e else it } } }
    fun jitter() = 0.5f + (Random.nextFloat() - 0.5f) * 0.3f
    fun newId() = System.nanoTime()

    BackHandler { book.save(); onClose() }
    LaunchedEffect(spread) { delay(400); book.save() }
    DisposableEffect(Unit) { onDispose { book.save() } }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val f = pending
        if (ok && f != null) add(Element(newId(), ElType.PHOTO, f.absolutePath, jitter(), jitter(), 1f, Random.nextFloat() * 8f - 4f))
        else f?.delete()
        pending = null
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(10)) { uris ->
        val sticker = asSticker
        scope.launch {
            uris.forEach { u ->
                val path = withContext(Dispatchers.IO) { copyIntoApp(ctx, u) } ?: return@forEach
                if (sticker) {
                    busy = true
                    val cut = CutOut.run(path, outline = true)
                    busy = false
                    if (cut != null) add(Element(newId(), ElType.STICKER, cut, jitter(), jitter(), 1f, Random.nextFloat() * 10f - 5f, false))
                    else {
                        Toast.makeText(ctx, "Cut-out needs Google Play services (and internet the first time). Added as a photo.", Toast.LENGTH_LONG).show()
                        add(Element(newId(), ElType.PHOTO, path, jitter(), jitter(), 1f, Random.nextFloat() * 8f - 4f))
                    }
                } else {
                    add(Element(newId(), ElType.PHOTO, path, jitter(), jitter(), 1f, Random.nextFloat() * 8f - 4f))
                }
            }
        }
    }

    fun pick(sticker: Boolean) {
        asSticker = sticker
        gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    fun openCamera() {
        try {
            val f = newPhotoFile(ctx)
            pending = f
            camera.launch(FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", f))
        } catch (e: Exception) {
            Toast.makeText(ctx, "No camera app available", Toast.LENGTH_SHORT).show()
        }
    }

    val sel = spread.elements.firstOrNull { it.id == selId }

    Box(Modifier.fillMaxSize()) {
        BookBackdrop(Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                PillButton("Done") { view.confirm(); book.save(); onClose() }
                Spacer(Modifier.weight(1f))
                ChipPill(if (spread.title.isBlank()) "Title" else "Title ✎") { prompt = -2L }
                Spacer(Modifier.width(8.dp))
                ChipPill("Paper") { book.replace(spread.copy(paper = (spread.paper + 1) % BookLayout.Papers.size)) }
            }
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                SpreadView(
                    spread, Modifier.fillMaxWidth(), selectedId = selId, interactive = true,
                    onSelect = { if (selId != it) { selId = it; view.tick() } },
                    onChange = { update(it) }, onBackgroundTap = { selId = null }
                )
            }
            Text(
                if (busy) "Cutting out…" else "Drag to move · pinch to resize · twist to rotate",
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                style = bodyStyle(12.sp, color = Color.White.copy(alpha = 0.7f)).copy(textAlign = TextAlign.Center)
            )

            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (sel != null) {
                    if (sel.type == ElType.PHOTO) {
                        ChipPill(if (sel.framed) "Frame: on" else "Frame: off") { update(sel.copy(framed = !sel.framed)) }
                        ChipPill(if (busy) "Cutting out…" else "Cut out") {
                            if (!busy) scope.launch {
                                busy = true
                                val cut = CutOut.run(sel.data, outline = true)
                                busy = false
                                if (cut == null) Toast.makeText(ctx, "Cut-out needs Google Play services (and internet the first time)", Toast.LENGTH_LONG).show()
                                else { view.confirm(); update(sel.copy(type = ElType.STICKER, data = cut, framed = false)) }
                            }
                        }
                    }
                    if (sel.type == ElType.TEXT) {
                        ChipPill("Edit") { prompt = sel.id }
                        ChipPill("Color") { update(sel.copy(color = (sel.color + 1) % BookLayout.InkColors.size)) }
                    }
                    ChipPill("Front") { mutate { l -> l - sel + sel } }
                    ChipPill("Delete") { mutate { l -> l.filter { it.id != sel.id } }; selId = null; view.confirm() }
                } else {
                    Text("Tap an item to edit it", style = bodyStyle(13.sp, color = Color.White.copy(alpha = 0.6f)), modifier = Modifier.padding(vertical = 9.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillButton("Photo") { pick(false) }
                PillButton("Camera") { openCamera() }
                PillButton("Sticker") { pick(true) }
                PillButton("Text") { prompt = -1L }
            }
        }

        if (prompt == -2L) {
            TextPrompt("Page title", spread.title) { t ->
                prompt = null
                if (t != null) book.replace(spread.copy(title = t.trim()))
            }
        } else prompt?.let { id ->
            val existing = spread.elements.firstOrNull { it.id == id }
            TextPrompt(if (id == -1L) "Add text" else "Edit text", existing?.data ?: "") { t ->
                prompt = null
                if (!t.isNullOrBlank()) {
                    if (existing != null) update(existing.copy(data = t.trim()))
                    else add(Element(newId(), ElType.TEXT, t.trim(), jitter(), jitter(), 1f, Random.nextFloat() * 6f - 3f))
                }
            }
        }
    }
}
