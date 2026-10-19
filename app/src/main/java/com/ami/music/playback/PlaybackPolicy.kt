package com.ami.music.playback

object PlaybackPolicy {
    data class Previous(val index: Int, val positionMs: Long)
    fun previous(index: Int, positionMs: Long): Previous = Previous(if (positionMs > 3000L) index.coerceAtLeast(0) else (index - 1).coerceAtLeast(0), 0L)
    fun seek(positionMs: Long, durationMs: Long): Long = positionMs.coerceIn(0L, durationMs.coerceAtLeast(0L))
    fun validPlaylistName(name: String): Boolean = name.isNotBlank()
    fun uniqueTracks(ids: List<String>): List<String> = ids.distinct()
    fun canRestore(mediaItemCount: Int, playWhenReady: Boolean): Boolean = mediaItemCount == 0 && !playWhenReady
    fun toggledPlayIntent(playWhenReady: Boolean): Boolean = !playWhenReady
}
