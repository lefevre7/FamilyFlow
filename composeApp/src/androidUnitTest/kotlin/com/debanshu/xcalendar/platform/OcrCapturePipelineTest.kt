package com.debanshu.xcalendar.platform

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OcrCapturePipelineTest {

    @Test
    fun captureStageMessages_areStable() {
        assertNull(OcrCaptureStage.IDLE.message())
        assertEquals("Waiting for crop...", OcrCaptureStage.WAITING_FOR_CROP.message())
        assertEquals("Preparing image...", OcrCaptureStage.PREPARING_IMAGE.message())
        assertEquals("Processing image...", OcrCaptureStage.PROCESSING_IMAGE.message())
    }

    @Test
    fun defaultPreparationActions_followExpectedOrder() {
        assertEquals(
            listOf(
                OcrPreparationAction.CROP,
                OcrPreparationAction.CROP_FLIP_HORIZONTAL,
                OcrPreparationAction.CROP_FLIP_VERTICAL,
                OcrPreparationAction.SKIP_CROP,
                OcrPreparationAction.CANCEL,
            ),
            defaultOcrPreparationActions(),
        )
    }

    @Test
    fun preparationFlags_matchActionBehavior() {
        assertTrue(OcrPreparationAction.CROP.requiresCrop())
        assertTrue(OcrPreparationAction.CROP_FLIP_HORIZONTAL.requiresCrop())
        assertTrue(OcrPreparationAction.CROP_FLIP_VERTICAL.requiresCrop())
        assertFalse(OcrPreparationAction.SKIP_CROP.requiresCrop())
        assertFalse(OcrPreparationAction.CANCEL.requiresCrop())

        assertTrue(OcrPreparationAction.CROP_FLIP_HORIZONTAL.requiresHorizontalFlip())
        assertFalse(OcrPreparationAction.CROP_FLIP_VERTICAL.requiresHorizontalFlip())

        assertTrue(OcrPreparationAction.CROP_FLIP_VERTICAL.requiresVerticalFlip())
        assertFalse(OcrPreparationAction.CROP_FLIP_HORIZONTAL.requiresVerticalFlip())
    }
}
