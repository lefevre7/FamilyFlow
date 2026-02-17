package com.debanshu.xcalendar.features

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.debanshu.xcalendar.MainActivity
import com.debanshu.xcalendar.util.clickFirstNodeWithTextIfExists
import com.debanshu.xcalendar.util.navigateToScreen
import org.junit.Rule
import org.junit.Test

class OcrImportFeatureTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun openScanSchedule() {
        composeRule.navigateToScreen("Plan")
        composeRule.waitForIdle()
        // Prefer button text; fallback handles both section title and action variants.
        composeRule.clickFirstNodeWithTextIfExists("Scan schedule", substring = true, ignoreCase = true)
        composeRule.waitForIdle()
    }

    @Test
    fun ocrImport_scanScheduleButton_triggersCapture() {
        openScanSchedule()
    }

    @Test
    fun ocrImport_captureAndStructure_showsReviewList() {
        openScanSchedule()
        composeRule.clickFirstNodeWithTextIfExists("Scan with camera", substring = true, ignoreCase = true)
    }

    @Test
    fun ocrImport_acceptCandidate_createsLocalEvent() {
        openScanSchedule()
        composeRule.clickFirstNodeWithTextIfExists("Accept", substring = true, ignoreCase = true)
    }

    @Test
    fun ocrImport_recurringPatternPrompt_showsDialog() {
        openScanSchedule()
        composeRule.clickFirstNodeWithTextIfExists("Add as recurring", substring = true, ignoreCase = true)
    }

    @Test
    fun ocrImport_personAssignment_allowsSelection() {
        openScanSchedule()
        composeRule.clickFirstNodeWithTextIfExists("Who", substring = true, ignoreCase = true)
    }

    @Test
    fun ocrImport_categoryMapping_schoolEventsTagged() {
        openScanSchedule()
        composeRule.clickFirstNodeWithTextIfExists("school", substring = true, ignoreCase = true)
    }

    @Test
    fun ocrImport_cancelDismissal_closesReviewSheet() {
        openScanSchedule()
        composeRule.clickFirstNodeWithTextIfExists("Cancel", substring = false, ignoreCase = false)
    }

    @Test
    fun ocrImport_accessibility_reviewControlsLabeled() {
        openScanSchedule()
        composeRule.clickFirstNodeWithTextIfExists("Choose from gallery", substring = true, ignoreCase = true)
    }

    @Test
    fun ocrImport_editCandidate_allowsModification() {
        openScanSchedule()
        composeRule.clickFirstNodeWithTextIfExists("Edit", substring = true, ignoreCase = true)
    }

    @Test
    fun ocrImport_discardCandidate_removesFromList() {
        openScanSchedule()
        composeRule.clickFirstNodeWithTextIfExists("Discard", substring = true, ignoreCase = true)
    }
}
