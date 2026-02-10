package com.debanshu.xcalendar.ui

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.work.testing.WorkManagerTestInitHelper
import com.debanshu.xcalendar.common.toLocalDateTime
import com.debanshu.xcalendar.domain.model.Person
import com.debanshu.xcalendar.domain.model.PersonRole
import com.debanshu.xcalendar.domain.model.Task
import com.debanshu.xcalendar.domain.model.TaskStatus
import com.debanshu.xcalendar.test.TestDependencies
import com.debanshu.xcalendar.test.buildTestDependencies
import com.debanshu.xcalendar.ui.screen.todayScreen.TodayScreen
import com.debanshu.xcalendar.ui.state.DateStateHolder
import com.debanshu.xcalendar.ui.theme.XCalendarTheme
import kotlinx.collections.immutable.persistentListOf
import kotlinx.datetime.TimeZone
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
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    application = Application::class,
)
class TodayQuickActionsTest {

    @get:Rule
    val composeRule = createIntentComposeRule<TestActivity>()

    private lateinit var deps: TestDependencies

    @Before
    fun setUp() {
        stopKoin()
        val context = ApplicationProvider.getApplicationContext<Context>()
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun doneAction_marksTaskDone() {
        val nowTask =
            Task(
                id = "task-done",
                title = "Laundry",
                status = TaskStatus.OPEN,
                assignedToPersonId = "person_mom",
                durationMinutes = 30,
            )
        startScreen(task = nowTask)

        composeRule.onNodeWithContentDescription("Mark Laundry done").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            deps.taskRepository.upserted.any { it.id == nowTask.id && it.status == TaskStatus.DONE }
        }

        val updated = deps.taskRepository.upserted.last { it.id == nowTask.id }
        assertEquals(TaskStatus.DONE, updated.status)
    }

    @Test
    fun snoozeAction_setsFutureScheduleForTask() {
        val snoozeTask =
            Task(
                id = "task-snooze",
                title = "Dishes",
                status = TaskStatus.OPEN,
                assignedToPersonId = "person_mom",
                durationMinutes = 25,
            )
        startScreen(task = snoozeTask)

        composeRule.onNodeWithContentDescription("Snooze Dishes").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            deps.taskRepository.upserted.any { it.id == snoozeTask.id && it.scheduledStart != null }
        }

        val updated = deps.taskRepository.upserted.last { it.id == snoozeTask.id }
        val start = updated.scheduledStart
        val end = updated.scheduledEnd
        assertNotNull(start)
        assertNotNull(end)
        assertTrue(start > Clock.System.now().toEpochMilliseconds())
        assertEquals(25 * 60_000L, end - start)
    }

    private fun startScreen(task: Task) {
        val timeZone = TimeZone.currentSystemDefault()
        val today = Clock.System.now().toEpochMilliseconds().toLocalDateTime(timeZone).date
        val dateStateHolder = DateStateHolder().apply { updateSelectedDateState(today) }
        val mom =
            Person(
                id = "person_mom",
                name = "Mom",
                role = PersonRole.MOM,
                color = 0,
                isAdmin = true,
            )

        deps = buildTestDependencies(people = listOf(mom), tasks = listOf(task))
        startKoin { modules(deps.module) }

        composeRule.setContent {
            XCalendarTheme {
                TodayScreen(
                    dateStateHolder = dateStateHolder,
                    events = persistentListOf(),
                    isVisible = true,
                )
            }
        }
    }
}
