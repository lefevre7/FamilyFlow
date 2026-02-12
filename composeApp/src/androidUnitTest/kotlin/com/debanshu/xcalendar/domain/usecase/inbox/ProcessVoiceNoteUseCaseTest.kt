package com.debanshu.xcalendar.domain.usecase.inbox

import com.debanshu.xcalendar.domain.model.Person
import com.debanshu.xcalendar.domain.model.PersonRole
import com.debanshu.xcalendar.domain.model.VoiceCaptureSource
import com.debanshu.xcalendar.test.TestDependencies
import com.debanshu.xcalendar.test.buildTestDependencies
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProcessVoiceNoteUseCaseTest {

    private lateinit var deps: TestDependencies

    @Before
    fun setUp() {
        stopKoin()
        deps =
            buildTestDependencies(
                people =
                    listOf(
                        Person(
                            id = "person_mom",
                            name = "Mom",
                            role = PersonRole.MOM,
                            color = 0xFFFFFF,
                            isAdmin = true,
                        ),
                    ),
            )
        startKoin { modules(deps.module) }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun processingFallsBackToHeuristicAndLogsDiagnostics() = runTest {
        val useCase: ProcessVoiceNoteUseCase = GlobalContext.get().get()

        val result =
            useCase(
                rawText = "Kid 1 has an appointment today at 8am",
                source = VoiceCaptureSource.TODAY_QUICK_CAPTURE,
                personId = "person_mom",
            )

        assertTrue(result is VoiceNoteProcessResult.Success)
        result as VoiceNoteProcessResult.Success
        assertEquals(1, result.taskCount)
        assertTrue(result.usedHeuristicFallback)
        assertEquals(1, deps.taskRepository.upserted.size)
        assertEquals("Kid 1 has an appointment today at 8am", deps.taskRepository.upserted.first().title)
        assertTrue(deps.inboxRepository.items.any { it.status.name == "PROCESSED" })
        assertTrue(
            deps.voiceDiagnosticsRepository.entriesValue().any { it.step.name == "LLM_UNAVAILABLE" },
        )
        assertTrue(
            deps.voiceDiagnosticsRepository.entriesValue().any { it.step.name == "COMPLETED" },
        )
    }

    @Test
    fun emptyTranscriptReturnsSpecificFailure() = runTest {
        val useCase: ProcessVoiceNoteUseCase = GlobalContext.get().get()

        val result =
            useCase(
                rawText = "   ",
                source = VoiceCaptureSource.QUICK_ADD_VOICE,
                personId = "person_mom",
            )

        assertTrue(result is VoiceNoteProcessResult.Failure)
        result as VoiceNoteProcessResult.Failure
        assertEquals(VoiceNoteFailureReason.EMPTY_TRANSCRIPT, result.reason)
        assertTrue(deps.taskRepository.upserted.isEmpty())
    }
}
