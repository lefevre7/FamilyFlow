package com.debanshu.xcalendar.domain.repository

import com.debanshu.xcalendar.test.FakeUiPreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for [IUiPreferencesRepository] contract and the [FakeUiPreferencesRepository]
 * implementation used across the test suite.
 *
 * These tests also serve as a spec for the Android DataStore implementation:
 * the real implementation must satisfy the same invariants.
 */
class UiPreferencesRepositoryTest {

    @Test
    fun navDragHintDismissed_defaultsToFalse() = runTest {
        val repo = FakeUiPreferencesRepository()

        assertFalse(
            repo.navDragHintDismissed.first(),
            "Hint should be visible (not dismissed) on first run",
        )
    }

    @Test
    fun setNavDragHintDismissed_true_persistsAcrossRead() = runTest {
        val repo = FakeUiPreferencesRepository()

        repo.setNavDragHintDismissed(true)

        assertTrue(
            repo.navDragHintDismissed.first(),
            "Hint should be dismissed after explicit dismissal",
        )
    }

    @Test
    fun setNavDragHintDismissed_false_canReset() = runTest {
        val repo = FakeUiPreferencesRepository(initialDismissed = true)

        repo.setNavDragHintDismissed(false)

        assertFalse(
            repo.navDragHintDismissed.first(),
            "Hint should be visible again after resetting dismissed flag",
        )
    }

    @Test
    fun initialDismissed_true_hintAlreadyDismissed() = runTest {
        val repo = FakeUiPreferencesRepository(initialDismissed = true)

        assertTrue(
            repo.navDragHintDismissed.first(),
            "Hint pre-dismissed at construction should read as dismissed",
        )
    }

    @Test
    fun multipleSetCalls_lastValueWins() = runTest {
        val repo = FakeUiPreferencesRepository()

        repo.setNavDragHintDismissed(true)
        repo.setNavDragHintDismissed(false)
        repo.setNavDragHintDismissed(true)

        assertTrue(
            repo.navDragHintDismissed.first(),
            "Last write (true) should win",
        )
    }

    // --- CalendarApp integration invariants ---

    @Test
    fun showDragHint_isInverseOfDismissed_whenNotDismissed() = runTest {
        val repo = FakeUiPreferencesRepository(initialDismissed = false)
        val dismissed = repo.navDragHintDismissed.first()

        assertTrue(!dismissed, "showDragHint = !dismissed should be true when not yet dismissed")
    }

    @Test
    fun showDragHint_isFalse_afterExplicitDismiss() = runTest {
        val repo = FakeUiPreferencesRepository(initialDismissed = false)

        // Simulates user tapping ✕ on the hint card
        repo.setNavDragHintDismissed(true)

        val showDragHint = !repo.navDragHintDismissed.first()
        assertFalse(showDragHint, "showDragHint should be false after explicit dismiss")
    }

    @Test
    fun showDragHint_isFalse_afterDragInteraction() = runTest {
        val repo = FakeUiPreferencesRepository(initialDismissed = false)

        // Simulates user dragging the dock (auto-dismiss)
        if (!repo.navDragHintDismissed.first()) {
            repo.setNavDragHintDismissed(true)
        }

        val showDragHint = !repo.navDragHintDismissed.first()
        assertFalse(showDragHint, "showDragHint should be false after dock drag interaction")
    }
}
