package com.debanshu.xcalendar.domain.util

import com.debanshu.xcalendar.domain.model.Event
import com.debanshu.xcalendar.domain.model.EventSource
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

object RecurringEventEditResolver {
    fun requiresScopePrompt(event: Event): Boolean =
        event.isRecurring || !event.recurringRule.isNullOrBlank() || (event.externalId?.contains("_") == true)

    fun resolveSeriesEvents(
        allEvents: List<Event>,
        seed: Event,
    ): List<Event> {
        val identity = seriesIdentity(seed) ?: return listOf(seed)
        val matches =
            allEvents.filter { event ->
                event.calendarId == seed.calendarId && seriesIdentity(event) == identity
            }
        return if (matches.isEmpty()) listOf(seed) else matches
    }

    fun applySeriesUpdate(
        target: Event,
        originalEditTarget: Event,
        updatedEditTarget: Event,
    ): Event {
        val timeZone = TimeZone.currentSystemDefault()
        val movedCalendar = originalEditTarget.calendarId != updatedEditTarget.calendarId
        val updatedStartLocal = Instant.fromEpochMilliseconds(updatedEditTarget.startTime).toLocalDateTime(timeZone)
        val targetDate = Instant.fromEpochMilliseconds(target.startTime).toLocalDateTime(timeZone).date
        val durationMillis = (updatedEditTarget.endTime - updatedEditTarget.startTime).coerceAtLeast(60_000L)

        val newStartLocal =
            if (updatedEditTarget.isAllDay) {
                LocalDateTime(targetDate.year, targetDate.month, targetDate.day, 0, 0)
            } else {
                LocalDateTime(
                    targetDate.year,
                    targetDate.month,
                    targetDate.day,
                    updatedStartLocal.hour,
                    updatedStartLocal.minute,
                )
            }
        val newStartMillis = newStartLocal.toInstant(timeZone).toEpochMilliseconds()
        val newEndMillis =
            if (updatedEditTarget.isAllDay) {
                LocalDateTime(targetDate.year, targetDate.month, targetDate.day, 23, 59)
                    .toInstant(timeZone)
                    .toEpochMilliseconds()
            } else {
                newStartMillis + durationMillis
            }

        return target.copy(
            calendarId = updatedEditTarget.calendarId,
            calendarName = updatedEditTarget.calendarName,
            title = updatedEditTarget.title,
            description = updatedEditTarget.description,
            location = updatedEditTarget.location,
            startTime = newStartMillis,
            endTime = newEndMillis,
            isAllDay = updatedEditTarget.isAllDay,
            reminderMinutes = updatedEditTarget.reminderMinutes,
            color = updatedEditTarget.color,
            source = if (movedCalendar) EventSource.LOCAL else target.source,
            externalId = if (movedCalendar) null else target.externalId,
            externalUpdatedAt = if (movedCalendar) null else target.externalUpdatedAt,
            lastSyncedAt = if (movedCalendar) null else target.lastSyncedAt,
            affectedPersonIds = updatedEditTarget.affectedPersonIds,
        )
    }

    private fun seriesIdentity(event: Event): String? =
        when {
            !event.externalId.isNullOrBlank() && event.externalId.contains("_") -> event.externalId.substringBefore("_")
            !event.externalId.isNullOrBlank() -> event.externalId
            !event.recurringRule.isNullOrBlank() -> "${event.calendarId}|${event.recurringRule}"
            event.isRecurring -> event.id
            else -> null
        }
}
