package com.debanshu.xcalendar.data.localDataSource

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.debanshu.xcalendar.data.localDataSource.model.GoogleAccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoogleAccountDao {
    @Query("SELECT * FROM google_accounts WHERE personId = :personId LIMIT 1")
    fun getAccountForPerson(personId: String): Flow<GoogleAccountEntity?>

    @Query("SELECT * FROM google_accounts")
    fun getAllAccounts(): Flow<List<GoogleAccountEntity>>

    @Query("SELECT * FROM google_accounts WHERE id = :accountId LIMIT 1")
    suspend fun getAccountById(accountId: String): GoogleAccountEntity?

    @Upsert
    suspend fun upsertAccount(account: GoogleAccountEntity)

    @Delete
    suspend fun deleteAccount(account: GoogleAccountEntity)
}
