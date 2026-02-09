package com.debanshu.xcalendar.platform

import androidx.compose.runtime.Composable

@Composable
actual fun rememberGoogleAuthController(
    onSuccess: (GoogleAuthResult) -> Unit,
    onError: (String) -> Unit,
): GoogleAuthController {
    return object : GoogleAuthController {
        override val isAvailable: Boolean = false

        override fun launch() {
            onError("Google OAuth is not supported on this platform.")
        }
    }
}
