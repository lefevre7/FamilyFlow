package com.debanshu.xcalendar.domain.model

data class GoogleAccountLink(
    val id: String,
    val email: String,
    val displayName: String? = null,
    val personId: String,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
)
