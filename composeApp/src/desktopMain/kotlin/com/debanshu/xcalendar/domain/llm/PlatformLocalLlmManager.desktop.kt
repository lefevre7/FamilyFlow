package com.debanshu.xcalendar.domain.llm

import org.koin.core.annotation.Single

@Single(binds = [LocalLlmManager::class])
actual class PlatformLocalLlmManager : LocalLlmManager {
    override val isAvailable: Boolean = false

    override fun getStatus(): LlmModelStatus {
        return LlmModelStatus(
            available = false,
            source = LlmModelSource.NONE,
            sizeBytes = 0L,
            requiredBytes = 0L,
            incompatibilityMessage = "Local AI is not supported on this platform.",
        )
    }

    override suspend fun ensureModelAvailable(): Boolean = false

    override suspend fun downloadModel(onProgress: (Int) -> Unit): Boolean = false

    override suspend fun generate(prompt: String, sampling: LlmSamplingConfig): String? = null

    override fun deleteModel(): Boolean = false

    override fun consumeWarningMessage(): String? = null
}
