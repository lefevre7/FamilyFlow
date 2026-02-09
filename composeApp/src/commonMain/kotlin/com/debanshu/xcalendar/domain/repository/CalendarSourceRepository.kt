package com.debanshu.xcalendar.domain.repository

import com.debanshu.xcalendar.common.model.asEntity
import com.debanshu.xcalendar.common.model.asSource
import com.debanshu.xcalendar.data.localDataSource.CalendarSourceDao
import com.debanshu.xcalendar.domain.model.CalendarSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

@Single(binds = [ICalendarSourceRepository::class])
class CalendarSourceRepository(
    private val calendarSourceDao: CalendarSourceDao,
) : BaseRepository(), ICalendarSourceRepository {
    override fun getSourceForCalendar(calendarId: String): Flow<CalendarSource?> =
        safeFlow(
            flowName = "getSourceForCalendar($calendarId)",
            defaultValue = null,
            flow = calendarSourceDao.getSourceForCalendar(calendarId).map { it?.asSource() },
        )

    override suspend fun getSourcesForAccount(accountId: String): List<CalendarSource> =
        safeCallOrThrow("getSourcesForAccount($accountId)") {
            calendarSourceDao.getSourcesForAccount(accountId).map { it.asSource() }
        }

    override suspend fun getAllSources(): List<CalendarSource> =
        safeCallOrThrow("getAllSources") {
            calendarSourceDao.getAllSources().map { it.asSource() }
        }

    override suspend fun upsertSources(sources: List<CalendarSource>) =
        safeCallOrThrow("upsertSources(${sources.size})") {
            calendarSourceDao.upsertSources(sources.map { it.asEntity() })
        }

    override suspend fun deleteSourcesForAccount(accountId: String) =
        safeCallOrThrow("deleteSourcesForAccount($accountId)") {
            calendarSourceDao.deleteSourcesForAccount(accountId)
        }

    override suspend fun deleteSourceForCalendar(calendarId: String) =
        safeCallOrThrow("deleteSourceForCalendar($calendarId)") {
            calendarSourceDao.deleteSourceForCalendar(calendarId)
        }
}
