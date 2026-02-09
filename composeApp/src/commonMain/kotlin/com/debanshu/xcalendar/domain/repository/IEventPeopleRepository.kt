package com.debanshu.xcalendar.domain.repository

import kotlinx.coroutines.flow.Flow

interface IEventPeopleRepository {
    val mappings: Flow<Map<String, List<String>>>

    suspend fun getPeopleForEvent(eventId: String): List<String>

    suspend fun setPeopleForEvent(eventId: String, personIds: List<String>)

    suspend fun removeEvent(eventId: String)
}
