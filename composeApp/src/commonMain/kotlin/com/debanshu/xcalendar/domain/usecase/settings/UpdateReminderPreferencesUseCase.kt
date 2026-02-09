package com.debanshu.xcalendar.domain.usecase.settings

import com.debanshu.xcalendar.domain.repository.IReminderPreferencesRepository
import org.koin.core.annotation.Factory

@Factory
class UpdateReminderPreferencesUseCase(
    private val repository: IReminderPreferencesRepository,
) {
    suspend fun setRemindersEnabled(enabled: Boolean) =
        repository.setRemindersEnabled(enabled)

    suspend fun setPrepMinutes(minutes: Int) =
        repository.setPrepMinutes(minutes)

    suspend fun setTravelBufferMinutes(minutes: Int) =
        repository.setTravelBufferMinutes(minutes)

    suspend fun setAllDayTime(hour: Int, minute: Int) =
        repository.setAllDayTime(hour, minute)

    suspend fun setSummaryEnabled(enabled: Boolean) =
        repository.setSummaryEnabled(enabled)

    suspend fun setSummaryTimes(
        morningHour: Int,
        morningMinute: Int,
        middayHour: Int,
        middayMinute: Int,
    ) = repository.setSummaryTimes(morningHour, morningMinute, middayHour, middayMinute)
}
