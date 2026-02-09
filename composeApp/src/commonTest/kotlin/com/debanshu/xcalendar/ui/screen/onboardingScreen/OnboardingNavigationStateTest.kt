package com.debanshu.xcalendar.ui.screen.onboardingScreen

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OnboardingNavigationStateTest {

    @Test
    fun middleStep_usesNextActionAndMovesForward() {
        val state = OnboardingNavigationState(pageIndex = 1, stepCount = 3)

        assertFalse(state.isLastPage)
        assertEquals("Next", state.primaryActionLabel)
        assertEquals(2, state.nextPage())
        assertEquals(0, state.previousPage())
    }

    @Test
    fun lastStep_usesOpenTodayAndStaysBounded() {
        val state = OnboardingNavigationState(pageIndex = 2, stepCount = 3)

        assertTrue(state.isLastPage)
        assertEquals("Open Today", state.primaryActionLabel)
        assertEquals(2, state.nextPage())
    }
}
