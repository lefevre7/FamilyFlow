package com.debanshu.xcalendar

import android.app.Application
import com.debanshu.xcalendar.di.initKoin
import com.debanshu.xcalendar.di.androidPlatformModule
import com.debanshu.xcalendar.sync.GoogleCalendarSyncWorker
import com.debanshu.xcalendar.domain.usecase.settings.RescheduleRemindersUseCase
import com.debanshu.xcalendar.notifications.NotificationChannels
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.mp.KoinPlatform

class XCalenderApp : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin{
            androidContext(this@XCalenderApp)
            androidLogger()
            modules(androidPlatformModule)
        }
        NotificationChannels.ensureChannels(this)
        GoogleCalendarSyncWorker.schedule(this)
        CoroutineScope(Dispatchers.Default).launch {
            runCatching {
                KoinPlatform.getKoin().get<RescheduleRemindersUseCase>().invoke()
            }
        }
    }
}
