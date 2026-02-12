package com.debanshu.xcalendar.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.debanshu.xcalendar.R
import com.debanshu.xcalendar.domain.model.ScheduleFilter
import com.debanshu.xcalendar.domain.notifications.AndroidReminderScheduler
import com.debanshu.xcalendar.domain.notifications.ReminderScheduler
import com.debanshu.xcalendar.domain.usecase.person.GetPeopleUseCase
import com.debanshu.xcalendar.domain.usecase.task.GetTasksUseCase
import com.debanshu.xcalendar.domain.usecase.user.GetCurrentUserUseCase
import com.debanshu.xcalendar.domain.usecase.event.GetEventsForDateRangeUseCase
import com.debanshu.xcalendar.domain.util.ScheduleEngine
import com.debanshu.xcalendar.ui.utils.DateTimeFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import org.koin.mp.KoinPlatform

class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                when (action) {
                    ReminderConstants.ACTION_REMINDER -> handleReminder(context, intent)
                    ReminderConstants.ACTION_SNOOZE -> handleSnooze(intent)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleReminder(context: Context, intent: Intent) {
        NotificationChannels.ensureChannels(context)
        val itemId = intent.getStringExtra(ReminderConstants.EXTRA_ITEM_ID).orEmpty()
        val itemType = intent.getStringExtra(ReminderConstants.EXTRA_ITEM_TYPE).orEmpty()
        val kind = intent.getStringExtra(ReminderConstants.EXTRA_KIND).orEmpty()
        val title = intent.getStringExtra(ReminderConstants.EXTRA_TITLE).orEmpty()
        val startTime = intent.getLongExtra(ReminderConstants.EXTRA_START_TIME, 0L)
        val endTime = intent.getLongExtra(ReminderConstants.EXTRA_END_TIME, 0L)
        val summarySlot = intent.getStringExtra(ReminderConstants.EXTRA_SUMMARY_SLOT)

        val (notificationTitle, notificationText, channelId) =
            when (kind) {
                ReminderConstants.KIND_PREP -> {
                    val timeLabel = formatTimeRange(startTime, endTime)
                    Triple("Prep: $title", "Starts at $timeLabel", ReminderConstants.CHANNEL_PREP)
                }
                ReminderConstants.KIND_START -> {
                    val timeLabel = formatTimeRange(startTime, endTime)
                    Triple(title, if (timeLabel.isNotBlank()) "Starting now • $timeLabel" else "Starting now", ReminderConstants.CHANNEL_START)
                }
                ReminderConstants.KIND_SUMMARY -> {
                    val summary = buildSummary()
                    val text = summary.ifBlank { "Open the app to see today’s plan." }
                    Triple("Today’s snapshot", text, ReminderConstants.CHANNEL_SUMMARY)
                }
                else -> Triple(title, "", ReminderConstants.CHANNEL_START)
            }

        val snoozeIntent =
            Intent(context, ReminderAlarmReceiver::class.java).apply {
                action = ReminderConstants.ACTION_SNOOZE
                putExtra(ReminderConstants.EXTRA_ITEM_ID, itemId)
                putExtra(ReminderConstants.EXTRA_ITEM_TYPE, itemType)
                putExtra(ReminderConstants.EXTRA_TITLE, title)
                putExtra(ReminderConstants.EXTRA_START_TIME, startTime)
                putExtra(ReminderConstants.EXTRA_END_TIME, endTime)
                putExtra(ReminderConstants.EXTRA_SUMMARY_SLOT, summarySlot)
            }
        val snoozePending =
            PendingIntent.getBroadcast(
                context,
                (itemId + itemType + "snooze").hashCode(),
                snoozeIntent,
                pendingIntentFlags(),
            )

        val builder =
            NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(notificationTitle)
                .setContentText(notificationText)
                .setStyle(NotificationCompat.BigTextStyle().bigText(notificationText))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)

        if (kind != ReminderConstants.KIND_SUMMARY) {
            builder.addAction(0, "Snooze 10m", snoozePending)
        }

        NotificationManagerCompat.from(context).notify(notificationId(itemId, kind), builder.build())

        if (kind == ReminderConstants.KIND_SUMMARY) {
            val scheduler = KoinPlatform.getKoin().get<ReminderScheduler>()
            val prefsRepo =
                KoinPlatform.getKoin().get<com.debanshu.xcalendar.domain.repository.IReminderPreferencesRepository>()
            val prefs = prefsRepo.preferences.first()
            scheduler.scheduleSummaries(prefs)
        }
    }

    private fun handleSnooze(intent: Intent) {
        val itemId = intent.getStringExtra(ReminderConstants.EXTRA_ITEM_ID) ?: return
        val itemType = intent.getStringExtra(ReminderConstants.EXTRA_ITEM_TYPE) ?: return
        val title = intent.getStringExtra(ReminderConstants.EXTRA_TITLE).orEmpty()
        val startTime = intent.getLongExtra(ReminderConstants.EXTRA_START_TIME, 0L)
        val endTime = intent.getLongExtra(ReminderConstants.EXTRA_END_TIME, 0L)
        val scheduler = KoinPlatform.getKoin().get<ReminderScheduler>()
        if (scheduler is AndroidReminderScheduler) {
            scheduler.scheduleSnooze(
                itemId = itemId,
                itemType = itemType,
                title = title,
                startTime = startTime,
                endTime = endTime,
            )
        }
    }

    private suspend fun buildSummary(): String {
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
        val people = getPeopleUseCase().first()
        val filter = ScheduleFilter()
        val aggregation = ScheduleEngine.aggregate(events, tasks, filter, nowMillis, timeZone)
        
        // Include all items: timed, all-day, and flexible
        // Sort by: timed items first (by start time), then all-day, then flexible
        val timedItems = aggregation.items.filter { it.startTime != null && !it.isAllDay }
            .sortedBy { it.startTime }
        val allDayItems = aggregation.items.filter { it.isAllDay }
        val flexibleItems = aggregation.items.filter { it.startTime == null && !it.isAllDay }
        
        val items = (timedItems + allDayItems + flexibleItems).take(3)
        
        if (items.isEmpty()) return ""
        val names = people.associateBy { it.id }
        return items.joinToString(separator = "\n") { item ->
            val time = when {
                item.isAllDay -> "All day"
                item.startTime != null -> {
                    val startMillis = item.startTime!!
                    val endMillis = item.endTime ?: startMillis
                    val startDateTime = Instant.fromEpochMilliseconds(startMillis).toLocalDateTime(timeZone)
                    val endDateTime = Instant.fromEpochMilliseconds(endMillis).toLocalDateTime(timeZone)
                    DateTimeFormatter.formatCompactTimeRange(startDateTime, endDateTime)
                }
                else -> "Flexible"
            }
            val personLabel =
                item.personIds.firstOrNull()?.let { names[it]?.name }?.let { " • $it" }.orEmpty()
            "$time — ${item.title}$personLabel"
        }
    }

    private fun formatTimeRange(startMillis: Long, endMillis: Long): String {
        if (startMillis <= 0L) return ""
        val timeZone = TimeZone.currentSystemDefault()
        val start = Instant.fromEpochMilliseconds(startMillis).toLocalDateTime(timeZone)
        val end = Instant.fromEpochMilliseconds(endMillis).toLocalDateTime(timeZone)
        return DateTimeFormatter.formatCompactTimeRange(start, end)
    }

    private fun notificationId(itemId: String, kind: String): Int =
        (itemId + kind).hashCode()

    private fun pendingIntentFlags(): Int {
        val immutable = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE
        } else {
            0
        }
        return PendingIntent.FLAG_UPDATE_CURRENT or immutable
    }
}
