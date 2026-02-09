package com.debanshu.xcalendar.domain.sync

import com.debanshu.xcalendar.domain.model.ExternalCalendar
import com.debanshu.xcalendar.domain.model.ExternalEvent

interface CalendarSyncManager {
    suspend fun listCalendars(accountId: String): List<ExternalCalendar>

    suspend fun listEvents(
        accountId: String,
        calendarId: String,
        timeMin: Long,
        timeMax: Long,
    ): List<ExternalEvent>

    suspend fun createEvent(
        accountId: String,
        calendarId: String,
        event: ExternalEvent,
    ): ExternalEvent?

    suspend fun updateEvent(
        accountId: String,
        calendarId: String,
        eventId: String,
        event: ExternalEvent,
    ): ExternalEvent?

    suspend fun deleteEvent(
        accountId: String,
        calendarId: String,
        eventId: String,
    ): Boolean
}
