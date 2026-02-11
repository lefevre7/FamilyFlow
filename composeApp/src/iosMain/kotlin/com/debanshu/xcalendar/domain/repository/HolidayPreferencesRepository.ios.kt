package com.debanshu.xcalendar.domain.repository

import com.debanshu.xcalendar.domain.model.HolidayPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.koin.core.annotation.Single

@Single(binds = [IHolidayPreferencesRepository::class])
class HolidayPreferencesRepository : IHolidayPreferencesRepository {
    override val preferences: Flow<HolidayPreferences> = flowOf(HolidayPreferences())

    override suspend fun setCountryCode(countryCode: String) {
        // iOS stub
    }

    override suspend fun setRegion(region: String) {
        // iOS stub
    }
}
