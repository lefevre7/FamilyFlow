package com.debanshu.xcalendar.domain.usecase.google

import com.debanshu.xcalendar.domain.auth.GoogleAuthTokens
import com.debanshu.xcalendar.domain.auth.GoogleTokenStore
import com.debanshu.xcalendar.domain.model.GoogleAccountLink
import com.debanshu.xcalendar.platform.GoogleAuthResult
import org.koin.core.annotation.Factory
import kotlin.time.Clock

@Factory
class LinkGoogleAccountUseCase(
    private val tokenStore: GoogleTokenStore,
    private val upsertGoogleAccountUseCase: UpsertGoogleAccountUseCase,
) {
    @OptIn(kotlin.time.ExperimentalTime::class)
    suspend operator fun invoke(
        personId: String,
        authResult: GoogleAuthResult,
    ) {
        val now = Clock.System.now().toEpochMilliseconds()
        tokenStore.saveTokens(
            authResult.accountId,
            GoogleAuthTokens(
                accessToken = authResult.accessToken,
                refreshToken = authResult.refreshToken,
                idToken = authResult.idToken,
                expiresAt = authResult.expiresAt,
            ),
        )
        val account = GoogleAccountLink(
            id = authResult.accountId,
            email = authResult.email,
            displayName = authResult.displayName,
            personId = personId,
            createdAt = now,
            updatedAt = now,
        )
        upsertGoogleAccountUseCase(account)
    }
}
