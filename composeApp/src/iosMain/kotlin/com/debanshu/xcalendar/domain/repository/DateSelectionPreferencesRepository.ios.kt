package com.debanshu.xcalendar.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.core.annotation.Single

@Single(binds = [IDateSelectionPreferencesRepository::class])
class DateSelectionPreferencesRepository : IDateSelectionPreferencesRepository {
    private val state = MutableStateFlow<String?>(null)

    override val selectedDateIso: Flow<String?> = state

    override suspend fun updateSelectedDateIso(isoDate: String) {
        state.value = isoDate.ifBlank { null }
    }
}
