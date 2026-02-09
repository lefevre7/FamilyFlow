package com.debanshu.xcalendar.domain.sync

import com.debanshu.xcalendar.data.google.GoogleCalendarApi
import com.debanshu.xcalendar.domain.model.ExternalCalendar
import com.debanshu.xcalendar.domain.model.ExternalEvent
import org.koin.core.annotation.Single

@Single(binds = [CalendarSyncManager::class])
class GoogleCalendarSyncManager(
    private val api: GoogleCalendarApi,
) : CalendarSyncManager {
    override suspend fun listCalendars(accountId: String): List<ExternalCalendar> =
        api.listCalendars(accountId)

    override suspend fun listEvents(
        accountId: String,
        calendarId: String,
        timeMin: Long,
        timeMax: Long,
    ): List<ExternalEvent> =
        api.listEvents(accountId, calendarId, timeMin, timeMax)

    override suspend fun createEvent(
        accountId: String,
        calendarId: String,
        event: ExternalEvent,
    ): ExternalEvent? =
        api.createEvent(accountId, calendarId, event)

    override suspend fun updateEvent(
        accountId: String,
        calendarId: String,
        eventId: String,
        event: ExternalEvent,
    ): ExternalEvent? =
        api.updateEvent(accountId, calendarId, eventId, event)

    override suspend fun deleteEvent(
        accountId: String,
        calendarId: String,
        eventId: String,
    ): Boolean =
        api.deleteEvent(accountId, calendarId, eventId)
}
