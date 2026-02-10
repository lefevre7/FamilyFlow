package com.debanshu.xcalendar.domain.usecase.person

import com.debanshu.xcalendar.domain.model.Person
import com.debanshu.xcalendar.domain.repository.IPersonRepository
import org.koin.core.annotation.Factory

@Factory
class DeletePersonUseCase(
    private val personRepository: IPersonRepository,
) {
    suspend operator fun invoke(person: Person) {
        personRepository.deletePerson(person)
    }
}
