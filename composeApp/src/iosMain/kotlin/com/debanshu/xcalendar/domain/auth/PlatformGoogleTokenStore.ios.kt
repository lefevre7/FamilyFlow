package com.debanshu.xcalendar.domain.auth

import org.koin.core.annotation.Single

@Single(binds = [GoogleTokenStore::class])
actual class PlatformGoogleTokenStore : GoogleTokenStore {
    override fun saveTokens(accountId: String, tokens: GoogleAuthTokens) = Unit

    override fun getTokens(accountId: String): GoogleAuthTokens? = null

    override fun clearTokens(accountId: String) = Unit
}
