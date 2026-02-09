package com.debanshu.xcalendar.data.localDataSource

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.debanshu.xcalendar.data.localDataSource.model.PersonEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonDao {
    @Query("SELECT * FROM people WHERE isArchived = 0 ORDER BY sortOrder ASC, name ASC")
    fun getActivePeople(): Flow<List<PersonEntity>>

    @Query("SELECT COUNT(*) FROM people")
    suspend fun getPersonCount(): Int

    @Upsert
    suspend fun upsertPerson(person: PersonEntity)

    @Upsert
    suspend fun upsertPeople(people: List<PersonEntity>)

    @Delete
    suspend fun deletePerson(person: PersonEntity)
}
