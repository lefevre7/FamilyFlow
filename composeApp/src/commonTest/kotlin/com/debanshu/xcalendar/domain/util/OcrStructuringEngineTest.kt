package com.debanshu.xcalendar.domain.util

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OcrStructuringEngineTest {

    @Test
    fun structureFromLlmJson_parsesValidJson() {
        val json =
            """
            {
              "events": [
                {
                  "title": "Preschool pickup",
                  "date": "03/15/2026",
                  "startTime": "11:30 am",
                  "endTime": "12:00 pm",
                  "allDay": false,
                  "sourceText": "Preschool pickup 11:30-12:00"
                }
              ]
            }
            """.trimIndent()

        val result = OcrStructuringEngine.structureFromLlmJson(json, LocalDate(2026, 2, 7))
        assertNotNull(result)
        assertEquals(1, result.candidates.size)

        val candidate = result.candidates.first()
        assertEquals("Preschool pickup", candidate.title)
        assertEquals(LocalDate(2026, 3, 15), candidate.startDate)
        assertEquals(11, candidate.startTime?.hour)
        assertEquals(30, candidate.startTime?.minute)
        assertEquals(12, candidate.endTime?.hour)
        assertEquals(0, candidate.endTime?.minute)
        assertTrue(!candidate.allDay)
    }

    @Test
    fun structureFromLlmJson_returnsNullOnInvalidJson() {
        val result = OcrStructuringEngine.structureFromLlmJson("not json", LocalDate(2026, 2, 7))
        assertNull(result)
    }

    @Test
    fun structure_fallsBackToHeuristicWhenJsonInvalid() {
        val raw = "Mar 18 9-10am Field Trip\nMar 19 All day Teacher Day"
        val result = OcrStructuringEngine.structure(raw, LocalDate(2026, 2, 7), TimeZone.UTC)
        assertTrue(result.candidates.isNotEmpty())
        assertTrue(result.candidates.first().title.contains("Field Trip"))
    }

    @Test
    fun inferRecurringRule_detectsWeeklyByDayToken() {
        val rule = OcrStructuringEngine.inferRecurringRule("Soccer practice every Tuesday")
        assertEquals("FREQ=WEEKLY;BYDAY=TU", rule)
    }

    @Test
    fun inferRecurringRule_usesDateFallbackForWeeklyPattern() {
        val rule =
            OcrStructuringEngine.inferRecurringRule(
                text = "Library pickup every week",
                date = LocalDate(2026, 3, 16),
            )
        assertEquals("FREQ=WEEKLY;BYDAY=MO", rule)
    }

    @Test
    fun inferRecurringRule_returnsNullWhenPatternMissing() {
        val rule = OcrStructuringEngine.inferRecurringRule("One-time school photo day")
        assertNull(rule)
    }
}
