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

    @Test
    fun scheduleSnooze_withKindStart_schedulesOneStartAlarmTenMinutesFromNow() {
        val scheduler = AndroidReminderScheduler()
        val before = System.currentTimeMillis()

        scheduler.scheduleSnooze(
            itemId = "e1",
            itemType = "event",
            title = "Meeting",
            startTime = before + 30 * MINUTE_MILLIS,
            endTime = before + 60 * MINUTE_MILLIS,
            kind = "start",
        )

        val alarms = Shadows.shadowOf(alarmManager).scheduledAlarms
        assertEquals(1, alarms.size)
        val triggerTime = alarms.first().triggerAtTime
        // Should fire approximately 10 minutes from now (allow 5 s tolerance for test timing).
        assertTrue(triggerTime >= before + 10 * MINUTE_MILLIS)
        assertTrue(triggerTime < before + 10 * MINUTE_MILLIS + 5_000L)
    }

    @Test
    fun scheduleSnooze_withKindPrep_schedulesPrepAlarmAndDoesNotClobberStartAlarm() =
        runBlocking {
            val scheduler = AndroidReminderScheduler()
            val now = System.currentTimeMillis()
            // Schedule the original event (which sets both KIND_PREP and KIND_START alarms).
            val event = buildEvent(startTime = now + 120 * MINUTE_MILLIS, reminderMinutes = listOf(20))
            scheduler.scheduleEvent(event, ReminderPreferences(remindersEnabled = true))
            val alarmsAfterEvent = Shadows.shadowOf(alarmManager).scheduledAlarms
            assertEquals(2, alarmsAfterEvent.size)

            // Snooze the KIND_PREP notification (user tapped snooze on the 20-min prep alert).
            scheduler.scheduleSnooze(
                itemId = event.id,
                itemType = "event",
                title = event.title,
                startTime = event.startTime,
                endTime = event.endTime,
                kind = "prep",
            )

            // There should now be 3 alarms: original KIND_START, original KIND_PREP overwritten
            // with the snoozed KIND_PREP (10 min from now), so still 2 (prep updated, start kept).
            val alarmsAfterSnooze = Shadows.shadowOf(alarmManager).scheduledAlarms
            assertEquals(2, alarmsAfterSnooze.size, "Snoozing KIND_PREP must not add or remove the KIND_START alarm")

            // The KIND_START alarm trigger time must remain unchanged.
            val startAlarmTrigger = alarmsAfterSnooze.map { it.triggerAtTime }.max()
            assertEquals(event.startTime, startAlarmTrigger, "KIND_START trigger must equal event.startTime")

            // The prep alarm must now be ~10 min from now, not 20 min before event start.
            val prepAlarmTrigger = alarmsAfterSnooze.map { it.triggerAtTime }.min()
            assertTrue(prepAlarmTrigger < event.startTime, "Snoozed prep alarm must fire before event start")
            assertTrue(prepAlarmTrigger >= now + 10 * MINUTE_MILLIS - 1_000L, "Snoozed prep alarm must be ~10 min from now")
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
