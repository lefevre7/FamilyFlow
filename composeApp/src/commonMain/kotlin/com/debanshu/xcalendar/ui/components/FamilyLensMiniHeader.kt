package com.debanshu.xcalendar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.debanshu.xcalendar.domain.model.FamilyLens
import com.debanshu.xcalendar.domain.model.FamilyLensSelection
import com.debanshu.xcalendar.domain.model.Person
import com.debanshu.xcalendar.domain.model.PersonRole
import com.debanshu.xcalendar.ui.theme.XCalendarTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FamilyLensMiniHeader(
    selection: FamilyLensSelection,
    people: List<Person>,
    onSelectionChange: (FamilyLensSelection) -> Unit,
    modifier: Modifier = Modifier,
) {
    val mom = people.firstOrNull { it.role == PersonRole.MOM }
    val selectedPerson = people.firstOrNull { it.id == selection.personId } ?: mom ?: people.firstOrNull()

    Column(
        modifier = modifier.testTag("lens_selector"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = selection.lens == FamilyLens.FAMILY,
                onClick = {
                    onSelectionChange(
                        selection.copy(
                            lens = FamilyLens.FAMILY,
                            personId = selection.personId ?: selectedPerson?.id,
                        )
                    )
                },
                label = {
                    Text(
                        text = "Family",
                        style = XCalendarTheme.typography.bodySmall,
                    )
                },
            )
            mom?.let {
                FilterChip(
                    selected = selection.lens == FamilyLens.MOM,
                    onClick = {
                        onSelectionChange(
                            selection.copy(
                                lens = FamilyLens.MOM,
                                personId = selection.personId ?: mom.id,
                            )
                        )
                    },
                    label = {
                        Text(
                            text = "Mom Focus",
                            style = XCalendarTheme.typography.bodySmall,
                        )
                    },
                )
            }
            if (selectedPerson != null) {
                FilterChip(
                    selected = selection.lens == FamilyLens.PERSON,
                    onClick = {
                        onSelectionChange(
                            selection.copy(
                                lens = FamilyLens.PERSON,
                                personId = selectedPerson.id,
                            )
                        )
                    },
                    label = {
                        Text(
                            text = "Person: ${selectedPerson.name.ifBlank { "Select" }}",
                            style = XCalendarTheme.typography.bodySmall,
                        )
                    },
                )
            }
        }

        if (selection.lens == FamilyLens.PERSON) {
            Text(
                text = "Choose person",
                style = XCalendarTheme.typography.bodySmall,
                color = XCalendarTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                people.forEach { person ->
                    FilterChip(
                        selected = selection.personId == person.id,
                        onClick = {
                            onSelectionChange(
                                selection.copy(
                                    lens = FamilyLens.PERSON,
                                    personId = person.id,
                                )
                            )
                        },
                        label = { LensPersonLabel(person = person, fallback = person.name) },
                    )
                }
            }
        }
    }
}

@Composable
private fun LensPersonLabel(
    person: Person,
    fallback: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(16.dp).background(Color(person.color), CircleShape),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = person.name.ifBlank { fallback },
            style = XCalendarTheme.typography.bodySmall,
        )
    }
}
