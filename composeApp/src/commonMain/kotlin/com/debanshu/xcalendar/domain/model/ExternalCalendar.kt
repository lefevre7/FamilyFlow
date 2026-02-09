package com.debanshu.xcalendar.domain.model

data class ExternalCalendar(
    val id: String,
    val name: String,
    val colorHex: String? = null,
    val primary: Boolean = false,
    val timeZone: String? = null,
    val accessRole: String? = null,
)
