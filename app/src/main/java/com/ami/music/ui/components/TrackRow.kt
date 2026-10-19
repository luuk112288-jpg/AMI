package com.ami.music.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ami.music.data.Track
import com.ami.music.ui.theme.Lime
import com.ami.music.ui.theme.Muted

fun durationLabel(ms: Long): String {
    val seconds = ms.coerceAtLeast(0) / 1000
    return "%d:%02d".format(seconds / 60, seconds % 60)
}

@Composable fun TrackRow(
    track: Track, active: Boolean, liked: Boolean,
    onPlay: () -> Unit, onLike: () -> Unit, onAdd: () -> Unit,
    onAlbum: () -> Unit, onArtist: () -> Unit,
    onRemove: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var menu by remember { mutableStateOf(false) }
    Row(modifier.fillMaxWidth().clickable(onClick = onPlay).padding(start = 20.dp, end = 8.dp, top = 7.dp, bottom = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Artwork(track.artwork, Modifier.size(52.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(track.title, color = if (active) Lime else MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (active) { Icon(Icons.Default.GraphicEq, "Current track", tint = Lime, modifier = Modifier.size(14.dp)); Spacer(Modifier.width(5.dp)) }
                Text(track.artist, color = Muted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        IconButton(onClick = onLike) { Icon(if (liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder, if (liked) "Unlike ${track.title}" else "Like ${track.title}", tint = if (liked) Lime else Muted, modifier = Modifier.size(20.dp)) }
        Box {
            IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, "More options for ${track.title}", tint = Muted) }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text("Add to playlist") }, onClick = { menu = false; onAdd() })
                DropdownMenuItem(text = { Text("Go to album") }, onClick = { menu = false; onAlbum() })
                DropdownMenuItem(text = { Text("Go to artist") }, onClick = { menu = false; onArtist() })
                if (onRemove != null) DropdownMenuItem(text = { Text("Remove from playlist") }, onClick = { menu = false; onRemove() })
            }
        }
    }
}
