package com.ami.music.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ami.music.data.Track
import com.ami.music.ui.theme.Muted

@Composable fun TrackDetails(track: Track, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Song details") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf("Title" to track.title, "Artist" to track.artist, "Album" to track.album,
                "Album artist" to track.albumArtist, "Year" to track.year, "Genre" to track.genre,
                "Track number" to track.trackNumber, "Duration" to durationLabel(track.durationMs),
                "File" to track.fileName, "Format" to track.mimeType,
                "Bitrate" to track.bitrate?.toLongOrNull()?.let { "${it / 1000} kbps" },
                "Size" to track.sizeBytes?.let { "${"%.1f".format(it / 1048576.0)} MB" },
                "Source" to if (track.device) "On your phone" else if (track.imported) "Imported copy" else "AMI Originals")
                .filter { !it.second.isNullOrBlank() }.forEach { (name, value) ->
                    Column { Text(name, color = Muted, style = MaterialTheme.typography.labelSmall); Text(value.orEmpty()) }
                }
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } })
}
