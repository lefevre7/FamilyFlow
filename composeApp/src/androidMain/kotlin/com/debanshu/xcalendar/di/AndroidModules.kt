package com.debanshu.xcalendar.di

import com.debanshu.xcalendar.data.google.GoogleCalendarApi
import com.debanshu.xcalendar.domain.auth.GoogleTokenManager
import com.debanshu.xcalendar.domain.auth.GoogleTokenStore
import com.debanshu.xcalendar.domain.auth.PlatformGoogleTokenStore
import com.debanshu.xcalendar.domain.llm.LocalLlmManager
import com.debanshu.xcalendar.domain.llm.LocalLlmRuntimeFactory
import com.debanshu.xcalendar.domain.llm.OcrLlmClient
import com.debanshu.xcalendar.domain.llm.PlatformLocalLlmManager
import com.debanshu.xcalendar.domain.llm.PlatformOcrLlmClient
import com.debanshu.xcalendar.domain.llm.LiteRtLlmRuntimeFactory
import com.debanshu.xcalendar.domain.notifications.AndroidReminderScheduler
import com.debanshu.xcalendar.domain.notifications.ReminderScheduler
import com.debanshu.xcalendar.domain.repository.DateSelectionPreferencesRepository
import com.debanshu.xcalendar.domain.repository.EventPeopleRepository
import com.debanshu.xcalendar.domain.repository.IDateSelectionPreferencesRepository
import com.debanshu.xcalendar.domain.repository.IEventPeopleRepository
import com.debanshu.xcalendar.domain.repository.IHolidayPreferencesRepository
import com.debanshu.xcalendar.domain.repository.ILensPreferencesRepository
import com.debanshu.xcalendar.domain.repository.IReminderPreferencesRepository
import com.debanshu.xcalendar.domain.repository.IUiPreferencesRepository
import com.debanshu.xcalendar.domain.repository.IVoiceDiagnosticsRepository
import com.debanshu.xcalendar.domain.repository.HolidayPreferencesRepository
import com.debanshu.xcalendar.domain.repository.LensPreferencesRepository
import com.debanshu.xcalendar.domain.repository.ReminderPreferencesRepository
import com.debanshu.xcalendar.domain.repository.UiPreferencesRepository
import com.debanshu.xcalendar.domain.repository.VoiceDiagnosticsRepository
import com.debanshu.xcalendar.domain.sync.CalendarSyncManager
import com.debanshu.xcalendar.domain.sync.GoogleCalendarSyncManager
import com.debanshu.xcalendar.domain.widgets.AndroidWidgetUpdater
import com.debanshu.xcalendar.domain.widgets.WidgetUpdater
import com.debanshu.xcalendar.platform.PlatformNotifier
import com.debanshu.xcalendar.platform.PlatformNotifierImpl
import org.koin.dsl.module

val androidPlatformModule =
    module {
        single { GoogleCalendarApi(get(), get()) }
        single<CalendarSyncManager> { GoogleCalendarSyncManager(get()) }

        single { GoogleTokenManager(get()) }
        single<GoogleTokenStore> { PlatformGoogleTokenStore() }
        single<IDateSelectionPreferencesRepository> { DateSelectionPreferencesRepository() }
        single<IEventPeopleRepository> { EventPeopleRepository() }
        single<IHolidayPreferencesRepository> { HolidayPreferencesRepository() }
        single<ILensPreferencesRepository> { LensPreferencesRepository() }
        single<IReminderPreferencesRepository> { ReminderPreferencesRepository() }
        single<IUiPreferencesRepository> { UiPreferencesRepository() }
        single<IVoiceDiagnosticsRepository> { VoiceDiagnosticsRepository() }
        single<ReminderScheduler> { AndroidReminderScheduler() }
        single<WidgetUpdater> { AndroidWidgetUpdater() }
        single<PlatformNotifier> { PlatformNotifierImpl() }

        single<LocalLlmRuntimeFactory> { LiteRtLlmRuntimeFactory() }
        single<LocalLlmManager> { PlatformLocalLlmManager() }
        single<OcrLlmClient> { PlatformOcrLlmClient() }
    }
