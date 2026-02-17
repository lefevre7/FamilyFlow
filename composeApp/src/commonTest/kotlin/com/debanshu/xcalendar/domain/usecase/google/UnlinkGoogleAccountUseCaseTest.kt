package com.debanshu.xcalendar.domain.usecase.google

import com.debanshu.xcalendar.domain.auth.GoogleAuthTokens
import com.debanshu.xcalendar.domain.auth.GoogleTokenStore
import com.debanshu.xcalendar.domain.model.Calendar
import com.debanshu.xcalendar.domain.model.CalendarSource
import com.debanshu.xcalendar.domain.model.GoogleAccountLink
import com.debanshu.xcalendar.domain.repository.ICalendarSourceRepository
import com.debanshu.xcalendar.domain.repository.IGoogleAccountRepository
import com.debanshu.xcalendar.domain.usecase.calendar.GetUserCalendarsUseCase
import com.debanshu.xcalendar.domain.usecase.calendarSource.DeleteCalendarSourcesForAccountUseCase
import com.debanshu.xcalendar.domain.usecase.user.GetCurrentUserUseCase
import com.debanshu.xcalendar.test.FakeCalendarRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UnlinkGoogleAccountUseCaseTest {

    private class FakeTokenStore : GoogleTokenStore {
        private val tokens = mutableMapOf<String, GoogleAuthTokens>()
        val clearedAccounts = mutableListOf<String>()

        override fun saveTokens(accountId: String, tokens: GoogleAuthTokens) {
            this.tokens[accountId] = tokens
        }

        override fun getTokens(accountId: String): GoogleAuthTokens? = tokens[accountId]

        override fun clearTokens(accountId: String) {
            tokens.remove(accountId)
            clearedAccounts += accountId
        }
    }

    private class FakeGoogleAccountRepository : IGoogleAccountRepository {
        val deletedAccounts = mutableListOf<GoogleAccountLink>()

        override fun getAccountForPerson(personId: String): Flow<GoogleAccountLink?> = flowOf(null)

        override fun getAllAccounts(): Flow<List<GoogleAccountLink>> = flowOf(emptyList())

        override suspend fun getAccountById(accountId: String): GoogleAccountLink? = null

        override suspend fun upsertAccount(account: GoogleAccountLink) = Unit

        override suspend fun deleteAccount(account: GoogleAccountLink) {
            deletedAccounts += account
        }
    }

    private class FakeCalendarSourceRepository : ICalendarSourceRepository {
        val deletedSourceAccounts = mutableListOf<String>()

        override fun getSourceForCalendar(calendarId: String): Flow<CalendarSource?> = flowOf(null)

        override suspend fun getSourcesForAccount(accountId: String): List<CalendarSource> = emptyList()

        override suspend fun getAllSources(): List<CalendarSource> = emptyList()

        override suspend fun upsertSources(sources: List<CalendarSource>) = Unit

        override suspend fun deleteSourcesForAccount(accountId: String) {
            deletedSourceAccounts += accountId
        }

        override suspend fun deleteSourceForCalendar(calendarId: String) = Unit
    }

    @Test
    fun unlink_removesImportedCalendars_andClearsAccountAndTokens() = runTest {
        val currentUserId = GetCurrentUserUseCase().invoke()
        val accountId = "account-1"
        val calendarRepository =
            FakeCalendarRepository(
                initialCalendars =
                    listOf(
                        Calendar(
                            id = "google:$accountId:primary",
                            name = "Google Primary",
                            color = 0,
                            userId = currentUserId,
                        ),
                        Calendar(
                            id = "google:$accountId:family",
                            name = "Google Family",
                            color = 0,
                            userId = currentUserId,
                        ),
                        Calendar(
                            id = "local-calendar",
                            name = "Local",
                            color = 0,
                            userId = currentUserId,
                        ),
                    ),
            )
        val sourceRepository = FakeCalendarSourceRepository()
        val accountRepository = FakeGoogleAccountRepository()
        val tokenStore = FakeTokenStore().apply {
            saveTokens(accountId, GoogleAuthTokens(accessToken = "access-token"))
        }
        val account =
            GoogleAccountLink(
                id = accountId,
                email = "mom@example.com",
                personId = "person_mom",
            )
        val useCase =
            UnlinkGoogleAccountUseCase(
                tokenStore = tokenStore,
                deleteGoogleAccountUseCase = DeleteGoogleAccountUseCase(accountRepository),
                deleteCalendarSourcesForAccountUseCase = DeleteCalendarSourcesForAccountUseCase(sourceRepository),
                getCurrentUserUseCase = GetCurrentUserUseCase(),
                getUserCalendarsUseCase = GetUserCalendarsUseCase(calendarRepository),
                calendarRepository = calendarRepository,
            )

        useCase(account)

        assertEquals(
            listOf("google:$accountId:primary", "google:$accountId:family"),
            calendarRepository.deletedCalendars.map { it.id },
        )
        assertEquals(listOf(accountId), sourceRepository.deletedSourceAccounts)
        assertEquals(listOf(account), accountRepository.deletedAccounts)
        assertEquals(listOf(accountId), tokenStore.clearedAccounts)
        assertTrue(tokenStore.getTokens(accountId) == null)
    }
}
