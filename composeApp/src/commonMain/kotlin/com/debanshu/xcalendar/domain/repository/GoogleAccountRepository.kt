package com.debanshu.xcalendar.domain.repository

import com.debanshu.xcalendar.common.model.asEntity
import com.debanshu.xcalendar.common.model.asLink
import com.debanshu.xcalendar.data.localDataSource.GoogleAccountDao
import com.debanshu.xcalendar.domain.model.GoogleAccountLink
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

@Single(binds = [IGoogleAccountRepository::class])
class GoogleAccountRepository(
    private val googleAccountDao: GoogleAccountDao,
) : BaseRepository(), IGoogleAccountRepository {
    override fun getAccountForPerson(personId: String): Flow<GoogleAccountLink?> =
        safeFlow(
            flowName = "getAccountForPerson($personId)",
            defaultValue = null,
            flow = googleAccountDao.getAccountForPerson(personId).map { it?.asLink() },
        )

    override fun getAllAccounts(): Flow<List<GoogleAccountLink>> =
        safeFlow(
            flowName = "getAllAccounts",
            defaultValue = emptyList(),
            flow = googleAccountDao.getAllAccounts().map { accounts -> accounts.map { it.asLink() } },
        )

    override suspend fun getAccountById(accountId: String): GoogleAccountLink? =
        safeCallOrThrow("getAccountById($accountId)") {
            googleAccountDao.getAccountById(accountId)?.asLink()
        }

    override suspend fun upsertAccount(account: GoogleAccountLink) =
        safeCallOrThrow("upsertAccount(${account.id})") {
            googleAccountDao.upsertAccount(account.asEntity())
        }

    override suspend fun deleteAccount(account: GoogleAccountLink) =
        safeCallOrThrow("deleteAccount(${account.id})") {
            googleAccountDao.deleteAccount(account.asEntity())
        }
}
