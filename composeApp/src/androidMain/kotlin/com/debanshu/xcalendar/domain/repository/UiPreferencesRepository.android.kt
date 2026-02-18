package com.debanshu.xcalendar.domain.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single
import org.koin.mp.KoinPlatform

private val Context.uiPreferencesDataStore by preferencesDataStore(name = "ui_preferences")

@Single(binds = [IUiPreferencesRepository::class])
class UiPreferencesRepository : IUiPreferencesRepository {
    private val context: Context by lazy { KoinPlatform.getKoin().get() }
    private val dataStore by lazy { context.uiPreferencesDataStore }

    override val navDragHintDismissed: Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[NAV_DRAG_HINT_DISMISSED_KEY] ?: false }

    override suspend fun setNavDragHintDismissed(dismissed: Boolean) {
        dataStore.edit { prefs ->
            prefs[NAV_DRAG_HINT_DISMISSED_KEY] = dismissed
        }
    }

    private companion object {
        val NAV_DRAG_HINT_DISMISSED_KEY = booleanPreferencesKey("nav_drag_hint_dismissed")
    }
}
