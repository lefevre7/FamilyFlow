package com.debanshu.xcalendar.ui

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.debanshu.xcalendar.domain.model.VoiceCaptureSource
import com.debanshu.xcalendar.domain.model.VoiceDiagnosticEntry
import com.debanshu.xcalendar.domain.model.VoiceDiagnosticLevel
import com.debanshu.xcalendar.domain.model.VoiceDiagnosticStep
import com.debanshu.xcalendar.ui.screen.settingsScreen.VoiceDiagnosticsSection
import com.debanshu.xcalendar.ui.theme.XCalendarTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    application = Application::class,
)
class SettingsVoiceDiagnosticsSectionTest {

    @get:Rule
    val composeRule = createIntentComposeRule<TestActivity>()

    @Test
    fun sectionRendersDiagnosticsAndInvokesActions() {
        var clearCount = 0
        var copyCount = 0
        var shareCount = 0

        val entries =
            listOf(
                VoiceDiagnosticEntry(
                    id = "entry-1",
                    sessionId = "session-1",
                    timestampMillis = 1_728_000_000_000L,
                    source = VoiceCaptureSource.QUICK_ADD_VOICE,
                    step = VoiceDiagnosticStep.LLM_RESPONSE_RECEIVED,
                    level = VoiceDiagnosticLevel.INFO,
                    message = "Received LLM response.",
                    attemptIndex = 1,
                    transcript = "Kid 1 has an appointment today at 8am",
                    llmRawResponse = """{"tasks":[{"title":"Kid 1 appointment"}]}""",
                ),
            )

        composeRule.setContent {
            XCalendarTheme {
                VoiceDiagnosticsSection(
                    enabled = true,
                    entries = entries,
                    message = "Diagnostics ready",
                    hasLatestAttempt = true,
                    onToggleEnabled = {},
                    onClear = { clearCount += 1 },
                    onCopyLatest = { copyCount += 1 },
                    onShareLatest = { shareCount += 1 },
                )
            }
        }

        composeRule.onNodeWithText("Voice + Local AI diagnostics").assertIsDisplayed()
        composeRule.onNodeWithText("Transcript: Kid 1 has an appointment today at 8am").assertIsDisplayed()
        composeRule.onNodeWithText("""Raw output: {"tasks":[{"title":"Kid 1 appointment"}]}""").assertIsDisplayed()
        composeRule.onNodeWithText("Copy latest attempt").performClick()
        composeRule.onNodeWithText("Share latest attempt").performClick()
        composeRule.onNodeWithText("Clear").performClick()

        composeRule.runOnIdle {
            assertEquals(1, copyCount)
            assertEquals(1, shareCount)
            assertEquals(1, clearCount)
        }
    }
}
