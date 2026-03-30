package com.debanshu.xcalendar.domain.usecase.kitchen

import com.debanshu.xcalendar.domain.llm.KitchenPlannerLlmSchema
import com.debanshu.xcalendar.domain.llm.LlmSamplingConfig
import com.debanshu.xcalendar.domain.llm.LocalLlmManager
import com.debanshu.xcalendar.domain.model.MealPlanResult
import org.koin.core.annotation.Factory

/**
 * Generates a 7-day meal plan from a grocery list (or from scratch if list is empty).
 * Returns human-readable formatted text, or null if the LLM is unavailable or fails.
 * Use [generateStructured] to obtain the parsed [MealPlanResult] domain model.
 */
@Factory
class GenerateMealPlanUseCase(
    private val localLlmManager: LocalLlmManager,
) {
    suspend operator fun invoke(
        groceryListText: String,
        dietaryNotes: String,
    ): String? {
        if (!localLlmManager.isAvailable) return null
        val prompt = KitchenPlannerLlmSchema.buildMealPlanPrompt(groceryListText, dietaryNotes)
        val rawResponse = runCatching {
            localLlmManager.generate(
                prompt = prompt,
                sampling = LlmSamplingConfig(topK = 40, topP = 0.9, temperature = 0.4),
            )
        }.getOrNull() ?: return null

        val parsed = KitchenPlannerLlmSchema.parseMealPlanJson(rawResponse)
        return when {
            parsed != null && parsed.days.isNotEmpty() -> KitchenPlannerLlmSchema.formatMealPlan(parsed)
            // Parsing failed — don't expose raw JSON/schema output to the user
            else -> null
        }
    }

    suspend fun generateStructured(
        groceryListText: String,
        dietaryNotes: String,
    ): MealPlanResult? {
        if (!localLlmManager.isAvailable) return null
        val prompt = KitchenPlannerLlmSchema.buildMealPlanPrompt(groceryListText, dietaryNotes)
        val rawResponse = runCatching {
            localLlmManager.generate(
                prompt = prompt,
                sampling = LlmSamplingConfig(topK = 40, topP = 0.9, temperature = 0.4),
            )
        }.getOrNull() ?: return null
        return KitchenPlannerLlmSchema.parseMealPlanJson(rawResponse)
    }
}
