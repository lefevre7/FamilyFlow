package com.debanshu.xcalendar.domain.usecase.kitchen

import com.debanshu.xcalendar.domain.llm.KitchenPlannerLlmSchema
import com.debanshu.xcalendar.domain.llm.LlmSamplingConfig
import com.debanshu.xcalendar.domain.llm.LocalLlmManager
import com.debanshu.xcalendar.domain.model.GroceryListResult
import org.koin.core.annotation.Factory

/**
 * Generates a categorized grocery list from a meal plan.
 * Returns human-readable formatted text, or null if the LLM is unavailable, the meal plan is
 * blank, or generation fails.
 * Use [generateStructured] to obtain the parsed [GroceryListResult] domain model.
 */
@Factory
class GenerateGroceryListUseCase(
    private val localLlmManager: LocalLlmManager,
) {
    suspend operator fun invoke(
        mealPlanText: String,
        dietaryNotes: String,
    ): String? {
        if (!localLlmManager.isAvailable || mealPlanText.isBlank()) return null
        val prompt = KitchenPlannerLlmSchema.buildGroceryListPrompt(mealPlanText, dietaryNotes)
        val rawResponse = runCatching {
            localLlmManager.generate(
                prompt = prompt,
                sampling = LlmSamplingConfig(topK = 40, topP = 0.9, temperature = 0.3),
            )
        }.getOrNull() ?: return null

        val parsed = KitchenPlannerLlmSchema.parseGroceryListJson(rawResponse)
        return when {
            parsed != null && parsed.categories.isNotEmpty() -> KitchenPlannerLlmSchema.formatGroceryList(parsed)
            // Parsing failed — don't expose raw JSON to the user; caller will show a retry message
            else -> null
        }
    }

    suspend fun generateStructured(
        mealPlanText: String,
        dietaryNotes: String,
    ): GroceryListResult? {
        if (!localLlmManager.isAvailable || mealPlanText.isBlank()) return null
        val prompt = KitchenPlannerLlmSchema.buildGroceryListPrompt(mealPlanText, dietaryNotes)
        val rawResponse = runCatching {
            localLlmManager.generate(
                prompt = prompt,
                sampling = LlmSamplingConfig(topK = 40, topP = 0.9, temperature = 0.3),
            )
        }.getOrNull() ?: return null
        return KitchenPlannerLlmSchema.parseGroceryListJson(rawResponse)
    }
}
