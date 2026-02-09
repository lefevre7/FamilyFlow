package com.debanshu.xcalendar.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.debanshu.xcalendar.domain.usecase.google.SyncGoogleCalendarsUseCase
import org.koin.mp.KoinPlatform
import java.util.concurrent.TimeUnit

class GoogleCalendarSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        return try {
            val syncUseCase = KoinPlatform.getKoin().get<SyncGoogleCalendarsUseCase>()
            syncUseCase(manual = false)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "google_calendar_sync"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request =
                PeriodicWorkRequestBuilder<GoogleCalendarSyncWorker>(6, TimeUnit.HOURS)
                    .setConstraints(constraints)
                    .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request,
                )
        }
    }
}
