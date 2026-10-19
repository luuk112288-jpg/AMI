package com.ami.music.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ami.music.playback.*
import com.ami.music.ui.components.*
import com.ami.music.ui.theme.*

@Composable fun QueueScreen(state: PlayerUiState, playback: PlaybackConnection, onBack: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            PageHeader("Your queue", onBack)
            Text(if (state.shuffle) "Shuffle is on. This is your original track order." else "Your listening order. Make it your own.", color = Muted, modifier = Modifier.padding(20.dp))
        }
        if (state.queue.isEmpty()) item { EmptyState(Icons.Default.QueueMusic, "No tracks queued", "Play an album, playlist, or track to build your queue.", "Back to music", onBack) }
        itemsIndexed(state.queue, key = { index, track -> "${track.id}-$index" }) { index, track ->
            Column {
                Row(Modifier.fillMaxWidth().clickable { playback.jumpTo(index) }.padding(start = 20.dp, end = 8.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    TrackArtwork(track, Modifier.size(48.dp))
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(track.title, color = if (state.current?.id == track.id) Lime else MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(track.artist, color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton(onClick = { playback.removeQueueItem(index) }) { Icon(Icons.Default.Close, "Remove ${track.title} from queue", tint = Muted) }
                }
                Row(Modifier.fillMaxWidth().padding(end = 8.dp), horizontalArrangement = Arrangement.End) {
                    IconButton(onClick = { playback.moveQueueItem(index, index - 1) }, enabled = index > 0) { Icon(Icons.Default.KeyboardArrowUp, "Move ${track.title} up") }
                    IconButton(onClick = { playback.moveQueueItem(index, index + 1) }, enabled = index < state.queue.lastIndex) { Icon(Icons.Default.KeyboardArrowDown, "Move ${track.title} down") }
                }
            }
        }
    }
}
