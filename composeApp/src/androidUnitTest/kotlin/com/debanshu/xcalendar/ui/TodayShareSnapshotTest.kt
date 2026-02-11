package com.debanshu.xcalendar.ui

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.work.testing.WorkManagerTestInitHelper
import com.debanshu.xcalendar.common.toLocalDateTime
import com.debanshu.xcalendar.domain.model.Event
import com.debanshu.xcalendar.domain.model.Person
import com.debanshu.xcalendar.domain.model.PersonRole
import com.debanshu.xcalendar.test.buildTestDependencies
import com.debanshu.xcalendar.ui.screen.todayScreen.TodayScreen
import com.debanshu.xcalendar.ui.state.DateStateHolder
import com.debanshu.xcalendar.ui.theme.XCalendarTheme
import kotlinx.collections.immutable.persistentListOf
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.time.Clock
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    application = Application::class,
)
class TodayShareSnapshotTest {

    @get:Rule
    val composeRule = createIntentComposeRule<TestActivity>()

    private lateinit var deps: com.debanshu.xcalendar.test.TestDependencies

    @Before
    fun setUp() {
        stopKoin()
        val context = ApplicationProvider.getApplicationContext<Context>()
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
        val mom =
            Person(
                id = "mom",
                name = "Mom",
                role = PersonRole.MOM,
                color = 0,
                isAdmin = true,
            )
        deps = buildTestDependencies(people = listOf(mom))
        startKoin { modules(deps.module) }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun shareSnapshot_sendsSharePayloadToNotifier() {
        val timeZone = TimeZone.currentSystemDefault()
        val nowMillis = Clock.System.now().toEpochMilliseconds()
        val today = nowMillis.toLocalDateTime(timeZone).date
        val dayStart = today.atStartOfDayIn(timeZone).toEpochMilliseconds()
        val event =
            Event(
                id = "event-1",
                calendarId = "local",
                calendarName = "Family",
                title = "Preschool pickup",
                startTime = dayStart + 12 * 60 * 60 * 1000L,
                endTime = dayStart + 13 * 60 * 60 * 1000L,
                color = 0,
            )
        val dateStateHolder = DateStateHolder().apply { updateSelectedDateState(today) }

        composeRule.setContent {
            XCalendarTheme {
                TodayScreen(
                    dateStateHolder = dateStateHolder,
                    events = persistentListOf(event),
                    holidays = persistentListOf(),
                    isVisible = true,
                )
            }
        }

        composeRule.onNodeWithText("Share snapshot").performClick()

        assertEquals(1, deps.notifier.sharedPayloads.size)
        val payload = deps.notifier.sharedPayloads.first()
        assertTrue(payload.first.contains("snapshot", ignoreCase = true))
        assertTrue(payload.second.contains("Preschool pickup"))
    }
}
