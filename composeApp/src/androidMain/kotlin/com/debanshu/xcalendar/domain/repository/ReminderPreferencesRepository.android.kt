package com.debanshu.xcalendar.domain.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.debanshu.xcalendar.domain.model.ReminderPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single
import org.koin.mp.KoinPlatform

private val Context.reminderDataStore by preferencesDataStore(name = "reminder_prefs")

@Single(binds = [IReminderPreferencesRepository::class])
class ReminderPreferencesRepository : IReminderPreferencesRepository {
    private val context: Context by lazy { KoinPlatform.getKoin().get() }
    private val dataStore by lazy { context.reminderDataStore }

    override val preferences: Flow<ReminderPreferences> =
        dataStore.data.map { prefs ->
            ReminderPreferences(
                remindersEnabled = prefs[REMINDERS_ENABLED] ?: true,
                prepMinutes = prefs[PREP_MINUTES] ?: 20,
                travelBufferMinutes = prefs[TRAVEL_BUFFER_MINUTES] ?: 0,
                allDayHour = prefs[ALL_DAY_HOUR] ?: 8,
                allDayMinute = prefs[ALL_DAY_MINUTE] ?: 0,
                summaryEnabled = prefs[SUMMARY_ENABLED] ?: true,
                summaryMorningHour = prefs[SUMMARY_MORNING_HOUR] ?: 8,
                summaryMorningMinute = prefs[SUMMARY_MORNING_MINUTE] ?: 0,
                summaryMiddayHour = prefs[SUMMARY_MIDDAY_HOUR] ?: 13,
                summaryMiddayMinute = prefs[SUMMARY_MIDDAY_MINUTE] ?: 0,
            )
        }

    override suspend fun setRemindersEnabled(enabled: Boolean) {
        dataStore.edit { it[REMINDERS_ENABLED] = enabled }
    }

    override suspend fun setPrepMinutes(minutes: Int) {
        dataStore.edit { it[PREP_MINUTES] = minutes }
    }

    override suspend fun setTravelBufferMinutes(minutes: Int) {
        dataStore.edit { it[TRAVEL_BUFFER_MINUTES] = minutes }
    }

    override suspend fun setAllDayTime(hour: Int, minute: Int) {
        dataStore.edit {
            it[ALL_DAY_HOUR] = hour
            it[ALL_DAY_MINUTE] = minute
        }
    }

    override suspend fun setSummaryEnabled(enabled: Boolean) {
        dataStore.edit { it[SUMMARY_ENABLED] = enabled }
    }

    override suspend fun setSummaryTimes(
        morningHour: Int,
        morningMinute: Int,
        middayHour: Int,
        middayMinute: Int,
    ) {
        dataStore.edit {
            it[SUMMARY_MORNING_HOUR] = morningHour
            it[SUMMARY_MORNING_MINUTE] = morningMinute
            it[SUMMARY_MIDDAY_HOUR] = middayHour
            it[SUMMARY_MIDDAY_MINUTE] = middayMinute
        }
    }

    private companion object {
        val REMINDERS_ENABLED = booleanPreferencesKey("reminders_enabled")
        val PREP_MINUTES = intPreferencesKey("prep_minutes")
        val TRAVEL_BUFFER_MINUTES = intPreferencesKey("travel_buffer_minutes")
        val ALL_DAY_HOUR = intPreferencesKey("all_day_hour")
        val ALL_DAY_MINUTE = intPreferencesKey("all_day_minute")
        val SUMMARY_ENABLED = booleanPreferencesKey("summary_enabled")
        val SUMMARY_MORNING_HOUR = intPreferencesKey("summary_morning_hour")
        val SUMMARY_MORNING_MINUTE = intPreferencesKey("summary_morning_minute")
        val SUMMARY_MIDDAY_HOUR = intPreferencesKey("summary_midday_hour")
        val SUMMARY_MIDDAY_MINUTE = intPreferencesKey("summary_midday_minute")
    }
}
