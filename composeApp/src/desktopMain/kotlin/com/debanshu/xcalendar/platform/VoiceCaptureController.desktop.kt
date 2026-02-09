package com.debanshu.xcalendar.platform

import androidx.compose.runtime.Composable

@Composable
actual fun rememberVoiceCaptureController(
    onResult: (String) -> Unit,
    onError: (String) -> Unit,
): VoiceCaptureController {
    return object : VoiceCaptureController {
        override val isAvailable: Boolean = false

        override fun start() {
            onError("Voice capture is not available on desktop")
        }
    }
}
