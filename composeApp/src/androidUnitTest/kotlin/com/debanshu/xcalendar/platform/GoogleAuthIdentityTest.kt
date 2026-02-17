package com.debanshu.xcalendar.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GoogleAuthIdentityTest {

    @Test
    fun resolveStableAccountId_acceptsTrustedIssuer() {
        val accountId = resolveStableAccountId("subject-123", "https://accounts.google.com")
        assertEquals("subject-123", accountId)
    }

    @Test
    fun resolveStableAccountId_rejectsMissingSubject() {
        val accountId = resolveStableAccountId(null, "https://accounts.google.com")
        assertNull(accountId)
    }

    @Test
    fun resolveStableAccountId_rejectsUntrustedIssuer() {
        val accountId = resolveStableAccountId("subject-123", "https://malicious.example.com")
        assertNull(accountId)
    }
}
