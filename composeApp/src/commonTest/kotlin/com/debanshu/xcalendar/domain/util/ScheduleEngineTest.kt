package com.debanshu.xcalendar.domain.util

import com.debanshu.xcalendar.domain.model.Event
import com.debanshu.xcalendar.domain.model.ScheduleFilter
import com.debanshu.xcalendar.domain.model.Task
import com.debanshu.xcalendar.domain.model.TaskEnergy
import com.debanshu.xcalendar.domain.model.TaskPriority
import com.debanshu.xcalendar.domain.model.TaskStatus
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock

class ScheduleEngineTest {

    @Test
    fun aggregate_sortsTimedBeforeFloatingAndByPriority() {
        val timeZone = TimeZone.currentSystemDefault()
        val nowMillis = Clock.System.now().toEpochMilliseconds()
        val today = Instant.fromEpochMilliseconds(nowMillis).toLocalDateTime(timeZone).date
        val dayStart = today.atStartOfDayIn(timeZone).toEpochMilliseconds()

        val eventEarly =
            Event(
                id = "event-early",
                calendarId = "local",
                calendarName = "Family",
                title = "Early",
                startTime = dayStart + 8 * 60 * 60 * 1000L,
                endTime = dayStart + 9 * 60 * 60 * 1000L,
                color = 0,
            )
        val eventLate =
            Event(
                id = "event-late",
                calendarId = "local",
                calendarName = "Family",
                title = "Late",
                startTime = dayStart + 10 * 60 * 60 * 1000L,
                endTime = dayStart + 11 * 60 * 60 * 1000L,
                color = 0,
            )

        val mustTask =
            Task(
                id = "task-must",
                title = "Zebra",
                priority = TaskPriority.MUST,
                energy = TaskEnergy.MEDIUM,
            )
        val niceTask =
            Task(
                id = "task-nice",
                title = "Alpha",
                priority = TaskPriority.NICE,
                energy = TaskEnergy.LOW,
            )

        val result =
            ScheduleEngine.aggregate(
                events = listOf(eventLate, eventEarly),
                tasks = listOf(niceTask, mustTask),
                filter = ScheduleFilter(),
                nowMillis = nowMillis,
                timeZone = timeZone,
            )

        val ids = result.items.map { it.id }
        assertEquals(listOf("event-early", "event-late", "task-must", "task-nice"), ids)
    }

    @Test
    fun aggregate_respectsPersonAndMustFilters() {
        val nowMillis = Clock.System.now().toEpochMilliseconds()
        val taskMust =
            Task(
                id = "task-must",
                title = "Must",
                priority = TaskPriority.MUST,
                assignedToPersonId = "mom",
            )
        val taskNice =
            Task(
                id = "task-nice",
                title = "Nice",
                priority = TaskPriority.NICE,
                assignedToPersonId = "mom",
            )
        val event =
            Event(
                id = "event",
                calendarId = "local",
                calendarName = "Family",
                title = "Event",
                startTime = nowMillis,
                endTime = nowMillis + 60 * 60 * 1000L,
                color = 0,
            )

        val result =
            ScheduleEngine.aggregate(
                events = listOf(event),
                tasks = listOf(taskNice, taskMust),
                filter = ScheduleFilter(personId = "mom", onlyMust = true, includeUnassignedEvents = false),
                nowMillis = nowMillis,
            )

        val ids = result.items.map { it.id }
        assertEquals(listOf("task-must"), ids)
    }

    @Test
    fun aggregate_detectsConflicts() {
        val nowMillis = Clock.System.now().toEpochMilliseconds()
        val eventA =
            Event(
                id = "event-a",
                calendarId = "local",
                calendarName = "Family",
                title = "Overlap A",
                startTime = nowMillis,
                endTime = nowMillis + 60 * 60 * 1000L,
                color = 0,
            )
        val eventB =
            Event(
                id = "event-b",
                calendarId = "local",
                calendarName = "Family",
                title = "Overlap B",
                startTime = nowMillis + 30 * 60 * 1000L,
                endTime = nowMillis + 90 * 60 * 1000L,
                color = 0,
            )

        val result =
            ScheduleEngine.aggregate(
                events = listOf(eventA, eventB),
                tasks = emptyList(),
                filter = ScheduleFilter(),
                nowMillis = nowMillis,
            )

        assertTrue(result.conflicts.isNotEmpty())
        assertTrue(result.conflicts.any { setOf(it.itemIdA, it.itemIdB) == setOf("event-a", "event-b") })
    }

    @Test
    fun aggregate_buildsSuggestionsForFlexibleTasks() {
        val timeZone = TimeZone.currentSystemDefault()
        val nowMillis = Clock.System.now().toEpochMilliseconds()
        val today = Instant.fromEpochMilliseconds(nowMillis).toLocalDateTime(timeZone).date
        val dayStart = today.atStartOfDayIn(timeZone).toEpochMilliseconds()

        val busyEvent =
            Event(
                id = "event-busy",
                calendarId = "local",
                calendarName = "Family",
                title = "Busy",
                startTime = dayStart + 12 * 60 * 60 * 1000L,
                endTime = dayStart + 13 * 60 * 60 * 1000L,
                color = 0,
            )
        val flexibleTask =
            Task(
                id = "task-flex",
                title = "Laundry",
                status = TaskStatus.OPEN,
                energy = TaskEnergy.HIGH,
                durationMinutes = 60,
                scheduledStart = null,
                scheduledEnd = null,
            )

        val result =
            ScheduleEngine.aggregate(
                events = listOf(busyEvent),
                tasks = listOf(flexibleTask),
                filter = ScheduleFilter(),
                nowMillis = nowMillis,
                timeZone = timeZone,
            )

        val suggestions = result.suggestions.filter { it.taskId == "task-flex" }
        assertEquals(2, suggestions.size)
        suggestions.forEach { suggestion ->
            val overlapsBusy =
                suggestion.startTime < busyEvent.endTime && busyEvent.startTime < suggestion.endTime
            assertTrue(!overlapsBusy)
        }
    }

    @Test
    fun aggregate_enforcesDefaultDailyLoadCapForMustShouldSuggestions() {
        val timeZone = TimeZone.currentSystemDefault()
        val nowMillis = Clock.System.now().toEpochMilliseconds()
        val today = Instant.fromEpochMilliseconds(nowMillis).toLocalDateTime(timeZone).date
        val dayStart = today.atStartOfDayIn(timeZone).toEpochMilliseconds()

        val scheduledMustShould =
            (0 until 5).map { index ->
                val start = dayStart + (8 + index) * 60 * 60 * 1000L
                Task(
                    id = "scheduled-$index",
                    title = "Scheduled $index",
                    status = TaskStatus.OPEN,
                    priority = if (index % 2 == 0) TaskPriority.MUST else TaskPriority.SHOULD,
                    scheduledStart = start,
                    scheduledEnd = start + 30 * 60 * 1000L,
                )
            }
        val extraMust =
            Task(
                id = "task-extra-must",
                title = "Extra must",
                status = TaskStatus.OPEN,
                priority = TaskPriority.MUST,
                scheduledStart = null,
                scheduledEnd = null,
            )
        val extraShould =
            Task(
                id = "task-extra-should",
                title = "Extra should",
                status = TaskStatus.OPEN,
                priority = TaskPriority.SHOULD,
                scheduledStart = null,
                scheduledEnd = null,
            )

        val result =
            ScheduleEngine.aggregate(
                events = emptyList(),
                tasks = scheduledMustShould + extraMust + extraShould,
                filter = ScheduleFilter(),
                nowMillis = nowMillis,
                timeZone = timeZone,
            )

        assertTrue(result.suggestions.none { it.taskId == extraMust.id })
        assertTrue(result.suggestions.none { it.taskId == extraShould.id })
    }

    @Test
    fun aggregate_ordersFlexibleSuggestionsByPriorityThenDueDate() {
        val timeZone = TimeZone.currentSystemDefault()
        val nowMillis = Clock.System.now().toEpochMilliseconds()
        val soonDueShould =
            Task(
                id = "task-soon",
                title = "Soon due",
                status = TaskStatus.OPEN,
                priority = TaskPriority.SHOULD,
                dueAt = nowMillis + 2 * 60 * 60 * 1000L,
                scheduledStart = null,
                scheduledEnd = null,
            )
        val laterDueShould =
            Task(
                id = "task-later",
                title = "Later due",
                status = TaskStatus.OPEN,
                priority = TaskPriority.SHOULD,
                dueAt = nowMillis + 10 * 60 * 60 * 1000L,
                scheduledStart = null,
                scheduledEnd = null,
            )

        val result =
            ScheduleEngine.aggregate(
                events = emptyList(),
                tasks = listOf(laterDueShould, soonDueShould),
                filter = ScheduleFilter(),
                nowMillis = nowMillis,
                timeZone = timeZone,
            )

        val firstSuggestedTaskId = result.suggestions.firstOrNull()?.taskId
        assertEquals(soonDueShould.id, firstSuggestedTaskId)
    }
}
