package com.ami.music

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ami.music.data.DemoCatalog
import com.ami.music.playback.AmiPlaybackService
import com.ami.music.playback.toMediaItem
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class PlaybackIntegrationTest {
    private lateinit var controller: MediaController
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    @Before fun connect() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        lateinit var future: com.google.common.util.concurrent.ListenableFuture<MediaController>
        instrumentation.runOnMainSync { future = MediaController.Builder(context, SessionToken(context, ComponentName(context, AmiPlaybackService::class.java))).buildAsync() }
        controller = future.get(10, TimeUnit.SECONDS)
        instrumentation.runOnMainSync {
            controller.setMediaItems(DemoCatalog.tracks().take(3).map { it.toMediaItem() }, 1, 0L)
            controller.repeatMode = Player.REPEAT_MODE_OFF
            controller.shuffleModeEnabled = false
            controller.playWhenReady = false
        }
    }
    @After fun release() { if (::controller.isInitialized) instrumentation.runOnMainSync { controller.pause(); controller.clearMediaItems(); controller.release() } }
    @Test fun manualNextIgnoresRepeatOne() {
        instrumentation.runOnMainSync {
            controller.repeatMode = Player.REPEAT_MODE_ONE
            controller.seekToNextMediaItem()
            assertEquals("demo-2", controller.currentMediaItem?.mediaId)
        }
    }
    @Test fun shuffleRetainsCurrentAndOriginalOrder() {
        instrumentation.runOnMainSync {
            controller.shuffleModeEnabled = true
            assertEquals("demo-1", controller.currentMediaItem?.mediaId)
            controller.shuffleModeEnabled = false
            assertEquals(listOf("demo-0", "demo-1", "demo-2"), (0 until controller.mediaItemCount).map { controller.getMediaItemAt(it).mediaId })
        }
    }
    @Test fun removingCurrentSelectsSuccessorAndEmptyQueueIsSilent() {
        instrumentation.runOnMainSync {
            controller.removeMediaItem(1)
            assertEquals("demo-2", controller.currentMediaItem?.mediaId)
            controller.clearMediaItems()
            assertEquals(0, controller.mediaItemCount)
            assertFalse(controller.isPlaying)
        }
    }
}
