package com.debanshu.xcalendar.domain.usecase.calendarSource

import com.debanshu.xcalendar.domain.model.CalendarSource
import com.debanshu.xcalendar.domain.repository.ICalendarSourceRepository
import org.koin.core.annotation.Factory

@Factory
class GetCalendarSourcesForAccountUseCase(
    private val repository: ICalendarSourceRepository,
) {
    suspend operator fun invoke(accountId: String): List<CalendarSource> =
        repository.getSourcesForAccount(accountId)
}
