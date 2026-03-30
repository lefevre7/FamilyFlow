package com.debanshu.xcalendar.domain.repository

import com.debanshu.xcalendar.domain.model.KitchenPlannerState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.core.annotation.Single

@Single(binds = [IKitchenPlannerRepository::class])
class KitchenPlannerRepository : IKitchenPlannerRepository {
    private val _state = MutableStateFlow(KitchenPlannerState())
    override val state: Flow<KitchenPlannerState> = _state

    override suspend fun saveMealPlan(text: String, savedAt: Long) {
        _state.value = _state.value.copy(mealPlanText = text, mealPlanSavedAt = savedAt)
    }

    override suspend fun saveGroceryList(text: String, savedAt: Long) {
        _state.value = _state.value.copy(groceryListText = text, groceryListSavedAt = savedAt)
    }

    override suspend fun saveDietaryNotes(notes: String) {
        _state.value = _state.value.copy(dietaryNotes = notes)
    }
}
