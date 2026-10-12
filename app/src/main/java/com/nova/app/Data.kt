package com.nova.app

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

enum class EntryType { NOTE, JOURNAL, PHOTO }

data class Entry(
    val id: Long,
    val type: EntryType,
    val title: String,
    val body: String,
    val photos: List<String>,
    val createdAt: Long
)

class EntryStore(ctx: Context) {
    private val prefs = ctx.getSharedPreferences("nova", Context.MODE_PRIVATE)

    fun load(): List<Entry> = runCatching {
        val arr = JSONArray(prefs.getString("entries", "[]"))
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val p = o.getJSONArray("photos")
            Entry(
                o.getLong("id"),
                EntryType.valueOf(o.getString("type")),
                o.optString("title"),
                o.optString("body"),
                (0 until p.length()).map { p.getString(it) },
                o.getLong("createdAt")
            )
        }
    }.getOrDefault(emptyList())

    fun save(list: List<Entry>) {
        val arr = JSONArray()
        list.forEach { e ->
            arr.put(JSONObject().apply {
                put("id", e.id); put("type", e.type.name)
                put("title", e.title); put("body", e.body)
                put("photos", JSONArray(e.photos)); put("createdAt", e.createdAt)
            })
        }
        prefs.edit().putString("entries", arr.toString()).apply()
    }
}

fun photosDir(ctx: Context): File = File(ctx.filesDir, "photos").apply { mkdirs() }

fun newPhotoFile(ctx: Context): File = File(photosDir(ctx), "nova_${System.currentTimeMillis()}.jpg")

/** Copies a picked gallery image into app storage so it survives permission loss. */
fun copyIntoApp(ctx: Context, uri: Uri): String? = runCatching {
    val out = newPhotoFile(ctx)
    ctx.contentResolver.openInputStream(uri)!!.use { input ->
        FileOutputStream(out).use { input.copyTo(it) }
    }
    out.absolutePath
}.getOrNull()
