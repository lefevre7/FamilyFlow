package com.debanshu.xcalendar.domain.usecase.ocr

import com.debanshu.xcalendar.domain.llm.OcrLlmClient
import com.debanshu.xcalendar.domain.model.OcrStructuredResult
import com.debanshu.xcalendar.domain.util.OcrStructuringEngine
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.koin.core.annotation.Factory

@Factory
class StructureOcrUseCase(
    private val ocrLlmClient: OcrLlmClient,
) {
    suspend operator fun invoke(
        rawText: String,
        referenceDate: LocalDate,
        timeZone: TimeZone,
    ): OcrStructuredResult {
        val llmJson =
            if (ocrLlmClient.isAvailable) {
                ocrLlmClient.structureOcr(rawText, referenceDate, timeZone)
            } else {
                null
            }
        val structuredFromLlm =
            llmJson?.let { OcrStructuringEngine.structureFromLlmJson(it, referenceDate) }
        return structuredFromLlm ?: OcrStructuringEngine.structure(rawText, referenceDate, timeZone)
    }
}
