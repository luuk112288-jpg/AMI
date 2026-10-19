package com.ami.music.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ami.music.data.LibraryState
import com.ami.music.data.Track
import com.ami.music.playback.*
import com.ami.music.ui.components.*
import com.ami.music.ui.theme.*

@Composable fun NowPlayingScreen(
    state: PlayerUiState, library: LibraryState, playback: PlaybackConnection,
    onBack: () -> Unit, onQueue: () -> Unit, onLike: (Track) -> Unit,
    onAdd: (Track) -> Unit, onArtist: (Track) -> Unit,
) {
    val track = state.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        PageHeader("Now playing", onBack, action = { IconButton(onClick = onQueue) { Icon(Icons.Default.QueueMusic, "Open queue") } })
        if (track == null) {
            EmptyState(Icons.Default.MusicNote, "A moment of quiet", "Choose a track to start listening.", "Find music", onBack)
        } else Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (track.imported) "FROM YOUR COLLECTION" else "AMI ORIGINALS", color = Muted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(vertical = 16.dp))
            Artwork(track.artwork, Modifier.widthIn(max = 380.dp).fillMaxWidth().aspectRatio(1f), track.album)
            Spacer(Modifier.height(28.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(track.title, style = MaterialTheme.typography.headlineMedium)
                    TextButton(onClick = { onArtist(track) }, contentPadding = PaddingValues(0.dp)) { Text(track.artist, color = Muted, fontSize = 16.sp) }
                }
                IconButton(onClick = { onLike(track) }) { Icon(if (track.id in library.likedIds) Icons.Default.Favorite else Icons.Default.FavoriteBorder, if (track.id in library.likedIds) "Unlike ${track.title}" else "Like ${track.title}", tint = if (track.id in library.likedIds) Lime else Muted) }
            }
            var seeking by remember(track.id) { mutableStateOf<Float?>(null) }
            val duration = state.durationMs.coerceAtLeast(1).toFloat()
            Slider(value = seeking ?: (state.elapsedMs.toFloat() / duration).coerceIn(0f, 1f), onValueChange = { seeking = it }, onValueChangeFinished = { seeking?.let { playback.seekTo((it * duration).toLong()) }; seeking = null }, enabled = state.durationMs > 0, modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Playback position"; stateDescription = "${durationLabel(state.elapsedMs)} of ${durationLabel(state.durationMs)}" })
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(durationLabel(seeking?.let { (it * duration).toLong() } ?: state.elapsedMs), color = Muted, fontSize = 11.sp)
                Text(durationLabel(state.durationMs), color = Muted, fontSize = 11.sp)
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 20.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { playback.setShuffle(!state.shuffle) }) { Icon(Icons.Default.Shuffle, if (state.shuffle) "Turn shuffle off" else "Turn shuffle on", tint = if (state.shuffle) Lime else Muted) }
                IconButton(onClick = playback::previous) { Icon(Icons.Default.SkipPrevious, "Previous track", Modifier.size(36.dp)) }
                FilledIconButton(onClick = playback::togglePlay, modifier = Modifier.size(72.dp)) {
                    if (state.buffering) CircularProgressIndicator(Modifier.size(28.dp), color = Ink, strokeWidth = 3.dp)
                    else Icon(if (state.playing) Icons.Default.Pause else Icons.Default.PlayArrow, if (state.playing) "Pause" else "Play", Modifier.size(40.dp))
                }
                IconButton(onClick = playback::next) { Icon(Icons.Default.SkipNext, "Next track", Modifier.size(36.dp)) }
                IconButton(onClick = { playback.setRepeat(when (state.repeat) { RepeatMode.Off -> RepeatMode.All; RepeatMode.All -> RepeatMode.One; RepeatMode.One -> RepeatMode.Off }) }) { Icon(if (state.repeat == RepeatMode.One) Icons.Default.RepeatOne else Icons.Default.Repeat, "Repeat: ${state.repeat.name}. Change repeat mode", tint = if (state.repeat != RepeatMode.Off) Lime else Muted) }
            }
            if (state.error != null) Card(Modifier.fillMaxWidth().padding(bottom = 16.dp)) { Text(state.error, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error) }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = playback::toggleMute) { Icon(if (state.volume == 0f) Icons.Default.VolumeOff else Icons.Default.VolumeUp, if (state.volume == 0f) "Unmute" else "Mute", tint = Muted) }
                Slider(value = state.volume, onValueChange = playback::setVolume, modifier = Modifier.weight(1f).semantics { contentDescription = "Volume"; stateDescription = "${(state.volume * 100).toInt()} percent" })
                IconButton(onClick = { onAdd(track) }) { Icon(Icons.Default.PlaylistAdd, "Add to playlist", tint = Muted) }
            }
            Text("YOUR SOUNDTRACK, EVERYWHERE YOU GO.", color = Muted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(vertical = 26.dp))
        }
    }
}
