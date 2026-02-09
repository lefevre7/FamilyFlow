package com.debanshu.xcalendar.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.debanshu.xcalendar.domain.model.InboxItem
import com.debanshu.xcalendar.domain.model.InboxSource
import com.debanshu.xcalendar.domain.model.InboxStatus
import com.debanshu.xcalendar.ui.screen.planScreen.BrainDumpSection
import com.debanshu.xcalendar.ui.theme.XCalendarTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    application = Application::class,
)
class PlanInlineCaptureTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<TestActivity>()

    @Test
    fun inlineCapture_addsInboxText_andProcessesNewItems() {
        val captured = mutableListOf<String>()
        var processAllCount = 0

        composeRule.setContent {
            var captureText by mutableStateOf("")
            XCalendarTheme {
                BrainDumpSection(
                    inboxItems =
                        listOf(
                            InboxItem(
                                id = "item-new",
                                rawText = "Existing item",
                                source = InboxSource.TEXT,
                                status = InboxStatus.NEW,
                                createdAt = 1L,
                            ),
                        ),
                    captureText = captureText,
                    onCaptureTextChanged = { captureText = it },
                    onCapture = {
                        val next = captureText.trim()
                        if (next.isNotEmpty()) {
                            captured += next
                            captureText = ""
                        }
                    },
                    onProcessAll = { processAllCount += 1 },
                    onProcessItem = {},
                    onArchive = {},
                )
            }
        }

        composeRule.onNode(hasSetTextAction()).performTextInput("Call preschool")
        composeRule.onNodeWithText("Add to inbox").assertIsEnabled().performClick()
        composeRule.runOnIdle {
            assertEquals(listOf("Call preschool"), captured)
        }

        composeRule.onNodeWithText("Process suggestions").assertIsEnabled().performClick()
        composeRule.runOnIdle {
            assertEquals(1, processAllCount)
        }
        composeRule.onNode(hasSetTextAction()).assertExists()
    }
}
