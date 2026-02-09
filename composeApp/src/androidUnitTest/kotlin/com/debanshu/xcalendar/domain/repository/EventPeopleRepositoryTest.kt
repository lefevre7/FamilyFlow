package com.debanshu.xcalendar.domain.repository

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    application = Application::class,
)
class EventPeopleRepositoryTest {

    @Before
    fun setUp() {
        stopKoin()
        val context = ApplicationProvider.getApplicationContext<Context>()
        startKoin {
            modules(
                module {
                    single<Context> { context }
                },
            )
        }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun setPeopleForEvent_deduplicatesAndRemovesMappings() =
        runBlocking {
            val repository = EventPeopleRepository()
            val eventId = "event-${System.nanoTime()}"

            repository.setPeopleForEvent(
                eventId = eventId,
                personIds = listOf("person_mom", "person_kid_4", "person_mom", "", " "),
            )
            assertEquals(
                listOf("person_mom", "person_kid_4"),
                repository.getPeopleForEvent(eventId),
            )

            repository.removeEvent(eventId)
            assertTrue(repository.getPeopleForEvent(eventId).isEmpty())
        }
}
