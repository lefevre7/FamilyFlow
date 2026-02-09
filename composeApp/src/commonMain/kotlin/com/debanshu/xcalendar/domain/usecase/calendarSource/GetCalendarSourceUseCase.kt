package com.debanshu.xcalendar.domain.usecase.calendarSource

import com.debanshu.xcalendar.domain.model.CalendarSource
import com.debanshu.xcalendar.domain.repository.ICalendarSourceRepository
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Factory

@Factory
class GetCalendarSourceUseCase(
    private val repository: ICalendarSourceRepository,
) {
    operator fun invoke(calendarId: String): Flow<CalendarSource?> =
        repository.getSourceForCalendar(calendarId)
}
