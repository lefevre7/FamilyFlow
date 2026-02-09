package com.debanshu.xcalendar.domain.model

import androidx.compose.runtime.Stable
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

@Stable
data class OcrCandidateEvent(
    val id: String,
    val title: String,
    val startDate: LocalDate?,
    val startTime: LocalTime?,
    val endTime: LocalTime?,
    val allDay: Boolean,
    val sourceText: String,
)

@Stable
data class OcrStructuredResult(
    val rawText: String,
    val candidates: List<OcrCandidateEvent>,
)
