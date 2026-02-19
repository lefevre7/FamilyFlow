package com.debanshu.xcalendar.data.remoteDataSource

import com.debanshu.xcalendar.data.remoteDataSource.error.DataError
import com.debanshu.xcalendar.data.remoteDataSource.model.calendar.CalendarResponseItem
import com.debanshu.xcalendar.data.remoteDataSource.model.calendar.EventResponseItem
import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single

@Single
class RemoteCalendarApiService(
    client: HttpClient,
    json: Json,
) {
    private val clientWrapper = ClientWrapper(client, json)
    private val baseUrl = "https://raw.githubusercontent.com/lefevre7/FamilyFlow/main/"

    /**
     * Returns empty list intentionally.
     *
     * Demo calendar data (assets/calendars.json) is only used in unit / instrumented tests.
     * In production, calendars come exclusively from Google Calendar via
     * [ImportGoogleCalendarsUseCase]. A guaranteed local "Personal" calendar is seeded by
     * [EnsureDefaultCalendarsUseCase] so the user always has at least one calendar to write to.
     */
    suspend fun fetchCalendarsForUser(@Suppress("UNUSED_PARAMETER") userId: String): Result<List<CalendarResponseItem>, DataError> =
        Result.Success(emptyList())

    suspend fun fetchEventsForCalendar(
        calendarIds: List<String>,
        startTime: Long,
        endTime: Long,
    ): Result<List<EventResponseItem>, DataError> =
        clientWrapper.networkGetUsecase<List<EventResponseItem>>(
            baseUrl + "assets/events.json",
            mapOf(
                "calendar_ids" to calendarIds.toString(),
                "start_time" to startTime.toString(),
                "end_time" to endTime.toString(),
            ),
        )
}
