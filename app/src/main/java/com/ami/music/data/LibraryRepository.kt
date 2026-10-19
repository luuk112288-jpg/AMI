package com.ami.music.data

import androidx.room.withTransaction
import com.ami.music.playback.PlaybackPolicy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

class LibraryRepository(private val database: AmiDatabase) {
    val dao = database.libraryDao()
    private val seedMutex = Mutex()
    private var seeded = false
    val state: Flow<LibraryState> = combine(dao.tracks(), dao.playlists(), dao.entries(), dao.likes(), dao.history()) { tracks, playlists, entries, likes, history ->
        LibraryState(tracks, playlists, entries, likes.map { it.trackId }.toSet(), history.map { it.trackId })
    }.retryWhen { _, _ ->
        emit(LibraryState(error = "Your library could not be read. Check free storage and try again."))
        delay(3000)
        true
    }
    fun observeTracks() = dao.tracks()
    fun observePlaylists() = dao.playlists()
    suspend fun ensureSeed() = seedMutex.withLock {
        if (!seeded) { dao.insertTracks(DemoCatalog.tracks()); seeded = true }
    }
    suspend fun setLiked(trackId: String, liked: Boolean) {
        requireNotNull(dao.track(trackId)) { "This track is no longer in your library." }
        if (liked) dao.like(Like(trackId)) else dao.unlike(trackId)
    }
    suspend fun createPlaylist(name: String): String {
        require(PlaybackPolicy.validPlaylistName(name)) { "Give your playlist a name." }
        val id = UUID.randomUUID().toString()
        dao.insertPlaylist(Playlist(id, name.trim().take(120)))
        return id
    }
    suspend fun renamePlaylist(id: String, name: String) {
        require(PlaybackPolicy.validPlaylistName(name)) { "Give your playlist a name." }
        val playlist = requireNotNull(dao.playlist(id)) { "Playlist not found." }
        dao.updatePlaylist(playlist.copy(name = name.trim().take(120), updatedAt = System.currentTimeMillis()))
    }
    suspend fun deletePlaylist(id: String) = dao.deletePlaylist(id)
    suspend fun setPlaylistTracks(id: String, trackIds: List<String>) = database.withTransaction {
        val playlist = requireNotNull(dao.playlist(id)) { "Playlist not found." }
        val validIds = dao.allTracks().map { it.id }.toSet()
        val ids = PlaybackPolicy.uniqueTracks(trackIds).filter { it in validIds }
        dao.clearEntries(id)
        dao.insertEntries(ids.mapIndexed { index, trackId -> PlaylistEntry(id, trackId, index) })
        dao.updatePlaylist(playlist.copy(updatedAt = System.currentTimeMillis()))
    }
    suspend fun addToPlaylist(id: String, trackId: String) = database.withTransaction {
        setPlaylistTracks(id, dao.playlistEntries(id).map { it.trackId } + trackId)
    }
    suspend fun removeFromPlaylist(id: String, trackId: String) = database.withTransaction {
        setPlaylistTracks(id, dao.playlistEntries(id).map { it.trackId }.filter { it != trackId })
    }
    suspend fun movePlaylistTrack(id: String, trackId: String, delta: Int) = database.withTransaction {
        val ids = dao.playlistEntries(id).map { it.trackId }.toMutableList()
        val from = ids.indexOf(trackId)
        if (from >= 0) {
            val to = (from + delta).coerceIn(ids.indices)
            ids.add(to, ids.removeAt(from))
            setPlaylistTracks(id, ids)
        }
    }
    suspend fun recordPlayed(id: String) { if (dao.track(id) != null) dao.record(History(id)) }
    suspend fun search(query: String): SearchResults = state.first().search(query)
}
