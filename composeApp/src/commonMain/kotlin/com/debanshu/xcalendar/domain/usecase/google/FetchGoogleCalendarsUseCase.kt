package com.debanshu.xcalendar.domain.usecase.google

import com.debanshu.xcalendar.domain.model.ExternalCalendar
import com.debanshu.xcalendar.domain.sync.CalendarSyncManager
import org.koin.core.annotation.Factory

@Factory
class FetchGoogleCalendarsUseCase(
    private val syncManager: CalendarSyncManager,
) {
    suspend operator fun invoke(accountId: String): List<ExternalCalendar> =
        syncManager.listCalendars(accountId)
}
