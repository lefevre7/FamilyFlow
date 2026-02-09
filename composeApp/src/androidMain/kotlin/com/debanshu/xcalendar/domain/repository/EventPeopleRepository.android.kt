package com.debanshu.xcalendar.domain.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single
import org.koin.mp.KoinPlatform

private val Context.eventPeopleDataStore by preferencesDataStore(name = "event_people_sidecar")

@Single(binds = [IEventPeopleRepository::class])
class EventPeopleRepository : IEventPeopleRepository {
    private val context: Context by lazy { KoinPlatform.getKoin().get() }
    private val dataStore by lazy { context.eventPeopleDataStore }
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override val mappings: Flow<Map<String, List<String>>> =
        dataStore.data.map { preferences ->
            decodeMappings(preferences[EVENT_PEOPLE_MAPPINGS_JSON])
        }

    override suspend fun getPeopleForEvent(eventId: String): List<String> =
        mappings.first()[eventId].orEmpty()

    override suspend fun setPeopleForEvent(eventId: String, personIds: List<String>) {
        val cleaned = personIds.filter { it.isNotBlank() }.distinct()
        dataStore.edit { preferences ->
            val current = decodeMappings(preferences[EVENT_PEOPLE_MAPPINGS_JSON]).toMutableMap()
            if (cleaned.isEmpty()) {
                current.remove(eventId)
            } else {
                current[eventId] = cleaned
            }
            writeMappings(preferences, current)
        }
    }

    override suspend fun removeEvent(eventId: String) {
        dataStore.edit { preferences ->
            val current = decodeMappings(preferences[EVENT_PEOPLE_MAPPINGS_JSON]).toMutableMap()
            if (current.remove(eventId) != null) {
                writeMappings(preferences, current)
            }
        }
    }

    private fun decodeMappings(raw: String?): Map<String, List<String>> {
        if (raw.isNullOrBlank()) return emptyMap()
        return runCatching {
            json.decodeFromString<Map<String, List<String>>>(raw)
        }.getOrDefault(emptyMap())
    }

    private fun writeMappings(
        preferences: androidx.datastore.preferences.core.MutablePreferences,
        mappings: Map<String, List<String>>,
    ) {
        if (mappings.isEmpty()) {
            preferences.remove(EVENT_PEOPLE_MAPPINGS_JSON)
            return
        }
        preferences[EVENT_PEOPLE_MAPPINGS_JSON] = json.encodeToString(mappings)
    }

    private companion object {
        val EVENT_PEOPLE_MAPPINGS_JSON = stringPreferencesKey("event_people_mappings_json")
    }
}
