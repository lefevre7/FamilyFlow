package com.debanshu.xcalendar.ui

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.work.testing.WorkManagerTestInitHelper
import com.debanshu.xcalendar.common.toLocalDateTime
import com.debanshu.xcalendar.domain.model.Holiday
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
class TodayHolidaySectionTest {
    @get:Rule
    val composeRule = createIntentComposeRule<TestActivity>()

    @Before
    fun setUp() {
        stopKoin()
        val context = ApplicationProvider.getApplicationContext<Context>()
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
        val mom =
            Person(
                id = "person_mom",
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
    fun displaysHolidaySectionForSelectedDate() {
        val timeZone = TimeZone.currentSystemDefault()
        val today = Clock.System.now().toEpochMilliseconds().toLocalDateTime(timeZone).date
        val dayStart = today.atStartOfDayIn(timeZone).toEpochMilliseconds()
        val holiday =
            Holiday(
                id = "holiday_today",
                name = "Family Day",
                date = dayStart,
                countryCode = "US",
                holidayType = "public_holiday",
            )
        val dateStateHolder = DateStateHolder().apply { updateSelectedDateState(today) }

        composeRule.setContent {
            XCalendarTheme {
                TodayScreen(
                    dateStateHolder = dateStateHolder,
                    events = persistentListOf(),
                    holidays = persistentListOf(holiday),
                    isVisible = true,
                )
            }
        }

        composeRule.onNodeWithText("Holidays").assertIsDisplayed()
        composeRule.onNodeWithText("Family Day").assertIsDisplayed()
    }
}
