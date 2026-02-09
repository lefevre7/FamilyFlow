package com.debanshu.xcalendar.domain.llm

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
interface OcrLlmClient {
    val isAvailable: Boolean

    suspend fun structureOcr(
        rawText: String,
        referenceDate: LocalDate,
        timeZone: TimeZone,
    ): String?
}

expect class PlatformOcrLlmClient() : OcrLlmClient

object OcrLlmSchema {
    const val JSON_SCHEMA: String = """
{
  "type": "object",
  "required": ["events"],
  "properties": {
    "events": {
      "type": "array",
      "items": {
        "type": "object",
        "required": ["title"],
        "properties": {
          "title": { "type": "string" },
          "date": { "type": ["string", "null"], "description": "MM/DD or YYYY-MM-DD" },
          "startTime": { "type": ["string", "null"], "description": "h:mm am/pm" },
          "endTime": { "type": ["string", "null"], "description": "h:mm am/pm" },
          "allDay": { "type": "boolean" },
          "sourceText": { "type": ["string", "null"] }
        },
        "additionalProperties": false
      }
    }
  },
  "additionalProperties": false
}
"""

    fun buildPrompt(rawText: String, referenceDate: LocalDate, timeZone: TimeZone): String {
        return """
You are extracting calendar events from OCR text.
Return ONLY valid JSON that matches the schema below (no markdown, no extra text).
Use the reference date ${referenceDate} and time zone ${timeZone} when resolving ambiguous dates.

Schema:
$JSON_SCHEMA

OCR text:
$rawText
""".trim()
    }
}
