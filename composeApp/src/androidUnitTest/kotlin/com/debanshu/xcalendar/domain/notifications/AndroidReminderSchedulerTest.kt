package com.debanshu.xcalendar.domain.notifications

import android.app.AlarmManager
import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.debanshu.xcalendar.domain.model.Event
import com.debanshu.xcalendar.domain.model.ReminderPreferences
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    application = Application::class,
)
class AndroidReminderSchedulerTest {
    private lateinit var context: Context
    private lateinit var alarmManager: AlarmManager

    @Before
    fun setUp() {
        stopKoin()
        context = ApplicationProvider.getApplicationContext()
        alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        startKoin {
            modules(
                module {
                    single<Context> { context }
                },
            )
        }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun scheduleEvent_schedulesPrepAndStartAlarms() =
        runBlocking {
            val scheduler = AndroidReminderScheduler()
            val now = System.currentTimeMillis()
            val event = buildEvent(startTime = now + 120 * MINUTE_MILLIS, reminderMinutes = listOf(20))

            scheduler.scheduleEvent(event, ReminderPreferences(remindersEnabled = true))

            val alarms = Shadows.shadowOf(alarmManager).scheduledAlarms
            assertEquals(2, alarms.size)
            assertTrue(alarms.all { it.type == AlarmManager.RTC_WAKEUP })

            val triggerTimes = alarms.map { it.triggerAtTime }.sorted()
            assertEquals(
                listOf(event.startTime - 20 * MINUTE_MILLIS, event.startTime),
                triggerTimes,
            )
        }

    @Test
    fun cancelSummaries_removesAllScheduledSummaryAlarms() =
        runBlocking {
            val scheduler = AndroidReminderScheduler()
            val prefs = ReminderPreferences(summaryEnabled = true)
            scheduler.scheduleSummaries(prefs)
            assertEquals(2, Shadows.shadowOf(alarmManager).scheduledAlarms.size)

            scheduler.cancelSummaries()

            assertEquals(0, Shadows.shadowOf(alarmManager).scheduledAlarms.size)
        }

    private fun buildEvent(
        startTime: Long,
        reminderMinutes: List<Int>,
    ): Event =
        Event(
            id = "event-${System.nanoTime()}",
            calendarId = "calendar-local",
            calendarName = "Local",
            title = "Reminder Test",
            startTime = startTime,
            endTime = startTime + 30 * MINUTE_MILLIS,
            reminderMinutes = reminderMinutes,
            color = 0xFF0000,
        )

    private companion object {
        const val MINUTE_MILLIS = 60_000L
    }
}
