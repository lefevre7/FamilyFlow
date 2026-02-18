package com.debanshu.xcalendar.domain.util

import com.debanshu.xcalendar.domain.model.Event
import com.debanshu.xcalendar.domain.model.EventSource
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RecurringEventEditResolverTest {
    private val timeZone = TimeZone.currentSystemDefault()

    @Test
    fun requiresScopePrompt_falseForSingleEvent() {
        val event = testEvent(externalId = null, recurringRule = null, isRecurring = false)
        assertFalse(RecurringEventEditResolver.requiresScopePrompt(event))
    }

    @Test
    fun requiresScopePrompt_trueForRecurringSignals() {
        val byRule = testEvent(recurringRule = "RRULE:FREQ=WEEKLY")
        val byExternalInstance = testEvent(externalId = "series-123_20260219T170000Z")

        assertTrue(RecurringEventEditResolver.requiresScopePrompt(byRule))
        assertTrue(RecurringEventEditResolver.requiresScopePrompt(byExternalInstance))
    }

    @Test
    fun resolveSeriesEvents_returnsMatchingSeriesOnly() {
        val seed = testEvent(id = "seed", calendarId = "google-family", externalId = "abc_20260218T090000Z")
        val sameSeries1 = testEvent(id = "s1", calendarId = "google-family", externalId = "abc_20260219T090000Z")
        val sameSeries2 = testEvent(id = "s2", calendarId = "google-family", externalId = "abc_20260220T090000Z")
        val differentSeries = testEvent(id = "d1", calendarId = "google-family", externalId = "xyz_20260221T090000Z")
        val samePrefixOtherCalendar = testEvent(id = "d2", calendarId = "other-cal", externalId = "abc_20260222T090000Z")

        val result =
            RecurringEventEditResolver.resolveSeriesEvents(
                allEvents = listOf(seed, sameSeries1, sameSeries2, differentSeries, samePrefixOtherCalendar),
                seed = seed,
            )

        assertEquals(listOf("seed", "s1", "s2"), result.map { it.id })
    }

    @Test
    fun resolveSeriesEvents_returnsSeedWhenNoSeriesIdentity() {
        val seed = testEvent(id = "single", externalId = null, recurringRule = null, isRecurring = false)
        val other = testEvent(id = "other", externalId = "abc_20260222T090000Z")

        val result = RecurringEventEditResolver.resolveSeriesEvents(allEvents = listOf(other), seed = seed)

        assertEquals(listOf("single"), result.map { it.id })
    }

    @Test
    fun applySeriesUpdate_appliesSharedFieldsButKeepsTargetDate() {
        val target =
            testEvent(
                id = "target",
                startTime = millis(2026, Month.FEBRUARY, 20, 8, 0),
                endTime = millis(2026, Month.FEBRUARY, 20, 9, 0),
                source = EventSource.GOOGLE,
                externalId = "series-a_20260220T080000Z",
                externalUpdatedAt = 100L,
                lastSyncedAt = 200L,
            )
        val originalEditTarget =
            testEvent(
                id = "seed",
                startTime = millis(2026, Month.FEBRUARY, 18, 8, 0),
                endTime = millis(2026, Month.FEBRUARY, 18, 9, 0),
                source = EventSource.GOOGLE,
                externalId = "series-a_20260218T080000Z",
            )
        val updatedEditTarget =
            originalEditTarget.copy(
                title = "Updated title",
                description = "Updated description",
                location = "New location",
                startTime = millis(2026, Month.FEBRUARY, 18, 10, 30),
                endTime = millis(2026, Month.FEBRUARY, 18, 11, 45),
                reminderMinutes = listOf(30),
                affectedPersonIds = listOf("mom", "kid-1"),
                color = 0xFF009688.toInt(),
            )

        val result =
            RecurringEventEditResolver.applySeriesUpdate(
                target = target,
                originalEditTarget = originalEditTarget,
                updatedEditTarget = updatedEditTarget,
            )

        val resultStart = Instant.fromEpochMilliseconds(result.startTime).toLocalDateTime(timeZone)
        val resultEnd = Instant.fromEpochMilliseconds(result.endTime).toLocalDateTime(timeZone)

        assertEquals("Updated title", result.title)
        assertEquals("Updated description", result.description)
        assertEquals("New location", result.location)
        assertEquals(20, resultStart.date.day)
        assertEquals(10, resultStart.hour)
        assertEquals(30, resultStart.minute)
        assertEquals(11, resultEnd.hour)
        assertEquals(45, resultEnd.minute)
        assertEquals(EventSource.GOOGLE, result.source)
        assertEquals("series-a_20260220T080000Z", result.externalId)
        assertEquals(100L, result.externalUpdatedAt)
        assertEquals(200L, result.lastSyncedAt)
        assertEquals(listOf("mom", "kid-1"), result.affectedPersonIds)
    }

    @Test
    fun applySeriesUpdate_whenCalendarMoved_clearsSyncFieldsAndSetsLocalSource() {
        val target =
            testEvent(
                id = "target",
                calendarId = "google-family",
                source = EventSource.GOOGLE,
                externalId = "series-b_20260220T120000Z",
                externalUpdatedAt = 111L,
                lastSyncedAt = 222L,
            )
        val originalEditTarget = testEvent(id = "seed", calendarId = "google-family")
        val updatedEditTarget = originalEditTarget.copy(calendarId = "local-family", calendarName = "Family")

        val result =
            RecurringEventEditResolver.applySeriesUpdate(
                target = target,
                originalEditTarget = originalEditTarget,
                updatedEditTarget = updatedEditTarget,
            )

        assertEquals(EventSource.LOCAL, result.source)
        assertNull(result.externalId)
        assertNull(result.externalUpdatedAt)
        assertNull(result.lastSyncedAt)
    }

    @Test
    fun applySeriesUpdate_allDayTargetsSpanWholeTargetDay() {
        val target =
            testEvent(
                id = "target",
                startTime = millis(2026, Month.MARCH, 1, 8, 0),
                endTime = millis(2026, Month.MARCH, 1, 9, 0),
            )
        val originalEditTarget =
            testEvent(
                id = "seed",
                startTime = millis(2026, Month.FEBRUARY, 25, 8, 0),
                endTime = millis(2026, Month.FEBRUARY, 25, 9, 0),
            )
        val updatedEditTarget =
            originalEditTarget.copy(
                isAllDay = true,
                startTime = millis(2026, Month.FEBRUARY, 25, 0, 0),
                endTime = millis(2026, Month.FEBRUARY, 25, 23, 59),
            )

        val result =
            RecurringEventEditResolver.applySeriesUpdate(
                target = target,
                originalEditTarget = originalEditTarget,
                updatedEditTarget = updatedEditTarget,
            )

        val start = Instant.fromEpochMilliseconds(result.startTime).toLocalDateTime(timeZone)
        val end = Instant.fromEpochMilliseconds(result.endTime).toLocalDateTime(timeZone)

        assertEquals(1, start.date.day)
        assertEquals(0, start.hour)
        assertEquals(0, start.minute)
        assertEquals(1, end.date.day)
        assertEquals(23, end.hour)
        assertEquals(59, end.minute)
    }

    private fun millis(
        year: Int,
        month: Month,
        day: Int,
        hour: Int,
        minute: Int,
    ): Long = LocalDateTime(year, month, day, hour, minute).toInstant(timeZone).toEpochMilliseconds()

    private fun testEvent(
        id: String = "event-id",
        calendarId: String = "calendar-id",
        calendarName: String = "Calendar",
        title: String = "Title",
        startTime: Long = millis(2026, Month.FEBRUARY, 18, 8, 0),
        endTime: Long = millis(2026, Month.FEBRUARY, 18, 9, 0),
        isAllDay: Boolean = false,
        isRecurring: Boolean = true,
        recurringRule: String? = "RRULE:FREQ=DAILY",
        color: Int = 0xFF4285F4.toInt(),
        source: EventSource = EventSource.GOOGLE,
        externalId: String? = "series-a_20260218T080000Z",
        externalUpdatedAt: Long? = null,
        lastSyncedAt: Long? = null,
    ): Event =
        Event(
            id = id,
            calendarId = calendarId,
            calendarName = calendarName,
            title = title,
            startTime = startTime,
            endTime = endTime,
            isAllDay = isAllDay,
            isRecurring = isRecurring,
            recurringRule = recurringRule,
            color = color,
            source = source,
            externalId = externalId,
            externalUpdatedAt = externalUpdatedAt,
            lastSyncedAt = lastSyncedAt,
        )
}
