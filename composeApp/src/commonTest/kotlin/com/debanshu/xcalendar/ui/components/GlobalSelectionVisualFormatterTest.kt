package com.debanshu.xcalendar.ui.components

import com.debanshu.xcalendar.domain.model.FamilyLens
import com.debanshu.xcalendar.domain.model.FamilyLensSelection
import com.debanshu.xcalendar.domain.model.Person
import com.debanshu.xcalendar.domain.model.PersonRole
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlin.test.Test
import kotlin.test.assertEquals

class GlobalSelectionVisualFormatterTest {

    @Test
    fun formatCompactSelectedDate_usesAbbreviatedWeekdayMonthAndDay() {
        val date = LocalDate(2026, Month.FEBRUARY, 19)

        assertEquals("Thu, Feb 19", formatCompactSelectedDate(date))
    }

    @Test
    fun resolveSelectedPersonVisual_familyLens_usesFamilyLabel() {
        val selection = FamilyLensSelection(lens = FamilyLens.FAMILY, personId = null)

        val visual = resolveSelectedPersonVisual(selection, samplePeople())

        assertEquals("Family", visual.label)
        assertEquals(null, visual.personColor)
    }

    @Test
    fun resolveSelectedPersonVisual_personLens_usesSelectedPersonNameAndColor() {
        val selection = FamilyLensSelection(lens = FamilyLens.PERSON, personId = "kid_a")

        val visual = resolveSelectedPersonVisual(selection, samplePeople())

        assertEquals("Kid A", visual.label)
        assertEquals(0xFF8AB4F8.toInt(), visual.personColor)
    }

    @Test
    fun resolveSelectedPersonVisual_momLens_usesMomFocusLabelAndMomColor() {
        val selection = FamilyLensSelection(lens = FamilyLens.MOM, personId = null)

        val visual = resolveSelectedPersonVisual(selection, samplePeople())

        assertEquals("Mom Focus", visual.label)
        assertEquals(0xFFE57399.toInt(), visual.personColor)
    }

    private fun samplePeople(): List<Person> =
        listOf(
            Person(
                id = "mom",
                name = "Mom",
                role = PersonRole.MOM,
                color = 0xFFE57399.toInt(),
                isAdmin = true,
            ),
            Person(
                id = "kid_a",
                name = "Kid A",
                role = PersonRole.CHILD,
                color = 0xFF8AB4F8.toInt(),
            ),
        )
}
