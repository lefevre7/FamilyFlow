package com.debanshu.xcalendar.domain.usecase.google

import com.debanshu.xcalendar.domain.model.GoogleAccountLink
import com.debanshu.xcalendar.domain.repository.IGoogleAccountRepository
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Factory

@Factory
class GetGoogleAccountForPersonUseCase(
    private val repository: IGoogleAccountRepository,
) {
    operator fun invoke(personId: String): Flow<GoogleAccountLink?> =
        repository.getAccountForPerson(personId)
}
