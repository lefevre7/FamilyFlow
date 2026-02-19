package com.debanshu.xcalendar.ui.components.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.debanshu.xcalendar.domain.model.Holiday
import com.debanshu.xcalendar.domain.model.HolidayAnnotation
import com.debanshu.xcalendar.ui.theme.XCalendarColors
import com.debanshu.xcalendar.ui.theme.XCalendarTheme
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import xcalendar.composeapp.generated.resources.Res
import xcalendar.composeapp.generated.resources.ic_close
import xcalendar.composeapp.generated.resources.ic_description
import xcalendar.composeapp.generated.resources.ic_calendar_view_month
import xcalendar.composeapp.generated.resources.ic_edit
import xcalendar.composeapp.generated.resources.ic_location
import xcalendar.composeapp.generated.resources.ic_notifications

/**
 * Modal bottom sheet dialog showing holiday details with an edit option.
 * Tapping the edit icon opens [HolidayAnnotationEditorSheet].
 *
 * @param holiday The holiday to display
 * @param annotation Persisted user annotation, or null if none
 * @param onEdit Callback invoked when the user taps the edit icon
 * @param onDismiss Callback when the sheet is dismissed
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HolidayDetailsDialog(
    holiday: Holiday,
    annotation: HolidayAnnotation? = null,
    onEdit: (Holiday) -> Unit = {},
    onDismiss: () -> Unit = {},
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val formattedDate = remember(holiday.date) {
        val localDate = Instant.fromEpochMilliseconds(holiday.date)
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
        val dayOfWeek = localDate.dayOfWeek.name.lowercase().replaceFirstChar { it.titlecase() }
        val month = localDate.month.name.lowercase().replaceFirstChar { it.titlecase() }
        "$dayOfWeek, ${localDate.day} $month ${localDate.year}"
    }

    val formattedType = remember(holiday.holidayType) {
        holiday.holidayType
            .replace('_', ' ')
            .lowercase()
            .split(' ')
            .joinToString(" ") { it.replaceFirstChar { c -> c.titlecase() } }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        properties = ModalBottomSheetProperties(shouldDismissOnBackPress = true),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // ── Header row: close (left) + edit (right) ───────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_close),
                    contentDescription = "Close",
                    modifier = Modifier.clickable { onDismiss() },
                    tint = XCalendarTheme.colorScheme.onSurfaceVariant,
                )
                Icon(
                    painter = painterResource(Res.drawable.ic_edit),
                    contentDescription = "Edit holiday",
                    modifier = Modifier.clickable { onEdit(holiday) },
                    tint = XCalendarTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Holiday title with color indicator ────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(XCalendarColors.holiday, CircleShape),
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = holiday.name,
                    style = XCalendarTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Fixed: Date ───────────────────────────────────────────────────
            HolidayDetailRow(
                icon = Res.drawable.ic_calendar_view_month,
                text = formattedDate,
            )

            // ── Fixed: Country code ───────────────────────────────────────────
            HolidayDetailRow(
                icon = Res.drawable.ic_location,
                text = holiday.countryCode.uppercase(),
            )

            // ── Fixed: Holiday type ───────────────────────────────────────────
            HolidayDetailRow(
                icon = Res.drawable.ic_description,
                text = formattedType,
            )

            // ── Annotation: Location ──────────────────────────────────────────
            annotation?.location?.takeIf { it.isNotBlank() }?.let { loc ->
                HolidayDetailRow(
                    icon = Res.drawable.ic_location,
                    text = loc,
                )
            }

            // ── Annotation: Reminder ──────────────────────────────────────────
            annotation?.reminderMinutes?.let { mins ->
                HolidayDetailRow(
                    icon = Res.drawable.ic_notifications,
                    text = "$mins minutes before",
                )
            }

            // ── Annotation: Notes/Description ─────────────────────────────────
            annotation?.description?.takeIf { it.isNotBlank() }?.let { notes ->
                HolidayDetailRow(
                    icon = Res.drawable.ic_description,
                    text = notes,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HolidayDetailRow(
    icon: org.jetbrains.compose.resources.DrawableResource,
    text: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = XCalendarTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            style = XCalendarTheme.typography.bodyMedium,
            color = XCalendarTheme.colorScheme.onSurface,
        )
    }
}
