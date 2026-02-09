package com.debanshu.xcalendar.domain.usecase.inbox

import com.debanshu.xcalendar.domain.model.InboxItem
import com.debanshu.xcalendar.domain.repository.IInboxRepository
import org.koin.core.annotation.Factory

@Factory
class CreateInboxItemUseCase(
    private val inboxRepository: IInboxRepository,
) {
    suspend operator fun invoke(item: InboxItem): InboxItem {
        inboxRepository.upsertInboxItem(item)
        return item
    }
}
