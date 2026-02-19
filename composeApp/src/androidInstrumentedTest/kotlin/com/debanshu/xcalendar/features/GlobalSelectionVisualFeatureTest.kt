package com.debanshu.xcalendar.features

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import com.debanshu.xcalendar.MainActivity
import com.debanshu.xcalendar.util.clickFirstNodeWithTextIfExists
import com.debanshu.xcalendar.util.navigateToScreen
import com.debanshu.xcalendar.util.waitUntilExists
import org.junit.Rule
import org.junit.Test

class GlobalSelectionVisualFeatureTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun globalSelectionVisual_visibleOnTodayWeekPlan() {
        listOf("Today", "Week", "Plan").forEach { screen ->
            composeRule.navigateToScreen(screen)
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("global_selection_visual", useUnmergedTree = true).assertIsDisplayed()
            composeRule.onNodeWithTag("global_selection_date_chip", useUnmergedTree = true).assertIsDisplayed()
            composeRule.onNodeWithTag("global_selection_person_chip", useUnmergedTree = true).assertIsDisplayed()
        }
    }

    @Test
    fun globalSelectionVisual_hiddenOnPeople() {
        composeRule.navigateToScreen("People")
        composeRule.waitForIdle()
        val nodes = composeRule.onAllNodesWithTag("global_selection_visual", useUnmergedTree = true)
        assert(nodes.fetchSemanticsNodes().isEmpty()) { "Global selection visual should be hidden on People" }
    }

    @Test
    fun globalSelectionVisual_reflectsLensChangesFromHeader() {
        composeRule.navigateToScreen("Today")
        composeRule.waitForIdle()

        composeRule.clickFirstNodeWithTextIfExists("Family", substring = false, ignoreCase = false)
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Selected person Family", useUnmergedTree = true).assertIsDisplayed()

        composeRule.clickFirstNodeWithTextIfExists("Mom Focus", substring = true, ignoreCase = true)
        composeRule.waitUntilExists(hasContentDescription("Selected person Mom Focus", ignoreCase = false), timeoutMillis = 5000)
        composeRule.onNodeWithContentDescription("Selected person Mom Focus", useUnmergedTree = true).assertIsDisplayed()
    }
}
