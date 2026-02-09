package com.debanshu.xcalendar.domain.usecase.google

import com.debanshu.xcalendar.domain.model.GoogleAccountLink
import com.debanshu.xcalendar.domain.repository.IGoogleAccountRepository
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Factory

@Factory
class GetAllGoogleAccountsUseCase(
    private val repository: IGoogleAccountRepository,
) {
    operator fun invoke(): Flow<List<GoogleAccountLink>> = repository.getAllAccounts()
}
