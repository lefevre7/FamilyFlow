package com.debanshu.xcalendar.domain.util

import com.debanshu.xcalendar.domain.model.TaskEnergy
import com.debanshu.xcalendar.domain.model.TaskPriority
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BrainDumpStructuringEngineTest {

    @Test
    fun structureFromLlmJson_parsesTasks() {
        val json =
            """
            {
              "tasks": [
                {
                  "title": "Call dentist",
                  "priority": "must",
                  "energy": "low",
                  "notes": "before 5"
                }
              ]
            }
            """.trimIndent()

        val result = BrainDumpStructuringEngine.structureFromLlmJson(json)
        assertNotNull(result)
        assertEquals(1, result.tasks.size)

        val task = result.tasks.first()
        assertEquals("Call dentist", task.title)
        assertEquals(TaskPriority.MUST, task.priority)
        assertEquals(TaskEnergy.LOW, task.energy)
        assertEquals("before 5", task.notes)
    }

    @Test
    fun structure_fallsBackToHeuristicOnPlainText() {
        val result = BrainDumpStructuringEngine.structure("Buy milk; Pack lunches")
        assertEquals(2, result.tasks.size)
        assertTrue(result.tasks.any { it.title == "Buy milk" })
        assertTrue(result.tasks.any { it.title == "Pack lunches" })
    }

    @Test
    fun structureFromLlmJson_handlesNullableFieldsAndMapsExtendedNotes() {
        val json =
            """
            {
              "tasks": [
                {
                  "title": null,
                  "priority": "must"
                },
                {
                  "title": "Kid 1 appointment",
                  "notes": "bring insurance card",
                  "dueDate": "today",
                  "dueTime": "8:00 AM",
                  "assignee": "Mom"
                }
              ]
            }
            """.trimIndent()

        val result = BrainDumpStructuringEngine.structureFromLlmJson(json)
        assertNotNull(result)
        assertEquals(1, result.tasks.size)
        val task = result.tasks.first()
        assertEquals("Kid 1 appointment", task.title)
        assertNotNull(task.notes)
        assertTrue(task.notes.contains("bring insurance card"))
        assertTrue(task.notes.contains("Due date: today"))
        assertTrue(task.notes.contains("Due time: 8:00 AM"))
        assertTrue(task.notes.contains("Assignee: Mom"))
    }
}
