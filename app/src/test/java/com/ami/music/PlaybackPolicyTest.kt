package com.ami.music

import com.ami.music.playback.PlaybackPolicy
import org.junit.Assert.*
import org.junit.Test

class PlaybackPolicyTest {
    @Test fun previousAfterThreeSecondsRestarts() {
        assertEquals(PlaybackPolicy.Previous(2, 0L), PlaybackPolicy.previous(2, 3001L))
    }
    @Test fun previousAtThreeSecondsReturnsPrevious() {
        assertEquals(PlaybackPolicy.Previous(1, 0L), PlaybackPolicy.previous(2, 3000L))
    }
    @Test fun firstTrackPreviousNeverGoesNegative() {
        assertEquals(PlaybackPolicy.Previous(0, 0L), PlaybackPolicy.previous(0, 100L))
    }
    @Test fun seekClampsToMediaBounds() {
        assertEquals(0L, PlaybackPolicy.seek(-10L, 1000L))
        assertEquals(1000L, PlaybackPolicy.seek(2000L, 1000L))
        assertEquals(0L, PlaybackPolicy.seek(2000L, -1L))
    }
    @Test fun playlistNameRejectsWhitespace() {
        assertFalse(PlaybackPolicy.validPlaylistName("  "))
        assertTrue(PlaybackPolicy.validPlaylistName("Night drive"))
    }
    @Test fun duplicateTrackIdsAreRejectedForLibraryPlaylists() {
        assertEquals(listOf("a", "b"), PlaybackPolicy.uniqueTracks(listOf("a", "b", "a")))
    }
    @Test fun restoreNeverOverridesNewQueueOrPendingPlay() {
        assertFalse(PlaybackPolicy.canRestore(1, false))
        assertFalse(PlaybackPolicy.canRestore(0, true))
        assertTrue(PlaybackPolicy.canRestore(0, false))
    }
    @Test fun pauseToggleUsesPendingPlaybackIntent() {
        assertFalse(PlaybackPolicy.toggledPlayIntent(true))
        assertTrue(PlaybackPolicy.toggledPlayIntent(false))
    }
}
