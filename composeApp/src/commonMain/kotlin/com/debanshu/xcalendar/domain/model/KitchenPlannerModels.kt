package com.debanshu.xcalendar.domain.model

data class KitchenPlannerState(
    val mealPlanText: String = "",
    val mealPlanSavedAt: Long = 0L,
    val groceryListText: String = "",
    val groceryListSavedAt: Long = 0L,
    val dietaryNotes: String = "",
)

enum class KitchenPlannerMode { MEAL_PLAN, GROCERY_LIST }

data class MealDay(
    val day: String,
    val breakfast: String?,
    val lunch: String?,
    val dinner: String?,
    val note: String? = null,
)

data class MealPlanResult(val days: List<MealDay>)

data class GroceryItem(val name: String, val quantity: String? = null)

data class GroceryCategory(val name: String, val items: List<GroceryItem>)

data class GroceryListResult(val categories: List<GroceryCategory>)
