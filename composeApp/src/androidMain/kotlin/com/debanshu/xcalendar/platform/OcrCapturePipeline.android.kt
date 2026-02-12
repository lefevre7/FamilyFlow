package com.debanshu.xcalendar.platform

internal enum class OcrCaptureStage {
    IDLE,
    WAITING_FOR_CROP,
    PREPARING_IMAGE,
    PROCESSING_IMAGE,
}

internal fun OcrCaptureStage.message(): String? =
    when (this) {
        OcrCaptureStage.IDLE -> null
        OcrCaptureStage.WAITING_FOR_CROP -> "Waiting for crop..."
        OcrCaptureStage.PREPARING_IMAGE -> "Preparing image..."
        OcrCaptureStage.PROCESSING_IMAGE -> "Processing image..."
    }

internal enum class OcrPreparationAction(val label: String) {
    CROP("Crop image"),
    CROP_FLIP_HORIZONTAL("Crop + flip horizontal"),
    CROP_FLIP_VERTICAL("Crop + flip vertical"),
    SKIP_CROP("Skip crop"),
    CANCEL("Cancel"),
}

internal fun defaultOcrPreparationActions(): List<OcrPreparationAction> =
    listOf(
        OcrPreparationAction.CROP,
        OcrPreparationAction.CROP_FLIP_HORIZONTAL,
        OcrPreparationAction.CROP_FLIP_VERTICAL,
        OcrPreparationAction.SKIP_CROP,
        OcrPreparationAction.CANCEL,
    )

internal fun OcrPreparationAction.requiresCrop(): Boolean =
    when (this) {
        OcrPreparationAction.CROP,
        OcrPreparationAction.CROP_FLIP_HORIZONTAL,
        OcrPreparationAction.CROP_FLIP_VERTICAL,
            -> true
        OcrPreparationAction.SKIP_CROP,
        OcrPreparationAction.CANCEL,
            -> false
    }

internal fun OcrPreparationAction.requiresHorizontalFlip(): Boolean =
    this == OcrPreparationAction.CROP_FLIP_HORIZONTAL

internal fun OcrPreparationAction.requiresVerticalFlip(): Boolean =
    this == OcrPreparationAction.CROP_FLIP_VERTICAL
