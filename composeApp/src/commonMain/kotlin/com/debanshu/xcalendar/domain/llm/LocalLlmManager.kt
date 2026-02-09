package com.debanshu.xcalendar.domain.llm

data class LlmSamplingConfig(
    val topK: Int = 40,
    val topP: Double = 0.95,
    val temperature: Double = 0.2,
)

enum class LlmModelSource {
    NONE,
    ASSET,
    LOCAL,
}

data class LlmModelStatus(
    val available: Boolean,
    val source: LlmModelSource,
    val sizeBytes: Long,
    val requiredBytes: Long,
    val incompatibilityMessage: String? = null,
)

interface LocalLlmManager {
    val isAvailable: Boolean

    fun getStatus(): LlmModelStatus

    suspend fun ensureModelAvailable(): Boolean

    suspend fun downloadModel(onProgress: (Int) -> Unit): Boolean

    suspend fun generate(
        prompt: String,
        sampling: LlmSamplingConfig = LlmSamplingConfig(),
    ): String?

    fun deleteModel(): Boolean

    fun consumeWarningMessage(): String?
}

expect class PlatformLocalLlmManager() : LocalLlmManager
