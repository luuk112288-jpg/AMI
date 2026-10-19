package com.ami.music.data

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class DeviceMusicScanner(private val context: Context, private val repository: LibraryRepository) {
    private val mutex = Mutex()
    companion object {
        fun permission(): String = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
        fun hasPermission(context: Context) = ContextCompat.checkSelfPermission(context, permission()) == PackageManager.PERMISSION_GRANTED
    }
    suspend fun scan(): Int = mutex.withLock {
        withContext(Dispatchers.IO) {
            val tracks = mutableListOf<Track>()
            val volumes = if (Build.VERSION.SDK_INT >= 29) MediaStore.getExternalVolumeNames(context) else setOf("external")
            // A failed or cancelled query must not replace the last known library.
            for (volume in volumes) {
                val collection = MediaStore.Audio.Media.getContentUri(volume)
                val projection = arrayOf("_id", "title", "artist", "album", "album_id", "duration", "_display_name", "mime_type", "_size", "date_added")
                val selection = "is_ringtone = 0 AND is_alarm = 0 AND is_notification = 0" + if (Build.VERSION.SDK_INT >= 29) " AND is_pending = 0" else ""
                val cursor = context.contentResolver.query(collection, projection, selection, null, "title ASC")
                    ?: error("Android could not read your music. Please try scanning again.")
                cursor.use {
                    fun text(name: String) = cleanTag(it.getString(it.getColumnIndexOrThrow(name)))
                    fun number(name: String) = it.getLong(it.getColumnIndexOrThrow(name))
                    while (it.moveToNext()) {
                        coroutineContext.ensureActive()
                        val id = number("_id")
                        val uri = ContentUris.withAppendedId(collection, id)
                        val tags = try { readAudioTags(context, uri) } catch (security: SecurityException) { throw security } catch (_: Exception) { AudioTags() }
                        val duration = tags.durationMs.takeIf { n -> n > 0 } ?: number("duration")
                        if (duration <= 0) continue
                        val filename = text("_display_name") ?: "Audio file"
                        val title = tags.title ?: text("title") ?: filename.substringBeforeLast('.')
                        val artist = tags.artist ?: text("artist") ?: "Unknown artist"
                        val album = tags.album ?: text("album") ?: "Unknown album"
                        val albumArtist = tags.albumArtist ?: artist
                        tracks += Track("device-$volume-$id", title, "artist-${musicKey(artist)}", artist,
                            "album-${musicKey(albumArtist)}-${musicKey(album)}", album, uri.toString(), duration,
                            artwork = (id % 4).toInt(), addedAt = number("date_added") * 1000,
                            artworkUri = tags.artworkUri, device = true, albumArtist = tags.albumArtist, genre = tags.genre,
                            year = tags.year, trackNumber = tags.trackNumber, fileName = filename,
                            mimeType = tags.mimeType ?: text("mime_type"), bitrate = tags.bitrate, sizeBytes = number("_size"))
                    }
                }
            }
            coroutineContext.ensureActive()
            repository.syncDeviceTracks(tracks)
            tracks.size
        }
    }
}
