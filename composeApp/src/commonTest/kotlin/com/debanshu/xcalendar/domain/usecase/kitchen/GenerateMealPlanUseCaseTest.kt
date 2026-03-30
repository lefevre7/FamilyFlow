package com.debanshu.xcalendar.domain.usecase.kitchen

import com.debanshu.xcalendar.domain.llm.LlmModelSource
import com.debanshu.xcalendar.domain.llm.LlmModelStatus
import com.debanshu.xcalendar.domain.llm.LlmSamplingConfig
import com.debanshu.xcalendar.domain.llm.LocalLlmManager
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GenerateMealPlanUseCaseTest {

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
        val useCase = GenerateMealPlanUseCase(FakeLlmManager(available = false))
        assertNull(useCase("milk, eggs, pasta", ""))
    }

    @Test
    fun returnsFormattedMealPlan_whenLlmReturnsValidJson() = runTest {
        val validJson = """
        {
          "days": [
            {"day": "Monday", "breakfast": "Oatmeal", "lunch": "Salad", "dinner": "Pasta"},
            {"day": "Tuesday", "breakfast": "Eggs", "lunch": "Soup", "dinner": "Stir fry"}
          ]
        }
        """.trimIndent()
        val useCase = GenerateMealPlanUseCase(FakeLlmManager(available = true, responses = listOf(validJson)))
        val result = useCase("milk, eggs, pasta", "")
        assertNotNull(result)
        assertTrue(result.contains("Monday"), "Expected 'Monday' in result: $result")
        assertTrue(result.contains("Pasta"), "Expected 'Pasta' in result: $result")
        assertTrue(result.contains("Tuesday"), "Expected 'Tuesday' in result: $result")
    }

    @Test
    fun returnsNull_whenJsonParseFails() = runTest {
        // Unparseable LLM output must never be shown raw to the user
        val rawText = "Monday: Oatmeal/Salad/Chicken\nTuesday: Eggs/Soup/Rice"
        val useCase = GenerateMealPlanUseCase(FakeLlmManager(available = true, responses = listOf(rawText)))
        val result = useCase("misc items", "")
        assertNull(result)
    }

    @Test
    fun returnsNull_whenLlmReturnsNull() = runTest {
        val useCase = GenerateMealPlanUseCase(FakeLlmManager(available = true, responses = listOf(null)))
        assertNull(useCase("items", ""))
    }

    @Test
    fun returnsNull_whenLlmReturnsEmptyString() = runTest {
        val useCase = GenerateMealPlanUseCase(FakeLlmManager(available = true, responses = listOf("")))
        assertNull(useCase("items", ""))
    }

    @Test
    fun generateStructured_parsesDaysCorrectly() = runTest {
        val json = """{"days":[{"day":"Monday","breakfast":"Oats","lunch":"Wrap","dinner":"Soup","note":"Quick meals"}]}"""
        val useCase = GenerateMealPlanUseCase(FakeLlmManager(available = true, responses = listOf(json)))
        val result = useCase.generateStructured("", "")
        assertNotNull(result)
        assertEquals(1, result.days.size)
        assertEquals("Monday", result.days[0].day)
        assertEquals("Quick meals", result.days[0].note)
    }

    @Test
    fun worksWithEmptyGroceryList_generatesGeneralMealPlan() = runTest {
        val json = """{"days":[{"day":"Monday","breakfast":"Cereal","lunch":"Sandwich","dinner":"Pasta"}]}"""
        val useCase = GenerateMealPlanUseCase(FakeLlmManager(available = true, responses = listOf(json)))
        val result = useCase("", "nut-free")
        assertNotNull(result)
        assertTrue(result.contains("Monday"))
    }

    @Test
    fun returnsFormattedText_whenJsonIsWrappedInMarkdown() = runTest {
        val wrapped = """
        Here is your meal plan:
        ```json
        {"days":[{"day":"Monday","breakfast":"Toast","lunch":"Salad","dinner":"Rice"}]}
        ```
        """.trimIndent()
        val useCase = GenerateMealPlanUseCase(FakeLlmManager(available = true, responses = listOf(wrapped)))
        val result = useCase("bread, lettuce, rice", "")
        assertNotNull(result)
        assertTrue(result.contains("Monday"))
    }
}
