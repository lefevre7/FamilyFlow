package com.debanshu.xcalendar.domain.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single
import org.koin.mp.KoinPlatform

private val Context.dateSelectionPreferencesDataStore by preferencesDataStore(name = "date_selection_preferences")

@Single(binds = [IDateSelectionPreferencesRepository::class])
class DateSelectionPreferencesRepository : IDateSelectionPreferencesRepository {
    private val context: Context by lazy { KoinPlatform.getKoin().get() }
    private val dataStore by lazy { context.dateSelectionPreferencesDataStore }

    override val selectedDateIso: Flow<String?> =
        dataStore.data.map { prefs -> prefs[SELECTED_DATE_ISO_KEY] }

    override suspend fun updateSelectedDateIso(isoDate: String) {
        dataStore.edit { prefs ->
            if (isoDate.isBlank()) {
                prefs.remove(SELECTED_DATE_ISO_KEY)
            } else {
                prefs[SELECTED_DATE_ISO_KEY] = isoDate
            }
        }
    }

    private companion object {
        val SELECTED_DATE_ISO_KEY = stringPreferencesKey("selected_date_iso")
    }
}
