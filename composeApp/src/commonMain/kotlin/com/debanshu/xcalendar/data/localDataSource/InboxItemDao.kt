package com.debanshu.xcalendar.data.localDataSource

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.debanshu.xcalendar.data.localDataSource.model.InboxItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InboxItemDao {
    @Query("SELECT * FROM inbox_items WHERE status != 'ARCHIVED' ORDER BY createdAt DESC")
    fun getActiveInboxItems(): Flow<List<InboxItemEntity>>

    @Query("SELECT * FROM inbox_items WHERE id = :itemId")
    suspend fun getInboxItemById(itemId: String): InboxItemEntity?

    @Upsert
    suspend fun upsertInboxItem(item: InboxItemEntity)

    @Upsert
    suspend fun upsertInboxItems(items: List<InboxItemEntity>)

    @Delete
    suspend fun deleteInboxItem(item: InboxItemEntity)
}
