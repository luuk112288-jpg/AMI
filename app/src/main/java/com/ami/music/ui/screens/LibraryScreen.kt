package com.ami.music.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ami.music.data.*
import com.ami.music.ui.components.*
import com.ami.music.ui.theme.*

@Composable fun LibraryScreen(
    library: LibraryState, importing: Boolean, onCollection: (String, String) -> Unit,
    onCreate: () -> Unit, onImport: () -> Unit,
    scanning: Boolean, scanMessage: String?, onScan: () -> Unit, onSettings: () -> Unit,
    trackRow: @Composable (Track, List<Track>, Int) -> Unit,
) {
    var filter by rememberSaveable { mutableStateOf("On device") }
    val imported = library.tracks.filter { it.imported }
    val deviceTracks = library.tracks.filter { it.device }.sortedBy { it.title.lowercase() }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            PageHeader("Your library", action = {
                IconButton(onClick = onImport, enabled = !importing) { Icon(Icons.Default.FileUpload, "Import audio") }
                IconButton(onClick = onCreate) { Icon(Icons.Default.Add, "Create playlist") }
            })
            Text("ALL THE MUSIC THAT FEELS LIKE YOU", color = Muted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("On device", "Playlists", "Tracks", "Albums", "Artists", "Imports")) { name -> FilterChip(selected = filter == name, onClick = { filter = name }, label = { Text(name) }) }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onScan, enabled = !scanning) { Icon(Icons.Default.FolderOpen, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(if (scanning) "Scanning…" else "Scan phone") }
                TextButton(onClick = onImport, enabled = !importing) { Text("Import audio") }
            }
            if (scanMessage != null) {
                Text(scanMessage, color = Muted, modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp))
                if (scanMessage.contains("settings")) TextButton(onClick = onSettings, modifier = Modifier.padding(horizontal = 12.dp)) { Text("Open settings") }
            }
            if (scanning) LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 20.dp))
            if (importing) LinearProgressIndicator(Modifier.fillMaxWidth().padding(20.dp))
        }
        when (filter) {
            "On device" -> {
                item { Text("Songs on your phone, played in place. Scan again after downloading music. Some unindexed downloads need Import audio.", color = Muted, modifier = Modifier.padding(20.dp)) }
                if (deviceTracks.isEmpty() && !scanning) item { EmptyState(Icons.Default.LibraryMusic, "Your music, right here", "Scan your phone to see your songs, albums and cover art. AMI never changes the original files.") }
                itemsIndexed(deviceTracks, key = { _, track -> track.id }) { index, track -> trackRow(track, deviceTracks, index) }
            }
            "Playlists" -> {
                item {
                    ListItem(headlineContent = { Text("Liked songs", style = MaterialTheme.typography.titleMedium) }, supportingContent = { Text("${library.likedIds.size} tracks · made by you", color = Muted) },
                        leadingContent = { Surface(color = Lime.copy(alpha = .16f), shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)) { Icon(Icons.Default.Favorite, null, tint = Lime, modifier = Modifier.padding(16.dp)) } }, modifier = Modifier.clickable { onCollection("liked", "all") })
                }
                items(library.playlists, key = { it.id }) { playlist ->
                    val tracks = library.playlistTracks(playlist.id)
                    ListItem(headlineContent = { Text(playlist.name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium) }, supportingContent = { Text("Playlist · ${tracks.size} tracks", color = Muted) },
                        leadingContent = { TrackArtwork(tracks.firstOrNull(), Modifier.size(56.dp)) }, modifier = Modifier.clickable { onCollection("playlist", playlist.id) })
                }
                if (library.playlists.isEmpty()) item { EmptyState(Icons.Default.PlaylistAdd, "Make room for your favorites", "A late-night mix. A morning ritual. Make a playlist that's all yours.", "Create playlist", onCreate) }
            }
            "Tracks" -> items(library.tracks, key = { it.id }) { track -> trackRow(track, library.tracks, library.tracks.indexOf(track)) }
            "Albums" -> items(library.tracks.distinctBy { it.albumId }, key = { it.albumId }) { track ->
                ListItem(headlineContent = { Text(track.album) }, supportingContent = { Text(track.artist, color = Muted) }, leadingContent = { TrackArtwork(track, Modifier.size(56.dp)) }, modifier = Modifier.clickable { onCollection("album", track.albumId) })
            }
            "Artists" -> items(library.tracks.distinctBy { it.artistId }, key = { it.artistId }) { track ->
                ListItem(headlineContent = { Text(track.artist) }, supportingContent = { Text("Artist", color = Muted) }, leadingContent = { TrackArtwork(track, Modifier.size(56.dp)) }, modifier = Modifier.clickable { onCollection("artist", track.artistId) })
            }
            "Imports" -> {
                item { Text("Audio you import is copied to this device and stays private. Uninstalling AMI removes these copies.", color = Muted, modifier = Modifier.padding(20.dp)) }
                if (imported.isEmpty()) item { EmptyState(Icons.Default.AudioFile, "Bring your own sound", "Choose audio from your phone. Your tracks will live alongside your AMI favorites.", "Import audio", onImport) }
                items(imported, key = { it.id }) { track -> trackRow(track, imported, imported.indexOf(track)) }
            }
        }
    }
}
