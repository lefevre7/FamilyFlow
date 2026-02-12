package com.debanshu.xcalendar.platform

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
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
import kotlin.math.max

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
        val boundsOptions =
            BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOptions)
        }
        if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) return null

        val maxDimension = MAX_OCR_DIMENSION
        val sampleSize = calculateInSampleSize(boundsOptions.outWidth, boundsOptions.outHeight, maxDimension)
        val decodeOptions =
            BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
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

    return runCatching {
        val normalizedBitmap = normalizeBitmapForOcr(bitmap)
        val api = TessBaseAPI()
        try {
            val initialized = api.init(dataPath, "eng")
            check(initialized) { "Failed to initialize OCR" }
            api.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO)
            api.setImage(normalizedBitmap)
            val text = api.utF8Text?.trim().orEmpty()
            check(text.isNotBlank()) { "No text found" }
            text
        } finally {
            api.recycle()
            if (normalizedBitmap !== bitmap && !normalizedBitmap.isRecycled) {
                normalizedBitmap.recycle()
            }
        }
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

private fun calculateInSampleSize(
    width: Int,
    height: Int,
    maxDimension: Int,
): Int {
    val largestDimension = max(width, height)
    if (largestDimension <= maxDimension) return 1
    var sampleSize = 1
    while (largestDimension / sampleSize > maxDimension) {
        sampleSize *= 2
    }
    return sampleSize
}

private fun normalizeBitmapForOcr(bitmap: Bitmap): Bitmap {
    val largestDimension = max(bitmap.width, bitmap.height)
    if (largestDimension <= MAX_OCR_DIMENSION) return bitmap
    val scale = MAX_OCR_DIMENSION.toFloat() / largestDimension.toFloat()
    val targetWidth = (bitmap.width * scale).toInt().coerceAtLeast(1)
    val targetHeight = (bitmap.height * scale).toInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
}

private const val MAX_OCR_DIMENSION = 2048
