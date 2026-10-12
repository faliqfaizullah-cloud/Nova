package com.nova.app

import android.graphics.Color as AColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.nova.app.ui.*
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AColor.TRANSPARENT)
        )
        NovaFonts.init(this)
        val store = EntryStore(this)
        setContent { NovaApp(store) }
    }
}

@Composable
fun NovaApp(store: EntryStore) {
    val entries = remember { mutableStateListOf<Entry>().also { it.addAll(store.load()) } }
    var editing by remember { mutableStateOf<Entry?>(null) }
    var isNew by remember { mutableStateOf(false) }

    fun upsert(e: Entry) {
        val i = entries.indexOfFirst { it.id == e.id }
        if (i >= 0) entries[i] = e else entries.add(0, e)
        store.save(entries.toList())
    }
    fun delete(e: Entry) {
        entries.removeAll { it.id == e.id }
        e.photos.forEach { File(it).delete() }
        store.save(entries.toList())
    }

    Box(Modifier.fillMaxSize().background(NovaColors.Green)) {
        AnimatedContent(
            targetState = editing,
            transitionSpec = {
                (scaleIn(initialScale = 0.85f, animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow)) + fadeIn())
                    .togetherWith(fadeOut())
            },
            label = "screen"
        ) { current ->
            if (current == null) {
                HomeScreen(
                    entries = entries,
                    onNew = { type ->
                        isNew = true
                        editing = Entry(System.currentTimeMillis(), type, "", "", emptyList(), System.currentTimeMillis())
                    },
                    onOpen = { isNew = false; editing = it }
                )
            } else {
                EditorScreen(
                    initial = current,
                    isNew = isNew,
                    onSave = { upsert(it); editing = null },
                    onDelete = { delete(it); editing = null },
                    onClose = { editing = null }
                )
            }
        }
    }
}
