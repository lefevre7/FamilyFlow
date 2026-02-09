package com.debanshu.xcalendar.domain.llm

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.koin.core.annotation.Single

@Single(binds = [OcrLlmClient::class])
actual class PlatformOcrLlmClient : OcrLlmClient {
    override val isAvailable: Boolean = false

    override suspend fun structureOcr(
        rawText: String,
        referenceDate: LocalDate,
        timeZone: TimeZone,
    ): String? = null
}
