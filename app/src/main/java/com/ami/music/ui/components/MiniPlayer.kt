package com.ami.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ami.music.playback.PlayerUiState
import com.ami.music.ui.theme.*

@Composable fun MiniPlayer(state: PlayerUiState, onOpen: () -> Unit, onToggle: () -> Unit, onNext: () -> Unit) {
    val track = state.current ?: return
    Column(Modifier.padding(horizontal = 8.dp).clip(RoundedCornerShape(12.dp)).background(Panel).clickable(onClick = onOpen)) {
        Row(Modifier.padding(start = 8.dp, top = 7.dp, bottom = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            TrackArtwork(track, Modifier.size(46.dp))
            Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                Text(track.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(track.artist, style = MaterialTheme.typography.bodySmall, color = Muted, maxLines = 1)
            }
            if (state.buffering) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
            IconButton(onClick = onToggle) { Icon(if (state.playing) Icons.Default.Pause else Icons.Default.PlayArrow, if (state.playing) "Pause" else "Play") }
            IconButton(onClick = onNext) { Icon(Icons.Default.SkipNext, "Next track") }
        }
        LinearProgressIndicator(progress = { if (state.durationMs > 0) (state.elapsedMs.toFloat() / state.durationMs).coerceIn(0f, 1f) else 0f }, modifier = Modifier.fillMaxWidth().height(2.dp), color = Lime, trackColor = Panel)
    }
}
