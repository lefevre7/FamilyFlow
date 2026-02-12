package com.debanshu.xcalendar.platform

import androidx.compose.runtime.Composable

@Composable
actual fun rememberOcrCaptureController(
    onResult: (String) -> Unit,
    onError: (String) -> Unit,
    onStatusChanged: (String?) -> Unit,
): OcrCaptureController {
    return object : OcrCaptureController {
        override val isAvailable: Boolean = false

        override fun captureFromCamera() {
            onError("OCR capture is not available on iOS yet")
        }

        override fun pickFromGallery() {
            onError("OCR capture is not available on iOS yet")
        }
    }
}
