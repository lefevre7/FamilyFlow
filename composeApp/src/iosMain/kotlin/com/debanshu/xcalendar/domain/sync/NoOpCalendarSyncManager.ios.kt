package com.debanshu.xcalendar.domain.sync

import com.debanshu.xcalendar.domain.model.ExternalCalendar
import com.debanshu.xcalendar.domain.model.ExternalEvent
import org.koin.core.annotation.Single

@Single(binds = [CalendarSyncManager::class])
class NoOpCalendarSyncManager : CalendarSyncManager {
    override suspend fun listCalendars(accountId: String): List<ExternalCalendar> = emptyList()

    override suspend fun listEvents(
        accountId: String,
        calendarId: String,
        timeMin: Long,
        timeMax: Long,
    ): List<ExternalEvent> = emptyList()

    override suspend fun createEvent(
        accountId: String,
        calendarId: String,
        event: ExternalEvent,
    ): ExternalEvent? = null

    override suspend fun updateEvent(
        accountId: String,
        calendarId: String,
        eventId: String,
        event: ExternalEvent,
    ): ExternalEvent? = null

    override suspend fun deleteEvent(
        accountId: String,
        calendarId: String,
        eventId: String,
    ): Boolean = false
}
