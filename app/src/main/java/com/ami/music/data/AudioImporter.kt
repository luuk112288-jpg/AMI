package com.ami.music.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.security.MessageDigest

sealed interface ImportResult {
    data class Success(val trackId: String) : ImportResult
    data class Duplicate(val trackId: String) : ImportResult
    data class Failure(val message: String) : ImportResult
}

class AudioImporter(private val context: Context, private val repository: LibraryRepository,
    private val storageDirectory: File = File(context.filesDir, "audio")) {
    private val mutex = Mutex()
    suspend fun refreshExistingMetadata() = mutex.withLock {
        withContext(Dispatchers.IO) {
            repository.dao.allTracks().filter { it.imported && it.metadataVersion == 0 }.forEach { track ->
                coroutineContext.ensureActive()
                try {
                    val tags = readAudioTags(context, Uri.parse(track.sourceUri))
                    repository.dao.upsertTracks(listOf(track.copy(artworkUri = tags.artworkUri, albumArtist = tags.albumArtist,
                        genre = tags.genre, year = tags.year, trackNumber = tags.trackNumber, mimeType = tags.mimeType,
                        bitrate = tags.bitrate, metadataVersion = 1)))
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { /* Keep unavailable imports intact and retry on the next launch. */ }
            }
        }
    }
    suspend fun importAudio(uri: Uri): ImportResult = mutex.withLock {
        withContext(Dispatchers.IO) {
            val directory = storageDirectory.apply { mkdirs() }
            var temporary: File? = null
            var movedFile: File? = null
            var committed = false
            try {
                val workingFile = File.createTempFile("import-", ".partial", directory)
                temporary = workingFile
                val digest = MessageDigest.getInstance("SHA-256")
                var size = 0L
                context.contentResolver.openInputStream(uri)?.use { input ->
                    workingFile.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            coroutineContext.ensureActive()
                            val count = input.read(buffer)
                            if (count < 0) break
                            size += count
                            require(size <= 512L * 1024 * 1024) { "Choose a file smaller than 512 MB." }
                            require(directory.usableSpace >= count + 1024 * 1024) { "Not enough free space to import this track." }
                            digest.update(buffer, 0, count)
                            output.write(buffer, 0, count)
                        }
                    }
                } ?: error("This file could not be opened. Please choose it again.")
                require(size > 0) { "This audio file is empty." }
                val hash = digest.digest().joinToString("") { "%02x".format(it) }
                repository.dao.duplicate(hash)?.let { return@withContext ImportResult.Duplicate(it.id) }
                val name = runCatching { context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                } }.getOrNull() ?: "Imported track"
                val metadata = MediaMetadataRetriever()
                val title: String
                val artist: String
                val album: String
                val duration: Long
                try {
                    metadata.setDataSource(workingFile.absolutePath)
                    duration = metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0
                    require(duration > 0) { "This file is not a supported audio track." }
                    val hasAudio = metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO)
                    require(hasAudio == "yes") { "Choose a file with a playable audio stream." }
                    title = metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.takeIf { it.isNotBlank() } ?: name.substringBeforeLast('.')
                    artist = metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.takeIf { it.isNotBlank() } ?: "Unknown artist"
                    album = metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)?.takeIf { it.isNotBlank() } ?: "Your imports"
                } finally { metadata.release() }
                val tags = readAudioTags(context, Uri.fromFile(workingFile))
                val finalFile = File(directory, "$hash.audio")
                val track = Track("local-$hash", title, "local-artist-${artist.lowercase()}", artist,
                    "local-album-${artist.lowercase()}-${album.lowercase()}", album, Uri.fromFile(finalFile).toString(), duration,
                    artwork = hash.take(2).toInt(16) % 4, imported = true, fingerprint = hash,
                    artworkUri = tags.artworkUri, albumArtist = tags.albumArtist, genre = tags.genre, year = tags.year,
                    trackNumber = tags.trackNumber, fileName = name, mimeType = tags.mimeType, bitrate = tags.bitrate, sizeBytes = size, metadataVersion = 1)
                withContext(NonCancellable) {
                    require(workingFile.renameTo(finalFile)) { "Could not save the audio file." }
                    movedFile = finalFile
                    repository.dao.insertTracks(listOf(track))
                    committed = true
                }
                ImportResult.Success(track.id)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                ImportResult.Failure(failure.message?.takeIf { it.isNotBlank() } ?: "Import failed. Check the audio file and available storage.")
            } finally {
                temporary?.delete()
                if (!committed) movedFile?.delete()
            }
        }
    }
}
