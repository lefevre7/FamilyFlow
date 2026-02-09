package com.debanshu.xcalendar.platform

import androidx.compose.runtime.Composable

data class GoogleAuthResult(
    val accountId: String,
    val email: String,
    val displayName: String?,
    val accessToken: String,
    val refreshToken: String?,
    val idToken: String?,
    val expiresAt: Long?,
)

interface GoogleAuthController {
    val isAvailable: Boolean
    fun launch()
}

@Composable
expect fun rememberGoogleAuthController(
    onSuccess: (GoogleAuthResult) -> Unit,
    onError: (String) -> Unit,
): GoogleAuthController
