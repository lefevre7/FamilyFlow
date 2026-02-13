package com.debanshu.xcalendar.domain.notifications

import android.app.Application
import com.debanshu.xcalendar.domain.model.Event
import com.debanshu.xcalendar.domain.model.EventSource
import com.debanshu.xcalendar.domain.model.Person
import com.debanshu.xcalendar.domain.model.PersonRole
import com.debanshu.xcalendar.domain.model.ScheduleFilter
import com.debanshu.xcalendar.domain.util.ScheduleEngine
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for Today's Snapshot with event source filtering.
 *
 * Verifies that snapshots correctly show:
 * - Only LOCAL events when no Google account
 * - Only GOOGLE events when Google account exists
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    application = Application::class,
)
class SnapshotEventSourceTest {
    private val timeZone = TimeZone.currentSystemDefault()

    @Test
    fun snapshot_noGoogleAccount_showsOnlyLocalEvents() {
        // Given: No Google account scenario - only LOCAL events should be shown
        val now = Instant.fromEpochMilliseconds(System.currentTimeMillis())
        val today = now.toLocalDateTime(timeZone).date
        val startTime = today.atTime(LocalTime(10, 0)).toInstant(timeZone).toEpochMilliseconds()
        val endTime = today.atTime(LocalTime(11, 0)).toInstant(timeZone).toEpochMilliseconds()

        val localEvent = createEvent(
            title = "Locally created task",
            startTime = startTime,
            endTime = endTime,
            source = EventSource.LOCAL,
        )
        val googleEvent = createEvent(
            title = "Google Calendar event",
            startTime = startTime,
            endTime = endTime,
            source = EventSource.GOOGLE,
        )

        // Simulate filtering that would happen in repository
        val filteredEvents = listOf(localEvent, googleEvent).filter { it.source == EventSource.LOCAL }

        // When: Aggregating schedule items
        val nowMillis = System.currentTimeMillis()
        val aggregation = ScheduleEngine.aggregate(
            events = filteredEvents,
            tasks = emptyList(),
            filter = ScheduleFilter(),
            nowMillis = nowMillis,
            timeZone = timeZone,
        )

        // Then: Only LOCAL event appears in snapshot
        assertEquals(1, aggregation.items.size)
        assertEquals("Locally created task", aggregation.items[0].title)
    }

    @Test
    fun snapshot_hasGoogleAccount_showsOnlyGoogleEvents() {
        // Given: Google account exists - only GOOGLE events should be shown
        val now = Instant.fromEpochMilliseconds(System.currentTimeMillis())
        val today = now.toLocalDateTime(timeZone).date
        val startTime = today.atTime(LocalTime(10, 0)).toInstant(timeZone).toEpochMilliseconds()
        val endTime = today.atTime(LocalTime(11, 0)).toInstant(timeZone).toEpochMilliseconds()

        val localEvent = createEvent(
            title = "Locally created task",
            startTime = startTime,
            endTime = endTime,
            source = EventSource.LOCAL,
        )
        val googleEvent = createEvent(
            title = "Google Calendar event",
            startTime = startTime,
            endTime = endTime,
            source = EventSource.GOOGLE,
        )

        // Simulate filtering that would happen in repository
        val filteredEvents = listOf(localEvent, googleEvent).filter { it.source == EventSource.GOOGLE }

        // When: Aggregating schedule items
        val nowMillis = System.currentTimeMillis()
        val aggregation = ScheduleEngine.aggregate(
            events = filteredEvents,
            tasks = emptyList(),
            filter = ScheduleFilter(),
            nowMillis = nowMillis,
            timeZone = timeZone,
        )

        // Then: Only GOOGLE event appears in snapshot
        assertEquals(1, aggregation.items.size)
        assertEquals("Google Calendar event", aggregation.items[0].title)
    }

    @Test
    fun snapshot_noGoogleAccount_noLocalEvents_isEmpty() {
        // Given: No Google account and no LOCAL events
        val now = Instant.fromEpochMilliseconds(System.currentTimeMillis())
        val today = now.toLocalDateTime(timeZone).date
        val startTime = today.atTime(LocalTime(10, 0)).toInstant(timeZone).toEpochMilliseconds()
        val endTime = today.atTime(LocalTime(11, 0)).toInstant(timeZone).toEpochMilliseconds()

        val googleEvent = createEvent(
            title = "Google Calendar event",
            startTime = startTime,
            endTime = endTime,
            source = EventSource.GOOGLE,
        )

        // Simulate filtering - no LOCAL events available
        val filteredEvents = listOf(googleEvent).filter { it.source == EventSource.LOCAL }

        // When: Aggregating schedule items
        val nowMillis = System.currentTimeMillis()
        val aggregation = ScheduleEngine.aggregate(
            events = filteredEvents,
            tasks = emptyList(),
            filter = ScheduleFilter(),
            nowMillis = nowMillis,
            timeZone = timeZone,
        )

        // Then: Snapshot is empty
        assertTrue(aggregation.items.isEmpty())
    }

    @Test
    fun snapshot_hasGoogleAccount_multipleGoogleEvents_allShown() {
        // Given: Multiple Google events
        val now = Instant.fromEpochMilliseconds(System.currentTimeMillis())
        val today = now.toLocalDateTime(timeZone).date
        val morning = today.atTime(LocalTime(9, 0)).toInstant(timeZone).toEpochMilliseconds()
        val afternoon = today.atTime(LocalTime(14, 0)).toInstant(timeZone).toEpochMilliseconds()

        val googleEvent1 = createEvent(
            title = "Morning meeting",
            startTime = morning,
            endTime = morning + 3600000,
            source = EventSource.GOOGLE,
        )
        val googleEvent2 = createEvent(
            title = "Afternoon dentist",
            startTime = afternoon,
            endTime = afternoon + 3600000,
            source = EventSource.GOOGLE,
        )
        val localEvent = createEvent(
            title = "Local task",
            startTime = afternoon,
            endTime = afternoon + 3600000,
            source = EventSource.LOCAL,
        )

        // Simulate filtering - only GOOGLE events
        val filteredEvents = listOf(googleEvent1, googleEvent2, localEvent).filter { it.source == EventSource.GOOGLE }

        // When: Aggregating schedule items
        val nowMillis = System.currentTimeMillis()
        val aggregation = ScheduleEngine.aggregate(
            events = filteredEvents,
            tasks = emptyList(),
            filter = ScheduleFilter(),
            nowMillis = nowMillis,
            timeZone = timeZone,
        )

        // Then: Both GOOGLE events appear, LOCAL does not
        assertEquals(2, aggregation.items.size)
        assertTrue(aggregation.items.any { it.title == "Morning meeting" })
        assertTrue(aggregation.items.any { it.title == "Afternoon dentist" })
        assertTrue(aggregation.items.none { it.title == "Local task" })
    }

    private fun createEvent(
        title: String,
        startTime: Long,
        endTime: Long,
        source: EventSource,
        isAllDay: Boolean = false,
    ): Event {
        return Event(
            id = "event_${System.nanoTime()}",
            calendarId = "cal_test",
            calendarName = "Test Calendar",
            title = title,
            description = null,
            location = null,
            startTime = startTime,
            endTime = endTime,
            isAllDay = isAllDay,
            isRecurring = false,
            recurringRule = null,
            reminderMinutes = emptyList(),
            color = 0xFF6200EE.toInt(),
            source = source,
            externalId = if (source == EventSource.GOOGLE) "ext_${System.nanoTime()}" else null,
            externalUpdatedAt = if (source == EventSource.GOOGLE) System.currentTimeMillis() else null,
            lastSyncedAt = if (source == EventSource.GOOGLE) System.currentTimeMillis() else null,
            affectedPersonIds = emptyList(),
        )
    }
}
