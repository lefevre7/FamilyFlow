package com.debanshu.xcalendar.domain.model

data class ReminderPreferences(
    val remindersEnabled: Boolean = true,
    val prepMinutes: Int = 20,
    val travelBufferMinutes: Int = 0,
    val allDayHour: Int = 8,
    val allDayMinute: Int = 0,
    val summaryEnabled: Boolean = true,
    val summaryMorningHour: Int = 8,
    val summaryMorningMinute: Int = 0,
    val summaryMiddayHour: Int = 13,
    val summaryMiddayMinute: Int = 0,
    val reducedMotionEnabled: Boolean = true,
    val highContrastEnabled: Boolean = false,
    val textScale: Float = 1.0f,
)
