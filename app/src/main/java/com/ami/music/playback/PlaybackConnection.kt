package com.ami.music.playback

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.ami.music.data.LibraryRepository
import com.ami.music.data.Track
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.retryWhen

enum class RepeatMode { Off, All, One }
data class PlayerUiState(
    val connected: Boolean = false,
    val current: Track? = null,
    val queue: List<Track> = emptyList(),
    val index: Int = 0,
    val elapsedMs: Long = 0,
    val durationMs: Long = 0,
    val playing: Boolean = false,
    val buffering: Boolean = false,
    val shuffle: Boolean = false,
    val repeat: RepeatMode = RepeatMode.Off,
    val volume: Float = 1f,
    val error: String? = null,
)

class PlaybackConnection(context: Context, repository: LibraryRepository) : AutoCloseable {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var tracks = emptyMap<String, Track>()
    private var controller: MediaController? = null
    private var previousVolume = 1f
    private val mutableState = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = mutableState.asStateFlow()
    private val future = MediaController.Builder(context.applicationContext,
        SessionToken(context, ComponentName(context, AmiPlaybackService::class.java))).buildAsync()
    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = refresh()
    }

    init {
        future.addListener({
            runCatching { future.get() }.onSuccess {
                controller = it
                it.addListener(listener)
                refresh()
            }.onFailure { mutableState.value = mutableState.value.copy(error = "Could not connect to the audio player. Reopen AMI to retry.") }
        }, ContextCompat.getMainExecutor(context))
        scope.launch { repository.observeTracks().retryWhen { _, _ ->
            mutableState.value = mutableState.value.copy(error = "Your audio library is temporarily unavailable.")
            delay(3000)
            true
        }.collect { tracks = it.associateBy(Track::id); refresh() } }
        scope.launch { while (isActive) { delay(400); refresh() } }
    }

    private fun refresh() {
        val player = controller ?: return
        val queue = (0 until player.mediaItemCount).mapNotNull { tracks[player.getMediaItemAt(it).mediaId] }
        mutableState.value = PlayerUiState(true, tracks[player.currentMediaItem?.mediaId], queue,
            player.currentMediaItemIndex.coerceAtLeast(0), player.currentPosition.coerceAtLeast(0),
            player.duration.takeIf { it > 0 } ?: tracks[player.currentMediaItem?.mediaId]?.durationMs ?: 0,
            player.playWhenReady, player.playbackState == Player.STATE_BUFFERING, player.shuffleModeEnabled,
            when (player.repeatMode) { Player.REPEAT_MODE_ALL -> RepeatMode.All; Player.REPEAT_MODE_ONE -> RepeatMode.One; else -> RepeatMode.Off },
            player.volume, player.playerError?.let { "This track could not be played. Try another file or re-import the audio." })
    }

    fun playTracks(tracks: List<Track>, startIndex: Int = 0) {
        if (tracks.isEmpty()) return
        val player = controller ?: return
        player.setMediaItems(tracks.map { it.toMediaItem() }, startIndex.coerceIn(tracks.indices), 0L)
        player.prepare()
        player.play()
    }
    fun togglePlay() {
        controller?.let { player ->
            if (!PlaybackPolicy.toggledPlayIntent(player.playWhenReady)) player.pause() else {
                if (player.playbackState == Player.STATE_IDLE) player.prepare()
                if (player.playbackState == Player.STATE_ENDED) player.seekTo(0)
                player.play()
            }
        }
    }
    fun next() {
        controller?.let { if (it.hasNextMediaItem()) it.seekToNextMediaItem() else { it.pause(); it.seekTo(0L) } }
    }
    fun previous() {
        controller?.let {
            if (it.currentPosition > 3000 || !it.hasPreviousMediaItem()) it.seekTo(0L) else it.seekToPreviousMediaItem()
        }
    }
    fun seekTo(positionMs: Long) { controller?.let { it.seekTo(PlaybackPolicy.seek(positionMs, state.value.durationMs)) } }
    fun setVolume(volume: Float) { controller?.volume = volume.coerceIn(0f, 1f) }
    fun toggleMute() {
        controller?.let { if (it.volume == 0f) it.volume = previousVolume else { previousVolume = it.volume; it.volume = 0f } }
    }
    fun setShuffle(enabled: Boolean) { controller?.shuffleModeEnabled = enabled }
    fun setRepeat(mode: RepeatMode) {
        controller?.repeatMode = when (mode) { RepeatMode.Off -> Player.REPEAT_MODE_OFF; RepeatMode.All -> Player.REPEAT_MODE_ALL; RepeatMode.One -> Player.REPEAT_MODE_ONE }
    }
    fun moveQueueItem(from: Int, to: Int) {
        controller?.let { if (from in 0 until it.mediaItemCount && to in 0 until it.mediaItemCount) it.moveMediaItem(from, to) }
    }
    fun removeQueueItem(index: Int) {
        controller?.let { if (index in 0 until it.mediaItemCount) { it.removeMediaItem(index); if (it.mediaItemCount == 0) it.pause() } }
    }
    fun jumpTo(index: Int) { controller?.let { if (index in 0 until it.mediaItemCount) { it.seekTo(index, 0L); it.play() } } }
    override fun close() {
        controller?.removeListener(listener)
        MediaController.releaseFuture(future)
        scope.cancel()
    }
}
