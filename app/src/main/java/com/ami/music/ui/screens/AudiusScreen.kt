package com.ami.music.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.ami.music.data.*
import com.ami.music.ui.components.*
import com.ami.music.ui.theme.*

@Composable fun AudiusScreen(
    state: CatalogUiState, onQuery: (String) -> Unit, onRetry: () -> Unit, onMore: () -> Unit,
    trackRow: @Composable (Track, List<Track>, Int) -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 28.dp)) {
        item {
            PageHeader("Discover on Audius")
            Text("Independent sounds. Your next favorite.", color = Muted, modifier = Modifier.padding(horizontal = 20.dp))
            OutlinedTextField(state.query, onQuery, label = { Text("Search online music") }, singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = { if (state.query.isNotEmpty()) IconButton(onClick = { onQuery("") }) { Icon(Icons.Default.Close, "Clear online search") } },
                modifier = Modifier.fillMaxWidth().padding(20.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(if (state.query.isBlank()) "TRENDING NOW" else "SEARCH RESULTS", color = Lime, style = MaterialTheme.typography.labelMedium)
                TextButton(onClick = { uriHandler.openUri("https://audius.co") }, contentPadding = PaddingValues(0.dp)) { Text("Powered by Audius", color = Muted, style = MaterialTheme.typography.labelSmall) }
            }
            Text("Streams use internet data. Only publicly playable tracks are shown; Audius has its own catalog.", color = Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
        }
        if (state.error != null) item {
            Card(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text(state.error, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = onRetry) { Text("Try again") }
                }
            }
        }
        itemsIndexed(state.tracks, key = { _, track -> track.id }) { index, track -> trackRow(track, state.tracks, index) }
        if (state.loading) item { LinearProgressIndicator(Modifier.fillMaxWidth().padding(20.dp)) }
        if (!state.loading && state.loaded && state.error == null && state.tracks.isEmpty()) item {
            EmptyState(Icons.Default.LibraryMusic, "No public tracks found", "Try another title or artist. Some Audius releases require access through Audius.")
        }
        if (state.hasMore && !state.loading) item {
            TextButton(onClick = onMore, modifier = Modifier.fillMaxWidth().padding(12.dp)) { Text("Load more tracks") }
        }
    }
}
