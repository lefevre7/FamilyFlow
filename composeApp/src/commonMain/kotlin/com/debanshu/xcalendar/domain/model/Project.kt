package com.debanshu.xcalendar.domain.model

import androidx.compose.runtime.Stable

@Stable
data class Project(
    val id: String,
    val title: String,
    val notes: String? = null,
    val status: ProjectStatus = ProjectStatus.ACTIVE,
    val seasonLabel: String? = null,
    val startAt: Long? = null,
    val endAt: Long? = null,
    val ownerPersonId: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
)

enum class ProjectStatus {
    ACTIVE,
    DONE,
    ARCHIVED,
}
