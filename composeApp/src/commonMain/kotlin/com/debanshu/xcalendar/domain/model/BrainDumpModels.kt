package com.debanshu.xcalendar.domain.model

data class BrainDumpTaskDraft(
    val title: String,
    val priority: TaskPriority? = null,
    val energy: TaskEnergy? = null,
    val notes: String? = null,
)

data class BrainDumpStructuredResult(
    val rawText: String,
    val tasks: List<BrainDumpTaskDraft>,
)
