package com.debanshu.xcalendar.ui

import com.debanshu.xcalendar.domain.model.GoogleAccountLink
import com.debanshu.xcalendar.domain.model.User
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for [deriveAccounts], the pure function that maps Google account
 * email / display-name data onto the [User] list shown in the event editor.
 */
class CalendarAccountsDerivationTest {

    // ──────────────────── helpers ────────────────────

    private fun makeUser(
        id: String = "user_id",
        name: String = "Demo User",
        email: String = "user@example.com",
        photoUrl: String = "https://example.com/photo.jpg",
    ) = User(id = id, name = name, email = email, photoUrl = photoUrl)

    private fun makeGoogleAccount(
        id: String = "google-account-1",
        email: String = "real@gmail.com",
        displayName: String? = "Real Name",
        personId: String = "person-mom",
    ) = GoogleAccountLink(
        id = id,
        email = email,
        displayName = displayName,
        personId = personId,
    )

    // ──────────────────── tests ────────────────────

    @Test
    fun `returns users unchanged when no Google accounts are connected`() {
        val users = listOf(makeUser(email = "user@example.com"))

        val result = deriveAccounts(users, emptyList())

        assertEquals(users, result)
    }

    @Test
    fun `replaces user email with Google account email when connected`() {
        val users = listOf(makeUser(email = "user@example.com"))
        val googleAccounts = listOf(makeGoogleAccount(email = "mom@gmail.com"))

        val result = deriveAccounts(users, googleAccounts)

        assertEquals(1, result.size)
        assertEquals("mom@gmail.com", result.first().email)
    }

    @Test
    fun `uses displayName from Google account as user name when available`() {
        val users = listOf(makeUser(name = "Demo User"))
        val googleAccounts = listOf(makeGoogleAccount(displayName = "Jane Smith"))

        val result = deriveAccounts(users, googleAccounts)

        assertEquals("Jane Smith", result.first().name)
    }

    @Test
    fun `falls back to existing user name when Google displayName is null`() {
        val users = listOf(makeUser(name = "Mom"))
        val googleAccounts = listOf(makeGoogleAccount(displayName = null))

        val result = deriveAccounts(users, googleAccounts)

        assertEquals("Mom", result.first().name)
    }

    @Test
    fun `preserves photoUrl from existing local user`() {
        val photoUrl = "https://lh3.googleusercontent.com/photo.jpg"
        val users = listOf(makeUser(photoUrl = photoUrl))
        val googleAccounts = listOf(makeGoogleAccount())

        val result = deriveAccounts(users, googleAccounts)

        assertEquals(photoUrl, result.first().photoUrl)
    }

    @Test
    fun `preserves existing user id`() {
        val users = listOf(makeUser(id = "user_id"))
        val googleAccounts = listOf(makeGoogleAccount())

        val result = deriveAccounts(users, googleAccounts)

        assertEquals("user_id", result.first().id)
    }

    @Test
    fun `returns one entry per Google account when multiple accounts are connected`() {
        val users = listOf(makeUser())
        val googleAccounts = listOf(
            makeGoogleAccount(id = "g-1", email = "mom@gmail.com"),
            makeGoogleAccount(id = "g-2", email = "work@gmail.com"),
        )

        val result = deriveAccounts(users, googleAccounts)

        assertEquals(2, result.size)
        assertEquals("mom@gmail.com", result[0].email)
        assertEquals("work@gmail.com", result[1].email)
    }

    @Test
    fun `handles empty user list when a Google account is connected`() {
        val googleAccounts = listOf(makeGoogleAccount(email = "mom@gmail.com"))

        val result = deriveAccounts(emptyList(), googleAccounts)

        assertEquals(1, result.size)
        assertEquals("mom@gmail.com", result.first().email)
        // Should not crash; falls back to sensible defaults
        assertTrue(result.first().id.isNotEmpty())
    }

    @Test
    fun `returns empty list when both users and Google accounts are empty`() {
        val result = deriveAccounts(emptyList(), emptyList())

        assertTrue(result.isEmpty())
    }
}
