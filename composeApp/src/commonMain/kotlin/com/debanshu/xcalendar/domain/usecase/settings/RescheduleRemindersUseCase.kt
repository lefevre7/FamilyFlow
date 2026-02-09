package com.debanshu.xcalendar.domain.usecase.settings

import com.debanshu.xcalendar.domain.model.ReminderPreferences
import com.debanshu.xcalendar.domain.notifications.ReminderScheduler
import com.debanshu.xcalendar.domain.repository.IEventRepository
import com.debanshu.xcalendar.domain.usecase.task.GetTasksUseCase
import com.debanshu.xcalendar.domain.usecase.user.GetCurrentUserUseCase
import com.debanshu.xcalendar.domain.widgets.WidgetUpdater
import com.debanshu.xcalendar.domain.model.TaskStatus
import kotlinx.coroutines.flow.first
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.atStartOfDayIn
import org.koin.core.annotation.Factory
import kotlin.time.Clock

@Factory
class RescheduleRemindersUseCase(
    private val reminderScheduler: ReminderScheduler,
    private val getReminderPreferencesUseCase: GetReminderPreferencesUseCase,
    private val eventRepository: IEventRepository,
    private val getTasksUseCase: GetTasksUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val widgetUpdater: WidgetUpdater,
) {
    suspend operator fun invoke() {
        val prefs = getReminderPreferencesUseCase().first()
        val userId = getCurrentUserUseCase()
        val (start, end) = reminderRange()
        val events = eventRepository.getEventsForCalendarsInRange(userId, start, end).first()
        val tasks = getTasksUseCase().first().filter { it.status == TaskStatus.OPEN }

        if (!prefs.remindersEnabled) {
            events.forEach { reminderScheduler.cancelEvent(it.id) }
            tasks.forEach { reminderScheduler.cancelTask(it.id) }
            reminderScheduler.cancelSummaries()
            return
        }

        events.forEach { reminderScheduler.scheduleEvent(it, prefs) }
        tasks.forEach { reminderScheduler.scheduleTask(it, prefs) }
        if (prefs.summaryEnabled) {
            reminderScheduler.scheduleSummaries(prefs)
        } else {
            reminderScheduler.cancelSummaries()
        }
        widgetUpdater.refreshTodayWidget()
    }

    private fun reminderRange(): Pair<Long, Long> {
        val timeZone = TimeZone.currentSystemDefault()
        val today = Clock.System.now().toLocalDateTime(timeZone).date
        val start = today.minus(DatePeriod(days = 1)).atStartOfDayIn(timeZone).toEpochMilliseconds()
        val end = today.plus(DatePeriod(days = 30)).atStartOfDayIn(timeZone).toEpochMilliseconds()
        return start to end
    }
}
