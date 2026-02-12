package com.debanshu.xcalendar.domain.repository

import com.debanshu.xcalendar.domain.model.VoiceDiagnosticEntry
import kotlinx.coroutines.flow.Flow

interface IVoiceDiagnosticsRepository {
    val diagnosticsEnabled: Flow<Boolean>
    val entries: Flow<List<VoiceDiagnosticEntry>>

    suspend fun isDiagnosticsEnabled(): Boolean

    suspend fun setDiagnosticsEnabled(enabled: Boolean)

    suspend fun append(entry: VoiceDiagnosticEntry)

    suspend fun clear()
}
