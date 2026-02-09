package com.debanshu.xcalendar.domain.repository

import com.debanshu.xcalendar.domain.model.ReminderPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.koin.core.annotation.Single

@Single(binds = [IReminderPreferencesRepository::class])
class ReminderPreferencesRepository : IReminderPreferencesRepository {
    private val state = MutableStateFlow(ReminderPreferences())

    override val preferences: Flow<ReminderPreferences> = state

    override suspend fun setRemindersEnabled(enabled: Boolean) {
        state.update { it.copy(remindersEnabled = enabled) }
    }

    override suspend fun setPrepMinutes(minutes: Int) {
        state.update { it.copy(prepMinutes = minutes) }
    }

    override suspend fun setTravelBufferMinutes(minutes: Int) {
        state.update { it.copy(travelBufferMinutes = minutes) }
    }

    override suspend fun setAllDayTime(hour: Int, minute: Int) {
        state.update { it.copy(allDayHour = hour, allDayMinute = minute) }
    }

    override suspend fun setSummaryEnabled(enabled: Boolean) {
        state.update { it.copy(summaryEnabled = enabled) }
    }

    override suspend fun setSummaryTimes(
        morningHour: Int,
        morningMinute: Int,
        middayHour: Int,
        middayMinute: Int,
    ) {
        state.update {
            it.copy(
                summaryMorningHour = morningHour,
                summaryMorningMinute = morningMinute,
                summaryMiddayHour = middayHour,
                summaryMiddayMinute = middayMinute,
            )
        }
    }
}
