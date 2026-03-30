package com.debanshu.xcalendar.domain.llm

import com.debanshu.xcalendar.domain.model.GroceryCategory
import com.debanshu.xcalendar.domain.model.GroceryItem
import com.debanshu.xcalendar.domain.model.GroceryListResult
import com.debanshu.xcalendar.domain.model.MealDay
import com.debanshu.xcalendar.domain.model.MealPlanResult
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object KitchenPlannerLlmSchema {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    fun buildMealPlanPrompt(groceryText: String, dietaryNotes: String): String {
        val truncated = groceryText.take(1500)
        val notesLine = if (dietaryNotes.isNotBlank()) "Dietary notes: $dietaryNotes\n" else ""
        val grocerySection = if (truncated.isNotBlank()) {
            "Grocery items available:\n$truncated"
        } else {
            "No specific grocery items — generate a balanced general 7-day family meal plan."
        }
        return """
You are a family meal planner. Create a practical 7-day meal plan.

Output ONLY a JSON object in exactly this format — replace the example values with real meals:
{"days":[{"day":"Monday","breakfast":"oatmeal with berries","lunch":"turkey sandwich","dinner":"spaghetti bolognese","note":null},{"day":"Tuesday","breakfast":"scrambled eggs","lunch":"Caesar salad","dinner":"chicken stir fry","note":null},{"day":"Wednesday","breakfast":"yogurt parfait","lunch":"grilled cheese","dinner":"beef tacos","note":null},{"day":"Thursday","breakfast":"pancakes","lunch":"tomato soup","dinner":"baked salmon","note":null},{"day":"Friday","breakfast":"toast and fruit","lunch":"chicken wrap","dinner":"pizza night","note":null},{"day":"Saturday","breakfast":"waffles","lunch":"BLT sandwich","dinner":"pot roast","note":null},{"day":"Sunday","breakfast":"French toast","lunch":"leftovers","dinner":"roast chicken","note":null}]}

Do NOT copy the example meals above. Use real meals based on the input below.
${notesLine}${grocerySection}

JSON:
""".trim()
    }

    fun buildGroceryListPrompt(mealPlanText: String, dietaryNotes: String): String {
        val truncated = mealPlanText.take(1500)
        val notesLine = if (dietaryNotes.isNotBlank()) "Dietary notes: $dietaryNotes\n" else ""
        return """
You are a grocery list assistant. Read the meal plan and list every ingredient needed, grouped by store section.

Output ONLY a JSON object in exactly this format — replace the example items with real ingredients from the meal plan:
{"categories":[{"name":"Produce","items":[{"name":"onion","quantity":"2"},{"name":"garlic","quantity":"1 head"}]},{"name":"Dairy","items":[{"name":"parmesan cheese","quantity":"8 oz"},{"name":"butter","quantity":"4 tbsp"}]},{"name":"Protein","items":[{"name":"chicken breast","quantity":"2 lbs"}]},{"name":"Grains","items":[{"name":"pasta","quantity":"12 oz"},{"name":"breadcrumbs","quantity":"1 cup"}]},{"name":"Pantry","items":[{"name":"olive oil","quantity":"2 tbsp"},{"name":"tomato sauce","quantity":"24 oz"}]},{"name":"Frozen","items":[]},{"name":"Other","items":[]}]}

Do NOT copy the example ingredients above. Use only real ingredients from the meal plan below.
${notesLine}
Meal plan:
$truncated

JSON:
""".trim()
    }

    fun parseMealPlanJson(rawText: String): MealPlanResult? {
        val jsonBlock = extractJsonBlock(rawText) ?: return null
        return runCatching {
            val response = json.decodeFromString<MealPlanLlmResponse>(jsonBlock)
            MealPlanResult(
                days = response.days.map { d ->
                    MealDay(
                        day = d.day.orEmpty(),
                        breakfast = d.breakfast,
                        lunch = d.lunch,
                        dinner = d.dinner,
                        note = d.note,
                    )
                }.filter { it.day.isNotBlank() },
            )
        }.getOrNull()
    }

    fun parseGroceryListJson(rawText: String): GroceryListResult? {
        val jsonBlock = extractJsonBlock(rawText) ?: return null
        return runCatching {
            val response = json.decodeFromString<GroceryListLlmResponse>(jsonBlock)
            GroceryListResult(
                categories = response.categories.map { c ->
                    GroceryCategory(
                        name = c.name.orEmpty(),
                        items = c.items.map { i ->
                            GroceryItem(name = i.name.orEmpty(), quantity = i.quantity)
                        }.filter { it.name.isNotBlank() },
                    )
                }.filter { it.name.isNotBlank() },
            )
        }.getOrNull()
    }

    fun formatMealPlan(result: MealPlanResult): String = buildString {
        result.days.forEach { day ->
            appendLine("── ${day.day} ──")
            day.breakfast?.takeIf { it.isNotBlank() }?.let { appendLine("  Breakfast: $it") }
            day.lunch?.takeIf { it.isNotBlank() }?.let { appendLine("  Lunch: $it") }
            day.dinner?.takeIf { it.isNotBlank() }?.let { appendLine("  Dinner: $it") }
            day.note?.takeIf { it.isNotBlank() }?.let { appendLine("  Note: $it") }
            appendLine()
        }
    }.trim()

    fun formatGroceryList(result: GroceryListResult): String = buildString {
        result.categories.forEach { cat ->
            appendLine("▸ ${cat.name}")
            cat.items.forEach { item ->
                val qty = item.quantity?.let { " ($it)" }.orEmpty()
                appendLine("  • ${item.name}$qty")
            }
            appendLine()
        }
    }.trim()

    fun extractJsonBlock(text: String): String? {
        val start = text.indexOf('{')
        if (start < 0) return null
        var depth = 0
        var inString = false
        var escaped = false
        for (index in start until text.length) {
            val char = text[index]
            if (inString) {
                if (escaped) {
                    escaped = false
                } else if (char == '\\') {
                    escaped = true
                } else if (char == '"') {
                    inString = false
                }
                continue
            }
            when (char) {
                '"' -> inString = true
                '{' -> depth += 1
                '}' -> {
                    depth -= 1
                    if (depth == 0) return text.substring(start, index + 1).trim()
                }
            }
        }
        return null
    }

    // Internal serialization models

    @Serializable
    internal data class MealPlanLlmResponse(
        val days: List<MealDayLlm> = emptyList(),
    )

    @Serializable
    internal data class MealDayLlm(
        val day: String? = null,
        val breakfast: String? = null,
        val lunch: String? = null,
        val dinner: String? = null,
        val note: String? = null,
    )

    @Serializable
    internal data class GroceryListLlmResponse(
        val categories: List<GroceryCategoryLlm> = emptyList(),
    )

    @Serializable
    internal data class GroceryCategoryLlm(
        val name: String? = null,
        val items: List<GroceryItemLlm> = emptyList(),
    )

    @Serializable
    internal data class GroceryItemLlm(
        val name: String? = null,
        val quantity: String? = null,
    )
}
