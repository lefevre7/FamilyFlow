package com.debanshu.xcalendar.domain.repository

import com.debanshu.xcalendar.domain.model.KitchenPlannerState
import kotlinx.coroutines.flow.Flow

interface IKitchenPlannerRepository {
    val state: Flow<KitchenPlannerState>
    suspend fun saveMealPlan(text: String, savedAt: Long)
    suspend fun saveGroceryList(text: String, savedAt: Long)
    suspend fun saveDietaryNotes(notes: String)
}
