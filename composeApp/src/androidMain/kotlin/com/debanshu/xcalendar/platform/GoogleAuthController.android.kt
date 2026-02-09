package com.debanshu.xcalendar.platform

import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.debanshu.xcalendar.BuildKonfig
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues
import net.openid.appauth.TokenRequest
import net.openid.appauth.TokenResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Composable
actual fun rememberGoogleAuthController(
    onSuccess: (GoogleAuthResult) -> Unit,
    onError: (String) -> Unit,
): GoogleAuthController {
    val context = LocalContext.current
    val authService = remember { AuthorizationService(context) }
    DisposableEffect(Unit) {
        onDispose { authService.dispose() }
    }
    val json = remember {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val data = result.data
        if (data == null) {
            onError("Authorization canceled.")
            return@rememberLauncherForActivityResult
        }
        val response = AuthorizationResponse.fromIntent(data)
        val exception = AuthorizationException.fromIntent(data)
        if (exception != null) {
            onError(exception.errorDescription ?: "Authorization failed.")
            return@rememberLauncherForActivityResult
        }
        if (response == null) {
            onError("Authorization canceled.")
            return@rememberLauncherForActivityResult
        }
        val tokenRequest = response.createTokenExchangeRequest()
        authService.performTokenRequest(tokenRequest) { tokenResponse, tokenException ->
            if (tokenException != null) {
                onError(tokenException.errorDescription ?: "Token exchange failed.")
                return@performTokenRequest
            }
            if (tokenResponse == null) {
                onError("Token exchange failed.")
                return@performTokenRequest
            }
            val idToken = tokenResponse.idToken
            val payload = idToken?.let { parseIdToken(it, json) }
            val accountId = payload?.subject ?: tokenResponse.accessToken?.hashCode()?.toString()
            if (accountId.isNullOrBlank()) {
                onError("Unable to identify Google account.")
                return@performTokenRequest
            }
            val email = payload?.email ?: "unknown"
            val displayName = payload?.name
            val accessToken = tokenResponse.accessToken ?: run {
                onError("Access token missing.")
                return@performTokenRequest
            }
            onSuccess(
                GoogleAuthResult(
                    accountId = accountId,
                    email = email,
                    displayName = displayName,
                    accessToken = accessToken,
                    refreshToken = tokenResponse.refreshToken,
                    idToken = idToken,
                    expiresAt = tokenResponse.accessTokenExpirationTime,
                )
            )
        }
    }

    val config = remember {
        AuthorizationServiceConfiguration(
            Uri.parse(AUTH_ENDPOINT),
            Uri.parse(TOKEN_ENDPOINT),
        )
    }
    val clientId = remember { BuildKonfig.CLIENT_ID }
    val redirectUri = remember { Uri.parse("${context.packageName}:/oauth2redirect") }

    val authRequest = remember {
        AuthorizationRequest.Builder(
            config,
            clientId,
            ResponseTypeValues.CODE,
            redirectUri,
        )
            .setScope(SCOPES)
            .setPrompt("consent")
            .setAdditionalParameters(
                mapOf(
                    "access_type" to "offline",
                )
            )
            .build()
    }

    return object : GoogleAuthController {
        override val isAvailable: Boolean = clientId.isNotBlank()

        override fun launch() {
            if (clientId.isBlank()) {
                onError("Google Client ID missing.")
                return
            }
            val intent = authService.getAuthorizationRequestIntent(authRequest)
            launcher.launch(intent)
        }
    }
}

private fun parseIdToken(token: String, json: Json): IdTokenPayload? {
    val parts = token.split(".")
    if (parts.size < 2) return null
    val payload = parts[1]
    return runCatching {
        val decoded = Base64.decode(payload, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        json.decodeFromString<IdTokenPayload>(decoded.decodeToString())
    }.getOrNull()
}

@Serializable
private data class IdTokenPayload(
    @SerialName("sub")
    val subject: String,
    val email: String? = null,
    val name: String? = null,
)

private const val AUTH_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth"
private const val TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token"
private const val SCOPES =
    "openid email profile https://www.googleapis.com/auth/calendar"
