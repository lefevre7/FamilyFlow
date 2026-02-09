package com.debanshu.xcalendar.notifications

internal object ReminderConstants {
    const val ACTION_REMINDER = "com.debanshu.xcalendar.action.REMINDER"
    const val ACTION_SNOOZE = "com.debanshu.xcalendar.action.SNOOZE"

    const val EXTRA_ITEM_ID = "extra_item_id"
    const val EXTRA_ITEM_TYPE = "extra_item_type"
    const val EXTRA_KIND = "extra_kind"
    const val EXTRA_TITLE = "extra_title"
    const val EXTRA_START_TIME = "extra_start_time"
    const val EXTRA_END_TIME = "extra_end_time"
    const val EXTRA_SUMMARY_SLOT = "extra_summary_slot"

    const val ITEM_EVENT = "event"
    const val ITEM_TASK = "task"
    const val ITEM_SUMMARY = "summary"

    const val KIND_PREP = "prep"
    const val KIND_START = "start"
    const val KIND_SUMMARY = "summary"

    const val SUMMARY_MORNING = "morning"
    const val SUMMARY_MIDDAY = "midday"

    const val CHANNEL_PREP = "reminder_prep"
    const val CHANNEL_START = "reminder_start"
    const val CHANNEL_SUMMARY = "reminder_summary"

    const val SNOOZE_MINUTES = 10
}
