package com.debanshu.xcalendar.platform

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.googlecode.tesseract.android.TessBaseAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Composable
actual fun rememberOcrCaptureController(
    onResult: (String) -> Unit,
    onError: (String) -> Unit,
): OcrCaptureController {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            scope.launch(Dispatchers.Default) {
                val result = runOcr(context, bitmap)
                withContext(Dispatchers.Main) {
                    result.onSuccess(onResult).onFailure { onError(it.message ?: "OCR failed") }
                }
            }
        } else {
            onError("No image captured")
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.Default) {
                val bitmap = decodeBitmap(context, uri)
                val result =
                    if (bitmap != null) {
                        runOcr(context, bitmap)
                    } else {
                        Result.failure(IllegalStateException("Unable to decode image"))
                    }
                withContext(Dispatchers.Main) {
                    result.onSuccess(onResult).onFailure { onError(it.message ?: "OCR failed") }
                }
            }
        } else {
            onError("No image selected")
        }
    }

    return object : OcrCaptureController {
        override val isAvailable: Boolean = true

        override fun captureFromCamera() {
            cameraLauncher.launch(null)
        }

        override fun pickFromGallery() {
            galleryLauncher.launch("image/*")
        }
    }
}

private fun decodeBitmap(context: Context, uri: Uri): Bitmap? {
    return runCatching {
        if (android.os.Build.VERSION.SDK_INT >= 28) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source)
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
    }.getOrNull()
}

private fun runOcr(
    context: Context,
    bitmap: Bitmap,
): Result<String> {
    val dataPath = ensureTessData(context) ?: return Result.failure(
        IllegalStateException("OCR data files missing"),
    )

    val api = TessBaseAPI()
    val initialized = api.init(dataPath, "eng")
    if (!initialized) {
        api.recycle()
        return Result.failure(IllegalStateException("Failed to initialize OCR"))
    }

    api.setImage(bitmap)
    val text = api.utF8Text ?: ""
    api.recycle()

    return if (text.isBlank()) {
        Result.failure(IllegalStateException("No text found"))
    } else {
        Result.success(text)
    }
}

private fun ensureTessData(context: Context): String? {
    return runCatching {
        val tessDir = File(context.filesDir, "tessdata")
        if (!tessDir.exists()) {
            tessDir.mkdirs()
        }
        val trainedData = File(tessDir, "eng.traineddata")
        if (!trainedData.exists()) {
            context.assets.open("tessdata/eng.traineddata").use { input ->
                FileOutputStream(trainedData).use { output ->
                    input.copyTo(output)
                }
            }
        }
        context.filesDir.absolutePath
    }.getOrNull()
}
