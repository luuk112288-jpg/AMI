package com.ami.music.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ami.music.data.*
import com.ami.music.ui.components.*
import com.ami.music.ui.theme.Muted

@Composable fun SearchScreen(
    library: LibraryState, onCollection: (String, String) -> Unit,
    trackRow: @Composable (Track, List<Track>, Int) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("All") }
    val results = remember(library, query) { library.search(query) }
    val tracks = if (filter in listOf("All", "Tracks")) results.tracks else emptyList()
    val albums = if (filter in listOf("All", "Albums")) results.albums else emptyList()
    val artists = if (filter in listOf("All", "Artists")) results.artists else emptyList()
    val playlists = if (filter in listOf("All", "Playlists")) results.playlists else emptyList()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 20.dp)) {
        item { PageHeader("Search") }
        item {
            OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                label = { Text("Tracks, artists, albums…") }, singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Default.Close, "Clear search") } })
            Spacer(Modifier.height(12.dp))
            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("All", "Tracks", "Albums", "Artists", "Playlists")) { name -> FilterChip(selected = filter == name, onClick = { filter = name }, label = { Text(name) }) }
            }
        }
        if (query.isBlank() && filter == "All") {
            item { SectionTitle("What's your mood?") }
            item {
                val moods = listOf("Night drive" to "Midnight", "Deep focus" to "Soft", "Slow mornings" to "Blue", "Fresh energy" to "Afterglow")
                Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    moods.chunked(2).forEachIndexed { row, pair ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            pair.forEachIndexed { col, mood -> Box(Modifier.weight(1f).height(115.dp).clickable { query = mood.second }) { Artwork(row * 2 + col, Modifier.fillMaxSize(), mood.first) } }
                        }
                    }
                }
            }
        }
        if (tracks.isNotEmpty()) {
            item { SectionTitle(if (query.isBlank()) "All tracks" else "Tracks") }
            items(tracks, key = { "track-${it.id}" }) { track -> trackRow(track, tracks, tracks.indexOf(track)) }
        }
        if (albums.isNotEmpty()) {
            item { SectionTitle("Albums") }
            item { LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) { items(albums, key = { it.albumId }) { AlbumCard(it) { onCollection("album", it.albumId) } } } }
        }
        if (artists.isNotEmpty()) {
            item { SectionTitle("Artists") }
            items(artists, key = { "artist-${it.artistId}" }) { track ->
                ListItem(headlineContent = { Text(track.artist) }, supportingContent = { Text("Artist", color = Muted) }, leadingContent = { Artwork(track.artwork, Modifier.size(50.dp)) }, modifier = Modifier.clickable { onCollection("artist", track.artistId) })
            }
        }
        if (playlists.isNotEmpty()) {
            item { SectionTitle("Playlists") }
            items(playlists, key = { "playlist-${it.id}" }) { playlist ->
                ListItem(headlineContent = { Text(playlist.name, maxLines = 1, overflow = TextOverflow.Ellipsis) }, leadingContent = { Icon(Icons.Default.PlaylistPlay, null) }, modifier = Modifier.clickable { onCollection("playlist", playlist.id) })
            }
        }
        if (tracks.isEmpty() && albums.isEmpty() && artists.isEmpty() && playlists.isEmpty()) item {
            EmptyState(Icons.Default.SearchOff, "Nothing here yet", if (query.isBlank()) "Your $filter will appear here." else "Try a different title, album, or artist.", if (query.isBlank()) null else "Clear search", { query = "" })
        }
    }
}
