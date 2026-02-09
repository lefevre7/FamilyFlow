package com.debanshu.xcalendar.domain.auth

import android.content.Context
import android.net.Uri
import com.debanshu.xcalendar.BuildKonfig
import kotlinx.coroutines.suspendCancellableCoroutine
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.TokenRequest
import net.openid.appauth.TokenResponse
import org.koin.core.annotation.Single
import org.koin.mp.KoinPlatform
import kotlin.coroutines.resume

@Single
class GoogleTokenManager(
    private val tokenStore: GoogleTokenStore,
) {
    private val context: Context by lazy { KoinPlatform.getKoin().get() }
    private val serviceConfig = AuthorizationServiceConfiguration(
        Uri.parse(AUTH_ENDPOINT),
        Uri.parse(TOKEN_ENDPOINT),
    )

    suspend fun getValidAccessToken(accountId: String): String? {
        val tokens = tokenStore.getTokens(accountId) ?: return null
        val expiresAt = tokens.expiresAt
        val now = System.currentTimeMillis()
        if (expiresAt == null || expiresAt - now > EXPIRY_SAFETY_MS) {
            return tokens.accessToken
        }
        val refreshToken = tokens.refreshToken ?: return tokens.accessToken
        val response = refreshToken(refreshToken)
        if (response?.accessToken == null) {
            return tokens.accessToken
        }
        tokenStore.saveTokens(
            accountId,
            GoogleAuthTokens(
                accessToken = response.accessToken!!,
                refreshToken = response.refreshToken ?: refreshToken,
                idToken = response.idToken ?: tokens.idToken,
                expiresAt = response.accessTokenExpirationTime ?: expiresAt,
            )
        )
        return response.accessToken
    }

    private suspend fun refreshToken(refreshToken: String): TokenResponse? {
        val clientId = BuildKonfig.CLIENT_ID
        if (clientId.isBlank()) return null
        val authService = AuthorizationService(context)
        val tokenRequest = TokenRequest.Builder(serviceConfig, clientId)
            .setGrantType("refresh_token")
            .setRefreshToken(refreshToken)
            .build()
        return suspendCancellableCoroutine { cont ->
            authService.performTokenRequest(tokenRequest) { response, _ ->
                authService.dispose()
                cont.resume(response)
            }
        }
    }

    companion object {
        private const val AUTH_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth"
        private const val TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token"
        private const val EXPIRY_SAFETY_MS = 60_000L
    }
}
