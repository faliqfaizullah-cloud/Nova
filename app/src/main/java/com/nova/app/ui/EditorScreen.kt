package com.nova.app.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.nova.app.CutOut
import com.nova.app.Entry
import com.nova.app.EntryType
import com.nova.app.copyIntoApp
import com.nova.app.isCutOut
import com.nova.app.newPhotoFile
import com.nova.app.originalOf
import com.nova.app.photoPath
import com.nova.app.photoTint
import com.nova.app.withTint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun EditorScreen(
    initial: Entry, isNew: Boolean,
    onSave: (Entry) -> Unit, onDelete: (Entry) -> Unit, onClose: () -> Unit
) {
    val ctx = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf(initial.title) }
    var body by remember { mutableStateOf(initial.body) }
    val photos = remember { mutableStateListOf<String>().also { it.addAll(initial.photos) } }
    var pending by remember { mutableStateOf<File?>(null) }
    var sel by remember { mutableIntStateOf(-1) }
    var busy by remember { mutableStateOf(false) }

    val bg = when (initial.type) {
        EntryType.NOTE -> NovaColors.Paper
        EntryType.JOURNAL -> NovaColors.Yellow
        EntryType.PHOTO -> NovaColors.Orange
    }

    fun commit() {
        val e = initial.copy(title = title.trim(), body = body.trim(), photos = photos.toList())
        if (e.title.isEmpty() && e.body.isEmpty() && e.photos.isEmpty()) onClose()
        else { view.confirm(); onSave(e) }
    }
    BackHandler { commit() }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val f = pending
        if (ok && f != null) photos.add(f.absolutePath) else f?.delete()
        pending = null
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(10)) { uris ->
        scope.launch {
            uris.forEach { u ->
                withContext(Dispatchers.IO) { copyIntoApp(ctx, u) }?.let { photos.add(it) }
            }
        }
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

    fun doCutOut() {
        val i = sel
        if (i !in photos.indices || busy) return
        val ref = photos[i]
        scope.launch {
            busy = true
            val out = CutOut.run(photoPath(ref))
            busy = false
            if (out == null) {
                Toast.makeText(ctx, "Cut-out needs Google Play services (and internet the first time)", Toast.LENGTH_LONG).show()
            } else if (i in photos.indices && photoPath(photos[i]) == photoPath(ref)) {
                view.confirm()
                photos[i] = withTint(out, photoTint(ref))
            }
        }
    }

    fun restore() {
        val i = sel
        if (i !in photos.indices) return
        val orig = originalOf(photoPath(photos[i]))
        if (File(orig).exists()) photos[i] = withTint(orig, photoTint(photos[i]))
    }

    fun nextColor() {
        val i = sel
        if (i !in photos.indices) return
        photos[i] = withTint(photos[i], (photoTint(photos[i]) + 1) % Colorways.size)
    }

    Box(
        Modifier.fillMaxSize()
            .statusBarsPadding().navigationBarsPadding().imePadding().padding(16.dp)
    ) {
        Column(
            Modifier.fillMaxSize().clip(RoundedCornerShape(44.dp)).background(bg).padding(24.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Wordmark(36.sp)
                Spacer(Modifier.weight(1f))
                if (!isNew) {
                    Text(
                        "DELETE",
                        Modifier.bounceClick { view.confirm(); onDelete(initial) }.padding(8.dp),
                        style = bodyStyle(13.sp, FontWeight.Bold, NovaColors.Ink.copy(alpha = 0.6f))
                    )
                }
                Text(initial.type.name, style = bodyStyle(13.sp, FontWeight.Bold))
            }
            Spacer(Modifier.height(16.dp))

            NovaField(title, { title = it }, "Title", displayStyle(30.sp))
            Spacer(Modifier.height(10.dp))
            Box(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                NovaField(body, { body = it }, "Start writing…", bodyStyle(18.sp).copy(lineHeight = 26.sp))
            }

            if (photos.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(photos.toList()) { i, ref ->
                        val selected = i == sel
                        Box(
                            Modifier.size(92.dp).then(
                                if (selected) Modifier.border(3.dp, NovaColors.Ink, RoundedCornerShape(18.dp)) else Modifier
                            )
                        ) {
                            PhotoImage(
                                ref,
                                Modifier.fillMaxSize().padding(if (selected) 3.dp else 0.dp)
                                    .bounceClick { sel = if (sel == i) -1 else i }
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(NovaColors.Ink.copy(alpha = 0.08f))
                            )
                            Box(
                                Modifier.align(Alignment.TopEnd).padding(4.dp).size(24.dp)
                                    .bounceClick { photos.removeAt(i); sel = -1 }
                                    .clip(CircleShape).background(NovaColors.Ink),
                                contentAlignment = Alignment.Center
                            ) { Text("×", style = bodyStyle(16.sp, FontWeight.Bold, NovaColors.Paper)) }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                if (sel in photos.indices) {
                    val ref = photos[sel]
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (isCutOut(ref)) ToolPill("Restore") { restore() }
                        else ToolPill(if (busy) "Cutting out…" else "Cut out") { doCutOut() }
                        ToolPill("Color: ${Colorways[photoTint(ref) % Colorways.size].name}") { nextColor() }
                    }
                } else {
                    Text(
                        "Tap a photo to cut out or recolor it",
                        style = bodyStyle(12.sp, color = NovaColors.Ink.copy(alpha = 0.55f))
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                PillButton("Camera") { openCamera() }
                PillButton("Gallery") {
                    gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
                PillButton("Save") { commit() }
            }
        }
    }
}

@Composable
private fun ToolPill(text: String, onClick: () -> Unit) {
    Box(
        Modifier.bounceClick(onClick)
            .clip(CircleShape).background(NovaColors.Ink.copy(alpha = 0.1f))
            .border(1.5.dp, NovaColors.Ink, CircleShape)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = bodyStyle(14.sp, FontWeight.SemiBold))
    }
}

@Composable
private fun NovaField(value: String, onChange: (String) -> Unit, hint: String, style: TextStyle) {
    BasicTextField(
        value = value, onValueChange = onChange,
        textStyle = style, cursorBrush = SolidColor(NovaColors.Ink),
        modifier = Modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) Text(hint, style = style.copy(color = NovaColors.Ink.copy(alpha = 0.35f)))
                inner()
            }
        }
    )
}
