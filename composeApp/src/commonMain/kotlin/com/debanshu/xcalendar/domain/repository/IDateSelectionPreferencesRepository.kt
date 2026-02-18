package com.debanshu.xcalendar.domain.repository

import kotlinx.coroutines.flow.Flow

interface IDateSelectionPreferencesRepository {
    val selectedDateIso: Flow<String?>

    suspend fun updateSelectedDateIso(isoDate: String)
}
