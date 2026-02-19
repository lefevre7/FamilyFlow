package com.debanshu.xcalendar.widget

import android.content.Context
import android.content.Intent
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.compose.ui.unit.dp
import com.debanshu.xcalendar.MainActivity
import com.debanshu.xcalendar.domain.model.ScheduleFilter
import com.debanshu.xcalendar.domain.usecase.event.GetEventsForDateRangeUseCase
import com.debanshu.xcalendar.domain.usecase.person.GetPeopleUseCase
import com.debanshu.xcalendar.domain.usecase.task.GetTasksUseCase
import com.debanshu.xcalendar.domain.usecase.user.GetCurrentUserUseCase
import com.debanshu.xcalendar.domain.util.ScheduleEngine
import com.debanshu.xcalendar.ui.components.dialog.QuickAddMode
import com.debanshu.xcalendar.ui.utils.DateTimeFormatter
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import org.koin.mp.KoinPlatform

private data class WidgetItem(
    val title: String,
    val timeLabel: String,
    val personLabel: String?,
)

class TodayWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val items = loadItems()
        provideContent {
            val openTodayIntent = Intent(context, MainActivity::class.java).apply {
                putExtra(EXTRA_NAVIGATE_TO, DESTINATION_TODAY)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            val openTodayAction = actionStartActivity(openTodayIntent)
            Column(modifier = GlanceModifier.fillMaxWidth().padding(12.dp)) {
                Text(
                    text = "Today",
                    style = TextStyle(fontWeight = FontWeight.Medium),
                    modifier = GlanceModifier.clickable(openTodayAction),
                )
                if (items.isEmpty()) {
                    Text(
                        text = "You are clear for now.",
                        style = TextStyle(),
                        modifier = GlanceModifier.clickable(openTodayAction),
                    )
                } else {
                    items.forEach { item ->
                        Row(
                            modifier = GlanceModifier.fillMaxWidth().padding(vertical = 4.dp)
                                .clickable(openTodayAction),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = item.timeLabel,
                                style = TextStyle(fontWeight = FontWeight.Bold),
                            )
                            Spacer(modifier = GlanceModifier.width(8.dp))
                            Text(text = item.title)
                            item.personLabel?.let { label ->
                                Text(text = " • $label")
                            }
                        }
                    }
                }
                Spacer(modifier = GlanceModifier.height(8.dp))
                Row(modifier = GlanceModifier.fillMaxWidth().padding(top = 8.dp)) {
                    val taskIntent =
                        Intent(context, MainActivity::class.java).apply {
                            putExtra(EXTRA_QUICK_ADD_MODE, QuickAddMode.TASK.name)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        }
                    val voiceIntent =
                        Intent(context, MainActivity::class.java).apply {
                            putExtra(EXTRA_QUICK_ADD_MODE, QuickAddMode.VOICE.name)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        }
                    Text(
                        text = "Add task",
                        modifier =
                            GlanceModifier
                                .padding(end = 8.dp)
                                .clickable(actionStartActivity(taskIntent)),
                    )
                    Spacer(modifier = GlanceModifier.width(12.dp))
                    Text(
                        text = "Voice",
                        modifier =
                            GlanceModifier
                                .clickable(actionStartActivity(voiceIntent)),
                    )
                }
            }
        }
    }

    private suspend fun loadItems(): List<WidgetItem> {
        val koin = KoinPlatform.getKoin()
        val getCurrentUserUseCase = koin.get<GetCurrentUserUseCase>()
        val getEventsForDateRangeUseCase = koin.get<GetEventsForDateRangeUseCase>()
        val getTasksUseCase = koin.get<GetTasksUseCase>()
        val getPeopleUseCase = koin.get<GetPeopleUseCase>()

        val userId = getCurrentUserUseCase()
        val nowMillis = System.currentTimeMillis()
        val timeZone = TimeZone.currentSystemDefault()
        val today = Instant.fromEpochMilliseconds(nowMillis).toLocalDateTime(timeZone).date
        val start = today.atStartOfDayIn(timeZone).toEpochMilliseconds()
        val end = start + 24 * 60 * 60 * 1000L

        val events = getEventsForDateRangeUseCase(userId, start, end).first()
        val tasks = getTasksUseCase().first()
        val people = getPeopleUseCase().first().associateBy { it.id }

        val aggregation = ScheduleEngine.aggregate(events, tasks, ScheduleFilter(), nowMillis, timeZone)
        return aggregation.items
            .filter { it.startTime != null }
            .sortedBy { it.startTime }
            .take(3)
            .map { item ->
                val startTime = item.startTime ?: 0L
                val endTime = item.endTime ?: startTime
                val startDateTime = Instant.fromEpochMilliseconds(startTime).toLocalDateTime(timeZone)
                val endDateTime = Instant.fromEpochMilliseconds(endTime).toLocalDateTime(timeZone)
                val timeLabel = DateTimeFormatter.formatCompactTimeRange(startDateTime, endDateTime)
                val personLabel = item.personIds.firstOrNull()?.let { people[it]?.name }
                WidgetItem(
                    title = item.title,
                    timeLabel = timeLabel,
                    personLabel = personLabel,
                )
            }
    }

    companion object {
        const val EXTRA_QUICK_ADD_MODE = "extra_quick_add_mode"

        /** Intent extra key used to request navigation to a specific screen on launch. */
        const val EXTRA_NAVIGATE_TO = "extra_navigate_to"

        /** Value for [EXTRA_NAVIGATE_TO] that navigates to the Today screen. */
        const val DESTINATION_TODAY = "today"
    }
}

class TodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayWidget()
}
