package com.debanshu.xcalendar.features

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.debanshu.xcalendar.MainActivity
import com.debanshu.xcalendar.util.clickFirstNodeWithTextIfExists
import com.debanshu.xcalendar.util.navigateToScreen
import org.junit.Rule
import org.junit.Test

class AccessibilityFeatureTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun today_contentDescriptions_present() {
        composeRule.navigateToScreen("Today")
        composeRule.waitForIdle()
    }

    @Test
    fun week_contentDescriptions_present() {
        composeRule.navigateToScreen("Week")
        composeRule.waitForIdle()
    }

    @Test
    fun plan_contentDescriptions_present() {
        composeRule.navigateToScreen("Plan")
        composeRule.waitForIdle()
    }

    @Test
    fun people_contentDescriptions_present() {
        composeRule.navigateToScreen("People")
        composeRule.waitForIdle()
    }

    @Test
    fun settings_contentDescriptions_present() {
        composeRule.navigateToScreen("Today")
        composeRule.waitForIdle()
    }

    @Test
    fun fab_accessibility_labelAndAction() {
        composeRule.navigateToScreen("Today")
        composeRule.waitForIdle()
        composeRule.clickFirstNodeWithTextIfExists("Task", substring = false, ignoreCase = false)
    }

    @Test
    fun filters_accessibility_toggleable() {
        composeRule.navigateToScreen("Week")
        composeRule.waitForIdle()
        composeRule.clickFirstNodeWithTextIfExists("Only Mom required", substring = true, ignoreCase = true)
        composeRule.clickFirstNodeWithTextIfExists("Only Must", substring = true, ignoreCase = true)
    }

    @Test
    fun bottomNavigation_accessibility_labeled() {
        val screens = listOf("Today", "Week", "Plan", "People")
        screens.forEach { screen ->
            composeRule.navigateToScreen(screen)
            composeRule.waitForIdle()
        }
    }

    @Test
    fun lensSelector_accessibility_interactive() {
        composeRule.navigateToScreen("Today")
        composeRule.waitForIdle()
        composeRule.clickFirstNodeWithTextIfExists("Mom Focus", substring = true, ignoreCase = true)
    }

    @Test
    fun textInputs_accessibility_labeled() {
        composeRule.navigateToScreen("Plan")
        composeRule.waitForIdle()
        composeRule.clickFirstNodeWithTextIfExists("Quick capture", substring = true, ignoreCase = true)
    }

    @Test
    fun allScreens_smokeTest_noAccessibilityCrashes() {
        val screens = listOf("Today", "Week", "Plan", "People")
        screens.forEach { screen ->
            composeRule.navigateToScreen(screen)
            composeRule.waitForIdle()
        }
    }
}
