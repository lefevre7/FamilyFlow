package com.debanshu.xcalendar.domain.repository

import com.debanshu.xcalendar.domain.model.Person
import kotlinx.coroutines.flow.Flow

interface IPersonRepository {
    fun getPeople(): Flow<List<Person>>
    suspend fun upsertPeople(people: List<Person>)
    suspend fun upsertPerson(person: Person)
    suspend fun deletePerson(person: Person)
    suspend fun ensureDefaultPeople()
}
