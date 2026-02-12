package com.debanshu.xcalendar.ui.screen.settingsScreen

import com.debanshu.xcalendar.domain.model.VoiceCaptureSource
import com.debanshu.xcalendar.domain.model.VoiceDiagnosticEntry
import com.debanshu.xcalendar.domain.model.VoiceDiagnosticLevel
import com.debanshu.xcalendar.domain.model.VoiceDiagnosticStep
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class VoiceDiagnosticsPayloadTest {

    @Test
    fun buildLatestVoiceDiagnosticPayload_includesOnlyLatestSession() {
        val older =
            VoiceDiagnosticEntry(
                id = "1",
                sessionId = "older",
                timestampMillis = 100L,
                source = VoiceCaptureSource.TODAY_QUICK_CAPTURE,
                step = VoiceDiagnosticStep.CAPTURE_RECEIVED,
                level = VoiceDiagnosticLevel.INFO,
                message = "Older session",
                transcript = "old transcript",
            )
        val latestStart =
            VoiceDiagnosticEntry(
                id = "2",
                sessionId = "latest",
                timestampMillis = 200L,
                source = VoiceCaptureSource.QUICK_ADD_VOICE,
                step = VoiceDiagnosticStep.CAPTURE_RECEIVED,
                level = VoiceDiagnosticLevel.INFO,
                message = "Latest start",
                transcript = "Kid 1 has an appointment today at 8am",
            )
        val latestFinish =
            VoiceDiagnosticEntry(
                id = "3",
                sessionId = "latest",
                timestampMillis = 300L,
                source = VoiceCaptureSource.QUICK_ADD_VOICE,
                step = VoiceDiagnosticStep.COMPLETED,
                level = VoiceDiagnosticLevel.INFO,
                message = "Completed",
                taskCount = 1,
            )

        val payload = buildLatestVoiceDiagnosticPayload(listOf(older, latestStart, latestFinish))

        assertNotNull(payload)
        assertTrue(payload.contains("Session: latest"))
        assertTrue(payload.contains("Kid 1 has an appointment today at 8am"))
        assertTrue(payload.contains("Completed"))
        assertTrue(!payload.contains("Session: older"))
    }
}
