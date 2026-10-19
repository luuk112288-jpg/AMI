package com.ami.music.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "tracks", indices = [Index("albumId"), Index("artistId")])
data class Track(
    @PrimaryKey val id: String,
    val title: String,
    val artistId: String,
    val artist: String,
    val albumId: String,
    val album: String,
    val sourceUri: String,
    val durationMs: Long,
    val artwork: Int = 0,
    val imported: Boolean = false,
    val fingerprint: String? = null,
    val addedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "playlist_entries", primaryKeys = ["playlistId", "trackId"],
    foreignKeys = [
        ForeignKey(entity = Playlist::class, parentColumns = ["id"], childColumns = ["playlistId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Track::class, parentColumns = ["id"], childColumns = ["trackId"], onDelete = ForeignKey.CASCADE),
    ], indices = [Index("trackId"), Index(value = ["playlistId", "position"], unique = true)],
)
data class PlaylistEntry(val playlistId: String, val trackId: String, val position: Int)

@Entity(tableName = "likes", foreignKeys = [ForeignKey(entity = Track::class, parentColumns = ["id"], childColumns = ["trackId"], onDelete = ForeignKey.CASCADE)])
data class Like(@PrimaryKey val trackId: String)

@Entity(tableName = "history", foreignKeys = [ForeignKey(entity = Track::class, parentColumns = ["id"], childColumns = ["trackId"], onDelete = ForeignKey.CASCADE)])
data class History(@PrimaryKey val trackId: String, val playedAt: Long = System.currentTimeMillis())

data class LibraryState(
    val tracks: List<Track> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val entries: List<PlaylistEntry> = emptyList(),
    val likedIds: Set<String> = emptySet(),
    val recentIds: List<String> = emptyList(),
    val error: String? = null,
) {
    fun playlistTracks(id: String): List<Track> {
        val byId = tracks.associateBy { it.id }
        return entries.filter { it.playlistId == id }.sortedBy { it.position }.mapNotNull { byId[it.trackId] }
    }
}

data class SearchResults(
    val tracks: List<Track>, val albums: List<Track>,
    val artists: List<Track>, val playlists: List<Playlist>,
)

fun LibraryState.search(query: String): SearchResults {
    val q = query.trim()
    return SearchResults(
        tracks.filter { "${it.title} ${it.artist} ${it.album}".contains(q, true) },
        tracks.distinctBy { it.albumId }.filter { "${it.album} ${it.artist}".contains(q, true) },
        tracks.distinctBy { it.artistId }.filter { it.artist.contains(q, true) },
        playlists.filter { it.name.contains(q, true) },
    )
}
