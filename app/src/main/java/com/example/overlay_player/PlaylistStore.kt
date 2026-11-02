package com.example.overlay_player

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Reine Umwandlung, getrennt vom Speicher, damit sie ohne Gerät prüfbar bleibt. */
object PlaylistCodec {
    fun encode(entries: List<Pair<String, String>>): String {
        val array = JSONArray()
        for ((uri, name) in entries) {
            array.put(JSONObject().put("uri", uri).put("name", name))
        }
        return array.toString()
    }

    fun decode(json: String): List<Pair<String, String>> {
        val array = JSONArray(json)
        val entries = ArrayList<Pair<String, String>>(array.length())
        val seen = HashSet<String>()
        for (i in 0 until array.length()) {
            val entry = array.optJSONObject(i) ?: continue
            val uri = entry.optString("uri")
            if (uri.isEmpty() || !seen.add(uri)) continue
            entries.add(uri to entry.optString("name", "Unbekannt"))
        }
        return entries
    }
}

/** Die Playlist lebt in den Einstellungen, damit sie das Schließen der App überdauert. */
class PlaylistStore(context: Context) {
    private val prefs = context.getSharedPreferences(
        AudioPlayerService.PREFS_NAME, Context.MODE_PRIVATE
    )

    /** null heißt: Es wurde noch nie gespeichert (Erststart oder Update von 1.0). */
    fun load(): List<Pair<String, String>>? {
        val json = prefs.getString(KEY_PLAYLIST, null) ?: return null
        return try {
            PlaylistCodec.decode(json)
        } catch (e: org.json.JSONException) {
            e.printStackTrace()
            null
        }
    }

    fun save(entries: List<Pair<String, String>>) {
        prefs.edit().putString(KEY_PLAYLIST, PlaylistCodec.encode(entries)).apply()
    }

    var selectedUri: String?
        get() = prefs.getString(KEY_SELECTED, null)
        set(value) {
            prefs.edit().putString(KEY_SELECTED, value).apply()
        }

    private companion object {
        const val KEY_PLAYLIST = "playlist_v1"
        const val KEY_SELECTED = "playlist_selected"
    }
}
