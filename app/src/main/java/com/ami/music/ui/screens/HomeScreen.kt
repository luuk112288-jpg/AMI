package com.ami.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.ami.music.R
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ami.music.data.*
import com.ami.music.playback.PlayerUiState
import com.ami.music.ui.components.*
import com.ami.music.ui.theme.*

@Composable fun HomeScreen(
    library: LibraryState, player: PlayerUiState,
    onCollection: (String, String) -> Unit, onPlay: (List<Track>, Int) -> Unit,
    trackRow: @Composable (Track, List<Track>, Int) -> Unit,
    onAbout: () -> Unit, onSearch: () -> Unit,
) {
    val albums = library.tracks.distinctBy { it.albumId }
    val recent = library.recentIds.mapNotNull { id -> library.tracks.find { it.id == id } }.take(6)
    val featured = library.tracks.firstOrNull { it.id == "demo-0" } ?: library.tracks.firstOrNull()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.ami_logo), "AMI — Artificial Music Intelligence", Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)))
                Column(Modifier.weight(1f).padding(start = 10.dp)) {
                    Text("AMI", color = Lime, fontSize = 27.sp, fontWeight = FontWeight.Black)
                    Text("Artificial Music Intelligence", color = Muted, fontSize = 9.sp)
                }
                IconButton(onClick = onSearch) { Icon(Icons.Default.Search, "Search music") }
                FilledTonalIconButton(onClick = onAbout, modifier = Modifier.size(38.dp), shape = CircleShape) { Text("A", fontWeight = FontWeight.Bold) }
            }
            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 22.dp)) {
                Text("FIND YOUR FREQUENCY", color = Muted, style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(10.dp))
                Text("Less noise.\nMore feeling.", style = MaterialTheme.typography.headlineLarge)
            }
            if (featured != null) Box(Modifier.padding(horizontal = 20.dp).fillMaxWidth().heightIn(min = 260.dp).clip(RoundedCornerShape(18.dp)).clickable { onCollection("album", featured.albumId) }) {
                Artwork(0, Modifier.matchParentSize())
                Box(Modifier.matchParentSize().background(Brush.horizontalGradient(listOf(Color.Black.copy(alpha = .65f), Color.Transparent))))
                Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(28.dp)) {
                    Surface(color = Color.White.copy(alpha = .13f), shape = RoundedCornerShape(50)) {
                        Text("THE AMI ORIGINALS", Modifier.padding(horizontal = 12.dp, vertical = 7.dp), color = Color.White, fontSize = 9.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text("Midnight\nCurrent", fontSize = 36.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = (-1).sp)
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("A little escape. On repeat.", color = Color.White.copy(alpha = .8f), fontSize = 13.sp, modifier = Modifier.weight(1f))
                            FilledIconButton(onClick = { onPlay(library.tracks.filter { it.albumId == featured.albumId }, 0) }, enabled = player.connected, modifier = Modifier.size(52.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = Lime, contentColor = Ink)) { Icon(Icons.Default.PlayArrow, "Play Midnight Current", Modifier.size(30.dp)) }
                        }
                    }
                }
            }
        }
        if (recent.isNotEmpty()) {
            item { SectionTitle("Back in rotation") }
            items(recent, key = { "recent-${it.id}" }) { track -> trackRow(track, recent, recent.indexOf(track)) }
        }
        item { SectionTitle("Made for your moments") }
        item {
            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                items(albums, key = { it.albumId }) { album -> AlbumCard(album) { onCollection("album", album.albumId) } }
            }
        }
        item { SectionTitle("A new kind of familiar") }
        items(library.tracks.take(4), key = { "discover-${it.id}" }) { track -> trackRow(track, library.tracks, library.tracks.indexOf(track)) }
        item { SectionTitle("Meet your new favorites") }
        item {
            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                items(library.tracks.distinctBy { it.artistId }, key = { it.artistId }) { track ->
                    Column(Modifier.width(115.dp).clickable { onCollection("artist", track.artistId) }, horizontalAlignment = Alignment.CenterHorizontally) {
                        TrackArtwork(track, Modifier.size(110.dp).clip(CircleShape))
                        Spacer(Modifier.height(10.dp))
                        Text(track.artist, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("Artist", color = Muted, fontSize = 12.sp)
                    }
                }
            }
        }
        item { Text("YOUR MUSIC. YOUR LITTLE WORLD.", color = Muted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(24.dp)) }
    }
}

@Composable fun AlbumCard(track: Track, onClick: () -> Unit) {
    Column(Modifier.width(155.dp).clickable(onClick = onClick)) {
        TrackArtwork(track, Modifier.size(155.dp), track.album)
        Spacer(Modifier.height(10.dp))
        Text(track.album, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(track.artist, color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
