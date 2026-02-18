package com.debanshu.xcalendar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.debanshu.xcalendar.domain.model.FamilyLens
import com.debanshu.xcalendar.domain.model.FamilyLensSelection
import com.debanshu.xcalendar.domain.model.Person
import com.debanshu.xcalendar.domain.model.PersonRole
import com.debanshu.xcalendar.ui.theme.XCalendarTheme
import kotlinx.datetime.LocalDate

internal data class SelectedPersonVisual(
    val label: String,
    val personColor: Int?,
)

@Composable
fun GlobalSelectionVisual(
    date: LocalDate,
    lensSelection: FamilyLensSelection,
    people: List<Person>,
    modifier: Modifier = Modifier,
) {
    val dateLabel = remember(date) { formatCompactSelectedDate(date) }
    val personVisual = remember(lensSelection, people) { resolveSelectedPersonVisual(lensSelection, people) }
    val dotColor = personVisual.personColor?.let(::Color) ?: XCalendarTheme.colorScheme.outline

    Column(
        modifier = modifier.testTag("global_selection_visual"),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        SummaryChip(
            label = dateLabel,
            contentDescription = "Selected date $dateLabel",
            modifier = Modifier.testTag("global_selection_date_chip"),
        )
        SummaryChip(
            label = personVisual.label,
            dotColor = dotColor,
            contentDescription = "Selected person ${personVisual.label}",
            modifier = Modifier.testTag("global_selection_person_chip"),
        )
    }
}

internal fun formatCompactSelectedDate(date: LocalDate): String {
    val weekday =
        date.dayOfWeek.name
            .lowercase()
            .replaceFirstChar { it.titlecase() }
            .take(3)
    val month =
        date.month.name
            .lowercase()
            .replaceFirstChar { it.titlecase() }
            .take(3)
    return "$weekday, $month ${date.day}"
}

internal fun resolveSelectedPersonVisual(
    selection: FamilyLensSelection,
    people: List<Person>,
): SelectedPersonVisual {
    val mom = people.firstOrNull { it.role == PersonRole.MOM }
    val selectedPerson = people.firstOrNull { it.id == selection.personId } ?: mom
    return when (selection.lens) {
        FamilyLens.FAMILY ->
            SelectedPersonVisual(
                label = "Family",
                personColor = null,
            )

        FamilyLens.MOM ->
            SelectedPersonVisual(
                label = "Mom Focus",
                personColor = mom?.color,
            )

        FamilyLens.PERSON ->
            SelectedPersonVisual(
                label = selectedPerson?.name?.ifBlank { "Person" } ?: "Person",
                personColor = selectedPerson?.color,
            )
    }
}

@Composable
private fun SummaryChip(
    label: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    dotColor: Color? = null,
) {
    Surface(
        modifier = modifier.semantics { this.contentDescription = contentDescription },
        color = XCalendarTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
        contentColor = XCalendarTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp,
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (dotColor != null) {
                Box(
                    modifier = Modifier.size(8.dp).background(dotColor, CircleShape),
                )
            }
            Text(
                text = label,
                style = XCalendarTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
