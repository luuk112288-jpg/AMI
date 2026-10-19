package com.ami.music.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryDao {
    @Query("SELECT * FROM tracks ORDER BY addedAt DESC, id ASC") fun tracks(): Flow<List<Track>>
    @Query("SELECT * FROM playlists ORDER BY updatedAt DESC, id ASC") fun playlists(): Flow<List<Playlist>>
    @Query("SELECT * FROM playlist_entries ORDER BY playlistId, position") fun entries(): Flow<List<PlaylistEntry>>
    @Query("SELECT * FROM likes") fun likes(): Flow<List<Like>>
    @Query("SELECT * FROM history ORDER BY playedAt DESC LIMIT 30") fun history(): Flow<List<History>>
    @Query("SELECT * FROM tracks WHERE id = :id") suspend fun track(id: String): Track?
    @Query("SELECT * FROM tracks WHERE fingerprint = :hash LIMIT 1") suspend fun duplicate(hash: String): Track?
    @Query("SELECT * FROM tracks") suspend fun allTracks(): List<Track>
    @Query("SELECT * FROM playlists WHERE id = :id") suspend fun playlist(id: String): Playlist?
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertTracks(tracks: List<Track>)
    @Upsert suspend fun upsertTracks(tracks: List<Track>)
    @Query("UPDATE tracks SET available = 0 WHERE device = 1") suspend fun markDeviceTracksUnavailable()
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertPlaylist(playlist: Playlist)
    @Update suspend fun updatePlaylist(playlist: Playlist)
    @Query("DELETE FROM playlists WHERE id = :id") suspend fun deletePlaylist(id: String)
    @Query("DELETE FROM playlist_entries WHERE playlistId = :id") suspend fun clearEntries(id: String)
    @Query("SELECT * FROM playlist_entries WHERE playlistId = :id ORDER BY position") suspend fun playlistEntries(id: String): List<PlaylistEntry>
    @Insert suspend fun insertEntries(entries: List<PlaylistEntry>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun like(like: Like)
    @Query("DELETE FROM likes WHERE trackId = :id") suspend fun unlike(id: String)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun record(history: History)
}
