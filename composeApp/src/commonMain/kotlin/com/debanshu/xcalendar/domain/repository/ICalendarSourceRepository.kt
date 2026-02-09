package com.debanshu.xcalendar.domain.repository

import com.debanshu.xcalendar.domain.model.CalendarSource
import kotlinx.coroutines.flow.Flow

interface ICalendarSourceRepository {
    fun getSourceForCalendar(calendarId: String): Flow<CalendarSource?>

    suspend fun getSourcesForAccount(accountId: String): List<CalendarSource>

    suspend fun getAllSources(): List<CalendarSource>

    suspend fun upsertSources(sources: List<CalendarSource>)

    suspend fun deleteSourcesForAccount(accountId: String)

    suspend fun deleteSourceForCalendar(calendarId: String)
}
