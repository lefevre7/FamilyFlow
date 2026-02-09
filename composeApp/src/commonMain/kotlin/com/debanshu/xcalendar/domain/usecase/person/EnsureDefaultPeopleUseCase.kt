package com.debanshu.xcalendar.domain.usecase.person

import com.debanshu.xcalendar.domain.repository.IPersonRepository
import org.koin.core.annotation.Factory

@Factory
class EnsureDefaultPeopleUseCase(
    private val personRepository: IPersonRepository,
) {
    suspend operator fun invoke() {
        personRepository.ensureDefaultPeople()
    }
}
