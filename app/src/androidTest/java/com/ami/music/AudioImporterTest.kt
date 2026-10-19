package com.ami.music

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ami.music.data.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class AudioImporterTest {
    private lateinit var database: AmiDatabase
    private lateinit var repository: LibraryRepository
    private lateinit var importer: AudioImporter
    private lateinit var audioDirectory: File
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private val created = mutableListOf<File>()
    @Before fun setup() {
        database = Room.inMemoryDatabaseBuilder(context, AmiDatabase::class.java).build()
        repository = LibraryRepository(database)
        audioDirectory = File(context.cacheDir, "test-audio-${System.nanoTime()}").apply { mkdirs() }
        importer = AudioImporter(context, repository, audioDirectory)
    }
    @After fun cleanup() {
        runBlocking { repository.dao.allTracks().filter { it.imported }.forEach { File(Uri.parse(it.sourceUri).path!!).delete() } }
        created.forEach { it.delete() }
        audioDirectory.listFiles().orEmpty().forEach { it.delete() }
        audioDirectory.delete()
        database.close()
    }
    @Test fun invalidAudioCreatesNoTrack() = runBlocking {
        val file = File.createTempFile("bad-audio", ".wav", context.cacheDir).also { it.writeText("not audio"); created += it }
        assertTrue(importer.importAudio(Uri.fromFile(file)) is ImportResult.Failure)
        assertTrue(repository.dao.allTracks().isEmpty())
        assertTrue(audioDirectory.listFiles().orEmpty().none { it.extension == "partial" })
    }
    @Test fun importedSongKeepsEmbeddedCoverAndTags() = runBlocking {
        val file = File.createTempFile("tags", ".mp3", context.cacheDir).also { created += it }
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().context.assets.open("tagged.mp3").use { input -> file.outputStream().use { input.copyTo(it) } }
        assertTrue(importer.importAudio(Uri.fromFile(file)) is ImportResult.Success)
        val track = repository.dao.allTracks().single()
        assertEquals("Phone Test Album", track.album)
        assertEquals("Various Test Artists", track.albumArtist)
        assertNotNull(track.artworkUri)
        context.contentResolver.openInputStream(Uri.parse(track.artworkUri))!!.use { assertTrue(it.read() >= 0) }
    }
    @Test fun duplicateImportDoesNotDuplicateAndPrivateCopySurvives() = runBlocking {
        val file = File.createTempFile("demo", ".wav", context.cacheDir).also { created += it }
        context.assets.open("audio/demo-0.wav").use { input -> file.outputStream().use { input.copyTo(it) } }
        val first = importer.importAudio(Uri.fromFile(file))
        assertTrue(first is ImportResult.Success)
        assertTrue(importer.importAudio(Uri.fromFile(file)) is ImportResult.Duplicate)
        assertEquals(1, repository.dao.allTracks().size)
        file.delete()
        assertTrue(File(Uri.parse(repository.dao.allTracks().single().sourceUri).path!!).exists())
    }
}
