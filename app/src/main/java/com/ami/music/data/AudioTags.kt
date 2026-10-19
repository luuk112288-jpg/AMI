package com.ami.music.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.security.MessageDigest
import java.util.Locale

data class AudioTags(
    val title: String? = null, val artist: String? = null, val album: String? = null,
    val albumArtist: String? = null, val genre: String? = null, val year: String? = null,
    val trackNumber: String? = null, val durationMs: Long = 0, val mimeType: String? = null,
    val bitrate: String? = null, val artworkUri: String? = null,
)

fun cleanTag(value: String?): String? = value?.trim()?.takeUnless { it.isBlank() || it == "<unknown>" }
fun musicKey(value: String): String = value.trim().lowercase(Locale.ROOT)

/** Called only on an IO dispatcher; cache only a bounded thumbnail, never the audio. */
fun readAudioTags(context: Context, uri: Uri): AudioTags {
    val reader = MediaMetadataRetriever()
    try {
        reader.setDataSource(context, uri)
        fun tag(key: Int) = cleanTag(reader.extractMetadata(key))
        val cover = runCatching { reader.embeddedPicture?.let { saveCover(context, it) } }.getOrNull()
        return AudioTags(tag(MediaMetadataRetriever.METADATA_KEY_TITLE), tag(MediaMetadataRetriever.METADATA_KEY_ARTIST),
            tag(MediaMetadataRetriever.METADATA_KEY_ALBUM), tag(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST),
            tag(MediaMetadataRetriever.METADATA_KEY_GENRE), tag(MediaMetadataRetriever.METADATA_KEY_YEAR),
            tag(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER), tag(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0,
            tag(MediaMetadataRetriever.METADATA_KEY_MIMETYPE), tag(MediaMetadataRetriever.METADATA_KEY_BITRATE), cover)
    } finally { reader.release() }
}

private fun saveCover(context: Context, bytes: ByteArray): String? {
    if (bytes.size > 12 * 1024 * 1024) return null
    val hash = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    val directory = File(context.filesDir, "covers").apply { mkdirs() }
    val output = File(directory, "$hash.jpg")
    if (!output.exists()) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 768) sample *= 2
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
        val temporary = File.createTempFile("cover-", ".tmp", directory)
        try {
            temporary.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it)) }
            if (!temporary.renameTo(output) && !output.exists()) error("Could not save cover")
        } finally { bitmap.recycle(); temporary.delete() }
    }
    return FileProvider.getUriForFile(context, "${context.packageName}.covers", output).toString()
}
