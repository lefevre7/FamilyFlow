package com.debanshu.xcalendar.domain.usecase.inbox

import com.debanshu.xcalendar.domain.llm.LlmModelSource
import com.debanshu.xcalendar.domain.llm.LlmModelStatus
import com.debanshu.xcalendar.domain.llm.LlmSamplingConfig
import com.debanshu.xcalendar.domain.llm.LocalLlmManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StructureBrainDumpUseCaseTest {

    private class FakeLocalLlmManager(
        private val available: Boolean,
        private val responses: List<String?> = emptyList(),
    ) : LocalLlmManager {
        private var index = 0
        override val isAvailable: Boolean = available

        override fun getStatus(): LlmModelStatus =
            LlmModelStatus(
                available = available,
                source = if (available) LlmModelSource.LOCAL else LlmModelSource.NONE,
                sizeBytes = 0L,
                requiredBytes = 0L,
            )

        override suspend fun ensureModelAvailable(): Boolean = available

        override suspend fun downloadModel(onProgress: (Int) -> Unit): Boolean = false

        override suspend fun generate(
            prompt: String,
            sampling: LlmSamplingConfig,
        ): String? {
            val response = responses.getOrNull(index)
            index += 1
            return response
        }

        override fun deleteModel(): Boolean = false

        override fun consumeWarningMessage(): String? = null
    }

    @Test
    fun structureWithLlmDiagnostics_extractsJsonFromWrappedResponse() = kotlinx.coroutines.test.runTest {
        val manager =
            FakeLocalLlmManager(
                available = true,
                responses =
                    listOf(
                        """
                        Sure, here is the JSON:
                        ```json
                        {"tasks":[{"title":"Call dentist","priority":"must"}]}
                        ```
                        """.trimIndent(),
                    ),
            )
        val useCase = StructureBrainDumpUseCase(manager)

        val diagnostics = useCase.structureWithLlmDiagnostics("call dentist", retryCount = 0)

        assertTrue(diagnostics.llmAvailable)
        assertEquals(1, diagnostics.attempts.size)
        val structured = diagnostics.structured
        assertNotNull(structured)
        assertEquals(1, structured.tasks.size)
        assertEquals("Call dentist", structured.tasks.first().title)
        assertEquals(null, diagnostics.attempts.first().failureReason)
        assertTrue(diagnostics.attempts.first().extractedJson?.startsWith("{") == true)
    }

    @Test
    fun structureWithLlmDiagnostics_marksMissingJsonBlock() = kotlinx.coroutines.test.runTest {
        val manager = FakeLocalLlmManager(available = true, responses = listOf("no object here"))
        val useCase = StructureBrainDumpUseCase(manager)

        val diagnostics = useCase.structureWithLlmDiagnostics("buy milk", retryCount = 0)

        assertTrue(diagnostics.llmAvailable)
        assertNull(diagnostics.structured)
        assertEquals(BrainDumpLlmFailureReason.JSON_BLOCK_NOT_FOUND, diagnostics.attempts.first().failureReason)
    }

    @Test
    fun invoke_fallsBackToHeuristicWhenLlmUnavailable() = kotlinx.coroutines.test.runTest {
        val manager = FakeLocalLlmManager(available = false)
        val useCase = StructureBrainDumpUseCase(manager)

        val structured = useCase("Kid 1 has appointment today at 8am")

        assertEquals(1, structured.tasks.size)
        assertEquals("Kid 1 has appointment today at 8am", structured.tasks.first().title)
    }
}
