package com.debanshu.xcalendar.data.localDataSource

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.debanshu.xcalendar.data.localDataSource.model.CalendarEntity
import com.debanshu.xcalendar.data.localDataSource.model.CalendarSourceEntity
import com.debanshu.xcalendar.data.localDataSource.model.EventEntity
import com.debanshu.xcalendar.data.localDataSource.model.UserEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    application = Application::class,
)
class UserDaoUpsertCascadeTest {
    private lateinit var database: AppDatabase
    private lateinit var userDao: UserDao
    private lateinit var calendarDao: CalendarDao
    private lateinit var calendarSourceDao: CalendarSourceDao
    private lateinit var eventDao: EventDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "user-upsert-${System.nanoTime()}.db"
        database =
            Room.databaseBuilder<AppDatabase>(
                context = context,
                name = context.getDatabasePath(dbName).absolutePath,
            )
                .allowMainThreadQueries()
                .build()
        userDao = database.getUserEntityDao()
        calendarDao = database.getCalendarEntityDao()
        calendarSourceDao = database.getCalendarSourceDao()
        eventDao = database.getEventEntityDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertUser_conflictUpdate_preservesCalendarGraph() =
        runTest {
            val user =
                UserEntity(
                    id = "user_id",
                    name = "Mom",
                    email = "mom@example.com",
                    photoUrl = "",
                )
            userDao.insertUser(user)

            val calendar =
                CalendarEntity(
                    id = "google:account-1:calendar-1",
                    name = "Family",
                    color = 0xFFAA33,
                    userId = user.id,
                    isVisible = true,
                    isPrimary = true,
                )
            calendarDao.upsertCalendar(listOf(calendar))

            calendarSourceDao.upsertSources(
                listOf(
                    CalendarSourceEntity(
                        calendarId = calendar.id,
                        provider = "GOOGLE",
                        providerCalendarId = "calendar-1",
                        providerAccountId = "account-1",
                        syncEnabled = true,
                        lastSyncedAt = null,
                    ),
                ),
            )

            eventDao.insertEventWithReminders(
                event =
                    EventEntity(
                        id = "event-1",
                        calendarId = calendar.id,
                        calendarName = calendar.name,
                        title = "School pickup",
                        description = null,
                        location = null,
                        startTime = 1_000L,
                        endTime = 2_000L,
                        isAllDay = false,
                        isRecurring = false,
                        recurringRule = null,
                    ),
                reminders = emptyList(),
            )

            userDao.insertUser(user.copy(name = "Mom Updated"))

            assertEquals(1, calendarDao.getCalendarsByUserId(user.id).first().size)
            assertEquals(1, calendarSourceDao.getAllSources().size)
            assertNotNull(eventDao.getEventById("event-1"))
        }
}
