package com.debanshu.xcalendar.domain.usecase.settings

import com.debanshu.xcalendar.domain.model.HolidayPreferences
import com.debanshu.xcalendar.domain.repository.IHolidayPreferencesRepository
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Factory

@Factory
class GetHolidayPreferencesUseCase(
    private val holidayPreferencesRepository: IHolidayPreferencesRepository
) {
    operator fun invoke(): Flow<HolidayPreferences> =
        holidayPreferencesRepository.preferences
}
