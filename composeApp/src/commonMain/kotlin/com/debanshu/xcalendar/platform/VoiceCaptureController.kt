package com.debanshu.xcalendar.platform

import androidx.compose.runtime.Composable

interface VoiceCaptureController {
    val isAvailable: Boolean
    fun start()
}

@Composable
expect fun rememberVoiceCaptureController(
    onResult: (String) -> Unit,
    onError: (String) -> Unit,
): VoiceCaptureController
