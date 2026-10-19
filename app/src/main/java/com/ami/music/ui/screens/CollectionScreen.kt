package com.ami.music.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ami.music.data.*
import com.ami.music.playback.PlayerUiState
import com.ami.music.ui.components.*
import com.ami.music.ui.theme.*

@Composable fun CollectionScreen(
    kind: String, id: String, library: LibraryState, player: PlayerUiState,
    onBack: () -> Unit, onPlay: (List<Track>, Int) -> Unit, onShuffle: (Boolean) -> Unit,
    onRename: (Playlist) -> Unit, onDelete: (Playlist) -> Unit, onSearch: () -> Unit,
    onMove: (String, Int) -> Unit,
    trackRow: @Composable (Track, List<Track>, Int) -> Unit,
) {
    val playlist = library.playlists.find { it.id == id }
    val tracks = when (kind) {
        "playlist" -> library.playlistTracks(id)
        "liked" -> library.tracks.filter { it.id in library.likedIds }
        "album" -> library.tracks.filter { it.albumId == id }
        "artist" -> library.tracks.filter { it.artistId == id }
        else -> emptyList()
    }
    val title = when (kind) { "playlist" -> playlist?.name ?: "Playlist"; "liked" -> "Liked songs"; "artist" -> tracks.firstOrNull()?.artist ?: "Artist"; else -> tracks.firstOrNull()?.album ?: "Album" }
    var menu by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            PageHeader(kind.replaceFirstChar { it.uppercase() }, onBack, action = {
                if (playlist != null) Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, "Playlist options") }
                    DropdownMenu(menu, { menu = false }) {
                        DropdownMenuItem(text = { Text("Rename playlist") }, onClick = { menu = false; onRename(playlist) })
                        DropdownMenuItem(text = { Text("Delete playlist") }, onClick = { menu = false; onDelete(playlist) })
                    }
                }
            })
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                TrackArtwork(tracks.firstOrNull(), Modifier.widthIn(max = 250.dp).fillMaxWidth().aspectRatio(1f), if (kind == "liked") "ALL YOUR\nFAVORITES" else title)
                Spacer(Modifier.height(22.dp))
                Text(title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(7.dp))
                Text(if (kind == "artist") "Artist · ${tracks.size} tracks" else "${tracks.size} tracks · ${durationLabel(tracks.sumOf { it.durationMs })}", color = Muted, modifier = Modifier.fillMaxWidth())
                Row(Modifier.fillMaxWidth().padding(vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (kind == "playlist" || kind == "liked") "Curated by you" else if (tracks.any { it.provider == "audius" }) "Includes online tracks from Audius" else if (tracks.any { it.imported || it.device }) "From your collection" else "AMI Originals · demo music", color = Muted, modifier = Modifier.weight(1f))
                    IconButton(enabled = tracks.isNotEmpty() && player.connected, onClick = { onShuffle(!player.shuffle) }) { Icon(Icons.Default.Shuffle, if (player.shuffle) "Turn shuffle off" else "Turn shuffle on", tint = if (player.shuffle) Lime else Muted) }
                    FilledIconButton(enabled = tracks.isNotEmpty() && player.connected, onClick = { onPlay(tracks, 0) }, modifier = Modifier.size(56.dp)) { Icon(Icons.Default.PlayArrow, "Play $title", Modifier.size(32.dp)) }
                }
            }
        }
        if (tracks.isEmpty()) item { EmptyState(Icons.Default.MusicNote, "Your next favorite belongs here", "Find a track and use its menu to add it to a playlist, or tap the heart to save it.", "Find music", onSearch) }
        itemsIndexed(tracks, key = { _, track -> track.id }) { index, track ->
            Column {
                trackRow(track, tracks, index)
                if (kind == "playlist" && tracks.size > 1) Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.End) {
                    IconButton(onClick = { onMove(track.id, -1) }, enabled = index > 0) { Icon(Icons.Default.KeyboardArrowUp, "Move ${track.title} up") }
                    IconButton(onClick = { onMove(track.id, 1) }, enabled = index < tracks.lastIndex) { Icon(Icons.Default.KeyboardArrowDown, "Move ${track.title} down") }
                }
            }
        }
    }
}
