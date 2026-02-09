package com.debanshu.xcalendar.domain.usecase.calendarSource

import com.debanshu.xcalendar.domain.repository.ICalendarSourceRepository
import org.koin.core.annotation.Factory

@Factory
class DeleteCalendarSourcesForAccountUseCase(
    private val repository: ICalendarSourceRepository,
) {
    suspend operator fun invoke(accountId: String) {
        repository.deleteSourcesForAccount(accountId)
    }
}
