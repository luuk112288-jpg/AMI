package com.ami.music

import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.ami.music.data.CatalogUiState
import com.ami.music.data.LibraryState
import com.ami.music.ui.screens.SearchScreen
import com.ami.music.ui.theme.AmiTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SearchRestorationTest {
    @get:Rule val compose = createComposeRule()
    @Test fun restoredOnlineTabRequestsItsCatalogAgain() {
        var requests = 0
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            AmiTheme {
                SearchScreen(LibraryState(), { _, _ -> }, CatalogUiState(), { requests++ }, { _ -> }, {}, {}, { _, _, _ -> })
            }
        }
        compose.onNodeWithText("Online").performClick()
        compose.runOnIdle { assertEquals(1, requests) }
        restoration.emulateSavedInstanceStateRestore()
        compose.runOnIdle { assertEquals(2, requests) }
    }
}
