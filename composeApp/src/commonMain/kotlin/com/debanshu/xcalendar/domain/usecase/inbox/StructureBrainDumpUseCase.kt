package com.debanshu.xcalendar.domain.usecase.inbox

import com.debanshu.xcalendar.domain.llm.LlmSamplingConfig
import com.debanshu.xcalendar.domain.llm.LocalLlmManager
import com.debanshu.xcalendar.domain.model.BrainDumpStructuredResult
import com.debanshu.xcalendar.domain.util.BrainDumpStructuringEngine
import org.koin.core.annotation.Factory

enum class BrainDumpLlmFailureReason {
    LLM_UNAVAILABLE,
    LLM_EXCEPTION,
    LLM_EMPTY_RESPONSE,
    JSON_BLOCK_NOT_FOUND,
    JSON_PARSE_FAILED,
    EMPTY_TASKS,
}

data class BrainDumpLlmAttemptDiagnostic(
    val attemptIndex: Int,
    val prompt: String,
    val rawResponse: String?,
    val extractedJson: String?,
    val tasksExtracted: Int,
    val failureReason: BrainDumpLlmFailureReason?,
    val errorMessage: String? = null,
)

data class BrainDumpLlmDiagnostics(
    val llmAvailable: Boolean,
    val attempts: List<BrainDumpLlmAttemptDiagnostic>,
    val structured: BrainDumpStructuredResult?,
)

@Factory
class StructureBrainDumpUseCase(
    private val localLlmManager: LocalLlmManager,
) {
    suspend operator fun invoke(rawText: String): BrainDumpStructuredResult {
        val trimmed = rawText.trim()
        val structuredWithLlm = structureWithLlmRetries(trimmed, retryCount = 0)
        return structuredWithLlm ?: BrainDumpStructuringEngine.structure(trimmed)
    }

    suspend fun structureWithLlmRetries(
        rawText: String,
        retryCount: Int = 2,
    ): BrainDumpStructuredResult? =
        structureWithLlmDiagnostics(rawText = rawText, retryCount = retryCount).structured

    suspend fun structureWithLlmDiagnostics(
        rawText: String,
        retryCount: Int = 2,
    ): BrainDumpLlmDiagnostics {
        val trimmed = rawText.trim()
        if (trimmed.isEmpty()) {
            return BrainDumpLlmDiagnostics(
                llmAvailable = localLlmManager.isAvailable,
                attempts = emptyList(),
                structured = null,
            )
        }
        if (!localLlmManager.isAvailable) {
            return BrainDumpLlmDiagnostics(
                llmAvailable = false,
                attempts = emptyList(),
                structured = null,
            )
        }

        val diagnostics = mutableListOf<BrainDumpLlmAttemptDiagnostic>()
        val attempts = retryCount.coerceAtLeast(0) + 1
        repeat(attempts) { index ->
            val prompt = BrainDumpLlmSchema.buildPrompt(trimmed)
            val result =
                runCatching {
                    localLlmManager.generate(
                        prompt = prompt,
                        sampling = LlmSamplingConfig(topK = 40, topP = 0.9, temperature = 0.3),
                    )
                }
            val rawResponse = result.getOrNull()
            val errorMessage = result.exceptionOrNull()?.message
            val extractedJson = rawResponse?.let(::extractJsonBlock)
            val structured = extractedJson?.let { BrainDumpStructuringEngine.structureFromLlmJson(it) }
            val tasksExtracted = structured?.tasks?.size ?: 0
            val failureReason =
                when {
                    result.isFailure -> BrainDumpLlmFailureReason.LLM_EXCEPTION
                    rawResponse.isNullOrBlank() -> BrainDumpLlmFailureReason.LLM_EMPTY_RESPONSE
                    extractedJson.isNullOrBlank() -> BrainDumpLlmFailureReason.JSON_BLOCK_NOT_FOUND
                    structured == null -> BrainDumpLlmFailureReason.JSON_PARSE_FAILED
                    tasksExtracted == 0 -> BrainDumpLlmFailureReason.EMPTY_TASKS
                    else -> null
                }
            diagnostics +=
                BrainDumpLlmAttemptDiagnostic(
                    attemptIndex = index + 1,
                    prompt = prompt,
                    rawResponse = rawResponse,
                    extractedJson = extractedJson,
                    tasksExtracted = tasksExtracted,
                    failureReason = failureReason,
                    errorMessage = errorMessage,
                )
            if (structured != null && structured.tasks.isNotEmpty()) {
                return BrainDumpLlmDiagnostics(
                    llmAvailable = true,
                    attempts = diagnostics,
                    structured = structured,
                )
            }
        }

        return BrainDumpLlmDiagnostics(
            llmAvailable = true,
            attempts = diagnostics,
            structured = null,
        )
    }
}

private fun extractJsonBlock(text: String): String? {
    val start = text.indexOf('{')
    if (start < 0) return null
    var depth = 0
    var inString = false
    var escaped = false
    for (index in start until text.length) {
        val char = text[index]
        if (inString) {
            if (escaped) {
                escaped = false
            } else if (char == '\\') {
                escaped = true
            } else if (char == '"') {
                inString = false
            }
            continue
        }
        when (char) {
            '"' -> inString = true
            '{' -> depth += 1
            '}' -> {
                depth -= 1
                if (depth == 0) {
                    return text.substring(start, index + 1).trim()
                }
            }
        }
    }
    return null
}

private object BrainDumpLlmSchema {
    private const val JSON_SCHEMA: String = """
{
  "type": "object",
  "required": ["tasks"],
  "properties": {
    "tasks": {
      "type": "array",
      "items": {
        "type": "object",
        "required": ["title"],
        "properties": {
          "title": { "type": ["string", "null"] },
          "priority": { "type": ["string", "null"], "description": "must, should, nice" },
          "energy": { "type": ["string", "null"], "description": "low, medium, high" },
          "notes": { "type": ["string", "null"] },
          "dueDate": { "type": ["string", "null"], "description": "optional due date text" },
          "dueTime": { "type": ["string", "null"], "description": "optional due time text" },
          "assignee": { "type": ["string", "null"], "description": "optional person reference" }
        },
        "additionalProperties": false
      }
    }
  },
  "additionalProperties": false
}
"""

    fun buildPrompt(rawText: String): String {
        val truncated = if (rawText.length > 1800) rawText.take(1800) else rawText
        return """
You are extracting actionable tasks from a brain-dump list.
Return ONLY valid JSON that matches the schema below (no markdown, no extra text).
If a field is unknown, use null.

Schema:
$JSON_SCHEMA

Brain dump:
$truncated
""".trim()
    }
}
