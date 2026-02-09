package com.debanshu.xcalendar.domain.repository

import com.debanshu.xcalendar.common.model.asInboxItem
import com.debanshu.xcalendar.common.model.asInboxItemEntity
import com.debanshu.xcalendar.data.localDataSource.InboxItemDao
import com.debanshu.xcalendar.domain.model.InboxItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

@Single(binds = [IInboxRepository::class])
class InboxRepository(
    private val inboxItemDao: InboxItemDao,
) : BaseRepository(), IInboxRepository {
    override fun getInboxItems(): Flow<List<InboxItem>> =
        safeFlow(
            flowName = "getInboxItems",
            defaultValue = emptyList(),
            flow = inboxItemDao.getActiveInboxItems().map { entities -> entities.map { it.asInboxItem() } },
        )

    override suspend fun getInboxItemById(itemId: String): InboxItem? =
        safeCallOrThrow("getInboxItemById($itemId)") {
            inboxItemDao.getInboxItemById(itemId)?.asInboxItem()
        }

    override suspend fun upsertInboxItem(item: InboxItem) =
        safeCallOrThrow("upsertInboxItem(${item.id})") {
            inboxItemDao.upsertInboxItem(item.asInboxItemEntity())
        }

    override suspend fun upsertInboxItems(items: List<InboxItem>) =
        safeCallOrThrow("upsertInboxItems(${items.size})") {
            inboxItemDao.upsertInboxItems(items.map { it.asInboxItemEntity() })
        }

    override suspend fun deleteInboxItem(item: InboxItem) =
        safeCallOrThrow("deleteInboxItem(${item.id})") {
            inboxItemDao.deleteInboxItem(item.asInboxItemEntity())
        }
}
