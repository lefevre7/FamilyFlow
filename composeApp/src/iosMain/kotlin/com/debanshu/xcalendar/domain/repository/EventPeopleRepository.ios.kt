package com.debanshu.xcalendar.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.koin.core.annotation.Single

@Single(binds = [IEventPeopleRepository::class])
class EventPeopleRepository : IEventPeopleRepository {
    private val state = MutableStateFlow<Map<String, List<String>>>(emptyMap())

    override val mappings: Flow<Map<String, List<String>>> = state

    override suspend fun getPeopleForEvent(eventId: String): List<String> =
        state.value[eventId].orEmpty()

    override suspend fun setPeopleForEvent(eventId: String, personIds: List<String>) {
        val cleaned = personIds.filter { it.isNotBlank() }.distinct()
        state.update { current ->
            val next = current.toMutableMap()
            if (cleaned.isEmpty()) {
                next.remove(eventId)
            } else {
                next[eventId] = cleaned
            }
            next
        }
    }

    override suspend fun removeEvent(eventId: String) {
        state.update { current ->
            if (!current.containsKey(eventId)) return@update current
            current.toMutableMap().apply { remove(eventId) }
        }
    }
}
