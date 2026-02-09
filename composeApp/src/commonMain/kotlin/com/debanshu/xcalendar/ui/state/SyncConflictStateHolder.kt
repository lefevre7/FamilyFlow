package com.debanshu.xcalendar.ui.state

import com.debanshu.xcalendar.domain.sync.SyncConflict
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.koin.core.annotation.Single

@Single
class SyncConflictStateHolder {
    private val _conflicts = MutableStateFlow<List<SyncConflict>>(emptyList())
    val conflicts: StateFlow<List<SyncConflict>> = _conflicts

    fun setConflicts(conflicts: List<SyncConflict>) {
        _conflicts.value = conflicts
    }

    fun resolveConflict(conflict: SyncConflict) {
        _conflicts.update { current -> current.filterNot { it == conflict } }
    }

    fun clear() {
        _conflicts.value = emptyList()
    }
}
