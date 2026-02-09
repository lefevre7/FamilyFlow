package com.debanshu.xcalendar.domain.repository

import com.debanshu.xcalendar.domain.model.ReminderPreferences
import kotlinx.coroutines.flow.Flow

interface IReminderPreferencesRepository {
    val preferences: Flow<ReminderPreferences>

    suspend fun setRemindersEnabled(enabled: Boolean)

    suspend fun setPrepMinutes(minutes: Int)

    suspend fun setTravelBufferMinutes(minutes: Int)

    suspend fun setAllDayTime(hour: Int, minute: Int)

    suspend fun setSummaryEnabled(enabled: Boolean)

    suspend fun setSummaryTimes(
        morningHour: Int,
        morningMinute: Int,
        middayHour: Int,
        middayMinute: Int,
    )
}
