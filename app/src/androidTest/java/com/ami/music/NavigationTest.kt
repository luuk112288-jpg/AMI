package com.ami.music

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun waitForHome() { compose.waitUntil(15_000) { compose.onAllNodesWithText("Less noise.\nMore feeling.").fetchSemanticsNodes().isNotEmpty() } }
    @Test fun phoneLibraryOffersScanAndExplainsLocalFiles() {
        waitForHome()
        compose.onNodeWithText("Your library").performClick()
        compose.onNodeWithText("Scan phone").assertIsDisplayed()
        compose.onNodeWithText("On device").performClick()
        compose.onNodeWithText("Your music, right here").assertIsDisplayed()
    }
    @Test fun homeShowsArtificialMusicIntelligenceBrand() {
        waitForHome()
        compose.onNodeWithContentDescription("AMI — Artificial Music Intelligence").assertIsDisplayed()
    }
    @Test fun searchFiltersEntities() {
        waitForHome()
        compose.onNodeWithContentDescription("Search music").performClick()
        compose.onNode(hasSetTextAction()).performTextInput("not a real song")
        compose.onNodeWithText("Nothing here yet").assertIsDisplayed()
        compose.onNodeWithContentDescription("Clear search").performClick()
        compose.onNodeWithText("What's your mood?").assertIsDisplayed()
    }
    @Test fun createRenameDeletePlaylist() {
        waitForHome()
        compose.onNodeWithText("Your library").performClick()
        compose.onNodeWithContentDescription("Create playlist").performClick()
        compose.onNode(hasSetTextAction()).performTextInput("UI test playlist")
        compose.onNodeWithText("Save").performClick()
        compose.waitUntil(5000) { compose.onAllNodesWithText("UI test playlist").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("Playlist options").performClick()
        compose.onNodeWithText("Rename playlist").performClick()
        compose.onNode(hasSetTextAction()).performTextReplacement("Renamed UI playlist")
        compose.onNodeWithText("Save").performClick()
        compose.waitUntil(5000) { compose.onAllNodesWithText("Renamed UI playlist").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("Playlist options").performClick()
        compose.onNodeWithText("Delete playlist").performClick()
        compose.onNodeWithText("Delete").performClick()
        compose.waitUntil(5000) { compose.onAllNodesWithText("Delete Renamed UI playlist?").fetchSemanticsNodes().isEmpty() }
        compose.onAllNodesWithText("Renamed UI playlist").assertCountEquals(0)
    }
}
