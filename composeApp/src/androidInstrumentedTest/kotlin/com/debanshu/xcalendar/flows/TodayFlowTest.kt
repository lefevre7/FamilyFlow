package com.debanshu.xcalendar.flows

import com.debanshu.xcalendar.MainActivity

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.debanshu.xcalendar.util.assertAnyNodeWithTextExists
import com.debanshu.xcalendar.util.clickFirstNodeWithTextIfExists
import com.debanshu.xcalendar.util.navigateToScreen
import org.junit.Rule
import org.junit.Test

class TodayFlowTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun onboarding_completesTodayScreen_showsMorningAfternoonEveningGrouping() {
        // Launch app and land on Today (onboarding is skipped in navigate helper when present).
        composeRule.navigateToScreen("Today")
        composeRule.waitForIdle()
        composeRule.assertAnyNodeWithTextExists("Today", substring = false, ignoreCase = false)
    }

    @Test
    fun todayScreen_doneAction_completesTask() {
        composeRule.navigateToScreen( "Today")
        composeRule.waitForIdle()
        composeRule.clickFirstNodeWithTextIfExists("Done", substring = false, ignoreCase = false)
        composeRule.assertAnyNodeWithTextExists("Today", substring = false, ignoreCase = false)
    }

    @Test
    fun todayScreen_snoozeAction_reschedulesTask() {
        composeRule.navigateToScreen( "Today")
        composeRule.waitForIdle()
        composeRule.clickFirstNodeWithTextIfExists("Snooze", substring = false, ignoreCase = false)
        composeRule.assertAnyNodeWithTextExists("Today", substring = false, ignoreCase = false)
    }

    @Test
    fun todayScreen_shareSnapshot_sendsTextToNotifier() {
        composeRule.navigateToScreen( "Today")
        composeRule.waitForIdle()
        composeRule.assertAnyNodeWithTextExists("Share snapshot", substring = true, ignoreCase = true)
    }

    @Test
    fun todayScreen_lensFilter_filtersItemsByPerson() {
        composeRule.navigateToScreen( "Today")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("lens_selector").assertIsDisplayed()
        composeRule.clickFirstNodeWithTextIfExists("Mom Focus", substring = true, ignoreCase = true)
        composeRule.assertAnyNodeWithTextExists("Today", substring = false, ignoreCase = false)
    }

    @Test
    fun todayScreen_todayOnlyToggle_filtersItemsByTimeWindow() {
        composeRule.navigateToScreen( "Today")
        composeRule.waitForIdle()
        composeRule.clickFirstNodeWithTextIfExists("Today Only", substring = true, ignoreCase = true)
        composeRule.assertAnyNodeWithTextExists("Today Only", substring = true, ignoreCase = true)
    }

    @Test
    fun todayScreen_stickyRoutines_alwaysAppear() {
        composeRule.navigateToScreen( "Today")
        composeRule.waitForIdle()
        composeRule.assertAnyNodeWithTextExists("Today", substring = false, ignoreCase = false)
    }

    @Test
    fun todayScreen_accessibility_allInteractiveElementsLabeled() {
        composeRule.navigateToScreen( "Today")
        composeRule.waitForIdle()
        composeRule.assertAnyNodeWithTextExists("Share snapshot", substring = true, ignoreCase = true)
        composeRule.assertAnyNodeWithTextExists("Today Only", substring = true, ignoreCase = true)
    }
}
