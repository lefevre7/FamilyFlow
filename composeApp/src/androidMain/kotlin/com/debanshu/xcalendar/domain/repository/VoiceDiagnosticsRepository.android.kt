package com.debanshu.xcalendar.domain.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.debanshu.xcalendar.domain.model.VoiceDiagnosticEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single
import org.koin.mp.KoinPlatform

private val Context.voiceDiagnosticsDataStore by preferencesDataStore(name = "voice_diagnostics")

@Single(binds = [IVoiceDiagnosticsRepository::class])
class VoiceDiagnosticsRepository : IVoiceDiagnosticsRepository {
    private val context: Context by lazy { KoinPlatform.getKoin().get() }
    private val dataStore by lazy { context.voiceDiagnosticsDataStore }
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    override val diagnosticsEnabled: Flow<Boolean> =
        dataStore.data.map { preferences ->
            preferences[DIAGNOSTICS_ENABLED] ?: true
        }

    override val entries: Flow<List<VoiceDiagnosticEntry>> =
        dataStore.data.map { preferences ->
            decodeEntries(preferences[ENTRIES_JSON])
        }

    override suspend fun isDiagnosticsEnabled(): Boolean = diagnosticsEnabled.first()

    override suspend fun setDiagnosticsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[DIAGNOSTICS_ENABLED] = enabled
        }
    }

    override suspend fun append(entry: VoiceDiagnosticEntry) {
        dataStore.edit { preferences ->
            val current = decodeEntries(preferences[ENTRIES_JSON])
            val updated = (current + entry).takeLast(MAX_ENTRIES)
            if (updated.isEmpty()) {
                preferences.remove(ENTRIES_JSON)
            } else {
                preferences[ENTRIES_JSON] = json.encodeToString(updated)
            }
        }
    }

    override suspend fun clear() {
        dataStore.edit { preferences ->
            preferences.remove(ENTRIES_JSON)
        }
    }

    private fun decodeEntries(raw: String?): List<VoiceDiagnosticEntry> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            json.decodeFromString<List<VoiceDiagnosticEntry>>(raw)
        }.getOrDefault(emptyList())
    }

    private companion object {
        private const val MAX_ENTRIES = 200
        val ENTRIES_JSON = stringPreferencesKey("voice_diagnostics_entries_json")
        val DIAGNOSTICS_ENABLED = booleanPreferencesKey("voice_diagnostics_enabled")
    }
}
