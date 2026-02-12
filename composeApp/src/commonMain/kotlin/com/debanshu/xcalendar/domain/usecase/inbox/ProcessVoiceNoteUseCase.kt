package com.debanshu.xcalendar.domain.usecase.inbox

import com.debanshu.xcalendar.common.AppLogger
import com.debanshu.xcalendar.domain.model.BrainDumpStructuredResult
import com.debanshu.xcalendar.domain.model.InboxItem
import com.debanshu.xcalendar.domain.model.InboxSource
import com.debanshu.xcalendar.domain.model.InboxStatus
import com.debanshu.xcalendar.domain.model.Task
import com.debanshu.xcalendar.domain.model.TaskEnergy
import com.debanshu.xcalendar.domain.model.TaskPriority
import com.debanshu.xcalendar.domain.model.TaskType
import com.debanshu.xcalendar.domain.model.VoiceCaptureSource
import com.debanshu.xcalendar.domain.model.VoiceDiagnosticEntry
import com.debanshu.xcalendar.domain.model.VoiceDiagnosticLevel
import com.debanshu.xcalendar.domain.model.VoiceDiagnosticStep
import com.debanshu.xcalendar.domain.repository.IVoiceDiagnosticsRepository
import com.debanshu.xcalendar.domain.usecase.task.CreateTaskUseCase
import com.debanshu.xcalendar.domain.util.BrainDumpStructuringEngine
import org.koin.core.annotation.Factory
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

enum class VoiceNoteFailureReason(val userMessage: String) {
    EMPTY_TRANSCRIPT("No speech captured."),
    NO_TASKS_EXTRACTED("No tasks could be extracted from that note. Saved to Brain Dump inbox."),
    TASK_SAVE_FAILED("Failed while saving tasks from this voice note. Saved to Brain Dump inbox."),
}

sealed class VoiceNoteProcessResult {
    data class Success(
        val sessionId: String,
        val taskCount: Int,
        val usedHeuristicFallback: Boolean,
    ) : VoiceNoteProcessResult()

    data class Failure(
        val sessionId: String,
        val reason: VoiceNoteFailureReason,
    ) : VoiceNoteProcessResult()
}

@Factory
class ProcessVoiceNoteUseCase(
    private val createTaskUseCase: CreateTaskUseCase,
    private val createInboxItemUseCase: CreateInboxItemUseCase,
    private val structureBrainDumpUseCase: StructureBrainDumpUseCase,
    private val voiceDiagnosticsRepository: IVoiceDiagnosticsRepository,
) {
    suspend operator fun invoke(
        rawText: String,
        source: VoiceCaptureSource,
        personId: String?,
    ): VoiceNoteProcessResult {
        val transcript = rawText.trim()
        val sessionId = "voice-${System.currentTimeMillis()}-${System.nanoTime()}"
        if (transcript.isEmpty()) {
            log(
                sessionId = sessionId,
                source = source,
                step = VoiceDiagnosticStep.FAILED,
                level = VoiceDiagnosticLevel.WARN,
                message = "Capture returned empty transcript.",
            )
            return VoiceNoteProcessResult.Failure(
                sessionId = sessionId,
                reason = VoiceNoteFailureReason.EMPTY_TRANSCRIPT,
            )
        }

        log(
            sessionId = sessionId,
            source = source,
            step = VoiceDiagnosticStep.CAPTURE_RECEIVED,
            message = "Voice transcript captured.",
            transcript = transcript,
        )

        val llmDiagnostics = structureBrainDumpUseCase.structureWithLlmDiagnostics(transcript, retryCount = 2)
        if (!llmDiagnostics.llmAvailable) {
            log(
                sessionId = sessionId,
                source = source,
                step = VoiceDiagnosticStep.LLM_UNAVAILABLE,
                level = VoiceDiagnosticLevel.WARN,
                message = "Local LLM unavailable. Using heuristic fallback.",
                transcript = transcript,
            )
        }

        llmDiagnostics.attempts.forEach { attempt ->
            log(
                sessionId = sessionId,
                source = source,
                step = VoiceDiagnosticStep.LLM_ATTEMPT_STARTED,
                message = "LLM attempt ${attempt.attemptIndex} started.",
                attemptIndex = attempt.attemptIndex,
            )
            log(
                sessionId = sessionId,
                source = source,
                step = VoiceDiagnosticStep.LLM_PROMPT_BUILT,
                message = "Built LLM prompt for attempt ${attempt.attemptIndex}.",
                attemptIndex = attempt.attemptIndex,
                llmPrompt = attempt.prompt,
            )
            log(
                sessionId = sessionId,
                source = source,
                step = VoiceDiagnosticStep.LLM_RESPONSE_RECEIVED,
                level = if (attempt.rawResponse.isNullOrBlank()) VoiceDiagnosticLevel.WARN else VoiceDiagnosticLevel.INFO,
                message = "Received LLM response for attempt ${attempt.attemptIndex}.",
                attemptIndex = attempt.attemptIndex,
                llmRawResponse = attempt.rawResponse,
                errorMessage = attempt.errorMessage,
            )
            log(
                sessionId = sessionId,
                source = source,
                step = VoiceDiagnosticStep.LLM_JSON_EXTRACTED,
                level = if (attempt.extractedJson.isNullOrBlank()) VoiceDiagnosticLevel.WARN else VoiceDiagnosticLevel.INFO,
                message = "Processed JSON extraction for attempt ${attempt.attemptIndex}.",
                attemptIndex = attempt.attemptIndex,
                llmExtractedJson = attempt.extractedJson,
            )
            when (attempt.failureReason) {
                null -> {
                    log(
                        sessionId = sessionId,
                        source = source,
                        step = VoiceDiagnosticStep.LLM_PARSE_SUCCESS,
                        message = "LLM parse succeeded for attempt ${attempt.attemptIndex}.",
                        attemptIndex = attempt.attemptIndex,
                        taskCount = attempt.tasksExtracted,
                    )
                }
                BrainDumpLlmFailureReason.EMPTY_TASKS -> {
                    log(
                        sessionId = sessionId,
                        source = source,
                        step = VoiceDiagnosticStep.LLM_PARSE_EMPTY,
                        level = VoiceDiagnosticLevel.WARN,
                        message = "LLM parsed JSON but extracted no tasks on attempt ${attempt.attemptIndex}.",
                        attemptIndex = attempt.attemptIndex,
                    )
                }
                else -> {
                    log(
                        sessionId = sessionId,
                        source = source,
                        step = VoiceDiagnosticStep.LLM_PARSE_FAILED,
                        level = VoiceDiagnosticLevel.WARN,
                        message =
                            "LLM parse failed on attempt ${attempt.attemptIndex}: ${attempt.failureReason.name}.",
                        attemptIndex = attempt.attemptIndex,
                        errorMessage = attempt.errorMessage,
                    )
                }
            }
        }

        val structured = llmDiagnostics.structured
        val usedHeuristicFallback = structured == null
        if (usedHeuristicFallback) {
            log(
                sessionId = sessionId,
                source = source,
                step = VoiceDiagnosticStep.HEURISTIC_FALLBACK_USED,
                level = VoiceDiagnosticLevel.WARN,
                message = "Falling back to heuristic structuring.",
                transcript = transcript,
            )
        }
        val finalStructured = structured ?: BrainDumpStructuringEngine.structure(transcript)

        if (finalStructured.tasks.isEmpty()) {
            saveInboxAudit(
                sessionId = sessionId,
                source = source,
                transcript = transcript,
                personId = personId,
                status = InboxStatus.NEW,
            )
            log(
                sessionId = sessionId,
                source = source,
                step = VoiceDiagnosticStep.FAILED,
                level = VoiceDiagnosticLevel.ERROR,
                message = "No tasks extracted after LLM and heuristic pass.",
            )
            return VoiceNoteProcessResult.Failure(
                sessionId = sessionId,
                reason = VoiceNoteFailureReason.NO_TASKS_EXTRACTED,
            )
        }

        return runCatching {
            val now = System.currentTimeMillis()
            saveTasks(
                structured = finalStructured,
                personId = personId,
                now = now,
            )
            log(
                sessionId = sessionId,
                source = source,
                step = VoiceDiagnosticStep.TASKS_SAVED,
                message = "Saved ${finalStructured.tasks.size} task(s) from voice note.",
                taskCount = finalStructured.tasks.size,
            )
            saveInboxAudit(
                sessionId = sessionId,
                source = source,
                transcript = transcript,
                personId = personId,
                status = InboxStatus.PROCESSED,
                now = now,
            )
            log(
                sessionId = sessionId,
                source = source,
                step = VoiceDiagnosticStep.COMPLETED,
                message = "Voice processing completed successfully.",
                taskCount = finalStructured.tasks.size,
            )
            VoiceNoteProcessResult.Success(
                sessionId = sessionId,
                taskCount = finalStructured.tasks.size,
                usedHeuristicFallback = usedHeuristicFallback,
            )
        }.getOrElse { error ->
            AppLogger.e(error) {
                "Voice note task save failed for session $sessionId: ${error.message}"
            }
            saveInboxAudit(
                sessionId = sessionId,
                source = source,
                transcript = transcript,
                personId = personId,
                status = InboxStatus.NEW,
            )
            log(
                sessionId = sessionId,
                source = source,
                step = VoiceDiagnosticStep.FAILED,
                level = VoiceDiagnosticLevel.ERROR,
                message = "Failed to save extracted tasks.",
                errorMessage = error.message,
            )
            VoiceNoteProcessResult.Failure(
                sessionId = sessionId,
                reason = VoiceNoteFailureReason.TASK_SAVE_FAILED,
            )
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun saveTasks(
        structured: BrainDumpStructuredResult,
        personId: String?,
        now: Long,
    ) {
        structured.tasks.forEach { draft ->
            createTaskUseCase(
                Task(
                    id = Uuid.random().toString(),
                    title = draft.title,
                    notes = draft.notes,
                    priority = draft.priority ?: TaskPriority.SHOULD,
                    energy = draft.energy ?: TaskEnergy.MEDIUM,
                    type = TaskType.FLEXIBLE,
                    assignedToPersonId = personId,
                    affectedPersonIds = personId?.let { listOf(it) } ?: emptyList(),
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun saveInboxAudit(
        sessionId: String,
        source: VoiceCaptureSource,
        transcript: String,
        personId: String?,
        status: InboxStatus,
        now: Long = System.currentTimeMillis(),
    ) {
        createInboxItemUseCase(
            InboxItem(
                id = Uuid.random().toString(),
                rawText = transcript,
                source = InboxSource.VOICE,
                status = status,
                createdAt = now,
                personId = personId,
            ),
        )
        log(
            sessionId = sessionId,
            source = source,
            step = VoiceDiagnosticStep.INBOX_AUDIT_SAVED,
            message = "Saved inbox audit item with status ${status.name}.",
        )
    }

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun log(
        sessionId: String,
        source: VoiceCaptureSource,
        step: VoiceDiagnosticStep,
        message: String,
        level: VoiceDiagnosticLevel = VoiceDiagnosticLevel.INFO,
        attemptIndex: Int? = null,
        taskCount: Int? = null,
        transcript: String? = null,
        llmPrompt: String? = null,
        llmRawResponse: String? = null,
        llmExtractedJson: String? = null,
        errorMessage: String? = null,
    ) {
        if (!voiceDiagnosticsRepository.isDiagnosticsEnabled()) return
        voiceDiagnosticsRepository.append(
            VoiceDiagnosticEntry(
                id = Uuid.random().toString(),
                sessionId = sessionId,
                timestampMillis = System.currentTimeMillis(),
                source = source,
                step = step,
                level = level,
                message = message,
                attemptIndex = attemptIndex,
                taskCount = taskCount,
                transcript = transcript,
                llmPrompt = llmPrompt,
                llmRawResponse = llmRawResponse,
                llmExtractedJson = llmExtractedJson,
                errorMessage = errorMessage,
            ),
        )
    }
}
