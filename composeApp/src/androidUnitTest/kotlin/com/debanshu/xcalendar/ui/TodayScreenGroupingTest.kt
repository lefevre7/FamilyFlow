package com.debanshu.xcalendar.ui

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
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

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    application = Application::class,
)
class TodayScreenGroupingTest {

    @get:Rule
    val composeRule = createIntentComposeRule<TestActivity>()

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
        val deps = buildTestDependencies(people = listOf(mom))
        startKoin { modules(deps.module) }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun showsMorningAfternoonEveningGroups() {
        val timeZone = TimeZone.currentSystemDefault()
        val nowMillis = Clock.System.now().toEpochMilliseconds()
        val today = nowMillis.toLocalDateTime(timeZone).date
        val dayStart = today.atStartOfDayIn(timeZone).toEpochMilliseconds()

        val morning =
            Event(
                id = "event-morning",
                calendarId = "local",
                calendarName = "Family",
                title = "Breakfast",
                startTime = dayStart + 9 * 60 * 60 * 1000L,
                endTime = dayStart + 10 * 60 * 60 * 1000L,
                color = 0,
            )
        val afternoon =
            Event(
                id = "event-afternoon",
                calendarId = "local",
                calendarName = "Family",
                title = "Playdate",
                startTime = dayStart + 14 * 60 * 60 * 1000L,
                endTime = dayStart + 15 * 60 * 60 * 1000L,
                color = 0,
            )
        val evening =
            Event(
                id = "event-evening",
                calendarId = "local",
                calendarName = "Family",
                title = "Dinner",
                startTime = dayStart + 19 * 60 * 60 * 1000L,
                endTime = dayStart + 20 * 60 * 60 * 1000L,
                color = 0,
            )

        val dateStateHolder = DateStateHolder().apply { updateSelectedDateState(today) }

        composeRule.setContent {
            XCalendarTheme {
                TodayScreen(
                    dateStateHolder = dateStateHolder,
                    events = persistentListOf(morning, afternoon, evening),
                    isVisible = true,
                )
            }
        }

        composeRule.onNodeWithText("Morning").assertExists()
        composeRule.onNodeWithText("Afternoon").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Evening").performScrollTo().assertIsDisplayed()
    }
}
