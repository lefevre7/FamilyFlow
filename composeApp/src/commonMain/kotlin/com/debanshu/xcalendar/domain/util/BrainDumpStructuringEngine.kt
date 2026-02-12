package com.debanshu.xcalendar.domain.util

import com.debanshu.xcalendar.domain.model.BrainDumpStructuredResult
import com.debanshu.xcalendar.domain.model.BrainDumpTaskDraft
import com.debanshu.xcalendar.domain.model.TaskEnergy
import com.debanshu.xcalendar.domain.model.TaskPriority
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object BrainDumpStructuringEngine {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    fun structure(rawText: String): BrainDumpStructuredResult {
        val trimmed = rawText.trim()
        structureFromLlmJson(trimmed)?.let { return it }
        return heuristicStructure(rawText)
    }

    fun structureFromLlmJson(rawText: String): BrainDumpStructuredResult? {
        if (!rawText.trimStart().startsWith("{")) return null
        return runCatching {
            val response = json.decodeFromString<BrainDumpLlmResponse>(rawText)
            val tasks =
                response.tasks.mapNotNull { task ->
                    val title = task.title?.trim().orEmpty()
                    if (title.isEmpty()) return@mapNotNull null
                    val notes =
                        buildList {
                            task.notes?.trim()?.takeIf { it.isNotEmpty() }?.let { add(it) }
                            task.dueDate?.trim()?.takeIf { it.isNotEmpty() }?.let { add("Due date: $it") }
                            task.dueTime?.trim()?.takeIf { it.isNotEmpty() }?.let { add("Due time: $it") }
                            task.assignee?.trim()?.takeIf { it.isNotEmpty() }?.let { add("Assignee: $it") }
                        }.joinToString("\n").ifBlank { null }
                    BrainDumpTaskDraft(
                        title = title,
                        priority = task.priority?.toPriority(),
                        energy = task.energy?.toEnergy(),
                        notes = notes,
                    )
                }
            BrainDumpStructuredResult(rawText = rawText, tasks = tasks)
        }.getOrNull()
    }

    private fun heuristicStructure(rawText: String): BrainDumpStructuredResult {
        val lines =
            rawText
                .lineSequence()
                .flatMap { line ->
                    line.split(";", "•", "·")
                        .map { it.trim() }
                        .asSequence()
                }
                .map { it.trim().trimStart('-', '•', '·') }
                .filter { it.isNotBlank() }
                .toList()
        val tasks = lines.map { BrainDumpTaskDraft(title = it) }
        return BrainDumpStructuredResult(rawText = rawText, tasks = tasks)
    }

    private fun String.toPriority(): TaskPriority? {
        return when (lowercase()) {
            "must", "urgent", "high" -> TaskPriority.MUST
            "should", "medium" -> TaskPriority.SHOULD
            "nice", "optional", "low" -> TaskPriority.NICE
            else -> null
        }
    }

    private fun String.toEnergy(): TaskEnergy? {
        return when (lowercase()) {
            "low" -> TaskEnergy.LOW
            "medium", "med" -> TaskEnergy.MEDIUM
            "high" -> TaskEnergy.HIGH
            else -> null
        }
    }
}

@Serializable
data class BrainDumpLlmResponse(
    @SerialName("tasks")
    val tasks: List<BrainDumpLlmTask> = emptyList(),
)

@Serializable
data class BrainDumpLlmTask(
    val title: String? = null,
    val priority: String? = null,
    val energy: String? = null,
    val notes: String? = null,
    val dueDate: String? = null,
    val dueTime: String? = null,
    val assignee: String? = null,
)
