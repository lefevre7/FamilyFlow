package com.debanshu.xcalendar.platform

import androidx.compose.runtime.Composable

interface OcrCaptureController {
    val isAvailable: Boolean
    fun captureFromCamera()
    fun pickFromGallery()
}

@Composable
expect fun rememberOcrCaptureController(
    onResult: (String) -> Unit,
    onError: (String) -> Unit,
): OcrCaptureController
