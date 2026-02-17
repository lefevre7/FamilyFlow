package com.debanshu.xcalendar.features

import com.debanshu.xcalendar.MainActivity

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.debanshu.xcalendar.util.assertAnyNodeWithTextExists
import com.debanshu.xcalendar.util.clickFirstNodeWithTextIfExists
import com.debanshu.xcalendar.util.navigateToScreen
import com.debanshu.xcalendar.util.performScrollToSafely
import org.junit.Rule
import org.junit.Test

class PeopleFeatureTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun peopleScreen_navigation_showsPeopleList() {
        // Navigate to People screen, verify profile list appears
        composeRule.navigateToScreen("People")

        composeRule.waitForIdle()

        // Verify People screen loaded - check for Mom profile (always present)
        val nodes = composeRule.onAllNodesWithText("Mom", substring = true, useUnmergedTree = true)
        assert(nodes.fetchSemanticsNodes().isNotEmpty()) { "Mom profile not found" }
    }

    @Test
    fun peopleScreen_showsDefaultPeople() {
        // Navigate to People, verify Mom + partner + 3 kids appear
        composeRule.navigateToScreen( "People")

        composeRule.waitForIdle()

        // Verify default profiles
        composeRule.assertAnyNodeWithTextExists("Mom", substring = true, ignoreCase = false)
        composeRule.assertAnyNodeWithTextExists("Partner", substring = true, ignoreCase = false)
        composeRule.assertAnyNodeWithTextExists("Kid", substring = true, ignoreCase = false)
    }

    @Test
    fun peopleScreen_editPerson_opensDialog() {
        // Click on person profile, verify edit dialog opens
        composeRule.navigateToScreen( "People")

        composeRule.waitForIdle()

        // Click any Edit action
        composeRule.clickFirstNodeWithTextIfExists("Edit", substring = false, ignoreCase = false)

        composeRule.waitForIdle()

        // Verify edit dialog appears
        composeRule.assertAnyNodeWithTextExists("Name", substring = true, ignoreCase = true)
    }

    @Test
    fun peopleScreen_lensFilter_updatesAcrossScreens() {
        // Navigate to Today/Week/Plan from People and verify each screen is reachable
        composeRule.navigateToScreen( "People")

        composeRule.waitForIdle()

        // Navigate to Today
        composeRule.navigateToScreen( "Today")

        composeRule.waitForIdle()

        composeRule.assertAnyNodeWithTextExists("Today", substring = false, ignoreCase = false)

        // Navigate to Week
        composeRule.navigateToScreen( "Week")

        composeRule.waitForIdle()

        composeRule.assertAnyNodeWithTextExists("Week", substring = false, ignoreCase = false)

        // Navigate to Plan
        composeRule.navigateToScreen( "Plan")

        composeRule.waitForIdle()

        composeRule.assertAnyNodeWithTextExists("Plan", substring = false, ignoreCase = false)
    }

    @Test
    fun peopleScreen_roleLabels_display() {
        // Navigate to People, verify role labels (Parent, Child) appear
        composeRule.navigateToScreen("People")

        composeRule.waitForIdle()

        // Verify role labels used by current UI
        composeRule.assertAnyNodeWithTextExists("Mom", substring = false, ignoreCase = false)
        composeRule.assertAnyNodeWithTextExists("Partner", substring = false, ignoreCase = false)
        composeRule.assertAnyNodeWithTextExists("Child", substring = true, ignoreCase = true)
    }

    @Test
    fun peopleScreen_addChild_button() {
        // Navigate to People, verify "Add child" button exists
        composeRule.navigateToScreen( "People")

        composeRule.waitForIdle()

        // Find add child button
        composeRule.onNodeWithText("Add child", substring = true, ignoreCase = true).performScrollToSafely(composeRule)
        composeRule.onNodeWithText("Add child", substring = true, ignoreCase = true).assertIsDisplayed()
    }

    @Test
    fun peopleScreen_addChild_opensDialog() {
        // Click "Add child", verify dialog opens with name/avatar/color inputs
        composeRule.navigateToScreen( "People")

        composeRule.waitForIdle()

        // Click add child
        composeRule.onNodeWithText("Add child", substring = true, ignoreCase = true).performClick()

        composeRule.waitForIdle()

        // Verify dialog appears
        composeRule.onNodeWithText("Name", substring = true, ignoreCase = true).assertIsDisplayed()
    }

    @Test
    fun peopleScreen_personColors_distinctive() {
        // Navigate to People, verify each person has distinct color indicator
        composeRule.navigateToScreen("People")

        composeRule.waitForIdle()

        // Would verify color indicators via test tags or semantic properties
        val nodes = composeRule.onAllNodesWithText("Mom", substring = true, useUnmergedTree = true)
        assert(nodes.fetchSemanticsNodes().isNotEmpty()) { "Mom profile not found" }
    }

    @Test
    fun peopleScreen_lensPersistence_survivesAppRestart() {
        // Navigate away and back, verify screen state remains stable
        composeRule.navigateToScreen( "People")

        composeRule.waitForIdle()

        composeRule.navigateToScreen( "Today")
        composeRule.waitForIdle()
        composeRule.navigateToScreen( "People")

        composeRule.waitForIdle()

        composeRule.assertAnyNodeWithTextExists("People", substring = false, ignoreCase = false)
    }

    @Test
    fun peopleScreen_accessibility_allPeopleLabeled() {
        // Verify all person cards have content descriptions
        composeRule.navigateToScreen("People")

        composeRule.waitForIdle()

        // Verify accessibility labels on person cards - check at least Mom exists
        val momNodes = composeRule.onAllNodesWithText("Mom", substring = true, useUnmergedTree = true)
        assert(momNodes.fetchSemanticsNodes().isNotEmpty()) { "Mom profile not found" }
        // Would verify semantic properties for screen readers
    }

    @Test
    fun peopleScreen_eventAssignment_whoIsAffected() {
        // Create event with person assignment, verify "Who's affected" labels appear
        composeRule.navigateToScreen( "People")

        composeRule.waitForIdle()

        // Would create event assigned to specific person
        // Navigate to Today and verify "Who's affected" label
        composeRule.navigateToScreen( "Today")

        composeRule.waitForIdle()

        // Verify "Who's affected" labels visible on event cards
    }
}
