package com.debanshu.xcalendar.domain.usecase.inbox

import com.debanshu.xcalendar.domain.model.InboxItem
import com.debanshu.xcalendar.domain.model.InboxStatus
import com.debanshu.xcalendar.domain.repository.IInboxRepository
import org.koin.core.annotation.Factory

@Factory
class UpdateInboxItemStatusUseCase(
    private val inboxRepository: IInboxRepository,
) {
    suspend operator fun invoke(item: InboxItem, status: InboxStatus): InboxItem {
        val updated = item.copy(status = status)
        inboxRepository.upsertInboxItem(updated)
        return updated
    }
}
