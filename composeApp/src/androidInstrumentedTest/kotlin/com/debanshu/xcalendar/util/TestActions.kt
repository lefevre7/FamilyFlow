package com.debanshu.xcalendar.util

import android.content.Context
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.activity.ComponentActivity

/**
 * Reusable user actions for instrumented tests.
 * These methods encapsulate common UI interactions to reduce duplication.
 */

/**
 * Navigate to a specific screen using bottom navigation.
 * Automatically skips onboarding if present.
 * @param screenName The name of the screen ("Today", "Week", "Plan", "People", "Settings")
 */
@OptIn(ExperimentalTestApi::class)
fun <A : ComponentActivity> AndroidComposeTestRule<ActivityScenarioRule<A>, A>.navigateToScreen(
    screenName: String
) {
    waitForIdle()

    // Best-effort onboarding skip for first launch.
    repeat(2) {
        clickFirstNodeWithTextIfExists("Get Started", substring = true, ignoreCase = true)
        clickFirstNodeWithTextIfExists("Skip", substring = true, ignoreCase = true)
        clickFirstNodeWithTextIfExists("Open Today", substring = false, ignoreCase = true)
    }

    // Navigate using bottom-nav label and icon contentDescription, with retries for startup races.
    repeat(4) {
        if (isScreenAnchorVisible(screenName)) return
        if (clickFirstNodeWithTextIfExists(screenName, substring = false, ignoreCase = false)) {
            waitForIdle()
            if (isScreenAnchorVisible(screenName)) return
        }
        clickFirstNodeWithContentDescriptionIfExists(screenName)
        waitForIdle()
        if (isScreenAnchorVisible(screenName)) return
    }
    waitForIdle()
}

@OptIn(ExperimentalTestApi::class)
fun <A : ComponentActivity> AndroidComposeTestRule<ActivityScenarioRule<A>, A>.clickFirstNodeWithTextIfExists(
    text: String,
    substring: Boolean = true,
    ignoreCase: Boolean = true,
): Boolean {
    val nodes = onAllNodesWithText(text, substring = substring, ignoreCase = ignoreCase, useUnmergedTree = true)
    val count = runCatching { nodes.fetchSemanticsNodes().size }.getOrElse { 0 }
    if (count == 0) return false
    for (index in 0 until count) {
        runCatching {
            nodes[index].performClick()
            waitForIdle()
            return true
        }
    }
    return false
}

@OptIn(ExperimentalTestApi::class)
private fun <A : ComponentActivity> AndroidComposeTestRule<ActivityScenarioRule<A>, A>.clickFirstNodeWithContentDescriptionIfExists(
    contentDescription: String,
): Boolean {
    val nodes = onAllNodesWithContentDescription(contentDescription, substring = false, ignoreCase = false, useUnmergedTree = true)
    val count = runCatching { nodes.fetchSemanticsNodes().size }.getOrElse { 0 }
    if (count == 0) return false
    for (index in 0 until count) {
        runCatching {
            nodes[index].performClick()
            waitForIdle()
            return true
        }
    }
    return false
}

private fun <A : ComponentActivity> AndroidComposeTestRule<ActivityScenarioRule<A>, A>.isScreenAnchorVisible(
    screenName: String,
): Boolean {
    val anchors = when (screenName) {
        "Today" -> listOf("Today Only")
        "Week" -> listOf("Only Mom required")
        "Plan" -> listOf("Brain Dump", "Scan schedule")
        "People" -> listOf("Add child")
        "Settings" -> listOf("Google Calendar Sync", "Accessibility")
        else -> listOf(screenName)
    }
    return anchors.any { anchor ->
        val nodes = onAllNodesWithText(anchor, substring = true, ignoreCase = true, useUnmergedTree = true)
        runCatching { nodes.fetchSemanticsNodes().isNotEmpty() }.getOrElse { false }
    }
}

@OptIn(ExperimentalTestApi::class)
fun <A : ComponentActivity> AndroidComposeTestRule<ActivityScenarioRule<A>, A>.assertAnyNodeWithTextExists(
    text: String,
    substring: Boolean = true,
    ignoreCase: Boolean = true,
) {
    val nodes = onAllNodesWithText(text, substring = substring, ignoreCase = ignoreCase, useUnmergedTree = true)
    val count = runCatching { nodes.fetchSemanticsNodes().size }.getOrElse { 0 }
    assert(count > 0) { "Expected at least one node containing '$text'" }
}

@OptIn(ExperimentalTestApi::class)
fun <A : ComponentActivity> AndroidComposeTestRule<ActivityScenarioRule<A>, A>.assertAnyOfTextsExists(
    texts: List<String>,
    substring: Boolean = true,
    ignoreCase: Boolean = true,
) {
    val foundAny =
        texts.any { candidate ->
            val nodes = onAllNodesWithText(candidate, substring = substring, ignoreCase = ignoreCase, useUnmergedTree = true)
            runCatching { nodes.fetchSemanticsNodes().isNotEmpty() }.getOrElse { false }
        }
    assert(foundAny) { "Expected at least one node containing any of ${texts.joinToString()}" }
}

@OptIn(ExperimentalTestApi::class)
fun <A : ComponentActivity> AndroidComposeTestRule<ActivityScenarioRule<A>, A>.assertNodeWithTagExists(
    tag: String,
) {
    val nodes = onAllNodesWithTag(tag, useUnmergedTree = true)
    val count = runCatching { nodes.fetchSemanticsNodes().size }.getOrElse { 0 }
    assert(count > 0) { "Expected at least one node with tag '$tag'" }
}

@OptIn(ExperimentalTestApi::class)
fun <A : ComponentActivity> AndroidComposeTestRule<ActivityScenarioRule<A>, A>.clickQuickAddFab() {
    val taggedFab = onAllNodesWithTag("fab_quick_add", useUnmergedTree = true)
    val taggedCount = runCatching { taggedFab.fetchSemanticsNodes().size }.getOrElse { 0 }
    if (taggedCount > 0) {
        taggedFab[0].performClick()
        waitForIdle()
        return
    }
    onNodeWithContentDescription("Quick add", substring = true, ignoreCase = true)
        .performClick()
    waitForIdle()
}

/**
 * Add a task via quick-add flow.
 * @param title The task title
 * @param waitForCompletion Whether to wait for the task to be saved
 */
@OptIn(ExperimentalTestApi::class)
fun <A : ComponentActivity> AndroidComposeTestRule<ActivityScenarioRule<A>, A>.addQuickTask(
    title: String,
    waitForCompletion: Boolean = true
) {
    // Open FAB
    onNodeWithContentDescription("Quick Add")
        .performClick()
    waitForIdle()
    
    // Enter title
    onNode(hasSetTextAction())
        .performTextInput(title)
    waitForIdle()
    
    // Save
    onNodeWithText("Save")
        .performClick()
    
    if (waitForCompletion) {
        waitUntilDoesNotExist(hasText("Save"), timeoutMillis = 5000)
    }
}

/**
 * Simulate Google login flow (mocked).
 * This triggers the OAuth flow with test credentials.
 */
fun <A : ComponentActivity> AndroidComposeTestRule<ActivityScenarioRule<A>, A>.loginWithGoogle() {
    // Navigate to Settings
    navigateToScreen("Settings")
    
    // Find and click "Connect Google Calendar"
    onNodeWithText("Connect Google Calendar", substring = true)
        .performScrollTo()
        .performClick()
    waitForIdle()
}

/**
 * Toggle a filter switch or checkbox.
 * @param filterName The name of the filter
 */
fun <A : ComponentActivity> AndroidComposeTestRule<ActivityScenarioRule<A>, A>.toggleFilter(
    filterName: String
) {
    onNodeWithText(filterName)
        .performScrollTo()
        .performClick()
    waitForIdle()
}

/**
 * Select a lens filter (Family, Mom, or Person).
 * @param lensName The name of the lens to select
 */
fun <A : ComponentActivity> AndroidComposeTestRule<ActivityScenarioRule<A>, A>.selectLens(
    lensName: String
) {
    onNodeWithContentDescription("Family lens selector", substring = true)
        .performClick()
    waitForIdle()
    
    onNodeWithText(lensName)
        .performClick()
    waitForIdle()
}

/**
 * Perform a quick action on a Today card (Done, Snooze, Share).
 * @param itemTitle The title of the item to act on
 * @param action The action to perform ("Done", "Snooze", "Share")
 */
fun <A : ComponentActivity> AndroidComposeTestRule<ActivityScenarioRule<A>, A>.performTodayAction(
    itemTitle: String,
    action: String
) {
    // Find the card by title
    onNodeWithText(itemTitle)
        .performScrollTo()
    waitForIdle()
    
    // Perform action
    onNodeWithText(action)
        .performClick()
    waitForIdle()
}

/**
 * Wait until a node matching the given matcher no longer exists.
 */
@OptIn(ExperimentalTestApi::class)
fun <A : ComponentActivity> AndroidComposeTestRule<ActivityScenarioRule<A>, A>.waitUntilDoesNotExist(
    matcher: androidx.compose.ui.test.SemanticsMatcher,
    timeoutMillis: Long = 10000
) {
    waitUntilDoesNotExist(hasClickAction().and(matcher), timeoutMillis)
}

/**
 * Wait until at least one node matching the given matcher exists.
 */
@OptIn(ExperimentalTestApi::class)
fun <A : ComponentActivity> AndroidComposeTestRule<ActivityScenarioRule<A>, A>.waitUntilExists(
    matcher: androidx.compose.ui.test.SemanticsMatcher,
    timeoutMillis: Long = 10000
) {
    waitUntilAtLeastOneExists(matcher, timeoutMillis)
}

/**
 * Scroll to and click a node with the given text.
 */
fun <A : ComponentActivity> AndroidComposeTestRule<ActivityScenarioRule<A>, A>.scrollToAndClick(
    text: String
) {
    onNodeWithText(text)
        .performScrollTo()
        .performClick()
    waitForIdle()
}

/**
 * Mark onboarding as complete to skip it in tests.
 * Call this before launching an activity to bypass onboarding.
 */
fun markOnboardingComplete() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    context.getSharedPreferences("onboarding_prefs", Context.MODE_PRIVATE)
        .edit()
        .putBoolean("onboarding_completed", true)
        .apply()
}

/**
 * Safely perform scroll to on a SemanticsNodeInteraction.
 * Uses the provided compose rule to ensure proper idle waiting.
 */
fun <A : ComponentActivity> SemanticsNodeInteraction.performScrollToSafely(
    composeRule: AndroidComposeTestRule<ActivityScenarioRule<A>, A>
): SemanticsNodeInteraction {
    performScrollTo()
    composeRule.waitForIdle()
    return this
}
