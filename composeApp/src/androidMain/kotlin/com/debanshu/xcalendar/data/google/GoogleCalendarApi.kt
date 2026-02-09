package com.debanshu.xcalendar.data.google

import com.debanshu.xcalendar.domain.auth.GoogleTokenManager
import com.debanshu.xcalendar.domain.model.ExternalCalendar
import com.debanshu.xcalendar.domain.model.ExternalEvent
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.encodeURLPath
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.koin.core.annotation.Single

@Single
class GoogleCalendarApi(
    private val client: HttpClient,
    private val tokenManager: GoogleTokenManager,
) {
    suspend fun listCalendars(accountId: String): List<ExternalCalendar> {
        val token = tokenManager.getValidAccessToken(accountId) ?: return emptyList()
        val calendars = mutableListOf<ExternalCalendar>()
        var pageToken: String? = null
        do {
            val response: CalendarListResponse = client.get("$BASE_URL/users/me/calendarList") {
                header(HttpHeaders.Authorization, "Bearer $token")
                url {
                    pageToken?.let { parameters.append("pageToken", it) }
                }
            }.body()
            calendars += response.items.map { item ->
                ExternalCalendar(
                    id = item.id,
                    name = item.summary,
                    colorHex = item.backgroundColor,
                    primary = item.primary ?: false,
                    timeZone = item.timeZone,
                    accessRole = item.accessRole,
                )
            }
            pageToken = response.nextPageToken
        } while (!pageToken.isNullOrBlank())
        return calendars
    }

    suspend fun listEvents(
        accountId: String,
        calendarId: String,
        timeMin: Long,
        timeMax: Long,
    ): List<ExternalEvent> {
        val token = tokenManager.getValidAccessToken(accountId) ?: return emptyList()
        val encodedId = calendarId.encodeURLPath()
        val events = mutableListOf<ExternalEvent>()
        var pageToken: String? = null
        do {
            val response: EventListResponse = client.get("$BASE_URL/calendars/$encodedId/events") {
                header(HttpHeaders.Authorization, "Bearer $token")
                url {
                    parameters.append("timeMin", Instant.fromEpochMilliseconds(timeMin).toString())
                    parameters.append("timeMax", Instant.fromEpochMilliseconds(timeMax).toString())
                    parameters.append("singleEvents", "true")
                    parameters.append("showDeleted", "true")
                    parameters.append("orderBy", "startTime")
                    parameters.append("maxResults", "2500")
                    pageToken?.let { parameters.append("pageToken", it) }
                }
            }.body()
            events += response.items.mapNotNull { item -> item.toExternalEvent() }
            pageToken = response.nextPageToken
        } while (!pageToken.isNullOrBlank())
        return events
    }

    suspend fun createEvent(
        accountId: String,
        calendarId: String,
        event: ExternalEvent,
    ): ExternalEvent? {
        val token = tokenManager.getValidAccessToken(accountId) ?: return null
        val request = event.toRequest()
        val encodedId = calendarId.encodeURLPath()
        val response: EventItem = client.post("$BASE_URL/calendars/$encodedId/events") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
        return response.toExternalEvent()
    }

    suspend fun updateEvent(
        accountId: String,
        calendarId: String,
        eventId: String,
        event: ExternalEvent,
    ): ExternalEvent? {
        val token = tokenManager.getValidAccessToken(accountId) ?: return null
        val request = event.toRequest()
        val encodedId = calendarId.encodeURLPath()
        val encodedEventId = eventId.encodeURLPath()
        val response: EventItem = client.put("$BASE_URL/calendars/$encodedId/events/$encodedEventId") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
        return response.toExternalEvent()
    }

    suspend fun deleteEvent(
        accountId: String,
        calendarId: String,
        eventId: String,
    ): Boolean {
        val token = tokenManager.getValidAccessToken(accountId) ?: return false
        val encodedId = calendarId.encodeURLPath()
        val encodedEventId = eventId.encodeURLPath()
        client.delete("$BASE_URL/calendars/$encodedId/events/$encodedEventId") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        return true
    }

    private fun EventItem.toExternalEvent(): ExternalEvent? {
        val updatedAt = updated?.let { Instant.parse(it).toEpochMilliseconds() } ?: 0L
        if (status == "cancelled") {
            return ExternalEvent(
                id = id,
                summary = summary ?: "(Cancelled)",
                description = description,
                location = location,
                startTime = 0L,
                endTime = 0L,
                isAllDay = false,
                updatedAt = updatedAt,
                cancelled = true,
            )
        }
        val startValue = start ?: return null
        val endValue = end ?: return null
        val isAllDay = startValue.date != null && startValue.dateTime == null
        val startMillis = startValue.toInstantMillis() ?: return null
        val endMillis = endValue.toInstantMillis() ?: return null
        return ExternalEvent(
            id = id,
            summary = summary ?: "(Untitled)",
            description = description,
            location = location,
            startTime = startMillis,
            endTime = endMillis,
            isAllDay = isAllDay,
            updatedAt = updatedAt,
            cancelled = false,
        )
    }

    private fun ExternalEvent.toRequest(): EventRequest {
        val startValue = if (isAllDay) {
            EventDateTime(date = Instant.fromEpochMilliseconds(startTime).toLocalDateString())
        } else {
            EventDateTime(dateTime = Instant.fromEpochMilliseconds(startTime).toString())
        }
        val endValue = if (isAllDay) {
            EventDateTime(date = Instant.fromEpochMilliseconds(endTime).toLocalDateString())
        } else {
            EventDateTime(dateTime = Instant.fromEpochMilliseconds(endTime).toString())
        }
        return EventRequest(
            summary = summary,
            description = description,
            location = location,
            start = startValue,
            end = endValue,
        )
    }

    private fun EventDateTime.toInstantMillis(): Long? {
        return when {
            dateTime != null -> Instant.parse(dateTime).toEpochMilliseconds()
            date != null -> {
                val localDate = LocalDate.parse(date)
                localDate.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
            }
            else -> null
        }
    }

    private fun Instant.toLocalDateString(): String {
        val date = this.toLocalDateTime(TimeZone.currentSystemDefault()).date
        val month = date.monthNumber.toString().padStart(2, '0')
        val day = date.dayOfMonth.toString().padStart(2, '0')
        return "${date.year}-$month-$day"
    }

    companion object {
        private const val BASE_URL = "https://www.googleapis.com/calendar/v3"
    }
}

@Serializable
private data class CalendarListResponse(
    val items: List<CalendarListItem> = emptyList(),
    val nextPageToken: String? = null,
)

@Serializable
private data class CalendarListItem(
    val id: String,
    val summary: String,
    val primary: Boolean? = null,
    val backgroundColor: String? = null,
    val timeZone: String? = null,
    val accessRole: String? = null,
)

@Serializable
private data class EventListResponse(
    val items: List<EventItem> = emptyList(),
    val nextPageToken: String? = null,
)

@Serializable
private data class EventItem(
    val id: String,
    val summary: String? = null,
    val description: String? = null,
    val location: String? = null,
    val updated: String? = null,
    val status: String? = null,
    val start: EventDateTime? = null,
    val end: EventDateTime? = null,
)

@Serializable
private data class EventRequest(
    val summary: String,
    val description: String? = null,
    val location: String? = null,
    val start: EventDateTime,
    val end: EventDateTime,
)

@Serializable
private data class EventDateTime(
    @SerialName("dateTime")
    val dateTime: String? = null,
    val date: String? = null,
)
