package com.debanshu.xcalendar.domain.util

import com.debanshu.xcalendar.domain.model.Event
import com.debanshu.xcalendar.domain.model.ScheduleAggregationResult
import com.debanshu.xcalendar.domain.model.ScheduleConflict
import com.debanshu.xcalendar.domain.model.ScheduleFilter
import com.debanshu.xcalendar.domain.model.ScheduleItem
import com.debanshu.xcalendar.domain.model.ScheduleSource
import com.debanshu.xcalendar.domain.model.ScheduleSuggestion
import com.debanshu.xcalendar.domain.model.Task
import com.debanshu.xcalendar.domain.model.TaskEnergy
import com.debanshu.xcalendar.domain.model.TaskPriority
import com.debanshu.xcalendar.domain.model.TaskStatus
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.math.abs

object ScheduleEngine {
    private const val MILLIS_PER_MINUTE = 60_000L
    private const val SLOT_STEP_MINUTES = 30
    private const val DEFAULT_DAY_START_HOUR = 7
    private const val DEFAULT_DAY_END_HOUR = 20

    fun aggregate(
        events: List<Event>,
        tasks: List<Task>,
        filter: ScheduleFilter,
        nowMillis: Long,
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
    ): ScheduleAggregationResult {
        val eventItems = events.map { event ->
            ScheduleItem(
                id = event.id,
                title = event.title,
                startTime = event.startTime,
                endTime = event.endTime,
                isAllDay = event.isAllDay,
                priority = null,
                energy = null,
                personIds = emptyList(),
                source = ScheduleSource.EVENT,
                isFlexible = false,
                originalEvent = event,
            )
        }
        val taskItems = tasks.map { task ->
            ScheduleItem(
                id = task.id,
                title = task.title,
                startTime = task.scheduledStart,
                endTime = task.scheduledEnd,
                isAllDay = false,
                priority = task.priority,
                energy = task.energy,
                personIds = buildTaskPersonIds(task),
                source = ScheduleSource.TASK,
                isFlexible = task.scheduledStart == null,
                originalTask = task,
            )
        }

        val filteredItems =
            (eventItems + taskItems)
                .filter { item -> passesPersonFilter(item, filter) }
                .filter { item -> passesMustFilter(item, filter) }
                .filter { item -> passesNowWindow(item, filter, nowMillis) }

        val sortedItems = sortItems(filteredItems)
        val conflicts = detectConflicts(sortedItems)
        val suggestions = buildSuggestions(tasks, events, nowMillis, timeZone)

        return ScheduleAggregationResult(
            items = sortedItems,
            conflicts = conflicts,
            suggestions = suggestions,
        )
    }

    private fun buildTaskPersonIds(task: Task): List<String> {
        val ids = mutableSetOf<String>()
        task.assignedToPersonId?.let { ids.add(it) }
        ids.addAll(task.affectedPersonIds)
        return ids.toList()
    }

    private fun passesPersonFilter(item: ScheduleItem, filter: ScheduleFilter): Boolean {
        val personId = filter.personId ?: return true
        if (item.source == ScheduleSource.EVENT) {
            return filter.includeUnassignedEvents || item.personIds.contains(personId)
        }
        return item.personIds.contains(personId)
    }

    private fun passesMustFilter(item: ScheduleItem, filter: ScheduleFilter): Boolean {
        if (!filter.onlyMust) return true
        return item.source == ScheduleSource.EVENT || item.priority == TaskPriority.MUST
    }

    private fun passesNowWindow(item: ScheduleItem, filter: ScheduleFilter, nowMillis: Long): Boolean {
        val windowMinutes = filter.nowWindowMinutes ?: return true
        val windowMillis = windowMinutes * MILLIS_PER_MINUTE
        val start = item.startTime ?: return true
        return start in (nowMillis - windowMillis)..(nowMillis + windowMillis)
    }

    private fun sortItems(items: List<ScheduleItem>): List<ScheduleItem> {
        val timed = items.filter { it.startTime != null }.sortedBy { it.startTime }
        val floating =
            items.filter { it.startTime == null }
                .sortedWith(
                    compareBy<ScheduleItem> { priorityRank(it.priority) }
                        .thenBy { it.title.lowercase() }
                )
        return timed + floating
    }

    private fun priorityRank(priority: TaskPriority?): Int = when (priority) {
        TaskPriority.MUST -> 0
        TaskPriority.SHOULD -> 1
        TaskPriority.NICE -> 2
        null -> 3
    }

    private fun detectConflicts(items: List<ScheduleItem>): List<ScheduleConflict> {
        val conflicts = mutableListOf<ScheduleConflict>()
        val timedItems = items.filter { it.startTime != null && it.endTime != null }
        for (i in timedItems.indices) {
            val a = timedItems[i]
            for (j in i + 1 until timedItems.size) {
                val b = timedItems[j]
                if (overlaps(a.startTime!!, a.endTime!!, b.startTime!!, b.endTime!!)) {
                    conflicts.add(ScheduleConflict(a.id, b.id))
                }
            }
        }
        return conflicts
    }

    private fun overlaps(startA: Long, endA: Long, startB: Long, endB: Long): Boolean {
        return startA < endB && startB < endA
    }

    private fun buildSuggestions(
        tasks: List<Task>,
        events: List<Event>,
        nowMillis: Long,
        timeZone: TimeZone,
    ): List<ScheduleSuggestion> {
        val flexibleTasks = tasks.filter { it.status == TaskStatus.OPEN && it.scheduledStart == null }
        if (flexibleTasks.isEmpty()) return emptyList()

        val today = Instant.fromEpochMilliseconds(nowMillis).toLocalDateTime(timeZone).date
        val busy = buildBusyIntervals(events, tasks, today, timeZone)
        val suggestions = mutableListOf<ScheduleSuggestion>()

        flexibleTasks.forEach { task ->
            val candidates = findCandidateSlots(task, today, timeZone, busy)
            candidates.take(2).forEach { slot ->
                suggestions.add(
                    ScheduleSuggestion(
                        taskId = task.id,
                        startTime = slot.first,
                        endTime = slot.second,
                    )
                )
            }
        }
        return suggestions
    }

    private fun buildBusyIntervals(
        events: List<Event>,
        tasks: List<Task>,
        date: LocalDate,
        timeZone: TimeZone,
    ): List<Pair<Long, Long>> {
        val dayStart = date.atStartOfDayIn(timeZone).toEpochMilliseconds()
        val dayEnd = dayStart + 24 * 60 * 60 * 1000L
        val eventIntervals =
            events
                .filter { it.startTime in dayStart until dayEnd }
                .map { it.startTime to it.endTime }
        val taskIntervals =
            tasks
                .filter { it.scheduledStart != null && it.scheduledEnd != null }
                .filter { it.scheduledStart!! in dayStart until dayEnd }
                .map { it.scheduledStart!! to it.scheduledEnd!! }
        return (eventIntervals + taskIntervals).sortedBy { it.first }
    }

    private fun findCandidateSlots(
        task: Task,
        date: LocalDate,
        timeZone: TimeZone,
        busyIntervals: List<Pair<Long, Long>>,
    ): List<Pair<Long, Long>> {
        val durationMillis = task.durationMinutes * MILLIS_PER_MINUTE
        val dayStart =
            date
                .atStartOfDayIn(timeZone)
                .toEpochMilliseconds() + DEFAULT_DAY_START_HOUR * 60 * 60 * 1000L
        val dayEnd =
            date
                .atStartOfDayIn(timeZone)
                .toEpochMilliseconds() + DEFAULT_DAY_END_HOUR * 60 * 60 * 1000L
        if (dayEnd - dayStart < durationMillis) return emptyList()

        val stepMillis = SLOT_STEP_MINUTES * MILLIS_PER_MINUTE
        val candidates = mutableListOf<Pair<Long, Long>>()
        var cursor = dayStart
        while (cursor + durationMillis <= dayEnd) {
            val candidate = cursor to (cursor + durationMillis)
            if (busyIntervals.none { overlaps(candidate.first, candidate.second, it.first, it.second) }) {
                candidates.add(candidate)
            }
            cursor += stepMillis
        }

        return candidates.sortedBy { scoreSlot(it.first, task.energy, dayStart) }
    }

    private fun scoreSlot(start: Long, energy: TaskEnergy, dayStart: Long): Int {
        val minutesFromStart = ((start - dayStart) / MILLIS_PER_MINUTE).toInt()
        val preferredMinutes =
            when (energy) {
                TaskEnergy.HIGH -> 120
                TaskEnergy.MEDIUM -> 360
                TaskEnergy.LOW -> 540
            }
        return abs(minutesFromStart - preferredMinutes)
    }
}
