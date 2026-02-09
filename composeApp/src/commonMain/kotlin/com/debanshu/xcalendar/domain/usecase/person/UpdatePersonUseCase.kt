package com.debanshu.xcalendar.domain.usecase.person

import com.debanshu.xcalendar.domain.model.Person
import com.debanshu.xcalendar.domain.repository.IPersonRepository
import org.koin.core.annotation.Factory

@Factory
class UpdatePersonUseCase(
    private val personRepository: IPersonRepository,
) {
    suspend operator fun invoke(person: Person): Person {
        personRepository.upsertPerson(person)
        return person
    }
}
