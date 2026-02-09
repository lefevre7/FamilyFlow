package com.debanshu.xcalendar.domain.repository

import com.debanshu.xcalendar.domain.model.FamilyLensSelection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.core.annotation.Single

@Single(binds = [ILensPreferencesRepository::class])
class LensPreferencesRepository : ILensPreferencesRepository {
    private val state = MutableStateFlow(FamilyLensSelection())

    override val selection: Flow<FamilyLensSelection> = state

    override suspend fun updateSelection(selection: FamilyLensSelection) {
        state.value = selection
    }
}
