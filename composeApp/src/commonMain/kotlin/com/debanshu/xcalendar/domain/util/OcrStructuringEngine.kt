package com.debanshu.xcalendar.domain.util

import com.debanshu.xcalendar.domain.model.OcrCandidateEvent
import com.debanshu.xcalendar.domain.model.OcrStructuredResult
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

object OcrStructuringEngine {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = false
    }

    fun structure(
        rawText: String,
        referenceDate: LocalDate,
        timeZone: TimeZone,
    ): OcrStructuredResult {
        val trimmed = rawText.trim()
        structureFromLlmJson(trimmed, referenceDate)?.let { return it }
        return heuristicStructure(rawText, referenceDate, timeZone)
    }

    fun structureFromLlmJson(
        rawText: String,
        referenceDate: LocalDate,
    ): OcrStructuredResult? {
        if (!rawText.trimStart().startsWith("{")) return null
        return runCatching {
            val response = json.decodeFromString<OcrLlmResponse>(rawText)
            val candidates =
                response.events.mapNotNull { event ->
                    val title = event.title.trim()
                    if (title.isEmpty()) return@mapNotNull null
                    val date = event.date?.let { parseDate(it, referenceDate) }
                    val startTime = event.startTime?.let { parseTime(it) }
                    val endTime = event.endTime?.let { parseTime(it) }
                    buildCandidate(
                        title = title,
                        date = date,
                        startTime = startTime,
                        endTime = endTime,
                        allDay = event.allDay,
                        sourceText = event.sourceText ?: title,
                    )
                }
            OcrStructuredResult(rawText = rawText, candidates = candidates)
        }.getOrNull()
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun heuristicStructure(
        rawText: String,
        referenceDate: LocalDate,
        timeZone: TimeZone,
    ): OcrStructuredResult {
        val lines =
            rawText
                .lineSequence()
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .toList()
        val candidates = mutableListOf<OcrCandidateEvent>()
        var lastDate: LocalDate? = null

        lines.forEach { line ->
            val dateFromLine = parseDate(line, referenceDate)
            if (dateFromLine != null) {
                lastDate = dateFromLine
            }
            val date = dateFromLine ?: lastDate
            val timeRange = parseTimeRange(line)
            val title = cleanTitle(line)
            if (title.isBlank()) return@forEach
            candidates.add(
                OcrCandidateEvent(
                    id = Uuid.random().toString(),
                    title = title,
                    startDate = date,
                    startTime = timeRange?.first,
                    endTime = timeRange?.second,
                    allDay = timeRange == null,
                    sourceText = line,
                )
            )
        }
        return OcrStructuredResult(rawText = rawText, candidates = candidates)
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun buildCandidate(
        title: String,
        date: LocalDate?,
        startTime: LocalTime?,
        endTime: LocalTime?,
        allDay: Boolean,
        sourceText: String,
    ): OcrCandidateEvent =
        OcrCandidateEvent(
            id = Uuid.random().toString(),
            title = title,
            startDate = date,
            startTime = startTime,
            endTime = endTime,
            allDay = allDay,
            sourceText = sourceText,
        )

    private fun cleanTitle(line: String): String {
        return line
            .replace(DATE_REGEX, "")
            .replace(TIME_RANGE_REGEX, "")
            .replace(MULTI_SPACE_REGEX, " ")
            .trim()
            .ifEmpty { line.trim() }
    }

    internal fun parseDate(text: String, referenceDate: LocalDate): LocalDate? {
        val numericMatch = DATE_REGEX.find(text)
        if (numericMatch != null) {
            val month = numericMatch.groupValues[1].toIntOrNull() ?: return null
            val day = numericMatch.groupValues[2].toIntOrNull() ?: return null
            val yearToken = numericMatch.groupValues.getOrNull(3).orEmpty()
            val year =
                when {
                    yearToken.length == 2 -> 2000 + (yearToken.toIntOrNull() ?: 0)
                    yearToken.length == 4 -> yearToken.toIntOrNull() ?: referenceDate.year
                    else -> referenceDate.year
                }
            return runCatching { LocalDate(year, month, day) }.getOrNull()
        }

        val monthMatch = MONTH_REGEX.find(text)
        if (monthMatch != null) {
            val monthName = monthMatch.groupValues[1]
            val day = monthMatch.groupValues[2].toIntOrNull() ?: return null
            val month = monthNameToNumber(monthName) ?: return null
            return runCatching { LocalDate(referenceDate.year, month, day) }.getOrNull()
        }
        return null
    }

    internal fun parseTimeRange(text: String): Pair<LocalTime, LocalTime>? {
        val match = TIME_RANGE_REGEX.find(text) ?: return null
        val start = parseTime(match.groupValues[1]) ?: return null
        val end = parseTime(match.groupValues[2]) ?: return null
        return start to end
    }

    internal fun parseTime(text: String): LocalTime? {
        val match = TIME_REGEX.find(text.trim()) ?: return null
        val hourRaw = match.groupValues[1].toIntOrNull() ?: return null
        val minute = match.groupValues.getOrNull(2)?.takeIf { it.isNotBlank() }?.toIntOrNull() ?: 0
        val amPm = match.groupValues.getOrNull(3)?.lowercase()
        val hour =
            when (amPm) {
                "am" -> if (hourRaw == 12) 0 else hourRaw
                "pm" -> if (hourRaw == 12) 12 else hourRaw + 12
                else -> hourRaw
            }
        if (hour !in 0..23 || minute !in 0..59) return null
        return LocalTime(hour, minute)
    }

    private fun monthNameToNumber(value: String): Int? {
        return when (value.lowercase().take(3)) {
            "jan" -> 1
            "feb" -> 2
            "mar" -> 3
            "apr" -> 4
            "may" -> 5
            "jun" -> 6
            "jul" -> 7
            "aug" -> 8
            "sep" -> 9
            "oct" -> 10
            "nov" -> 11
            "dec" -> 12
            else -> null
        }
    }

    private val DATE_REGEX = Regex("(\\d{1,2})[/-](\\d{1,2})(?:[/-](\\d{2,4}))?")
    private val MONTH_REGEX = Regex("(jan|feb|mar|apr|may|jun|jul|aug|sep|sept|oct|nov|dec)[a-z]*\\s+(\\d{1,2})", RegexOption.IGNORE_CASE)
    private val TIME_RANGE_REGEX = Regex("(\\d{1,2}(?::\\d{2})?\\s*(?:am|pm)?)\\s*[-–]\\s*(\\d{1,2}(?::\\d{2})?\\s*(?:am|pm)?)", RegexOption.IGNORE_CASE)
    private val TIME_REGEX = Regex("(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?", RegexOption.IGNORE_CASE)
    private val MULTI_SPACE_REGEX = Regex("\\s{2,}")
}

@Serializable
data class OcrLlmResponse(
    @SerialName("events")
    val events: List<OcrLlmEvent> = emptyList(),
)

@Serializable
data class OcrLlmEvent(
    val title: String,
    val date: String? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val allDay: Boolean = false,
    val sourceText: String? = null,
)
