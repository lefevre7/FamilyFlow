package com.debanshu.xcalendar.ui

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.work.testing.WorkManagerTestInitHelper
import com.debanshu.xcalendar.test.TestDependencies
import com.debanshu.xcalendar.test.buildTestDependencies
import com.debanshu.xcalendar.ui.components.dialog.QuickAddContent
import com.debanshu.xcalendar.ui.components.dialog.QuickAddMode
import com.debanshu.xcalendar.ui.theme.XCalendarTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    application = Application::class,
)
class QuickAddFlowTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<TestActivity>()

    private lateinit var deps: TestDependencies

    @Before
    fun setUp() {
        stopKoin()
        val context = ApplicationProvider.getApplicationContext<Context>()
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
        deps = buildTestDependencies()
        startKoin { modules(deps.module) }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun quickAddTaskSavesTask() {
        composeRule.setContent {
            var mode by mutableStateOf(QuickAddMode.TASK)
            XCalendarTheme {
                QuickAddContent(
                    mode = mode,
                    onModeChange = { mode = it },
                    onRequestEvent = {},
                    onDismiss = {},
                )
            }
        }

        composeRule.onNode(hasSetTextAction()).performTextInput("Laundry")
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Save").assertIsEnabled().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            deps.taskRepository.upserted.isNotEmpty()
        }

        assertTrue(deps.taskRepository.upserted.isNotEmpty())
        assertEquals("Laundry", deps.taskRepository.upserted.last().title)
    }

    @Test
    fun quickAddEventShowsEventSheetAction() {
        var requestedEvent = false
        composeRule.setContent {
            var mode by mutableStateOf(QuickAddMode.TASK)
            XCalendarTheme {
                QuickAddContent(
                    mode = mode,
                    onModeChange = { mode = it },
                    onRequestEvent = { requestedEvent = true },
                    onDismiss = {},
                )
            }
        }

        composeRule.onNodeWithText("Event").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Create a timed event").assertIsDisplayed()
        composeRule.onNodeWithText("Open event sheet").performClick()
        composeRule.waitForIdle()
        composeRule.runOnIdle {
            assertTrue(requestedEvent)
        }
    }
}
