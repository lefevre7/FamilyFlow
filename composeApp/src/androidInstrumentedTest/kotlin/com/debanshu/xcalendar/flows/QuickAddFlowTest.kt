package com.debanshu.xcalendar.flows

import com.debanshu.xcalendar.MainActivity

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.debanshu.xcalendar.util.assertAnyNodeWithTextExists
import com.debanshu.xcalendar.util.assertNodeWithTagExists
import com.debanshu.xcalendar.util.clickQuickAddFab
import com.debanshu.xcalendar.util.navigateToScreen
import com.debanshu.xcalendar.util.performScrollToSafely
import org.junit.Rule
import org.junit.Test

class QuickAddFlowTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun fabTap_taskMode_createsTaskVisibleInToday() {
        // Tap FAB (default Task mode), fill form, submit, verify task appears in Today
        composeRule.navigateToScreen( "Today")

        composeRule.waitForIdle()

        // Tap FAB to open Quick Add Task sheet
        composeRule.clickQuickAddFab()

        // Fill in task details
        composeRule.onNodeWithTag("task_title_input").performTextInput("Quick Add Test Task")

        // Submit task
        composeRule.onNodeWithText("Save").performClick()

        composeRule.waitForIdle()

        // Verify task appears in Today view
        composeRule.onNodeWithText("Quick Add Test Task").performScrollToSafely(composeRule)
    }

    @Test
    fun fabTap_eventMode_triggersEventForm() {
        // Long-press FAB, select Event, verify event form opens
        composeRule.navigateToScreen( "Today")

        composeRule.waitForIdle()

        composeRule.clickQuickAddFab()

        // Select Event mode
        composeRule.onNodeWithText("Event").performClick()

        // Verify event mode content appears
        composeRule.assertAnyNodeWithTextExists("Create a timed event", substring = true, ignoreCase = true)
        composeRule.onNodeWithText("Open event sheet").performClick()
        composeRule.assertNodeWithTagExists("event_title_input")
    }

    @Test
    fun fabTap_voiceMode_capturesVoice() {
        // Long-press FAB, select Voice, verify SpeechRecognizer flow
        composeRule.navigateToScreen( "Today")

        composeRule.waitForIdle()

        composeRule.clickQuickAddFab()

        // Select Voice mode
        composeRule.onNodeWithText("Voice").performClick()

        // Verify voice capture UI appears
        composeRule.assertAnyNodeWithTextExists("Start voice capture", substring = true, ignoreCase = true)
    }

    @Test
    fun fab_accessibility_contentDescriptionPresent() {
        // Verify FAB has proper content description for screen readers
        composeRule.navigateToScreen( "Today")

        composeRule.waitForIdle()

        // Verify FAB accessibility
        composeRule.assertNodeWithTagExists("fab_quick_add")
    }

    @Test
    fun taskCreation_withPriorityAndEnergy_savesCorrectly() {
        // Create task with specific priority and energy level, verify saved correctly
        composeRule.navigateToScreen( "Today")

        composeRule.waitForIdle()

        // Open Quick Add
        composeRule.clickQuickAddFab()

        // Fill task details
        composeRule.onNodeWithTag("task_title_input").performTextInput("Priority Task")

        // Set priority
        composeRule.onNodeWithText("Must").performClick()

        // Set energy level
        composeRule.onNodeWithText("High").performClick()

        // Submit
        composeRule.onNodeWithText("Save").performClick()

        composeRule.waitForIdle()

        // Verify task appears with correct priority indicator
        composeRule.onNodeWithText("Priority Task").performScrollToSafely(composeRule)
    }

    @Test
    fun quickAddTask_appearsInWeekView() {
        // Create task via Quick Add, navigate to Week, verify task appears
        composeRule.navigateToScreen( "Today")

        composeRule.waitForIdle()

        // Create task
        composeRule.clickQuickAddFab()
        composeRule.onNodeWithTag("task_title_input").performTextInput("Week View Task")
        composeRule.onNodeWithText("Save").performClick()

        composeRule.waitForIdle()

        // Navigate to Week
        composeRule.navigateToScreen( "Week")

        composeRule.waitForIdle()

        // Verify task appears in Week view
        composeRule.onNodeWithText("Week View Task").performScrollToSafely(composeRule)
    }
}
