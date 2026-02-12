package com.debanshu.xcalendar.domain.notifications

import android.app.Application
import com.debanshu.xcalendar.domain.model.Event
import com.debanshu.xcalendar.domain.model.EventSource
import com.debanshu.xcalendar.domain.model.Person
import com.debanshu.xcalendar.domain.model.PersonRole
import com.debanshu.xcalendar.domain.model.ScheduleFilter
import com.debanshu.xcalendar.domain.model.Task
import com.debanshu.xcalendar.domain.model.TaskEnergy
import com.debanshu.xcalendar.domain.model.TaskPriority
import com.debanshu.xcalendar.domain.model.TaskStatus
import com.debanshu.xcalendar.domain.model.TaskType
import com.debanshu.xcalendar.domain.util.ScheduleEngine
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Test suite for Today's Snapshot data aggregation.
 * Verifies that the schedule aggregation includes:
 * - Timed events
 * - All-day events
 * - Flexible tasks
 * 
 * This tests the core logic used by ReminderAlarmReceiver.buildSummary()
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    application = Application::class,
)
class SummaryNotificationTest {
    private val timeZone = TimeZone.currentSystemDefault()
    private var counter = 0L

    @Test
    fun scheduleAggregation_includesTimedEvents() {
        // Given: A timed event for today
        val now = Instant.fromEpochMilliseconds(java.lang.System.currentTimeMillis())
        val today = now.toLocalDateTime(timeZone).date
        val startTime = today.atTime(LocalTime(10, 0)).toInstant(timeZone).toEpochMilliseconds()
        val endTime = today.atTime(LocalTime(11, 0)).toInstant(timeZone).toEpochMilliseconds()
        
        val event = createEvent(
            title = "Morning Meeting",
            startTime = startTime,
            endTime = endTime,
            isAllDay = false,
        )

        // When: Aggregating schedule items
        val nowMillis = java.lang.System.currentTimeMillis()
        val aggregation = ScheduleEngine.aggregate(
            events = listOf(event),
            tasks = emptyList(),
            filter = ScheduleFilter(),
            nowMillis = nowMillis,
            timeZone = timeZone,
        )

        // Then: Timed event is included
        assertTrue(aggregation.items.any { it.title == "Morning Meeting" })
        val item = aggregation.items.first { it.title == "Morning Meeting" }
        assertEquals(startTime, item.startTime)
        assertFalse(item.isAllDay)
    }

    @Test
    fun scheduleAggregation_includesAllDayEvents() {
        // Given: An all-day event for today
        val now = Instant.fromEpochMilliseconds(java.lang.System.currentTimeMillis())
        val today = now.toLocalDateTime(timeZone).date
        val startTime = today.atStartOfDayIn(timeZone).toEpochMilliseconds()
        
        val event = createEvent(
            title = "Team Offsite",
            startTime = startTime,
            endTime = startTime,
            isAllDay = true,
        )

        // When: Aggregating schedule items
        val nowMillis = java.lang.System.currentTimeMillis()
        val aggregation = ScheduleEngine.aggregate(
            events = listOf(event),
            tasks = emptyList(),
            filter = ScheduleFilter(),
            nowMillis = nowMillis,
            timeZone = timeZone,
        )

        // Then: All-day event is included
        assertTrue(aggregation.items.any { it.title == "Team Offsite" })
        val item = aggregation.items.first { it.title == "Team Offsite" }
        assertTrue(item.isAllDay)
    }

    @Test
    fun scheduleAggregation_includesFlexibleTasks() {
        // Given: A flexible task (no specific time)
        val task = createTask(
            title = "Review preschool forms",
            priority = TaskPriority.MUST,
            scheduledStart = null,
        )

        // When: Aggregating schedule items
        val nowMillis = java.lang.System.currentTimeMillis()
        val aggregation = ScheduleEngine.aggregate(
            events = emptyList(),
            tasks = listOf(task),
            filter = ScheduleFilter(),
            nowMillis = nowMillis,
            timeZone = timeZone,
        )

        // Then: Flexible task is included
        assertTrue(aggregation.items.any { it.title == "Review preschool forms" })
        val item = aggregation.items.first { it.title == "Review preschool forms" }
        assertEquals(null, item.startTime)
        assertFalse(item.isAllDay)
    }

    @Test
    fun scheduleAggregation_includesMixedItemTypes() {
        // Given: Mix of timed event, all-day event, and flexible task
        val now = Instant.fromEpochMilliseconds(java.lang.System.currentTimeMillis())
        val today = now.toLocalDateTime(timeZone).date
        val timedStart = today.atTime(LocalTime(9, 0)).toInstant(timeZone).toEpochMilliseconds()
        val timedEnd = today.atTime(LocalTime(10, 0)).toInstant(timeZone).toEpochMilliseconds()
        val allDayStart = today.atStartOfDayIn(timeZone).toEpochMilliseconds()
        
        val timedEvent = createEvent(
            title = "Preschool pickup",
            startTime = timedStart,
            endTime = timedEnd,
            isAllDay = false,
        )
        val allDayEvent = createEvent(
            title = "Field trip day",
            startTime = allDayStart,
            endTime = allDayStart,
            isAllDay = true,
        )
        val flexibleTask = createTask(
            title = "Pack lunch boxes",
            priority = TaskPriority.SHOULD,
            scheduledStart = null,
        )

        // When: Aggregating schedule items
        val nowMillis = java.lang.System.currentTimeMillis()
        val aggregation = ScheduleEngine.aggregate(
            events = listOf(timedEvent, allDayEvent),
            tasks = listOf(flexibleTask),
            filter = ScheduleFilter(),
            nowMillis = nowMillis,
            timeZone = timeZone,
        )

        // Then: All three item types are included
        assertEquals(3, aggregation.items.size)
        assertTrue(aggregation.items.any { it.title == "Preschool pickup" })
        assertTrue(aggregation.items.any { it.title == "Field trip day" })
        assertTrue(aggregation.items.any { it.title == "Pack lunch boxes" })
    }

    @Test
    fun scheduleAggregation_sortsTimedItemsFirst() {
        // Given: Mix of timed and flexible items
        val now = Instant.fromEpochMilliseconds(java.lang.System.currentTimeMillis())
        val today = now.toLocalDateTime(timeZone).date
        val timedStart = today.atTime(LocalTime(14, 0)).toInstant(timeZone).toEpochMilliseconds()
        val timedEnd = today.atTime(LocalTime(15, 0)).toInstant(timeZone).toEpochMilliseconds()
        
        val timedEvent = createEvent(
            title = "Doctor appointment",
            startTime = timedStart,
            endTime = timedEnd,
            isAllDay = false,
        )
        val flexibleTask = createTask(
            title = "Buy groceries",
            priority = TaskPriority.SHOULD,
            scheduledStart = null,
        )

        // When: Aggregating schedule items
        val nowMillis = java.lang.System.currentTimeMillis()
        val aggregation = ScheduleEngine.aggregate(
            events = listOf(timedEvent),
            tasks = listOf(flexibleTask),
            filter = ScheduleFilter(),
            nowMillis = nowMillis,
            timeZone = timeZone,
        )

        // Then: Timed items should come before flexible items when taking top N
        val sortedByTimePresence = aggregation.items
            .sortedBy { it.startTime ?: Long.MAX_VALUE }
        
        // The first item (with lowest sort key) should be the timed event
        assertTrue(
            sortedByTimePresence.first().startTime != null,
            "Timed events should sort before flexible tasks"
        )
    }

    @Test
    fun scheduleAggregation_handlesEmptySchedule() {
        // Given: No events or tasks
        val nowMillis = java.lang.System.currentTimeMillis()
        
        // When: Aggregating schedule items
        val aggregation = ScheduleEngine.aggregate(
            events = emptyList(),
            tasks = emptyList(),
            filter = ScheduleFilter(),
            nowMillis = nowMillis,
            timeZone = timeZone,
        )

        // Then: Result is empty
        assertTrue(aggregation.items.isEmpty())
    }

    private fun createEvent(
        title: String,
        startTime: Long,
        endTime: Long,
        isAllDay: Boolean,
        affectedPersonIds: List<String> = emptyList(),
    ): Event {
        counter++
        return Event(
            id = "event-$counter",
            calendarId = "local",
            calendarName = "Local",
            title = title,
            description = "",
            location = "",
            startTime = startTime,
            endTime = endTime,
            isAllDay = isAllDay,
            color = 0xFF6200EE.toInt(),
            reminderMinutes = emptyList(),
            affectedPersonIds = affectedPersonIds,
            source = EventSource.LOCAL,
        )
    }

    private fun createTask(
        title: String,
        priority: TaskPriority,
        scheduledStart: Long?,
    ): Task {
        counter++
        return Task(
            id = "task-$counter",
            title = title,
            notes = "",
            status = TaskStatus.OPEN,
            priority = priority,
            energy = TaskEnergy.MEDIUM,
            type = TaskType.FLEXIBLE,
            scheduledStart = scheduledStart,
            scheduledEnd = null,
            dueAt = null,
            durationMinutes = 30,
            assignedToPersonId = null,
            affectedPersonIds = emptyList(),
            projectId = null,
            routineId = null,
            createdAt = java.lang.System.currentTimeMillis(),
        )
    }

    private fun createPerson(name: String): Person {
        counter++
        return Person(
            id = "person-$counter",
            name = name,
            role = PersonRole.CHILD,
            color = 0xFF03DAC5.toInt(),
            avatarUrl = "",
        )
    }
}
