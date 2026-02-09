package com.debanshu.xcalendar.domain.repository

import com.debanshu.xcalendar.domain.model.GoogleAccountLink
import kotlinx.coroutines.flow.Flow

interface IGoogleAccountRepository {
    fun getAccountForPerson(personId: String): Flow<GoogleAccountLink?>

    fun getAllAccounts(): Flow<List<GoogleAccountLink>>

    suspend fun getAccountById(accountId: String): GoogleAccountLink?

    suspend fun upsertAccount(account: GoogleAccountLink)

    suspend fun deleteAccount(account: GoogleAccountLink)
}
