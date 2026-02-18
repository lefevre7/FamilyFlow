package com.debanshu.xcalendar.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.core.annotation.Single

@Single(binds = [IUiPreferencesRepository::class])
class UiPreferencesRepository : IUiPreferencesRepository {
    private val _navDragHintDismissed = MutableStateFlow(false)

    override val navDragHintDismissed: Flow<Boolean> = _navDragHintDismissed

    override suspend fun setNavDragHintDismissed(dismissed: Boolean) {
        _navDragHintDismissed.value = dismissed
    }
}
