package com.debanshu.xcalendar.ui

import android.app.Application
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.debanshu.xcalendar.domain.model.FamilyLens
import com.debanshu.xcalendar.domain.model.FamilyLensSelection
import com.debanshu.xcalendar.domain.model.Person
import com.debanshu.xcalendar.domain.model.PersonRole
import com.debanshu.xcalendar.domain.repository.ILensPreferencesRepository
import com.debanshu.xcalendar.ui.components.FamilyLensMiniHeader
import com.debanshu.xcalendar.ui.state.LensStateHolder
import com.debanshu.xcalendar.ui.theme.XCalendarTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    application = Application::class,
)
class FamilyLensMiniHeaderPersistenceTest {

    @get:Rule
    val composeRule = createIntentComposeRule<TestActivity>()

    private class FakeLensPreferencesRepository(
        initial: FamilyLensSelection,
    ) : ILensPreferencesRepository {
        val state = MutableStateFlow(initial)
        override val selection: Flow<FamilyLensSelection> = state

        override suspend fun updateSelection(selection: FamilyLensSelection) {
            state.value = selection
        }
    }

    @Test
    fun selectedPersonLens_persistsAcrossHolderRecreation() {
        val people =
            listOf(
                Person(
                    id = "person_mom",
                    name = "Mom",
                    role = PersonRole.MOM,
                    color = 0,
                    isAdmin = true,
                ),
                Person(
                    id = "person_kid_a",
                    name = "Kid A",
                    role = PersonRole.CHILD,
                    color = 0,
                ),
                Person(
                    id = "person_kid_b",
                    name = "Kid B",
                    role = PersonRole.CHILD,
                    color = 0,
                ),
            )
        val repository =
            FakeLensPreferencesRepository(
                FamilyLensSelection(
                    lens = FamilyLens.PERSON,
                    personId = "person_kid_a",
                ),
            )
        val firstHolder = LensStateHolder(repository)

        composeRule.setContent {
            val selection by firstHolder.selection.collectAsState()
            XCalendarTheme {
                FamilyLensMiniHeader(
                    selection = selection,
                    people = people,
                    onSelectionChange = firstHolder::updateSelection,
                )
            }
        }

        composeRule.onNodeWithText("Kid B").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            repository.state.value.personId == "person_kid_b"
        }
        assertEquals(FamilyLens.PERSON, repository.state.value.lens)
        assertEquals("person_kid_b", repository.state.value.personId)

        val recreatedHolder = LensStateHolder(repository)
        composeRule.waitUntil(timeoutMillis = 5_000) {
            recreatedHolder.selection.value.personId == "person_kid_b"
        }
        assertEquals("person_kid_b", recreatedHolder.selection.value.personId)
    }
}
