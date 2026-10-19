package com.ami.music.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ami.music.AmiApplication
import com.ami.music.data.*
import com.ami.music.playback.PlaybackConnection
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

class AmiViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as AmiApplication
    private val repository = app.repository
    val library = repository.state.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LibraryState())
    val playback = PlaybackConnection(application, repository)
    val player = playback.state
    private val catalogClient = AudiusCatalog()
    private val mutableCatalog = MutableStateFlow(CatalogUiState())
    val catalog = mutableCatalog.asStateFlow()
    private var catalogJob: Job? = null
    fun openCatalog() { if (!mutableCatalog.value.loaded && !mutableCatalog.value.loading) loadCatalog(mutableCatalog.value.query) }
    fun searchCatalog(query: String) { loadCatalog(query.take(200), debounce = true) }
    fun retryCatalog() { loadCatalog(mutableCatalog.value.query) }
    fun moreCatalog() { if (!mutableCatalog.value.loading && mutableCatalog.value.hasMore) loadCatalog(mutableCatalog.value.query, append = true) }
    private fun loadCatalog(query: String, append: Boolean = false, debounce: Boolean = false) {
        catalogJob?.cancel()
        val previous = mutableCatalog.value
        val offset = if (append) previous.nextOffset else 0
        mutableCatalog.value = previous.copy(query = query, tracks = if (append) previous.tracks else emptyList(), loading = true, error = null, hasMore = if (append) previous.hasMore else false)
        catalogJob = viewModelScope.launch {
            try {
                if (debounce) delay(450)
                val page = catalogClient.load(query, offset)
                mutableCatalog.value = CatalogUiState(query, ((if (append) previous.tracks else emptyList()) + page.tracks).distinctBy { it.id },
                    nextOffset = page.nextOffset, hasMore = page.hasMore, loaded = true)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { mutableCatalog.value = mutableCatalog.value.copy(loading = false, loaded = true, error = error.message ?: "Online music is unavailable. Please retry.") }
        }
    }
    fun playTracks(tracks: List<Track>, index: Int) { launchAction { repository.cacheOnlineTracks(tracks); playback.playTracks(tracks, index) } }
    private val mutableMessages = MutableSharedFlow<String>(extraBufferCapacity = 10)
    val messages = mutableMessages.asSharedFlow()
    private val mutableImporting = MutableStateFlow(false)
    val importing = mutableImporting.asStateFlow()
    private val mutableScanning = MutableStateFlow(false)
    val scanning = mutableScanning.asStateFlow()
    private val mutableScanMessage = MutableStateFlow<String?>(null)
    val scanMessage = mutableScanMessage.asStateFlow()
    fun scanPhone() {
        if (mutableScanning.value) return
        if (!DeviceMusicScanner.hasPermission(app)) { musicPermissionDenied(); return }
        mutableScanning.value = true
        mutableScanMessage.value = null
        viewModelScope.launch {
            try {
                val count = app.deviceScanner.scan()
                mutableScanMessage.value = if (count == 0) "No songs found. Use Import audio for files Android has not indexed." else "$count songs found on your phone."
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: SecurityException) { musicPermissionDenied() }
            catch (_: Exception) { mutableScanMessage.value = "Could not scan your music. Your saved library is unchanged. Try again." }
            finally { mutableScanning.value = false }
        }
    }
    fun musicPermissionDenied() {
        mutableScanMessage.value = "Allow Music and audio access in Android settings to scan your phone, or use Import audio to choose files."
    }
    private val mutableReady = MutableStateFlow(false)
    val ready = mutableReady.asStateFlow()
    private val mutableStartupError = MutableStateFlow<String?>(null)
    val startupError = mutableStartupError.asStateFlow()
    init { initialize() }
    fun initialize() {
        mutableStartupError.value = null
        viewModelScope.launch {
            try { repository.ensureSeed(); mutableReady.value = true; app.importer.refreshExistingMetadata() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { mutableStartupError.value = "Your library could not be opened. Check that your phone has free storage, then retry." }
        }
    }

    private fun launchAction(block: suspend () -> Unit) = viewModelScope.launch {
        try { block() }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (failure: Exception) { mutableMessages.emit(failure.message ?: "Something went wrong. Please try again.") }
    }
    fun like(track: Track) = launchAction { repository.cacheOnlineTracks(listOf(track)); repository.setLiked(track.id, track.id !in library.value.likedIds) }
    fun createPlaylist(name: String, onCreated: (String) -> Unit) = launchAction { onCreated(repository.createPlaylist(name)) }
    fun renamePlaylist(id: String, name: String) = launchAction { repository.renamePlaylist(id, name) }
    fun deletePlaylist(id: String, onDeleted: () -> Unit) = launchAction { repository.deletePlaylist(id); onDeleted() }
    fun addToPlaylist(id: String, track: Track) = launchAction {
        repository.cacheOnlineTracks(listOf(track))
        repository.addToPlaylist(id, track.id)
        mutableMessages.emit("Added to playlist")
    }
    fun removeFromPlaylist(id: String, track: Track) = launchAction {
        repository.removeFromPlaylist(id, track.id)
    }
    fun movePlaylistTrack(id: String, trackId: String, delta: Int) = launchAction {
        repository.movePlaylistTrack(id, trackId, delta)
    }
    fun importAudio(uris: List<Uri>) {
        if (uris.isEmpty() || mutableImporting.value) return
        mutableImporting.value = true
        viewModelScope.launch {
            try {
                var count = 0
                var duplicates = 0
                uris.forEach { uri ->
                    when (val result = app.importer.importAudio(uri)) {
                        is ImportResult.Success -> count++
                        is ImportResult.Duplicate -> duplicates++
                        is ImportResult.Failure -> mutableMessages.emit(result.message)
                    }
                }
                if (count > 0 || duplicates > 0) mutableMessages.emit("Imported $count track${if (count == 1) "" else "s"}${if (duplicates > 0) " · $duplicates already in your library" else ""}")
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { mutableMessages.emit("Import failed. Check the file and available storage, then try again.") }
            finally { mutableImporting.value = false }
        }
    }
    override fun onCleared() { playback.close(); super.onCleared() }
}
