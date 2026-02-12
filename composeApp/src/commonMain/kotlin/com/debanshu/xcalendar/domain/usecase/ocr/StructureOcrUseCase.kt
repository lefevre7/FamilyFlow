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
        val llmJson = structureWithLlmRetries(rawText, referenceDate, timeZone)
        val structuredFromLlm =
            llmJson?.let { OcrStructuringEngine.structureFromLlmJson(it, referenceDate) }
        return structuredFromLlm ?: OcrStructuringEngine.structure(rawText, referenceDate, timeZone)
    }

    suspend fun structureWithLlmRetries(
        rawText: String,
        referenceDate: LocalDate,
        timeZone: TimeZone,
        retryCount: Int = 2,
    ): String? {
        if (!ocrLlmClient.isAvailable) return null

        val attempts = retryCount.coerceAtLeast(0) + 1
        repeat(attempts) {
            val llmJson =
                runCatching {
                    ocrLlmClient.structureOcr(rawText, referenceDate, timeZone)
                }.getOrNull()
            if (!llmJson.isNullOrBlank()) return llmJson
        }
        return null
    }
}
