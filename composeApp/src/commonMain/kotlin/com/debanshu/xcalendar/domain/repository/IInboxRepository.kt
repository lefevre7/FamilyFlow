package com.debanshu.xcalendar.domain.repository

import com.debanshu.xcalendar.domain.model.InboxItem
import kotlinx.coroutines.flow.Flow

interface IInboxRepository {
    fun getInboxItems(): Flow<List<InboxItem>>
    suspend fun getInboxItemById(itemId: String): InboxItem?
    suspend fun upsertInboxItem(item: InboxItem)
    suspend fun upsertInboxItems(items: List<InboxItem>)
    suspend fun deleteInboxItem(item: InboxItem)
}
