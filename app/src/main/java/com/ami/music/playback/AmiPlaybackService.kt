package com.ami.music.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.ami.music.AmiApplication
import com.ami.music.MainActivity
import com.ami.music.data.SavedPlayback
import com.ami.music.data.Track
import kotlinx.coroutines.*

class AmiPlaybackService : MediaSessionService() {
    private var session: MediaSession? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var restored = false
    private val app get() = application as AmiApplication

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this).build().apply {
            setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), true)
            setHandleAudioBecomingNoisy(true)
        }
        val activity = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        session = MediaSession.Builder(this, player).setSessionActivity(activity).build()
        player.addListener(object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                if (restored) persist(player)
                if (player.isPlaying && (events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION) || events.contains(Player.EVENT_IS_PLAYING_CHANGED))) {
                    player.currentMediaItem?.mediaId?.let { id -> scope.launch { runCatching { app.repository.recordPlayed(id) } } }
                }
            }
        })
        scope.launch {
            while (isActive && !restored) {
                try {
                    app.repository.ensureSeed()
                    val saved = app.preferences.read()
                    if (saved != null) {
                        val byId = app.repository.dao.allTracks().filter { it.available }.associateBy { it.id }
                        val items = saved.ids.mapNotNull { byId[it] }
                        // All suspending reads finish before checking for new user commands.
                        if (items.isNotEmpty() && PlaybackPolicy.canRestore(player.mediaItemCount, player.playWhenReady)) {
                            val originalCurrent = saved.ids.getOrNull(saved.index)
                            val index = items.indexOfFirst { it.id == originalCurrent }.coerceAtLeast(0)
                            val position = PlaybackPolicy.seek(saved.position, items[index].durationMs)
                            player.setMediaItems(items.map { it.toMediaItem() }, index, position)
                            player.repeatMode = saved.repeat.takeIf { it in 0..2 } ?: Player.REPEAT_MODE_OFF
                            player.shuffleModeEnabled = saved.shuffle
                            player.volume = saved.volume
                            player.playWhenReady = false
                            player.prepare()
                        }
                    }
                    restored = true
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { delay(3000) }
            }
            while (isActive) {
                delay(5_000)
                persist(player)
            }
        }
    }

    private fun snapshot(player: Player): SavedPlayback = SavedPlayback(
        (0 until player.mediaItemCount).map { player.getMediaItemAt(it).mediaId },
        player.currentMediaItemIndex.coerceAtLeast(0), player.currentPosition.coerceAtLeast(0),
        player.repeatMode, player.shuffleModeEnabled, player.volume,
    )

    private fun persist(player: Player) {
        val value = snapshot(player)
        app.playbackSnapshots.trySend(value)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onDestroy() {
        session?.player?.let { if (restored) persist(it) }
        session?.run { player.release(); release() }
        session = null
        scope.cancel()
        super.onDestroy()
    }
}

fun Track.toMediaItem(): MediaItem = MediaItem.Builder().setMediaId(id).setUri(sourceUri)
    .setMediaMetadata(MediaMetadata.Builder().setTitle(title).setArtist(artist).setAlbumTitle(album).setAlbumArtist(albumArtist).setGenre(genre).setArtworkUri(artworkUri?.let { android.net.Uri.parse(it) }).build()).build()
