package com.debanshu.xcalendar.domain.model

import androidx.compose.runtime.Stable

@Stable
data class Person(
    val id: String,
    val name: String,
    val role: PersonRole,
    val ageYears: Int? = null,
    val color: Int,
    val avatarUrl: String = "",
    val isAdmin: Boolean = false,
    val isDefault: Boolean = false,
    val sortOrder: Int = 0,
    val isArchived: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
)

enum class PersonRole {
    MOM,
    PARTNER,
    CHILD,
    CAREGIVER,
    OTHER,
}
