package com.ami.music

import android.content.Context
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ami.music.data.AudiusCatalog
import com.ami.music.playback.toMediaItem
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/** Live external-service smoke test: do not replace failures with assumptions or a skipped pass. */
@RunWith(AndroidJUnit4::class)
class AudiusPlaybackTest {
    @Test fun publicAudiusTrackCanPlayInAndroid() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val tracks = AudiusCatalog().load("", 0).tracks.take(3)
        assertTrue("Audius should offer a public track", tracks.isNotEmpty())
        val errors = mutableListOf<String>()
        var success = false
        for (track in tracks) {
            val playing = CountDownLatch(1)
            val failure = AtomicReference<String>()
            var player: ExoPlayer? = null
            try {
                instrumentation.runOnMainSync {
                    player = ExoPlayer.Builder(context).build().apply {
                        addListener(object : Player.Listener {
                            override fun onIsPlayingChanged(isPlaying: Boolean) { if (isPlaying) playing.countDown() }
                            override fun onPlayerError(error: PlaybackException) { failure.set(error.errorCodeName); playing.countDown() }
                        })
                        setMediaItem(track.toMediaItem())
                        prepare()
                        play()
                    }
                }
                val signalled = playing.await(35, TimeUnit.SECONDS)
                if (signalled && failure.get() == null) { success = true; break }
                errors += "${track.id}: ${failure.get() ?: "timeout"}"
            } finally { instrumentation.runOnMainSync { player?.release() } }
        }
        assertTrue("No public Audius track played: $errors", success)
    }
}
