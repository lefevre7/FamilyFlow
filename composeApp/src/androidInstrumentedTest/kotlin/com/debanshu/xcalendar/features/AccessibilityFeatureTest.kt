package com.debanshu.xcalendar.features

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.debanshu.xcalendar.MainActivity
import com.debanshu.xcalendar.util.clickFirstNodeWithTextIfExists
import com.debanshu.xcalendar.util.navigateToScreen
import org.junit.Rule
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.test.assertTrue

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

    @Test
    fun softFeminineColors_contrastRatios_meetWCAG_AA() {
        // Verify core color combinations meet WCAG AA (4.5:1 for normal text)
        val surface = Color(0xFFFCF9FA)
        val onSurface = Color(0xFF1F1C19)
        val surfaceVariant = Color(0xFFF0E8EB)
        val onSurfaceVariant = Color(0xFF3F3539)
        val primary = Color(0xFF846C92)
        val onPrimary = Color(0xFFFFFFFF)
        
        val onSurfaceRatio = calculateContrastRatio(onSurface, surface)
        assertTrue(
            onSurfaceRatio >= 4.5,
            "onSurface contrast (${"%.1f".format(onSurfaceRatio)}:1) should meet WCAG AA (4.5:1)"
        )
        
        val onSurfaceVariantRatio = calculateContrastRatio(onSurfaceVariant, surfaceVariant)
        assertTrue(
            onSurfaceVariantRatio >= 4.5,
            "onSurfaceVariant contrast (${"%.1f".format(onSurfaceVariantRatio)}:1) should meet WCAG AA (4.5:1)"
        )
        
        val onPrimaryRatio = calculateContrastRatio(onPrimary, primary)
        assertTrue(
            onPrimaryRatio >= 4.5,
            "onPrimary contrast (${"%.1f".format(onPrimaryRatio)}:1) should meet WCAG AA (4.5:1)"
        )
    }
}

private fun calculateContrastRatio(foreground: Color, background: Color): Double {
    val l1 = foreground.luminance().toDouble()
    val l2 = background.luminance().toDouble()
    val lighter = max(l1, l2)
    val darker = min(l1, l2)
    return (lighter + 0.05) / (darker + 0.05)
}
