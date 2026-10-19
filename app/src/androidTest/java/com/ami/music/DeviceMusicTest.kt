package com.ami.music

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ami.music.data.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeviceMusicTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var db: AmiDatabase
    private lateinit var repo: LibraryRepository
    private var uri: Uri? = null
    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(context, AmiDatabase::class.java).build()
        repo = LibraryRepository(db)
        InstrumentationRegistry.getInstrumentation().uiAutomation.adoptShellPermissionIdentity("android.permission.READ_MEDIA_AUDIO", "android.permission.READ_EXTERNAL_STORAGE")
    }
    @After fun cleanup() {
        uri?.let { context.contentResolver.delete(it, null, null) }
        InstrumentationRegistry.getInstrumentation().uiAutomation.dropShellPermissionIdentity()
        db.close()
    }
    private fun addSong(): Uri {
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, "ami-test-${System.nanoTime()}.mp3")
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/mpeg")
            put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/AMI-tests")
            put(MediaStore.Audio.Media.IS_PENDING, 1)
        }
        val target = requireNotNull(context.contentResolver.insert(MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values))
        uri = target
        InstrumentationRegistry.getInstrumentation().context.assets.open("tagged.mp3").use { input ->
            context.contentResolver.openOutputStream(target)!!.use { input.copyTo(it) }
        }
        context.contentResolver.update(target, ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, 0) }, null, null)
        return target
    }
    @Test fun scanReadsEmbeddedMetadataAndCoverWithoutCopyingAudio() = runBlocking {
        val target = addSong()
        DeviceMusicScanner(context, repo).scan()
        val track = repo.dao.allTracks().single { it.sourceUri == target.toString() }
        assertTrue(track.device)
        assertFalse(track.imported)
        assertEquals("Phone Test Song", track.title)
        assertEquals("Phone Test Artist", track.artist)
        assertEquals("Phone Test Album", track.album)
        assertEquals("Various Test Artists", track.albumArtist)
        assertEquals("Electronic", track.genre)
        assertEquals("2024", track.year)
        assertTrue(track.durationMs > 0)
        assertNotNull(track.artworkUri)
        context.contentResolver.openInputStream(Uri.parse(track.artworkUri))!!.use { assertTrue(it.read() >= 0) }
        val ready = java.util.concurrent.CountDownLatch(1)
        val error = java.util.concurrent.atomic.AtomicReference<Throwable>()
        var player: androidx.media3.exoplayer.ExoPlayer? = null
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        try {
            instrumentation.runOnMainSync {
                player = androidx.media3.exoplayer.ExoPlayer.Builder(context).build().apply {
                    addListener(object : androidx.media3.common.Player.Listener {
                        override fun onPlaybackStateChanged(state: Int) { if (state == androidx.media3.common.Player.STATE_READY) ready.countDown() }
                        override fun onPlayerError(failure: androidx.media3.common.PlaybackException) { error.set(failure); ready.countDown() }
                    })
                    setMediaItem(androidx.media3.common.MediaItem.fromUri(track.sourceUri))
                    prepare()
                }
            }
            assertTrue("Scanned content URI should prepare for playback", ready.await(15, java.util.concurrent.TimeUnit.SECONDS))
            assertNull(error.get())
        } finally { instrumentation.runOnMainSync { player?.release() } }
        DeviceMusicScanner(context, repo).scan()
        assertEquals(1, repo.dao.allTracks().count { it.sourceUri == target.toString() })
    }
    @Test fun rescanKeepsLikesAndPlaylistWhenMetadataChangesOrFileDisappears() = runBlocking {
        val track = Track("device-test", "Before", "a", "Artist", "b", "Album", "content://media/external/audio/media/123", 1000, device = true)
        repo.syncDeviceTracks(listOf(track))
        repo.setLiked(track.id, true)
        val playlist = repo.createPlaylist("Phone favorites")
        repo.addToPlaylist(playlist, track.id)
        repo.syncDeviceTracks(listOf(track.copy(title = "After")))
        assertEquals("After", repo.dao.track(track.id)?.title)
        assertTrue(repo.state.first().likedIds.contains(track.id))
        assertEquals(track.id, repo.dao.playlistEntries(playlist).single().trackId)
        repo.syncDeviceTracks(emptyList())
        assertFalse(repo.dao.track(track.id)!!.available)
        assertTrue(repo.state.first().tracks.isEmpty())
        repo.syncDeviceTracks(listOf(track))
        assertEquals(track.id, repo.state.first().playlistTracks(playlist).single().id)
        assertTrue(repo.state.first().likedIds.contains(track.id))
    }
}
