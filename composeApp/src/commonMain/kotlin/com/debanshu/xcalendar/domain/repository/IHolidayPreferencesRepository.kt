package com.debanshu.xcalendar.domain.repository

import com.debanshu.xcalendar.domain.model.HolidayPreferences
import kotlinx.coroutines.flow.Flow

interface IHolidayPreferencesRepository {
    val preferences: Flow<HolidayPreferences>

    suspend fun setCountryCode(countryCode: String)

    suspend fun setRegion(region: String)
}
