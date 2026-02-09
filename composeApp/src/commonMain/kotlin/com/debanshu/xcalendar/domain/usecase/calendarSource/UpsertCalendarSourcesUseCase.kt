package com.debanshu.xcalendar.domain.usecase.calendarSource

import com.debanshu.xcalendar.domain.model.CalendarSource
import com.debanshu.xcalendar.domain.repository.ICalendarSourceRepository
import org.koin.core.annotation.Factory

@Factory
class UpsertCalendarSourcesUseCase(
    private val repository: ICalendarSourceRepository,
) {
    suspend operator fun invoke(sources: List<CalendarSource>) {
        repository.upsertSources(sources)
    }
}
