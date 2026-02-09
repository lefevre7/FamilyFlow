package com.debanshu.xcalendar.domain.repository

import com.debanshu.xcalendar.domain.model.FamilyLensSelection
import kotlinx.coroutines.flow.Flow

interface ILensPreferencesRepository {
    val selection: Flow<FamilyLensSelection>

    suspend fun updateSelection(selection: FamilyLensSelection)
}
