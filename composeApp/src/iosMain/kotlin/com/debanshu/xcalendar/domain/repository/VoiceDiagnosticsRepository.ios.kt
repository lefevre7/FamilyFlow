package com.debanshu.xcalendar.domain.repository

import com.debanshu.xcalendar.domain.model.VoiceDiagnosticEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import org.koin.core.annotation.Single

@Single(binds = [IVoiceDiagnosticsRepository::class])
class VoiceDiagnosticsRepository : IVoiceDiagnosticsRepository {
    private val enabledState = MutableStateFlow(true)
    private val entriesState = MutableStateFlow<List<VoiceDiagnosticEntry>>(emptyList())

    override val diagnosticsEnabled: Flow<Boolean> = enabledState
    override val entries: Flow<List<VoiceDiagnosticEntry>> = entriesState

    override suspend fun isDiagnosticsEnabled(): Boolean = diagnosticsEnabled.first()

    override suspend fun setDiagnosticsEnabled(enabled: Boolean) {
        enabledState.value = enabled
    }

    override suspend fun append(entry: VoiceDiagnosticEntry) {
        entriesState.update { current ->
            (current + entry).takeLast(MAX_ENTRIES)
        }
    }

    override suspend fun clear() {
        entriesState.value = emptyList()
    }

    private companion object {
        private const val MAX_ENTRIES = 200
    }
}
