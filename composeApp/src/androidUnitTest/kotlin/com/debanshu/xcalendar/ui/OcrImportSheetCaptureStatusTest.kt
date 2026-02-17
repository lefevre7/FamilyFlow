package com.debanshu.xcalendar.ui

import android.app.Application
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import com.debanshu.xcalendar.domain.model.Calendar
import com.debanshu.xcalendar.domain.model.OcrStructuredResult
import com.debanshu.xcalendar.domain.model.Person
import com.debanshu.xcalendar.platform.OcrCaptureController
import com.debanshu.xcalendar.ui.screen.planScreen.OcrImportSheet
import com.debanshu.xcalendar.ui.theme.XCalendarTheme
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.time.Clock

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    application = Application::class,
)
class OcrImportSheetCaptureStatusTest {

    @get:Rule
    val composeRule = createIntentComposeRule<TestActivity>()

    @Test
    fun captureStatus_updatesAcrossCancelAndSkipFlows() {
        val harness = FakeOcrControllerHarness()

        composeRule.setContent {
            XCalendarTheme {
                OcrImportSheet(
                    calendars =
                        listOf(
                            Calendar(
                                id = "calendar-personal",
                                name = "Personal",
                                color = 0xFF00AA,
                                userId = "user-1",
                                isVisible = true,
                                isPrimary = true,
                            ),
                        ),
                    selectedCalendarId = "calendar-personal",
                    onCalendarSelected = {},
                    onDismiss = {},
                    createEventOverride = {},
                    structureOcrOverride = { rawText, _, _ ->
                        OcrStructuredResult(rawText = rawText, candidates = emptyList())
                    },
                    peopleFlowOverride = flowOf<List<Person>>(emptyList()),
                    ocrControllerFactory = { onResult, onError, onStatusChanged ->
                        harness.bind(
                            onResult = onResult,
                            onError = onError,
                            onStatusChanged = onStatusChanged,
                        )
                    },
                )
            }
        }

        composeRule.onAllNodesWithText("Scan with camera").assertCountEquals(1)
        composeRule.runOnIdle { harness.waitingForCrop() }
        composeRule.onAllNodesWithText("Waiting for crop...").assertCountEquals(1)

        composeRule.runOnIdle { harness.cancel() }
        composeRule.waitForIdle()
        composeRule.onAllNodesWithText("Waiting for crop...").assertCountEquals(0)

        composeRule.runOnIdle { harness.waitingForCrop() }
        composeRule.onAllNodesWithText("Waiting for crop...").assertCountEquals(1)

        composeRule.runOnIdle { harness.processing() }
        composeRule.waitForIdle()
        composeRule.onAllNodesWithText("Processing image...").assertCountEquals(1)

        val timestamp = Clock.System.now().toLocalDateTime(TimeZone.UTC).time.hour
        composeRule.runOnIdle {
            harness.result("PTA meeting at $timestamp pm")
        }
        composeRule.waitForIdle()
        composeRule.onAllNodesWithText("Image text").assertCountEquals(1)
        composeRule.onAllNodesWithText("Processing image...").assertCountEquals(0)
    }

    @Test
    fun ocrText_requiresExplicitStructureAction() {
        val harness = FakeOcrControllerHarness()
        var structureCalls = 0

        composeRule.setContent {
            XCalendarTheme {
                OcrImportSheet(
                    calendars =
                        listOf(
                            Calendar(
                                id = "calendar-personal",
                                name = "Personal",
                                color = 0xFF00AA,
                                userId = "user-1",
                                isVisible = true,
                                isPrimary = true,
                            ),
                        ),
                    selectedCalendarId = "calendar-personal",
                    onCalendarSelected = {},
                    onDismiss = {},
                    createEventOverride = {},
                    structureOcrOverride = { rawText, _, _ ->
                        structureCalls += 1
                        OcrStructuredResult(rawText = rawText, candidates = emptyList())
                    },
                    peopleFlowOverride = flowOf<List<Person>>(emptyList()),
                    ocrControllerFactory = { onResult, onError, onStatusChanged ->
                        harness.bind(
                            onResult = onResult,
                            onError = onError,
                            onStatusChanged = onStatusChanged,
                        )
                    },
                )
            }
        }

        composeRule.runOnIdle { harness.result("School day starts 8:00 AM") }
        composeRule.waitForIdle()
        composeRule.runOnIdle { assertEquals(0, structureCalls) }
        composeRule.onAllNodesWithText("Image text").assertCountEquals(1)
        composeRule.onAllNodesWithText("Edit text").assertCountEquals(1)
        composeRule.onAllNodesWithText("Structure with AI").assertCountEquals(1)
        composeRule.runOnIdle { assertEquals(0, structureCalls) }
    }
}

private class FakeOcrControllerHarness {
    private var onResult: ((String) -> Unit)? = null
    private var onError: ((String) -> Unit)? = null
    private var onStatusChanged: ((String?) -> Unit)? = null

    fun bind(
        onResult: (String) -> Unit,
        onError: (String) -> Unit,
        onStatusChanged: (String?) -> Unit,
    ): OcrCaptureController {
        this.onResult = onResult
        this.onError = onError
        this.onStatusChanged = onStatusChanged
        return controller
    }

    fun cancel() {
        onStatusChanged?.invoke(null)
    }

    fun processing() {
        onStatusChanged?.invoke("Processing image...")
    }

    fun waitingForCrop() {
        onStatusChanged?.invoke("Waiting for crop...")
    }

    fun result(text: String) {
        onResult?.invoke(text)
    }

    private val controller =
        object : OcrCaptureController {
            override val isAvailable: Boolean = true

            override fun captureFromCamera() {
                onStatusChanged?.invoke("Waiting for crop...")
            }

            override fun pickFromGallery() {
                onStatusChanged?.invoke("Waiting for crop...")
            }
        }
}
