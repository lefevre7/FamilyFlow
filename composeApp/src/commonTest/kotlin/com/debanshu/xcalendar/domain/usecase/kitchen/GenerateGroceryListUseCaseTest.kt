package com.debanshu.xcalendar.domain.usecase.kitchen

import com.debanshu.xcalendar.domain.llm.LlmModelSource
import com.debanshu.xcalendar.domain.llm.LlmModelStatus
import com.debanshu.xcalendar.domain.llm.LlmSamplingConfig
import com.debanshu.xcalendar.domain.llm.LocalLlmManager
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GenerateGroceryListUseCaseTest {

    private class FakeLlmManager(
        private val available: Boolean,
        private val responses: List<String?> = emptyList(),
    ) : LocalLlmManager {
        private var index = 0
        override val isAvailable: Boolean = available

        override fun getStatus() = LlmModelStatus(
            available = available,
            source = if (available) LlmModelSource.LOCAL else LlmModelSource.NONE,
            sizeBytes = 0L,
            requiredBytes = 0L,
        )

        override suspend fun ensureModelAvailable() = available
        override suspend fun downloadModel(onProgress: (Int) -> Unit) = false
        override suspend fun generate(prompt: String, sampling: LlmSamplingConfig): String? {
            val r = responses.getOrNull(index); index++; return r
        }
        override fun deleteModel() = false
        override fun consumeWarningMessage(): String? = null
    }

    @Test
    fun returnsNull_whenLlmUnavailable() = runTest {
        val useCase = GenerateGroceryListUseCase(FakeLlmManager(available = false))
        assertNull(useCase("Monday: Pasta for dinner", ""))
    }

    @Test
    fun returnsNull_whenMealPlanIsBlank() = runTest {
        val useCase = GenerateGroceryListUseCase(FakeLlmManager(available = true, responses = listOf("{}")))
        assertNull(useCase("   ", ""))
    }

    @Test
    fun returnsFormattedGroceryList_whenLlmReturnsValidJson() = runTest {
        val validJson = """
        {
          "categories": [
            {"name": "Produce", "items": [{"name": "spinach", "quantity": "1 bag"}, {"name": "tomatoes", "quantity": "6"}]},
            {"name": "Protein", "items": [{"name": "chicken breast", "quantity": "2 lbs"}]}
          ]
        }
        """.trimIndent()
        val useCase = GenerateGroceryListUseCase(FakeLlmManager(available = true, responses = listOf(validJson)))
        val result = useCase("Monday: Chicken Salad", "")
        assertNotNull(result)
        assertTrue(result.contains("Produce"), "Expected 'Produce' in result: $result")
        assertTrue(result.contains("spinach"), "Expected 'spinach' in result: $result")
        assertTrue(result.contains("Protein"), "Expected 'Protein' in result: $result")
        assertTrue(result.contains("1 bag"), "Expected quantity in result: $result")
    }

    @Test
    fun returnsNull_whenJsonParseFails() = runTest {
        // Unparseable LLM output must never be shown raw to the user
        val rawText = "Produce: apples, bananas\nDairy: milk, cheese"
        val useCase = GenerateGroceryListUseCase(FakeLlmManager(available = true, responses = listOf(rawText)))
        val result = useCase("Breakfast: fruit salad, Lunch: cheese sandwich", "")
        assertNull(result)
    }

    @Test
    fun returnsNull_whenLlmReturnsNull() = runTest {
        val useCase = GenerateGroceryListUseCase(FakeLlmManager(available = true, responses = listOf(null)))
        assertNull(useCase("Monday: soup", ""))
    }

    @Test
    fun generateStructured_parsesCategoriesCorrectly() = runTest {
        val json = """{"categories":[{"name":"Dairy","items":[{"name":"milk","quantity":"1 gallon"},{"name":"eggs","quantity":"12"}]}]}"""
        val useCase = GenerateGroceryListUseCase(FakeLlmManager(available = true, responses = listOf(json)))
        val result = useCase.generateStructured("cereal and omelettes all week", "")
        assertNotNull(result)
        assertTrue(result.categories.isNotEmpty())
        val dairy = result.categories.first { it.name == "Dairy" }
        assertTrue(dairy.items.any { it.name == "milk" })
        assertTrue(dairy.items.any { it.quantity == "12" })
    }

    @Test
    fun handlesItemsWithNoQuantity() = runTest {
        val json = """{"categories":[{"name":"Pantry","items":[{"name":"olive oil"},{"name":"salt"}]}]}"""
        val useCase = GenerateGroceryListUseCase(FakeLlmManager(available = true, responses = listOf(json)))
        val result = useCase.generateStructured("salad dressing recipe", "")
        assertNotNull(result)
        val pantry = result.categories.firstOrNull { it.name == "Pantry" }
        assertNotNull(pantry)
        assertTrue(pantry.items.all { it.quantity == null })
    }
}
