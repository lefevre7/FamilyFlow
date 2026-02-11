package com.debanshu.xcalendar.domain.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.debanshu.xcalendar.domain.model.HolidayPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single
import org.koin.mp.KoinPlatform

private val Context.holidayPrefsDataStore by preferencesDataStore(name = "holiday_prefs")

@Single(binds = [IHolidayPreferencesRepository::class])
class HolidayPreferencesRepository : IHolidayPreferencesRepository {
    private val context: Context by lazy { KoinPlatform.getKoin().get() }
    private val dataStore by lazy { context.holidayPrefsDataStore }

    override val preferences: Flow<HolidayPreferences> =
        dataStore.data.map { prefs ->
            HolidayPreferences(
                countryCode = prefs[COUNTRY_CODE] ?: "usa",
                region = prefs[REGION] ?: "utah"
            )
        }

    override suspend fun setCountryCode(countryCode: String) {
        dataStore.edit { it[COUNTRY_CODE] = countryCode }
    }

    override suspend fun setRegion(region: String) {
        dataStore.edit { it[REGION] = region }
    }

    companion object {
        private val COUNTRY_CODE = stringPreferencesKey("country_code")
        private val REGION = stringPreferencesKey("region")
    }
}
