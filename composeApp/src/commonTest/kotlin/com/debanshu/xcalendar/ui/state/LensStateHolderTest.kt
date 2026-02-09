package com.debanshu.xcalendar.ui.state

import com.debanshu.xcalendar.domain.model.FamilyLens
import com.debanshu.xcalendar.domain.model.FamilyLensSelection
import com.debanshu.xcalendar.domain.repository.ILensPreferencesRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals

class LensStateHolderTest {

    private class FakeLensPreferencesRepository(
        initial: FamilyLensSelection,
    ) : ILensPreferencesRepository {
        val backingState = MutableStateFlow(initial)
        val updates = mutableListOf<FamilyLensSelection>()

        override val selection: Flow<FamilyLensSelection> = backingState

        override suspend fun updateSelection(selection: FamilyLensSelection) {
            updates += selection
            backingState.value = selection
        }
    }

    @Test
    fun init_usesPersistedLensSelection() = runBlocking {
        val repository =
            FakeLensPreferencesRepository(
                FamilyLensSelection(
                    lens = FamilyLens.MOM,
                    personId = "person_mom",
                ),
            )
        val holder = LensStateHolder(repository)

        val expected = FamilyLensSelection(lens = FamilyLens.MOM, personId = "person_mom")
        withTimeout(5_000L) {
            while (holder.selection.value != expected) {
                delay(10L)
            }
        }

        assertEquals(expected, holder.selection.value)
    }

    @Test
    fun updateSelection_persistsToRepository() = runBlocking {
        val repository = FakeLensPreferencesRepository(FamilyLensSelection())
        val holder = LensStateHolder(repository)
        val target = FamilyLensSelection(lens = FamilyLens.PERSON, personId = "kid-4")

        holder.updateSelection(target)

        withTimeout(5_000L) {
            while (repository.backingState.value != target) {
                delay(10L)
            }
        }

        assertEquals(target, repository.backingState.value)
        assertEquals(target, repository.updates.last())
    }
}
