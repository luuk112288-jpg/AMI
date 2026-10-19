package com.ami.music

import com.ami.music.data.*
import org.junit.Assert.*
import org.junit.Test

class SearchTest {
    private val tracks = listOf(
        Track("a", "Midnight Current", "luma", "Luma", "night", "Night", "asset:///a.wav", 1000),
        Track("b", "Daylight", "sora", "Sora", "day", "Day", "asset:///b.wav", 2000),
    )
    private val library = LibraryState(tracks, listOf(Playlist("p", "Night drive")), listOf(PlaylistEntry("p", "b", 0), PlaylistEntry("p", "a", 1)))
    @Test fun searchMatchesCaseInsensitively() { assertEquals(listOf("a"), library.search("  MIDNIGHT ").tracks.map { it.id }) }
    @Test fun searchIncludesArtistAndAlbum() {
        assertEquals(listOf("b"), library.search("Sora").tracks.map { it.id })
        assertEquals("night", library.search("Night").albums.single().albumId)
        assertEquals("p", library.search("drive").playlists.single().id)
    }
    @Test fun noMatchesIsEmpty() { assertTrue(library.search("no such song").tracks.isEmpty()) }
    @Test fun playlistUsesStoredOrder() { assertEquals(listOf("b", "a"), library.playlistTracks("p").map { it.id }) }
    @Test fun missingTrackReferencesAreExcluded() {
        val broken = library.copy(entries = library.entries + PlaylistEntry("p", "missing", 2))
        assertEquals(2, broken.playlistTracks("p").size)
    }
}
