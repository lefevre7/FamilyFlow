package com.debanshu.xcalendar.domain.usecase.settings

import com.debanshu.xcalendar.domain.repository.IHolidayPreferencesRepository
import org.koin.core.annotation.Factory

@Factory
class UpdateHolidayPreferencesUseCase(
    private val holidayPreferencesRepository: IHolidayPreferencesRepository
) {
    suspend operator fun invoke(countryCode: String, region: String) {
        holidayPreferencesRepository.setCountryCode(countryCode)
        holidayPreferencesRepository.setRegion(region)
    }
}
