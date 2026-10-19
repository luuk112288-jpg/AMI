package com.ami.music

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ami.music.data.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LibraryRepositoryTest {
    private lateinit var database: AmiDatabase
    private lateinit var repository: LibraryRepository
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    @Before fun setup() { database = Room.inMemoryDatabaseBuilder(context, AmiDatabase::class.java).build(); repository = LibraryRepository(database) }
    @After fun close() { database.close() }
    @Test fun playlistOrderSurvivesEdit() = runBlocking {
        repository.ensureSeed()
        val id = repository.createPlaylist("Night drive")
        repository.setPlaylistTracks(id, listOf("demo-2", "demo-0", "demo-2"))
        assertEquals(listOf("demo-2", "demo-0"), repository.dao.playlistEntries(id).map { it.trackId })
        repository.setPlaylistTracks(id, listOf("demo-0", "demo-2"))
        assertEquals(listOf("demo-0", "demo-2"), repository.dao.playlistEntries(id).map { it.trackId })
    }
    @Test fun deletingPlaylistKeepsAudioAndLikes() = runBlocking {
        repository.ensureSeed()
        repository.setLiked("demo-0", true)
        val id = repository.createPlaylist("Keep the music")
        repository.addToPlaylist(id, "demo-0")
        repository.deletePlaylist(id)
        assertEquals(8, repository.dao.allTracks().size)
        assertTrue(repository.state.first().likedIds.contains("demo-0"))
        assertTrue(repository.dao.playlistEntries(id).isEmpty())
    }
    @Test fun seedingDoesNotDuplicateDemoTracks() = runBlocking {
        repository.ensureSeed(); repository.ensureSeed()
        assertEquals(8, repository.dao.allTracks().size)
    }
    @Test fun onlineQueueCacheDoesNotFillLibraryUntilSavedAndSurvivesPhoneScan() = runBlocking {
        val track = Track("audius-test", "Online", "audius-a", "Artist", "audius-b", "Release", "https://api.audius.co/v1/tracks/test/stream?app_name=AMI", 1000, provider = "audius")
        repository.cacheOnlineTracks(listOf(track))
        assertTrue(repository.state.first().tracks.isEmpty())
        repository.setLiked(track.id, true)
        assertEquals(track.id, repository.state.first().tracks.single().id)
        repository.syncDeviceTracks(emptyList())
        assertTrue(repository.dao.track(track.id)!!.available)
        val playlist = repository.createPlaylist("Mixed music")
        repository.addToPlaylist(playlist, track.id)
        repository.setLiked(track.id, false)
        assertEquals(track.id, repository.state.first().playlistTracks(playlist).single().id)
    }
    @Test fun consecutiveRemovalsUseCurrentDatabaseEntries() = runBlocking {
        repository.ensureSeed()
        val id = repository.createPlaylist("Edits")
        repository.setPlaylistTracks(id, listOf("demo-0", "demo-1", "demo-2"))
        repository.removeFromPlaylist(id, "demo-0")
        repository.removeFromPlaylist(id, "demo-1")
        assertEquals(listOf("demo-2"), repository.dao.playlistEntries(id).map { it.trackId })
    }
    @Test fun repeatedMovesUseTrackIdentity() = runBlocking {
        repository.ensureSeed()
        val id = repository.createPlaylist("Moves")
        repository.setPlaylistTracks(id, listOf("demo-0", "demo-1", "demo-2"))
        repository.movePlaylistTrack(id, "demo-2", -1)
        repository.movePlaylistTrack(id, "demo-2", -1)
        assertEquals(listOf("demo-2", "demo-0", "demo-1"), repository.dao.playlistEntries(id).map { it.trackId })
    }
    @Test fun blankPlaylistNameRejected() = runBlocking {
        try { repository.createPlaylist("  "); fail("Blank names must be rejected") } catch (_: IllegalArgumentException) { }
    }
    @Test fun createPlaylistPersistsAfterReopen() = runBlocking {
        val filename = "ami-test-${System.nanoTime()}.db"
        try {
            val first = Room.databaseBuilder(context, AmiDatabase::class.java, filename).build()
            val repo = LibraryRepository(first)
            repo.ensureSeed()
            val id = repo.createPlaylist("Saved")
            repo.setLiked("demo-1", true)
            first.close()
            val second = Room.databaseBuilder(context, AmiDatabase::class.java, filename).build()
            try {
                assertEquals("Saved", second.libraryDao().playlist(id)?.name)
                assertTrue(LibraryRepository(second).state.first().likedIds.contains("demo-1"))
            } finally { second.close() }
        } finally { context.deleteDatabase(filename) }
    }
}
