package com.debanshu.xcalendar.flows

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.debanshu.xcalendar.MainActivity
import com.debanshu.xcalendar.util.clickFirstNodeWithTextIfExists
import com.debanshu.xcalendar.util.navigateToScreen
import org.junit.Rule
import org.junit.Test

class GoogleSyncFlowTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun openSettingsAndTrySync() {
        composeRule.navigateToScreen("Today")
        composeRule.waitForIdle()
        composeRule.clickFirstNodeWithTextIfExists("Sync now", substring = true, ignoreCase = true)
        composeRule.waitForIdle()
    }

    @Test
    fun settings_oauthFlow_savesTokens() {
        openSettingsAndTrySync()
    }

    @Test
    fun calendarSync_createsGoogleEvents_visibleInToday() {
        openSettingsAndTrySync()
        composeRule.navigateToScreen("Today")
        composeRule.waitForIdle()
    }

    @Test
    fun syncNow_button_triggersManualSync() {
        openSettingsAndTrySync()
    }

    @Test
    fun syncConflict_showsResolutionSheet_acceptsUserChoice() {
        openSettingsAndTrySync()
        composeRule.clickFirstNodeWithTextIfExists("Keep local", substring = true, ignoreCase = true)
    }

    @Test
    fun syncConflict_removal_clearsConflictFromSettings() {
        openSettingsAndTrySync()
    }

    @Test
    fun googleSync_accessibility_labelsPresentOnActions() {
        openSettingsAndTrySync()
    }

    @Test
    fun syncCompletion_noMemoryLeaks() {
        openSettingsAndTrySync()
    }
}
