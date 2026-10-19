package com.ami.music.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ami.music.data.Track
import com.ami.music.data.MusicHttp
import com.ami.music.data.safeRemoteImage
import okhttp3.HttpUrl.Companion.toHttpUrl
import com.ami.music.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

private val coverCache = object : LruCache<String, Bitmap>(16 * 1024 * 1024) {
    override fun sizeOf(key: String, value: Bitmap) = value.byteCount
}
private val coverWorkers = Semaphore(3)

@Composable fun TrackArtwork(track: Track?, modifier: Modifier = Modifier, label: String? = null) {
    if (track == null || (!track.device && !track.imported && track.provider == null)) {
        Artwork(track?.artwork ?: 0, modifier, label)
        return
    }
    val context = LocalContext.current.applicationContext
    val cover by produceState<Bitmap?>(null, track.artworkUri) {
        value = null
        val source = track.artworkUri ?: return@produceState
        value = withContext(Dispatchers.IO) {
            coverWorkers.withPermit {
                coverCache.get(source) ?: runCatching {
                    if (safeRemoteImage(source) != null) {
                        val bytes = MusicHttp.get(MusicHttp.client, source.toHttpUrl(), 2 * 1024 * 1024)
                        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                        var sample = 1
                        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 768) sample *= 2
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })?.also { coverCache.put(source, it) }
                    } else {
                    val uri = Uri.parse(source)
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                    var sample = 1
                    while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 768) sample *= 2
                    context.contentResolver.openInputStream(uri)?.use {
                        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
                    }?.also { coverCache.put(source, it) }
                    }
                }.getOrNull()
            }
        }
    }
    Box(modifier.clip(RoundedCornerShape(10.dp)).background(Panel), contentAlignment = Alignment.Center) {
        val image = cover
        if (image != null) Image(image.asImageBitmap(), "Album cover: ${track.album}", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else {
            Icon(Icons.Default.MusicNote, "No album cover", Modifier.fillMaxSize(.42f), tint = Lime.copy(alpha = .65f))
            if (label != null) Text(label, Modifier.align(Alignment.BottomStart).padding(16.dp), color = Muted)
        }
    }
}
