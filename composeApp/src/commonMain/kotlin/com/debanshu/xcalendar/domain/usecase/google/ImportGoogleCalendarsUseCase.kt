package com.debanshu.xcalendar.domain.usecase.google

import com.debanshu.xcalendar.common.convertStringToColor
import com.debanshu.xcalendar.common.parseHexColor
import com.debanshu.xcalendar.domain.model.Calendar
import com.debanshu.xcalendar.domain.model.CalendarProvider
import com.debanshu.xcalendar.domain.model.CalendarSource
import com.debanshu.xcalendar.domain.model.ExternalCalendar
import com.debanshu.xcalendar.domain.repository.ICalendarRepository
import com.debanshu.xcalendar.domain.usecase.calendarSource.UpsertCalendarSourcesUseCase
import org.koin.core.annotation.Factory
import kotlin.time.Clock

@Factory
class ImportGoogleCalendarsUseCase(
    private val calendarRepository: ICalendarRepository,
    private val upsertCalendarSourcesUseCase: UpsertCalendarSourcesUseCase,
) {
    @OptIn(kotlin.time.ExperimentalTime::class)
    suspend operator fun invoke(
        userId: String,
        accountId: String,
        calendars: List<ExternalCalendar>,
    ): List<Calendar> {
        val now = Clock.System.now().toEpochMilliseconds()
        val localCalendars =
            calendars.map { calendar ->
                val localId = buildLocalCalendarId(accountId, calendar.id)
                val fallbackColor = convertStringToColor(localId + calendar.name)
                Calendar(
                    id = localId,
                    name = calendar.name,
                    color = parseHexColor(calendar.colorHex, fallbackColor),
                    userId = userId,
                    isVisible = true,
                    isPrimary = calendar.primary,
                )
            }
        if (localCalendars.isNotEmpty()) {
            calendarRepository.upsertCalendar(localCalendars)
            val sources =
                calendars.map { calendar ->
                    CalendarSource(
                        calendarId = buildLocalCalendarId(accountId, calendar.id),
                        provider = CalendarProvider.GOOGLE,
                        providerCalendarId = calendar.id,
                        providerAccountId = accountId,
                        syncEnabled = true,
                        lastSyncedAt = now,
                    )
                }
            upsertCalendarSourcesUseCase(sources)
        }
        return localCalendars
    }

    private fun buildLocalCalendarId(accountId: String, calendarId: String): String =
        "google:$accountId:$calendarId"
}
