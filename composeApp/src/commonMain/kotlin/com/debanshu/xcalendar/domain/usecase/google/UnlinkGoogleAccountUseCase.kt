package com.debanshu.xcalendar.domain.usecase.google

import com.debanshu.xcalendar.domain.auth.GoogleTokenStore
import com.debanshu.xcalendar.domain.model.GoogleAccountLink
import com.debanshu.xcalendar.domain.usecase.calendarSource.DeleteCalendarSourcesForAccountUseCase
import org.koin.core.annotation.Factory

@Factory
class UnlinkGoogleAccountUseCase(
    private val tokenStore: GoogleTokenStore,
    private val deleteGoogleAccountUseCase: DeleteGoogleAccountUseCase,
    private val deleteCalendarSourcesForAccountUseCase: DeleteCalendarSourcesForAccountUseCase,
) {
    suspend operator fun invoke(account: GoogleAccountLink) {
        tokenStore.clearTokens(account.id)
        deleteCalendarSourcesForAccountUseCase(account.id)
        deleteGoogleAccountUseCase(account)
    }
}
