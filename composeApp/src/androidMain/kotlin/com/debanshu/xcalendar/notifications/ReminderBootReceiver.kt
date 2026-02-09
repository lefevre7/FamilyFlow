package com.debanshu.xcalendar.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.debanshu.xcalendar.domain.usecase.settings.RescheduleRemindersUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatform

class ReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (
            action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED &&
            action != Intent.ACTION_TIMEZONE_CHANGED
        ) {
            return
        }
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val rescheduler = KoinPlatform.getKoin().get<RescheduleRemindersUseCase>()
                rescheduler()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
