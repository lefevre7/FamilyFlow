package com.debanshu.xcalendar.domain.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.debanshu.xcalendar.domain.model.Event
import com.debanshu.xcalendar.domain.model.ReminderPreferences
import com.debanshu.xcalendar.domain.model.Task
import com.debanshu.xcalendar.notifications.ReminderAlarmReceiver
import com.debanshu.xcalendar.notifications.ReminderConstants
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.koin.core.annotation.Single
import org.koin.mp.KoinPlatform

@Single(binds = [ReminderScheduler::class])
class AndroidReminderScheduler : ReminderScheduler {
    private val context: Context by lazy { KoinPlatform.getKoin().get() }
    private val alarmManager: AlarmManager by lazy {
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    override suspend fun scheduleEvent(event: Event, preferences: ReminderPreferences) {
        if (!preferences.remindersEnabled) {
            cancelEvent(event.id)
            return
        }
        val basePrep = event.reminderMinutes.firstOrNull()
        if (basePrep == null || basePrep <= 0) {
            cancelEvent(event.id)
            return
        }
        val travelBuffer =
            if (!event.location.isNullOrBlank()) {
                preferences.travelBufferMinutes
            } else {
                0
            }
        val prepMinutes = basePrep + travelBuffer
        val startMillis = resolveStartTime(event, preferences)
        scheduleReminder(
            itemId = event.id,
            itemType = ReminderConstants.ITEM_EVENT,
            kind = ReminderConstants.KIND_PREP,
            title = event.title,
            atMillis = startMillis - prepMinutes * MINUTE_MILLIS,
            startTime = startMillis,
            endTime = event.endTime,
        )
        scheduleReminder(
            itemId = event.id,
            itemType = ReminderConstants.ITEM_EVENT,
            kind = ReminderConstants.KIND_START,
            title = event.title,
            atMillis = startMillis,
            startTime = startMillis,
            endTime = event.endTime,
        )
    }

    override suspend fun cancelEvent(eventId: String) {
        cancelReminder(eventId, ReminderConstants.ITEM_EVENT, ReminderConstants.KIND_PREP)
        cancelReminder(eventId, ReminderConstants.ITEM_EVENT, ReminderConstants.KIND_START)
    }

    override suspend fun scheduleTask(task: Task, preferences: ReminderPreferences) {
        if (!preferences.remindersEnabled) {
            cancelTask(task.id)
            return
        }
        if (task.status != com.debanshu.xcalendar.domain.model.TaskStatus.OPEN) {
            cancelTask(task.id)
            return
        }
        val startMillis = task.scheduledStart ?: run {
            cancelTask(task.id)
            return
        }
        val endMillis = task.scheduledEnd ?: (startMillis + task.durationMinutes * MINUTE_MILLIS)
        scheduleReminder(
            itemId = task.id,
            itemType = ReminderConstants.ITEM_TASK,
            kind = ReminderConstants.KIND_PREP,
            title = task.title,
            atMillis = startMillis - preferences.prepMinutes * MINUTE_MILLIS,
            startTime = startMillis,
            endTime = endMillis,
        )
        scheduleReminder(
            itemId = task.id,
            itemType = ReminderConstants.ITEM_TASK,
            kind = ReminderConstants.KIND_START,
            title = task.title,
            atMillis = startMillis,
            startTime = startMillis,
            endTime = endMillis,
        )
    }

    override suspend fun cancelTask(taskId: String) {
        cancelReminder(taskId, ReminderConstants.ITEM_TASK, ReminderConstants.KIND_PREP)
        cancelReminder(taskId, ReminderConstants.ITEM_TASK, ReminderConstants.KIND_START)
    }

    override suspend fun scheduleSummaries(preferences: ReminderPreferences) {
        if (!preferences.summaryEnabled) {
            cancelSummaries()
            return
        }
        val now = Instant.fromEpochMilliseconds(System.currentTimeMillis())
        val timeZone = TimeZone.currentSystemDefault()
        scheduleSummary(
            slot = ReminderConstants.SUMMARY_MORNING,
            hour = preferences.summaryMorningHour,
            minute = preferences.summaryMorningMinute,
            now = now,
            timeZone = timeZone,
        )
        scheduleSummary(
            slot = ReminderConstants.SUMMARY_MIDDAY,
            hour = preferences.summaryMiddayHour,
            minute = preferences.summaryMiddayMinute,
            now = now,
            timeZone = timeZone,
        )
    }

    override suspend fun cancelSummaries() {
        cancelReminder(SUMMARY_ID_MORNING, ReminderConstants.ITEM_SUMMARY, ReminderConstants.KIND_SUMMARY)
        cancelReminder(SUMMARY_ID_MIDDAY, ReminderConstants.ITEM_SUMMARY, ReminderConstants.KIND_SUMMARY)
    }

    fun scheduleSnooze(
        itemId: String,
        itemType: String,
        title: String,
        startTime: Long,
        endTime: Long,
        minutes: Int = ReminderConstants.SNOOZE_MINUTES,
    ) {
        val atMillis = System.currentTimeMillis() + minutes * MINUTE_MILLIS
        scheduleReminder(
            itemId = itemId,
            itemType = itemType,
            kind = ReminderConstants.KIND_START,
            title = title,
            atMillis = atMillis,
            startTime = startTime,
            endTime = endTime,
        )
    }

    private fun resolveStartTime(event: Event, preferences: ReminderPreferences): Long {
        if (!event.isAllDay) return event.startTime
        val timeZone = TimeZone.currentSystemDefault()
        val date = Instant.fromEpochMilliseconds(event.startTime).toLocalDateTime(timeZone).date
        val allDayDateTime =
            LocalDateTime(
                date.year,
                date.month,
                date.dayOfMonth,
                preferences.allDayHour,
                preferences.allDayMinute,
            )
        return allDayDateTime.toInstant(timeZone).toEpochMilliseconds()
    }

    private fun scheduleSummary(
        slot: String,
        hour: Int,
        minute: Int,
        now: Instant,
        timeZone: TimeZone,
    ) {
        val today = now.toLocalDateTime(timeZone).date
        var target =
            LocalDateTime(today.year, today.month, today.dayOfMonth, hour, minute)
        if (target.toInstant(timeZone) <= now) {
            val tomorrow = today.plus(DatePeriod(days = 1))
            target = LocalDateTime(tomorrow.year, tomorrow.month, tomorrow.dayOfMonth, hour, minute)
        }
        val id = if (slot == ReminderConstants.SUMMARY_MORNING) SUMMARY_ID_MORNING else SUMMARY_ID_MIDDAY
        scheduleReminder(
            itemId = id,
            itemType = ReminderConstants.ITEM_SUMMARY,
            kind = ReminderConstants.KIND_SUMMARY,
            title = "Daily summary",
            atMillis = target.toInstant(timeZone).toEpochMilliseconds(),
            startTime = target.toInstant(timeZone).toEpochMilliseconds(),
            endTime = target.toInstant(timeZone).toEpochMilliseconds(),
            summarySlot = slot,
        )
    }

    private fun scheduleReminder(
        itemId: String,
        itemType: String,
        kind: String,
        title: String,
        atMillis: Long,
        startTime: Long,
        endTime: Long,
        summarySlot: String? = null,
    ) {
        val now = System.currentTimeMillis()
        if (atMillis <= now) return
        val intent =
            Intent(context, ReminderAlarmReceiver::class.java).apply {
                action = ReminderConstants.ACTION_REMINDER
                putExtra(ReminderConstants.EXTRA_ITEM_ID, itemId)
                putExtra(ReminderConstants.EXTRA_ITEM_TYPE, itemType)
                putExtra(ReminderConstants.EXTRA_KIND, kind)
                putExtra(ReminderConstants.EXTRA_TITLE, title)
                putExtra(ReminderConstants.EXTRA_START_TIME, startTime)
                putExtra(ReminderConstants.EXTRA_END_TIME, endTime)
                summarySlot?.let { putExtra(ReminderConstants.EXTRA_SUMMARY_SLOT, it) }
            }
        val pendingIntent =
            PendingIntent.getBroadcast(
                context,
                requestCode(itemId, itemType, kind, summarySlot),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or pendingIntentImmutableFlag(),
            )

        if (canScheduleExact()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pendingIntent)
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, atMillis, pendingIntent)
        }
    }

    private fun cancelReminder(
        itemId: String,
        itemType: String,
        kind: String,
        summarySlot: String? = null,
    ) {
        val intent =
            Intent(context, ReminderAlarmReceiver::class.java).apply {
                action = ReminderConstants.ACTION_REMINDER
            }
        val pendingIntent =
            PendingIntent.getBroadcast(
                context,
                requestCode(itemId, itemType, kind, summarySlot),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or pendingIntentImmutableFlag(),
            )
        alarmManager.cancel(pendingIntent)
    }

    private fun requestCode(
        itemId: String,
        itemType: String,
        kind: String,
        summarySlot: String? = null,
    ): Int {
        var hash = itemId.hashCode()
        hash = 31 * hash + itemType.hashCode()
        hash = 31 * hash + kind.hashCode()
        summarySlot?.let { hash = 31 * hash + it.hashCode() }
        return hash
    }

    private fun canScheduleExact(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    private fun pendingIntentImmutableFlag(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0

    private companion object {
        const val MINUTE_MILLIS = 60_000L
        const val SUMMARY_ID_MORNING = "summary_morning"
        const val SUMMARY_ID_MIDDAY = "summary_midday"
    }
}
