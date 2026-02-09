package com.debanshu.xcalendar.domain.model

data class ExternalEvent(
    val id: String,
    val summary: String,
    val description: String? = null,
    val location: String? = null,
    val startTime: Long,
    val endTime: Long,
    val isAllDay: Boolean,
    val updatedAt: Long,
    val cancelled: Boolean = false,
)
