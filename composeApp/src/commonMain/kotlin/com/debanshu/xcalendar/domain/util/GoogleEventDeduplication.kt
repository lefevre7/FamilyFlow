package com.debanshu.xcalendar.domain.util

import com.debanshu.xcalendar.domain.model.Event
import com.debanshu.xcalendar.domain.model.EventSource

/**
 * Deduplication helpers for Google-sourced events keyed by (calendarId, externalId).
 */
object GoogleEventDeduplication {
    data class DuplicateResolution(
        val winner: Event,
        val losers: List<Event>,
        val mergedWinner: Event,
    )

    fun resolveDuplicates(events: List<Event>): List<DuplicateResolution> =
        events
            .asSequence()
            .filter { event -> event.source == EventSource.GOOGLE && !event.externalId.isNullOrBlank() }
            .groupBy { event -> event.calendarId to event.externalId.orEmpty() }
            .values
            .mapNotNull { group ->
                if (group.size < 2) {
                    null
                } else {
                    val winner = pickWinner(group)
                    val loserIds = group.asSequence().map { it.id }.filter { it != winner.id }.toSet()
                    val losers = group.filter { it.id in loserIds }
                    DuplicateResolution(
                        winner = winner,
                        losers = losers,
                        mergedWinner = winner.mergeFrom(group),
                    )
                }
            }
            .toList()

    /**
     * Defensive UI-level dedupe. Keeps item ordering stable while removing duplicate rows.
     */
    fun dedupeForDisplay(events: List<Event>): List<Event> {
        val resolutions = resolveDuplicates(events)
        if (resolutions.isEmpty()) return events

        val loserIds = resolutions.flatMap { it.losers }.map { it.id }.toSet()
        val mergedWinnerById = resolutions.associate { it.winner.id to it.mergedWinner }
        return events.mapNotNull { event ->
            when {
                event.id in loserIds -> null
                event.id in mergedWinnerById -> mergedWinnerById[event.id]
                else -> event
            }
        }
    }

    private fun pickWinner(group: List<Event>): Event =
        group.maxWithOrNull(
            compareBy<Event>(
                { event -> event.lastSyncedAt ?: Long.MIN_VALUE },
                { event -> event.externalUpdatedAt ?: Long.MIN_VALUE },
                { event -> event.id },
            ),
        ) ?: group.first()

    private fun Event.mergeFrom(group: List<Event>): Event {
        val mergedReminderMinutes = linkedSetOf<Int>()
        val mergedPersonIds = linkedSetOf<String>()

        // Preserve winner ordering first, then fold additional rows.
        reminderMinutes.forEach { mergedReminderMinutes.add(it) }
        affectedPersonIds.forEach { mergedPersonIds.add(it) }
        group.forEach { duplicate ->
            duplicate.reminderMinutes.forEach { mergedReminderMinutes.add(it) }
            duplicate.affectedPersonIds.forEach { mergedPersonIds.add(it) }
        }

        return copy(
            reminderMinutes = mergedReminderMinutes.toList(),
            affectedPersonIds = mergedPersonIds.toList(),
        )
    }
}
