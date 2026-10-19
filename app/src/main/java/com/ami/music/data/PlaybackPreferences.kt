package com.ami.music.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

private val Context.playerDataStore by preferencesDataStore(name = "ami_player")
data class SavedPlayback(val ids: List<String>, val index: Int, val position: Long, val repeat: Int, val shuffle: Boolean, val volume: Float)

class PlaybackPreferences(private val context: Context) {
    private val key = stringPreferencesKey("player_v1")
    suspend fun read(): SavedPlayback? = runCatching {
        val json = context.playerDataStore.data.first()[key] ?: return@runCatching null
        val value = JSONObject(json)
        val ids = value.getJSONArray("ids")
        SavedPlayback(List(ids.length()) { ids.getString(it) }, value.optInt("index"), value.optLong("position"),
            value.optInt("repeat"), value.optBoolean("shuffle"), value.optDouble("volume", 1.0).toFloat().coerceIn(0f, 1f))
    }.getOrNull()
    suspend fun save(state: SavedPlayback) {
        val value = JSONObject().put("ids", JSONArray(state.ids)).put("index", state.index)
            .put("position", state.position).put("repeat", state.repeat).put("shuffle", state.shuffle).put("volume", state.volume)
        context.playerDataStore.edit { it[key] = value.toString() }
    }
}
