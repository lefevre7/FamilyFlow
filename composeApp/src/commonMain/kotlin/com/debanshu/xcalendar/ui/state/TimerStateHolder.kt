package com.debanshu.xcalendar.ui.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.annotation.Single

data class ActiveTimer(
    val itemId: String,
    val title: String,
    val startedAt: Long,
    val endsAt: Long,
)

@Single
class TimerStateHolder {
    private val _timer = MutableStateFlow<ActiveTimer?>(null)
    val timer: StateFlow<ActiveTimer?> = _timer

    fun startTimer(itemId: String, title: String, durationMillis: Long) {
        val now = System.currentTimeMillis()
        _timer.value = ActiveTimer(
            itemId = itemId,
            title = title,
            startedAt = now,
            endsAt = now + durationMillis,
        )
    }

    fun stopTimer() {
        _timer.value = null
    }
}
