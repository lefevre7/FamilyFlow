package com.debanshu.xcalendar.ui.state

import com.debanshu.xcalendar.domain.model.FamilyLens
import com.debanshu.xcalendar.domain.model.FamilyLensSelection
import com.debanshu.xcalendar.domain.repository.ILensPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single

@Single
class LensStateHolder(
    private val repository: ILensPreferencesRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _selection = MutableStateFlow(FamilyLensSelection())
    val selection: StateFlow<FamilyLensSelection> = _selection

    init {
        scope.launch {
            repository.selection.collectLatest { persisted ->
                _selection.value = persisted
            }
        }
    }

    fun selectFamily() {
        updateSelection(FamilyLensSelection(lens = FamilyLens.FAMILY, personId = _selection.value.personId))
    }

    fun selectMom() {
        updateSelection(FamilyLensSelection(lens = FamilyLens.MOM, personId = _selection.value.personId))
    }

    fun selectPerson(personId: String?) {
        updateSelection(FamilyLensSelection(lens = FamilyLens.PERSON, personId = personId))
    }

    fun updateSelection(next: FamilyLensSelection) {
        _selection.update { next }
        scope.launch {
            repository.updateSelection(next)
        }
    }
}
