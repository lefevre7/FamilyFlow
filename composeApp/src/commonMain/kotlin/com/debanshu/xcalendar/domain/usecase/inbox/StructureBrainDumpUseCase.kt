package com.debanshu.xcalendar.domain.usecase.inbox

import com.debanshu.xcalendar.domain.llm.LlmSamplingConfig
import com.debanshu.xcalendar.domain.llm.LocalLlmManager
import com.debanshu.xcalendar.domain.model.BrainDumpStructuredResult
import com.debanshu.xcalendar.domain.util.BrainDumpStructuringEngine
import org.koin.core.annotation.Factory

@Factory
class StructureBrainDumpUseCase(
    private val localLlmManager: LocalLlmManager,
) {
    suspend operator fun invoke(rawText: String): BrainDumpStructuredResult {
        val trimmed = rawText.trim()
        val llmJson =
            if (localLlmManager.isAvailable) {
                val prompt = BrainDumpLlmSchema.buildPrompt(trimmed)
                localLlmManager.generate(
                    prompt = prompt,
                    sampling = LlmSamplingConfig(topK = 40, topP = 0.9, temperature = 0.3),
                )
            } else {
                null
            }
        val structured = llmJson?.let { BrainDumpStructuringEngine.structureFromLlmJson(it) }
        return structured ?: BrainDumpStructuringEngine.structure(trimmed)
    }
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
          "title": { "type": "string" },
          "priority": { "type": ["string", "null"], "description": "must, should, nice" },
          "energy": { "type": ["string", "null"], "description": "low, medium, high" },
          "notes": { "type": ["string", "null"] }
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

Schema:
$JSON_SCHEMA

Brain dump:
$truncated
""".trim()
    }
}
