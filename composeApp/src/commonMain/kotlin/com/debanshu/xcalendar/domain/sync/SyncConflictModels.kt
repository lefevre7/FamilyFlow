package com.debanshu.xcalendar.domain.sync

import com.debanshu.xcalendar.domain.model.Event
import com.debanshu.xcalendar.domain.model.ExternalEvent

data class SyncConflict(
    val calendarId: String,
    val localEvent: Event,
    val remoteEvent: ExternalEvent,
)

enum class SyncResolutionAction {
    KEEP_LOCAL,
    KEEP_REMOTE,
    DUPLICATE,
}
