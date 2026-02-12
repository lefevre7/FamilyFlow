package com.debanshu.xcalendar.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class VoiceCaptureSource {
    TODAY_QUICK_CAPTURE,
    QUICK_ADD_VOICE,
}

@Serializable
enum class VoiceDiagnosticLevel {
    INFO,
    WARN,
    ERROR,
}

@Serializable
enum class VoiceDiagnosticStep {
    CAPTURE_RECEIVED,
    LLM_UNAVAILABLE,
    LLM_ATTEMPT_STARTED,
    LLM_PROMPT_BUILT,
    LLM_RESPONSE_RECEIVED,
    LLM_JSON_EXTRACTED,
    LLM_PARSE_FAILED,
    LLM_PARSE_EMPTY,
    LLM_PARSE_SUCCESS,
    HEURISTIC_FALLBACK_USED,
    TASKS_SAVED,
    INBOX_AUDIT_SAVED,
    COMPLETED,
    FAILED,
}

@Serializable
data class VoiceDiagnosticEntry(
    val id: String,
    val sessionId: String,
    val timestampMillis: Long,
    val source: VoiceCaptureSource,
    val step: VoiceDiagnosticStep,
    val level: VoiceDiagnosticLevel = VoiceDiagnosticLevel.INFO,
    val message: String,
    val attemptIndex: Int? = null,
    val taskCount: Int? = null,
    val transcript: String? = null,
    val llmPrompt: String? = null,
    val llmRawResponse: String? = null,
    val llmExtractedJson: String? = null,
    val errorMessage: String? = null,
)
