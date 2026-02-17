package com.debanshu.xcalendar.data.localDataSource

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.debanshu.xcalendar.data.localDataSource.model.CalendarEntity
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

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    application = Application::class,
)
class EventDaoRangeOverlapTest {
    private lateinit var database: AppDatabase
    private lateinit var userDao: UserDao
    private lateinit var calendarDao: CalendarDao
    private lateinit var eventDao: EventDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "event-range-overlap-${System.nanoTime()}.db"
        database =
            Room.databaseBuilder<AppDatabase>(
                context = context,
                name = context.getDatabasePath(dbName).absolutePath,
            )
                .allowMainThreadQueries()
                .build()
        userDao = database.getUserEntityDao()
        calendarDao = database.getCalendarEntityDao()
        eventDao = database.getEventEntityDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun getEventsWithRemindersBetweenDates_returnsOverlappingEvents() = runTest {
        val userId = "user_id"
        userDao.insertUser(
            UserEntity(
                id = userId,
                name = "Mom",
                email = "mom@example.com",
                photoUrl = "",
            ),
        )
        calendarDao.upsertCalendar(
            listOf(
                CalendarEntity(
                    id = "cal-1",
                    name = "Family",
                    color = 0,
                    userId = userId,
                    isVisible = true,
                    isPrimary = true,
                ),
            ),
        )

        eventDao.insertEventWithReminders(
            event =
                EventEntity(
                    id = "event-overlap-start",
                    calendarId = "cal-1",
                    calendarName = "Family",
                    title = "Overlap Start",
                    startTime = 500L,
                    endTime = 1_200L,
                    isAllDay = false,
                    isRecurring = false,
                ),
            reminders = emptyList(),
        )
        eventDao.insertEventWithReminders(
            event =
                EventEntity(
                    id = "event-overlap-end",
                    calendarId = "cal-1",
                    calendarName = "Family",
                    title = "Overlap End",
                    startTime = 1_500L,
                    endTime = 2_500L,
                    isAllDay = false,
                    isRecurring = false,
                ),
            reminders = emptyList(),
        )
        eventDao.insertEventWithReminders(
            event =
                EventEntity(
                    id = "event-outside",
                    calendarId = "cal-1",
                    calendarName = "Family",
                    title = "Outside",
                    startTime = 2_000L,
                    endTime = 3_000L,
                    isAllDay = false,
                    isRecurring = false,
                ),
            reminders = emptyList(),
        )

        val events = eventDao.getEventsWithRemindersBetweenDates(userId, 1_000L, 2_000L).first()

        assertEquals(
            setOf("event-overlap-start", "event-overlap-end"),
            events.map { it.event.id }.toSet(),
        )
    }
}
