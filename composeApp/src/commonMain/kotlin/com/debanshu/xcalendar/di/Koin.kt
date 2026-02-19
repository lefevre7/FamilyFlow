package com.debanshu.xcalendar.di

import com.debanshu.xcalendar.data.localDataSource.AppDatabase
import com.debanshu.xcalendar.data.localDataSource.CalendarDao
import com.debanshu.xcalendar.data.localDataSource.CalendarSourceDao
import com.debanshu.xcalendar.data.localDataSource.EventDao
import com.debanshu.xcalendar.data.localDataSource.GoogleAccountDao
import com.debanshu.xcalendar.data.localDataSource.HolidayAnnotationDao
import com.debanshu.xcalendar.data.localDataSource.HolidayDao
import com.debanshu.xcalendar.data.localDataSource.InboxItemDao
import com.debanshu.xcalendar.data.localDataSource.PersonDao
import com.debanshu.xcalendar.data.localDataSource.ProjectDao
import com.debanshu.xcalendar.data.localDataSource.RoutineDao
import com.debanshu.xcalendar.data.localDataSource.SyncFailureDao
import com.debanshu.xcalendar.data.localDataSource.TaskDao
import com.debanshu.xcalendar.data.localDataSource.UserDao
import com.debanshu.xcalendar.data.store.EventBookkeeperFactory
import com.debanshu.xcalendar.data.store.EventKey
import com.debanshu.xcalendar.data.store.EventStoreFactory
import com.debanshu.xcalendar.data.store.HolidayKey
import com.debanshu.xcalendar.data.store.HolidayStoreFactory
import com.debanshu.xcalendar.data.store.SingleEventBookkeeperFactory
import com.debanshu.xcalendar.data.store.SingleEventKey
import com.debanshu.xcalendar.data.store.SingleEventStoreFactory
import com.debanshu.xcalendar.data.remoteDataSource.HolidayApiService
import com.debanshu.xcalendar.data.remoteDataSource.RemoteCalendarApiService
import com.debanshu.xcalendar.domain.model.Event
import com.debanshu.xcalendar.domain.model.Holiday
import com.debanshu.xcalendar.domain.usecase.google.GetAllGoogleAccountsUseCase
import org.mobilenativefoundation.store.store5.Bookkeeper
import org.mobilenativefoundation.store.store5.MutableStore
import org.mobilenativefoundation.store.store5.Store
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.ktor.http.ContentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.ksp.generated.module

@Module
class PlatformModule {
    @Single
    fun getLocalDatabase() = getDatabase()
}

expect fun getDatabase(): AppDatabase

@Module
@ComponentScan("com.debanshu.xcalendar.data")
class DataModule {

    @Single
    fun json() = Json {
        prettyPrint = true
        isLenient = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    @Single
    fun httpClient(json: Json) = HttpClient {
        install(ContentNegotiation) {
            json(json, contentType = ContentType.Application.Json)
        }
        
        // Configure timeouts
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000L  // 30 seconds
            connectTimeoutMillis = 15_000L  // 15 seconds
            socketTimeoutMillis = 30_000L   // 30 seconds
        }
        
        // Configure retry logic for transient failures
        install(HttpRequestRetry) {
            maxRetries = 3
            retryIf { _, response ->
                !response.status.isSuccess() && response.status.value in 500..599
            }
            retryOnServerErrors(maxRetries = 3)
            exponentialDelay()
        }
    }

    @Single
    fun getUserEntityDao(appDatabase: AppDatabase): UserDao = appDatabase.getUserEntityDao()

    @Single
    fun getCalendarEntityDao(appDatabase: AppDatabase): CalendarDao =
        appDatabase.getCalendarEntityDao()

    @Single
    fun getCalendarSourceDao(appDatabase: AppDatabase): CalendarSourceDao =
        appDatabase.getCalendarSourceDao()

    @Single
    fun getEventEntityDao(appDatabase: AppDatabase): EventDao = appDatabase.getEventEntityDao()

    @Single
    fun getHolidayEntityDao(appDatabase: AppDatabase): HolidayDao =
        appDatabase.getHolidayEntityDao()

    @Single
    fun getSyncFailureDao(appDatabase: AppDatabase): SyncFailureDao =
        appDatabase.getSyncFailureDao()

    @Single
    fun getPersonDao(appDatabase: AppDatabase): PersonDao = appDatabase.getPersonDao()

    @Single
    fun getTaskDao(appDatabase: AppDatabase): TaskDao = appDatabase.getTaskDao()

    @Single
    fun getRoutineDao(appDatabase: AppDatabase): RoutineDao = appDatabase.getRoutineDao()

    @Single
    fun getProjectDao(appDatabase: AppDatabase): ProjectDao = appDatabase.getProjectDao()

    @Single
    fun getInboxItemDao(appDatabase: AppDatabase): InboxItemDao = appDatabase.getInboxItemDao()

    @Single
    fun getGoogleAccountDao(appDatabase: AppDatabase): GoogleAccountDao =
        appDatabase.getGoogleAccountDao()

    @Single
    fun getHolidayAnnotationDao(appDatabase: AppDatabase): HolidayAnnotationDao =
        appDatabase.getHolidayAnnotationDao()

    @Single
    fun provideHolidayStore(
        holidayApiService: HolidayApiService,
        holidayDao: HolidayDao
    ): Store<HolidayKey, List<Holiday>> =
        HolidayStoreFactory.create(holidayApiService, holidayDao)

    @Single
    @Named("eventBookkeeper")
    fun provideEventBookkeeper(
        syncFailureDao: SyncFailureDao
    ): Bookkeeper<EventKey> =
        EventBookkeeperFactory.create(syncFailureDao)

    @Single
    @Named("singleEventBookkeeper")
    fun provideSingleEventBookkeeper(
        syncFailureDao: SyncFailureDao
    ): Bookkeeper<SingleEventKey> =
        SingleEventBookkeeperFactory.create(syncFailureDao)

    @Single
    @Named("eventStore")
    fun provideEventStore(
        apiService: RemoteCalendarApiService,
        eventDao: EventDao,
        @Named("eventBookkeeper") bookkeeper: Bookkeeper<EventKey>,
    ): MutableStore<EventKey, List<Event>> =
        EventStoreFactory.create(apiService, eventDao, bookkeeper)

    @Single
    @Named("singleEventStore")
    fun provideSingleEventStore(
        eventDao: EventDao,
        @Named("singleEventBookkeeper") bookkeeper: Bookkeeper<SingleEventKey>
    ): MutableStore<SingleEventKey, Event> =
        SingleEventStoreFactory.create(eventDao, bookkeeper)
}

@Module
@ComponentScan("com.debanshu.xcalendar.ui")
class ViewModelModule

@Module
@ComponentScan("com.debanshu.xcalendar.domain.repository")
class DomainModule

@Module
@ComponentScan("com.debanshu.xcalendar.domain.auth")
class AuthModule

@Module
@ComponentScan("com.debanshu.xcalendar.domain.llm")
class LlmModule

@Module
@ComponentScan("com.debanshu.xcalendar.domain.sync")
class SyncModule

@Module
@ComponentScan("com.debanshu.xcalendar.domain.notifications")
class NotificationsModule

@Module
@ComponentScan("com.debanshu.xcalendar.domain.widgets")
class WidgetsModule

@Module
@ComponentScan("com.debanshu.xcalendar.domain.usecase")
class UseCaseModule

@Module
@ComponentScan("com.debanshu.xcalendar.domain.states")
class StateModule

@Module
@ComponentScan("com.debanshu.xcalendar.ui.state")
class UiStateModule

@Module(
    includes = [PlatformModule::class, DataModule::class, ViewModelModule::class,
        DomainModule::class, AuthModule::class, LlmModule::class, SyncModule::class,
        NotificationsModule::class, WidgetsModule::class,
        UseCaseModule::class, StateModule::class, UiStateModule::class]
)
class AppModule

fun initKoin(config: KoinAppDeclaration? = null) {
    startKoin {
        modules(
            AppModule().module
        )
        config?.invoke(this)
    }
}
