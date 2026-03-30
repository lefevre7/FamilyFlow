package com.debanshu.xcalendar.domain.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.debanshu.xcalendar.domain.model.KitchenPlannerState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single
import org.koin.mp.KoinPlatform

private val Context.kitchenPlannerDataStore by preferencesDataStore(name = "kitchen_planner")

@Single(binds = [IKitchenPlannerRepository::class])
class KitchenPlannerRepository : IKitchenPlannerRepository {
    private val context: Context by lazy { KoinPlatform.getKoin().get() }
    private val dataStore by lazy { context.kitchenPlannerDataStore }

    override val state: Flow<KitchenPlannerState> = dataStore.data.map { prefs ->
        KitchenPlannerState(
            mealPlanText = prefs[MEAL_PLAN_TEXT] ?: "",
            mealPlanSavedAt = prefs[MEAL_PLAN_SAVED_AT] ?: 0L,
            groceryListText = prefs[GROCERY_LIST_TEXT] ?: "",
            groceryListSavedAt = prefs[GROCERY_LIST_SAVED_AT] ?: 0L,
            dietaryNotes = prefs[DIETARY_NOTES] ?: "",
        )
    }

    override suspend fun saveMealPlan(text: String, savedAt: Long) {
        dataStore.edit { prefs ->
            prefs[MEAL_PLAN_TEXT] = text
            prefs[MEAL_PLAN_SAVED_AT] = savedAt
        }
    }

    override suspend fun saveGroceryList(text: String, savedAt: Long) {
        dataStore.edit { prefs ->
            prefs[GROCERY_LIST_TEXT] = text
            prefs[GROCERY_LIST_SAVED_AT] = savedAt
        }
    }

    override suspend fun saveDietaryNotes(notes: String) {
        dataStore.edit { prefs ->
            prefs[DIETARY_NOTES] = notes
        }
    }

    private companion object {
        val MEAL_PLAN_TEXT = stringPreferencesKey("kitchen_meal_plan_text")
        val MEAL_PLAN_SAVED_AT = longPreferencesKey("kitchen_meal_plan_saved_at")
        val GROCERY_LIST_TEXT = stringPreferencesKey("kitchen_grocery_list_text")
        val GROCERY_LIST_SAVED_AT = longPreferencesKey("kitchen_grocery_list_saved_at")
        val DIETARY_NOTES = stringPreferencesKey("kitchen_dietary_notes")
    }
}
