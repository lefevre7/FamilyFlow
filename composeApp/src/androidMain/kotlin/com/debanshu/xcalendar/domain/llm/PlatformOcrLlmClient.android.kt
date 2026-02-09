package com.debanshu.xcalendar.domain.llm

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.koin.core.annotation.Single
import org.koin.mp.KoinPlatform

@Single(binds = [OcrLlmClient::class])
actual class PlatformOcrLlmClient : OcrLlmClient {
    private val llmManager by lazy { KoinPlatform.getKoin().get<LocalLlmManager>() }
    override val isAvailable: Boolean
        get() = llmManager.isAvailable

    override suspend fun structureOcr(
        rawText: String,
        referenceDate: LocalDate,
        timeZone: TimeZone,
    ): String? {
        if (!llmManager.isAvailable) return null
        val trimmedText =
            rawText.trim().let { if (it.length > MAX_OCR_CHARS) it.take(MAX_OCR_CHARS) else it }
        val prompt = OcrLlmSchema.buildPrompt(trimmedText, referenceDate, timeZone)
        val response =
            llmManager.generate(
                prompt = prompt,
                sampling = LlmSamplingConfig(topK = 40, topP = 0.9, temperature = 0.2),
            )
        return response?.let { extractJsonBlock(it) }
    }

    private fun extractJsonBlock(text: String): String? {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        return text.substring(start, end + 1).trim()
    }

    companion object {
        private const val MAX_OCR_CHARS = 2000
    }
}
