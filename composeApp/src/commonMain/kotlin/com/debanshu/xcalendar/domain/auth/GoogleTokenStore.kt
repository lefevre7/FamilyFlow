package com.debanshu.xcalendar.domain.auth

data class GoogleAuthTokens(
    val accessToken: String,
    val refreshToken: String? = null,
    val idToken: String? = null,
    val expiresAt: Long? = null,
)

interface GoogleTokenStore {
    fun saveTokens(accountId: String, tokens: GoogleAuthTokens)
    fun getTokens(accountId: String): GoogleAuthTokens?
    fun clearTokens(accountId: String)
}

expect class PlatformGoogleTokenStore() : GoogleTokenStore
