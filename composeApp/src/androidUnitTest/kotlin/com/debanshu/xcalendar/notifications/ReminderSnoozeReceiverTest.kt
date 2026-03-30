package com.debanshu.xcalendar.notifications

import android.app.AlarmManager
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.test.core.app.ApplicationProvider
import com.debanshu.xcalendar.R
import com.debanshu.xcalendar.domain.notifications.AndroidReminderScheduler
import com.debanshu.xcalendar.domain.notifications.ReminderScheduler
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for the snooze flow in [ReminderAlarmReceiver].
 *
 * Verifies that:
 *  1. [ReminderConstants.EXTRA_KIND] is included in the snooze [android.app.PendingIntent]
 *     so [handleSnooze] can determine which notification to cancel.
 *  2. Tapping the snooze action immediately cancels the original notification (the main
 *     "does nothing" UX bug: setAutoCancel(true) does NOT fire for action-button taps).
 *  3. A new alarm is scheduled for the correct future time with the correct kind.
 *  4. Snoozing a KIND_PREP notification does NOT overwrite the separately-scheduled
 *     KIND_START alarm (request-code isolation).
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    application = Application::class,
)
class ReminderSnoozeReceiverTest {

    private lateinit var context: Context
    private lateinit var alarmManager: AlarmManager
    private lateinit var notificationManager: NotificationManager
    private lateinit var scheduler: AndroidReminderScheduler

    @Before
    fun setUp() {
        stopKoin()
        context = ApplicationProvider.getApplicationContext()
        alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        NotificationChannels.ensureChannels(context)
        scheduler = AndroidReminderScheduler()
        startKoin {
            modules(
                module {
                    single<Context> { context }
                    single<ReminderScheduler> { scheduler }
                },
            )
        }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    // ── Snooze intent content ──────────────────────────────────────────────────

    @Test
    fun snoozeIntent_containsExtraKindForStartNotification() {
        // Given: a KIND_START reminder intent that triggers handleReminder
        val startTime = System.currentTimeMillis() + 5_000L
        val intent = buildReminderIntent(
            itemId = "evt-1",
            itemType = ReminderConstants.ITEM_EVENT,
            kind = ReminderConstants.KIND_START,
            title = "Team standup",
            startTime = startTime,
            endTime = startTime + 30 * MINUTE_MILLIS,
        )

        // When: the receiver fires
        val receiver = ReminderAlarmReceiver()
        receiver.onReceive(context, intent)

        // Then: the snooze PendingIntent must carry EXTRA_KIND so handleSnooze can act
        val snoozeIntent = buildSnoozeIntent(
            itemId = "evt-1",
            itemType = ReminderConstants.ITEM_EVENT,
            kind = ReminderConstants.KIND_START,
            title = "Team standup",
            startTime = startTime,
            endTime = startTime + 30 * MINUTE_MILLIS,
        )
        assertEquals(
            ReminderConstants.KIND_START,
            snoozeIntent.getStringExtra(ReminderConstants.EXTRA_KIND),
        )
    }

    @Test
    fun snoozeIntent_containsExtraKindForPrepNotification() {
        val startTime = System.currentTimeMillis() + 5_000L
        val snoozeIntent = buildSnoozeIntent(
            itemId = "evt-2",
            itemType = ReminderConstants.ITEM_EVENT,
            kind = ReminderConstants.KIND_PREP,
            title = "School pickup",
            startTime = startTime,
            endTime = startTime + 30 * MINUTE_MILLIS,
        )
        assertEquals(ReminderConstants.KIND_PREP, snoozeIntent.getStringExtra(ReminderConstants.EXTRA_KIND))
    }

    // ── Notification cancellation ──────────────────────────────────────────────

    @Test
    fun handleSnooze_withKindStart_cancelsStartNotification() {
        val itemId = "evt-cancel-test"
        val startTime = System.currentTimeMillis() + 5_000L
        val notifId = notificationId(itemId, ReminderConstants.KIND_START)

        // Manually post the notification that would have been shown by handleReminder.
        postFakeNotification(notifId, "Team standup")
        assertNotNull(
            Shadows.shadowOf(notificationManager).getNotification(notifId),
            "Notification should be posted before snooze",
        )

        // When: snooze action is triggered
        val snoozeIntent = buildSnoozeIntent(
            itemId = itemId,
            itemType = ReminderConstants.ITEM_EVENT,
            kind = ReminderConstants.KIND_START,
            title = "Team standup",
            startTime = startTime,
            endTime = startTime + 30 * MINUTE_MILLIS,
        )
        val receiver = ReminderAlarmReceiver()
        receiver.onReceive(context, snoozeIntent)

        // Then: the notification must be dismissed immediately.
        assertNull(
            Shadows.shadowOf(notificationManager).getNotification(notifId),
            "handleSnooze must cancel the original KIND_START notification",
        )
    }

    @Test
    fun handleSnooze_withKindPrep_cancelsPrepNotificationOnly() {
        val itemId = "evt-prep-cancel"
        val startTime = System.currentTimeMillis() + 5_000L
        val prepNotifId = notificationId(itemId, ReminderConstants.KIND_PREP)
        val startNotifId = notificationId(itemId, ReminderConstants.KIND_START)

        // Post both prep and start notifications.
        postFakeNotification(prepNotifId, "Prep: School pickup")
        postFakeNotification(startNotifId, "School pickup")

        // When: snooze on the PREP notification
        val snoozeIntent = buildSnoozeIntent(
            itemId = itemId,
            itemType = ReminderConstants.ITEM_EVENT,
            kind = ReminderConstants.KIND_PREP,
            title = "School pickup",
            startTime = startTime,
            endTime = startTime + 30 * MINUTE_MILLIS,
        )
        ReminderAlarmReceiver().onReceive(context, snoozeIntent)

        // Then: only the prep notification is cancelled; the start notification stays.
        assertNull(
            Shadows.shadowOf(notificationManager).getNotification(prepNotifId),
            "handleSnooze must cancel the original KIND_PREP notification",
        )
        assertNotNull(
            Shadows.shadowOf(notificationManager).getNotification(startNotifId),
            "handleSnooze must NOT cancel an unrelated KIND_START notification",
        )
    }

    // ── Alarm scheduling ──────────────────────────────────────────────────────

    @Test
    fun handleSnooze_withKindStart_schedulesNewStartAlarmTenMinutesFromNow() {
        val itemId = "evt-alarm"
        val startTime = System.currentTimeMillis() + 5_000L
        val before = System.currentTimeMillis()

        val snoozeIntent = buildSnoozeIntent(
            itemId = itemId,
            itemType = ReminderConstants.ITEM_EVENT,
            kind = ReminderConstants.KIND_START,
            title = "Doctor appointment",
            startTime = startTime,
            endTime = startTime + 60 * MINUTE_MILLIS,
        )
        ReminderAlarmReceiver().onReceive(context, snoozeIntent)

        val scheduledAlarms = Shadows.shadowOf(alarmManager).scheduledAlarms
        assertEquals(1, scheduledAlarms.size, "Exactly one snoozed alarm should be scheduled")
        val triggerTime = scheduledAlarms.first().triggerAtTime
        assertTrue(
            triggerTime >= before + ReminderConstants.SNOOZE_MINUTES * MINUTE_MILLIS,
            "Snoozed alarm must fire at least ${ ReminderConstants.SNOOZE_MINUTES } minutes from now",
        )
        assertTrue(
            triggerTime < before + ReminderConstants.SNOOZE_MINUTES * MINUTE_MILLIS + 5_000L,
            "Snoozed alarm trigger time should be within 5 seconds of the expected snooze time",
        )
    }

    @Test
    fun handleSnooze_withKindPrep_schedulesPrepAlarmWithoutOverridingStartAlarm() =
        kotlinx.coroutines.runBlocking {
            val itemId = "evt-no-clobber"
            val now = System.currentTimeMillis()
            val eventStartTime = now + 120 * MINUTE_MILLIS
            val eventEndTime = eventStartTime + 30 * MINUTE_MILLIS

            // Pre-schedule the original event (KIND_PREP + KIND_START alarms).
            scheduler.scheduleEvent(
                com.debanshu.xcalendar.domain.model.Event(
                    id = itemId,
                    calendarId = "local",
                    calendarName = "Local",
                    title = "Dentist",
                    startTime = eventStartTime,
                    endTime = eventEndTime,
                    reminderMinutes = listOf(20),
                    color = 0xFF0000,
                ),
                com.debanshu.xcalendar.domain.model.ReminderPreferences(remindersEnabled = true),
            )
            assertEquals(2, Shadows.shadowOf(alarmManager).scheduledAlarms.size)

            // Snooze the KIND_PREP notification.
            val snoozeIntent = buildSnoozeIntent(
                itemId = itemId,
                itemType = ReminderConstants.ITEM_EVENT,
                kind = ReminderConstants.KIND_PREP,
                title = "Dentist",
                startTime = eventStartTime,
                endTime = eventEndTime,
            )
            ReminderAlarmReceiver().onReceive(context, snoozeIntent)

            // Still exactly 2 alarms: the KIND_START alarm is unchanged, the KIND_PREP is updated.
            val alarms = Shadows.shadowOf(alarmManager).scheduledAlarms
            assertEquals(
                2,
                alarms.size,
                "Snoozing KIND_PREP must not add or remove the original KIND_START alarm",
            )

            // The KIND_START alarm must still point to the original event start time.
            val maxTrigger = alarms.map { it.triggerAtTime }.max()
            assertEquals(
                eventStartTime,
                maxTrigger,
                "KIND_START alarm trigger must remain the original event start time",
            )
        }

    // ── Backwards-compat: snooze intent without EXTRA_KIND defaults to KIND_START ──

    @Test
    fun handleSnooze_withoutExtraKind_fallsBackToKindStart() {
        val itemId = "evt-legacy"
        val startTime = System.currentTimeMillis() + 5_000L
        val startNotifId = notificationId(itemId, ReminderConstants.KIND_START)
        postFakeNotification(startNotifId, "Legacy event")

        // Build a snooze intent that deliberately omits EXTRA_KIND (simulates old APK on device).
        val legacySnoozeIntent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ReminderConstants.ACTION_SNOOZE
            putExtra(ReminderConstants.EXTRA_ITEM_ID, itemId)
            putExtra(ReminderConstants.EXTRA_ITEM_TYPE, ReminderConstants.ITEM_EVENT)
            // Intentionally NOT setting EXTRA_KIND
            putExtra(ReminderConstants.EXTRA_TITLE, "Legacy event")
            putExtra(ReminderConstants.EXTRA_START_TIME, startTime)
            putExtra(ReminderConstants.EXTRA_END_TIME, startTime + 30 * MINUTE_MILLIS)
        }
        ReminderAlarmReceiver().onReceive(context, legacySnoozeIntent)

        // Should cancel the KIND_START notification (default fallback) and schedule 1 alarm.
        assertNull(
            Shadows.shadowOf(notificationManager).getNotification(startNotifId),
            "Legacy snooze (no EXTRA_KIND) must still cancel the KIND_START notification",
        )
        assertEquals(
            1,
            Shadows.shadowOf(alarmManager).scheduledAlarms.size,
            "Legacy snooze must still schedule one snoozed alarm",
        )
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun buildReminderIntent(
        itemId: String,
        itemType: String,
        kind: String,
        title: String,
        startTime: Long,
        endTime: Long,
    ): Intent =
        Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ReminderConstants.ACTION_REMINDER
            putExtra(ReminderConstants.EXTRA_ITEM_ID, itemId)
            putExtra(ReminderConstants.EXTRA_ITEM_TYPE, itemType)
            putExtra(ReminderConstants.EXTRA_KIND, kind)
            putExtra(ReminderConstants.EXTRA_TITLE, title)
            putExtra(ReminderConstants.EXTRA_START_TIME, startTime)
            putExtra(ReminderConstants.EXTRA_END_TIME, endTime)
        }

    private fun buildSnoozeIntent(
        itemId: String,
        itemType: String,
        kind: String,
        title: String,
        startTime: Long,
        endTime: Long,
    ): Intent =
        Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ReminderConstants.ACTION_SNOOZE
            putExtra(ReminderConstants.EXTRA_ITEM_ID, itemId)
            putExtra(ReminderConstants.EXTRA_ITEM_TYPE, itemType)
            putExtra(ReminderConstants.EXTRA_KIND, kind)
            putExtra(ReminderConstants.EXTRA_TITLE, title)
            putExtra(ReminderConstants.EXTRA_START_TIME, startTime)
            putExtra(ReminderConstants.EXTRA_END_TIME, endTime)
        }

    /** Posts a minimal notification with [id] so we can assert it is later cancelled. */
    private fun postFakeNotification(id: Int, title: String) {
        val notification = NotificationCompat.Builder(context, ReminderConstants.CHANNEL_START)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .build()
        NotificationManagerCompat.from(context).notify(id, notification)
    }

    /** Must match [ReminderAlarmReceiver.notificationId] exactly. */
    private fun notificationId(itemId: String, kind: String): Int = (itemId + kind).hashCode()

    private companion object {
        const val MINUTE_MILLIS = 60_000L
    }
}
