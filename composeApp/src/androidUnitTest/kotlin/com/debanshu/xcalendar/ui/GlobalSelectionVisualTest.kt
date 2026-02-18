package com.debanshu.xcalendar.ui

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import com.debanshu.xcalendar.domain.model.FamilyLens
import com.debanshu.xcalendar.domain.model.FamilyLensSelection
import com.debanshu.xcalendar.domain.model.Person
import com.debanshu.xcalendar.domain.model.PersonRole
import com.debanshu.xcalendar.ui.components.GlobalSelectionVisual
import com.debanshu.xcalendar.ui.theme.XCalendarTheme
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    application = Application::class,
)
class GlobalSelectionVisualTest {

    @get:Rule
    val composeRule = createIntentComposeRule<TestActivity>()

    @Test
    fun rendersDateAndPersonChipsWithAccessibilityDescriptions() {
        composeRule.setContent {
            XCalendarTheme {
                GlobalSelectionVisual(
                    date = LocalDate(2026, Month.FEBRUARY, 19),
                    lensSelection = FamilyLensSelection(lens = FamilyLens.MOM, personId = "person_mom"),
                    people = samplePeople(),
                )
            }
        }

        composeRule.onNodeWithTag("global_selection_visual").assertIsDisplayed()
        composeRule.onNodeWithTag("global_selection_date_chip").assertIsDisplayed()
        composeRule.onNodeWithTag("global_selection_person_chip").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Selected date Thu, Feb 19").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Selected person Mom Focus").assertIsDisplayed()
    }

    private fun samplePeople(): List<Person> =
        listOf(
            Person(
                id = "person_mom",
                name = "Mom",
                role = PersonRole.MOM,
                color = 0xFFE57399.toInt(),
                isAdmin = true,
            ),
            Person(
                id = "person_kid_a",
                name = "Kid A",
                role = PersonRole.CHILD,
                color = 0xFF8AB4F8.toInt(),
            ),
        )
}
