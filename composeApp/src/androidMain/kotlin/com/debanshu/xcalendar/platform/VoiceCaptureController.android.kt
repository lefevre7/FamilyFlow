package com.debanshu.xcalendar.platform

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
actual fun rememberVoiceCaptureController(
    onResult: (String) -> Unit,
    onError: (String) -> Unit,
): VoiceCaptureController {
    val context = LocalContext.current
    val isAvailable = remember { SpeechRecognizer.isRecognitionAvailable(context) }

    val resultLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        val matches = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        val text = matches?.firstOrNull().orEmpty()
        if (text.isNotBlank()) {
            onResult(text)
        } else {
            onError("No speech captured")
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchRecognizer(resultLauncher, onError)
        } else {
            onError("Microphone permission denied")
        }
    }

    return object : VoiceCaptureController {
        override val isAvailable: Boolean = isAvailable

        override fun start() {
            if (!isAvailable) {
                onError("Voice capture not available on this device")
                return
            }
            val granted =
                ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
            if (granted) {
                launchRecognizer(resultLauncher, onError)
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }
}

private fun launchRecognizer(
    launcher: androidx.activity.result.ActivityResultLauncher<Intent>,
    onError: (String) -> Unit,
) {
    val intent =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }
    try {
        launcher.launch(intent)
    } catch (exception: Exception) {
        onError("Unable to start voice capture")
    }
}
