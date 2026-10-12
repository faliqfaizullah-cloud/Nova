package com.nova.app

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject

enum class ElType { PHOTO, STICKER, TEXT }

/** One user-made item on a page. x/y are the centre in 0..1 of the spread; scale/rot are user gestures. */
data class Element(
    val id: Long,
    val type: ElType,
    val data: String,           // file path (PHOTO/STICKER) or the text (TEXT)
    val x: Float = 0.5f,
    val y: Float = 0.5f,
    val scale: Float = 1f,
    val rot: Float = 0f,
    val framed: Boolean = true, // polaroid + tape (PHOTO)
    val color: Int = 0          // ink colour index (TEXT)
)

/** A spread = two facing pages with a spiral binding in the middle. */
data class Spread(val id: Long, val paper: Int, val elements: List<Element>)

object BookLayout {
    const val ASPECT = 1.3f
    const val PHOTO_W = 0.26f
    const val STICKER_W = 0.28f
    const val TEXT_SP_FRAC = 0.055f
    val Papers = longArrayOf(0xFFFBF7EFL, 0xFFEBD9BCL, 0xFFDCE9F5L, 0xFFF8DDE3L, 0xFF2B2F3AL)
    val InkColors = longArrayOf(0xFF1A1A1AL, 0xFFFFFFFFL, 0xFFC4551BL, 0xFF0D4A2FL, 0xFF3B5BDBL, 0xFFD6336CL)
}

class BookStore(ctx: Context) {
    private val prefs = ctx.getSharedPreferences("nova_book", Context.MODE_PRIVATE)

    var title: String
        get() = prefs.getString("title", "Journal") ?: "Journal"
        set(v) { prefs.edit().putString("title", v).apply() }

    fun load(): List<Spread> = runCatching {
        val arr = JSONArray(prefs.getString("spreads", "[]"))
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val ea = o.getJSONArray("els")
            Spread(
                o.getLong("id"), o.optInt("paper", 0),
                (0 until ea.length()).map { j ->
                    val e = ea.getJSONObject(j)
                    Element(
                        e.getLong("id"), ElType.valueOf(e.getString("type")), e.getString("data"),
                        e.optDouble("x", 0.5).toFloat(), e.optDouble("y", 0.5).toFloat(),
                        e.optDouble("scale", 1.0).toFloat(), e.optDouble("rot", 0.0).toFloat(),
                        e.optBoolean("framed", true), e.optInt("color", 0)
                    )
                }
            )
        }
    }.getOrDefault(emptyList())

    fun save(list: List<Spread>) {
        val arr = JSONArray()
        list.forEach { s ->
            val ea = JSONArray()
            s.elements.forEach { e ->
                ea.put(JSONObject().apply {
                    put("id", e.id); put("type", e.type.name); put("data", e.data)
                    put("x", e.x.toDouble()); put("y", e.y.toDouble())
                    put("scale", e.scale.toDouble()); put("rot", e.rot.toDouble())
                    put("framed", e.framed); put("color", e.color)
                })
            }
            arr.put(JSONObject().apply { put("id", s.id); put("paper", s.paper); put("els", ea) })
        }
        prefs.edit().putString("spreads", arr.toString()).apply()
    }
}

class BookState(private val store: BookStore) {
    val spreads = mutableStateListOf<Spread>().also { it.addAll(store.load()) }
    var title by mutableStateOf(store.title)
        private set

    init { if (spreads.isEmpty()) { spreads.add(blank()); save() } }

    private fun blank() = Spread(System.nanoTime(), spreads.size % BookLayout.Papers.size, emptyList())

    fun rename(t: String) { title = t.ifBlank { "Journal" }; store.title = title }
    fun save() = store.save(spreads.toList())

    fun add(): Spread { val s = blank(); spreads.add(s); save(); return s }

    fun replace(s: Spread) {
        val i = spreads.indexOfFirst { it.id == s.id }
        if (i >= 0) spreads[i] = s
    }

    fun remove(id: Long) {
        spreads.removeAll { it.id == id }
        if (spreads.isEmpty()) spreads.add(blank())
        save()
    }

    fun duplicate(id: Long) {
        val i = spreads.indexOfFirst { it.id == id }
        if (i < 0) return
        val src = spreads[i]
        val base = System.nanoTime()
        spreads.add(i + 1, src.copy(id = base, elements = src.elements.mapIndexed { k, e -> e.copy(id = base + 1 + k) }))
        save()
    }
}
