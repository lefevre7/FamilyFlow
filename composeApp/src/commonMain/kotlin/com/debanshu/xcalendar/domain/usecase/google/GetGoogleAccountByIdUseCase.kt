package com.debanshu.xcalendar.domain.usecase.google

import com.debanshu.xcalendar.domain.model.GoogleAccountLink
import com.debanshu.xcalendar.domain.repository.IGoogleAccountRepository
import org.koin.core.annotation.Factory

@Factory
class GetGoogleAccountByIdUseCase(
    private val repository: IGoogleAccountRepository,
) {
    suspend operator fun invoke(accountId: String): GoogleAccountLink? =
        repository.getAccountById(accountId)
}
