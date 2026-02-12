package com.debanshu.xcalendar.platform

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.googlecode.tesseract.android.TessBaseAPI
import com.yalantis.ucrop.UCrop
import com.yalantis.ucrop.UCropActivity
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
    onStatusChanged: (String?) -> Unit,
): OcrCaptureController {
    val context = LocalContext.current
    val activity = context.findActivity()
    val scope = rememberCoroutineScope()

    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    fun updateStage(stage: OcrCaptureStage) {
        onStatusChanged(stage.message())
    }

    fun runOcrFromUriAsync(uri: Uri) {
        updateStage(OcrCaptureStage.PROCESSING_IMAGE)
        scope.launch(Dispatchers.Default) {
            val result = runOcrFromUri(context, uri)
            withContext(Dispatchers.Main) {
                updateStage(OcrCaptureStage.IDLE)
                result.onSuccess(onResult).onFailure { onError(it.message ?: "OCR failed") }
            }
        }
    }

    fun launchCropOrFallback(
        sourceUri: Uri,
        cropLauncher: ActivityResultLauncher<Intent>,
    ) {
        val outputUri = createTempImageUri(context, "ocr_crop_")
        if (outputUri == null) {
            runOcrFromUriAsync(sourceUri)
            return
        }
        val intent = createCropIntent(context, sourceUri, outputUri)
        val launched =
            runCatching {
                cropLauncher.launch(intent)
            }.isSuccess
        if (!launched) {
            runOcrFromUriAsync(sourceUri)
        }
    }

    fun launchPreparationPrompt(
        sourceUri: Uri,
        cropLauncher: ActivityResultLauncher<Intent>,
    ) {
        val hostActivity = activity
        if (hostActivity == null) {
            updateStage(OcrCaptureStage.IDLE)
            onError("Unable to start image crop.")
            return
        }
        updateStage(OcrCaptureStage.WAITING_FOR_CROP)
        showOcrPreparationDialog(hostActivity) { action ->
            when (action) {
                OcrPreparationAction.CANCEL -> {
                    updateStage(OcrCaptureStage.IDLE)
                }

                OcrPreparationAction.SKIP_CROP -> {
                    runOcrFromUriAsync(sourceUri)
                }

                else -> {
                    if (!action.requiresHorizontalFlip() && !action.requiresVerticalFlip()) {
                        launchCropOrFallback(sourceUri, cropLauncher)
                        return@showOcrPreparationDialog
                    }
                    updateStage(OcrCaptureStage.PREPARING_IMAGE)
                    scope.launch(Dispatchers.Default) {
                        val flippedUri =
                            createFlippedImageUri(
                                context = context,
                                sourceUri = sourceUri,
                                horizontal = action.requiresHorizontalFlip(),
                                vertical = action.requiresVerticalFlip(),
                            )
                        withContext(Dispatchers.Main) {
                            if (flippedUri == null) {
                                runOcrFromUriAsync(sourceUri)
                            } else {
                                updateStage(OcrCaptureStage.WAITING_FOR_CROP)
                                launchCropOrFallback(flippedUri, cropLauncher)
                            }
                        }
                    }
                }
            }
        }
    }

    val cropLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val croppedUri = result.data?.let(UCrop::getOutput)
        when {
            result.resultCode == Activity.RESULT_OK && croppedUri != null -> runOcrFromUriAsync(croppedUri)
            result.resultCode == Activity.RESULT_CANCELED -> updateStage(OcrCaptureStage.IDLE)
            else -> {
                val cropError = result.data?.let(UCrop::getError)
                updateStage(OcrCaptureStage.IDLE)
                onError(cropError?.message ?: "Image crop failed")
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { success ->
        val sourceUri = pendingCameraUri
        pendingCameraUri = null
        if (!success || sourceUri == null) {
            updateStage(OcrCaptureStage.IDLE)
            return@rememberLauncherForActivityResult
        }
        launchPreparationPrompt(sourceUri, cropLauncher)
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri == null) {
            updateStage(OcrCaptureStage.IDLE)
        } else {
            launchPreparationPrompt(uri, cropLauncher)
        }
    }

    val legacyGalleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri == null) {
            updateStage(OcrCaptureStage.IDLE)
        } else {
            launchPreparationPrompt(uri, cropLauncher)
        }
    }

    return object : OcrCaptureController {
        override val isAvailable: Boolean = true

        override fun captureFromCamera() {
            val sourceUri = createTempImageUri(context, "ocr_camera_")
            if (sourceUri == null) {
                onError("Unable to start camera capture.")
                return
            }
            pendingCameraUri = sourceUri
            updateStage(OcrCaptureStage.WAITING_FOR_CROP)
            cameraLauncher.launch(sourceUri)
        }

        override fun pickFromGallery() {
            updateStage(OcrCaptureStage.WAITING_FOR_CROP)
            if (PickVisualMedia.isPhotoPickerAvailable(context)) {
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            } else {
                legacyGalleryLauncher.launch("image/*")
            }
        }
    }
}

private fun runOcrFromUri(
    context: Context,
    uri: Uri,
): Result<String> {
    val bitmap = decodeBitmap(context, uri) ?: return Result.failure(IllegalStateException("Unable to decode image"))
    return try {
        runOcr(context, bitmap)
    } finally {
        if (!bitmap.isRecycled) {
            bitmap.recycle()
        }
    }
}

private fun decodeBitmap(
    context: Context,
    uri: Uri,
    maxDimension: Int = MAX_OCR_DIMENSION,
): Bitmap? {
    return runCatching {
        val boundsOptions =
            BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOptions)
        }
        if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) return null

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

private fun createFlippedImageUri(
    context: Context,
    sourceUri: Uri,
    horizontal: Boolean,
    vertical: Boolean,
): Uri? {
    if (!horizontal && !vertical) return sourceUri
    return runCatching {
        val bitmap = decodeBitmap(context, sourceUri, maxDimension = MAX_FLIP_DIMENSION)
            ?: return null
        val matrix =
            Matrix().apply {
                preScale(if (horizontal) -1f else 1f, if (vertical) -1f else 1f)
            }
        val flipped = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        val outputUri = createTempImageUri(context, "ocr_flip_") ?: return null
        context.contentResolver.openOutputStream(outputUri)?.use { stream ->
            check(flipped.compress(Bitmap.CompressFormat.JPEG, CROPPED_JPEG_QUALITY, stream)) {
                "Unable to write flipped image."
            }
        } ?: return null
        if (!bitmap.isRecycled) {
            bitmap.recycle()
        }
        if (!flipped.isRecycled) {
            flipped.recycle()
        }
        outputUri
    }.getOrNull()
}

private fun createCropIntent(
    context: Context,
    sourceUri: Uri,
    outputUri: Uri,
): Intent {
    val options =
        UCrop.Options().apply {
            setCompressionFormat(Bitmap.CompressFormat.JPEG)
            setCompressionQuality(CROPPED_JPEG_QUALITY)
            setToolbarTitle("Crop schedule image")
            setFreeStyleCropEnabled(true)
            setHideBottomControls(false)
            setAllowedGestures(
                UCropActivity.ALL,
                UCropActivity.ALL,
                UCropActivity.ALL,
            )
        }

    return UCrop.of(sourceUri, outputUri)
        .withOptions(options)
        .getIntent(context)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
}

private fun createTempImageUri(
    context: Context,
    prefix: String,
): Uri? {
    return runCatching {
        val directory =
            File(context.cacheDir, OCR_IMAGE_CACHE_DIR).apply {
                if (!exists()) mkdirs()
            }
        val file = File.createTempFile(prefix, ".jpg", directory)
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }.getOrNull()
}

private fun showOcrPreparationDialog(
    activity: Activity,
    onActionSelected: (OcrPreparationAction) -> Unit,
) {
    val actions = defaultOcrPreparationActions().filterNot { it == OcrPreparationAction.CANCEL }
    val contentLayout =
        LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dialogPaddingPx(activity), dialogPaddingPx(activity), dialogPaddingPx(activity), dialogPaddingPx(activity))

            addView(
                TextView(activity).apply {
                    text = "Crop can improve OCR. You can crop first or skip and run OCR now."
                },
            )

            actions.forEach { action ->
                addView(
                    Button(activity).apply {
                        text = action.label
                        isAllCaps = false
                    },
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ).apply {
                        topMargin = dialogButtonMarginPx(activity)
                    },
                )
            }
        }

    var dialog: AlertDialog? = null
    val buttonViews = mutableListOf<Button>()
    for (index in 1 until contentLayout.childCount) {
        val button = contentLayout.getChildAt(index) as? Button ?: continue
        buttonViews += button
    }

    buttonViews.forEachIndexed { index, button ->
        val action = actions[index]
        button.setOnClickListener {
            dialog?.dismiss()
            onActionSelected(action)
        }
    }

    dialog = AlertDialog.Builder(activity)
        .setTitle("Prepare image for OCR")
        .setView(contentLayout)
        .setNegativeButton("Cancel") { alert, _ ->
            alert.dismiss()
            onActionSelected(OcrPreparationAction.CANCEL)
        }
        .setOnCancelListener { onActionSelected(OcrPreparationAction.CANCEL) }
        .show()
}

private fun dialogPaddingPx(context: Context): Int =
    (16 * context.resources.displayMetrics.density).toInt().coerceAtLeast(1)

private fun dialogButtonMarginPx(context: Context): Int =
    (8 * context.resources.displayMetrics.density).toInt().coerceAtLeast(1)

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
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
private const val MAX_FLIP_DIMENSION = 4096
private const val CROPPED_JPEG_QUALITY = 95
private const val OCR_IMAGE_CACHE_DIR = "ocr_images"
