package com.debanshu.xcalendar.domain.usecase.person

import com.debanshu.xcalendar.domain.model.Person
import com.debanshu.xcalendar.domain.repository.IPersonRepository
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Factory

@Factory
class GetPeopleUseCase(
    private val personRepository: IPersonRepository,
) {
    operator fun invoke(): Flow<List<Person>> = personRepository.getPeople()
}
