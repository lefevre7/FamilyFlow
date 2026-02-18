package com.debanshu.xcalendar.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Persists one-time UI hint / onboarding-tip dismissed flags so they are never re-shown
 * after the user explicitly dismisses them, even across full app restarts.
 *
 * Add new hint flags here as the product grows (e.g. weekFilterTipDismissed).
 */
interface IUiPreferencesRepository {
    /** True once the user has dismissed (or interacted with) the nav-dock drag hint. */
    val navDragHintDismissed: Flow<Boolean>

    suspend fun setNavDragHintDismissed(dismissed: Boolean)
}
