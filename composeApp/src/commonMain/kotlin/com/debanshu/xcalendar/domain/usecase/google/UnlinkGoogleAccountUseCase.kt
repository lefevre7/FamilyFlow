package com.debanshu.xcalendar.domain.usecase.google

import com.debanshu.xcalendar.domain.auth.GoogleTokenStore
import com.debanshu.xcalendar.domain.model.GoogleAccountLink
import com.debanshu.xcalendar.domain.repository.ICalendarRepository
import com.debanshu.xcalendar.domain.usecase.calendar.GetUserCalendarsUseCase
import com.debanshu.xcalendar.domain.usecase.calendarSource.DeleteCalendarSourcesForAccountUseCase
import com.debanshu.xcalendar.domain.usecase.user.GetCurrentUserUseCase
import kotlinx.coroutines.flow.first
import org.koin.core.annotation.Factory

@Factory
class UnlinkGoogleAccountUseCase(
    private val tokenStore: GoogleTokenStore,
    private val deleteGoogleAccountUseCase: DeleteGoogleAccountUseCase,
    private val deleteCalendarSourcesForAccountUseCase: DeleteCalendarSourcesForAccountUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getUserCalendarsUseCase: GetUserCalendarsUseCase,
    private val calendarRepository: ICalendarRepository,
) {
    suspend operator fun invoke(account: GoogleAccountLink) {
        val importedCalendarPrefix = "google:${account.id}:"
        val userId = getCurrentUserUseCase()
        getUserCalendarsUseCase(userId)
            .first()
            .filter { calendar -> calendar.id.startsWith(importedCalendarPrefix) }
            .forEach { calendar -> calendarRepository.deleteCalendar(calendar) }

        tokenStore.clearTokens(account.id)
        deleteCalendarSourcesForAccountUseCase(account.id)
        deleteGoogleAccountUseCase(account)
    }
}
