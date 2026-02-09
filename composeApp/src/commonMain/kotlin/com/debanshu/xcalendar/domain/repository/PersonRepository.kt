package com.debanshu.xcalendar.domain.repository

import com.debanshu.xcalendar.common.convertStringToColor
import com.debanshu.xcalendar.common.model.asPerson
import com.debanshu.xcalendar.common.model.asPersonEntity
import com.debanshu.xcalendar.data.localDataSource.PersonDao
import com.debanshu.xcalendar.domain.model.Person
import com.debanshu.xcalendar.domain.model.PersonRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import org.koin.core.annotation.Single

@Single(binds = [IPersonRepository::class])
class PersonRepository(
    private val personDao: PersonDao,
) : BaseRepository(), IPersonRepository {
    override fun getPeople(): Flow<List<Person>> =
        safeFlow(
            flowName = "getPeople",
            defaultValue = emptyList(),
            flow = personDao.getActivePeople().map { entities -> entities.map { it.asPerson() } },
        )

    override suspend fun upsertPeople(people: List<Person>) =
        safeCallOrThrow("upsertPeople(${people.size})") {
            personDao.upsertPeople(people.map { it.asPersonEntity() })
        }

    override suspend fun upsertPerson(person: Person) =
        safeCallOrThrow("upsertPerson(${person.id})") {
            personDao.upsertPerson(person.asPersonEntity())
        }

    override suspend fun deletePerson(person: Person) =
        safeCallOrThrow("deletePerson(${person.id})") {
            personDao.deletePerson(person.asPersonEntity())
        }

    override suspend fun ensureDefaultPeople() =
        safeCallOrThrow("ensureDefaultPeople") {
            if (personDao.getPersonCount() > 0) return@safeCallOrThrow
            val now = Clock.System.now().toEpochMilliseconds()
            val defaults =
                listOf(
                    createPerson(
                        id = "person_mom",
                        name = "Mom",
                        role = PersonRole.MOM,
                        ageYears = null,
                        isAdmin = true,
                        isDefault = true,
                        sortOrder = 0,
                        now = now,
                    ),
                    createPerson(
                        id = "person_partner",
                        name = "Partner",
                        role = PersonRole.PARTNER,
                        ageYears = null,
                        isAdmin = false,
                        isDefault = false,
                        sortOrder = 1,
                        now = now,
                    ),
                    createPerson(
                        id = "person_kid_4",
                        name = "Kid (4)",
                        role = PersonRole.CHILD,
                        ageYears = 4,
                        isAdmin = false,
                        isDefault = false,
                        sortOrder = 2,
                        now = now,
                    ),
                    createPerson(
                        id = "person_kid_2",
                        name = "Kid (2)",
                        role = PersonRole.CHILD,
                        ageYears = 2,
                        isAdmin = false,
                        isDefault = false,
                        sortOrder = 3,
                        now = now,
                    ),
                    createPerson(
                        id = "person_kid_1",
                        name = "Kid (1)",
                        role = PersonRole.CHILD,
                        ageYears = 1,
                        isAdmin = false,
                        isDefault = false,
                        sortOrder = 4,
                        now = now,
                    ),
                )
            personDao.upsertPeople(defaults.map { it.asPersonEntity() })
        }

    private fun createPerson(
        id: String,
        name: String,
        role: PersonRole,
        ageYears: Int?,
        isAdmin: Boolean,
        isDefault: Boolean,
        sortOrder: Int,
        now: Long,
    ): Person {
        return Person(
            id = id,
            name = name,
            role = role,
            ageYears = ageYears,
            color = convertStringToColor(id + name),
            avatarUrl = "",
            isAdmin = isAdmin,
            isDefault = isDefault,
            sortOrder = sortOrder,
            isArchived = false,
            createdAt = now,
            updatedAt = now,
        )
    }
}
