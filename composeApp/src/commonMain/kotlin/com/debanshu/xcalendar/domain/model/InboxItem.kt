package com.debanshu.xcalendar.domain.model

import androidx.compose.runtime.Stable

@Stable
data class InboxItem(
    val id: String,
    val rawText: String,
    val source: InboxSource = InboxSource.TEXT,
    val status: InboxStatus = InboxStatus.NEW,
    val createdAt: Long = 0L,
    val personId: String? = null,
    val linkedTaskId: String? = null,
)

enum class InboxStatus {
    NEW,
    PROCESSED,
    ARCHIVED,
}

enum class InboxSource {
    TEXT,
    VOICE,
    OCR,
}
