package com.debanshu.xcalendar.domain.usecase.inbox

import com.debanshu.xcalendar.domain.model.InboxItem
import com.debanshu.xcalendar.domain.repository.IInboxRepository
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Factory

@Factory
class GetInboxItemsUseCase(
    private val inboxRepository: IInboxRepository,
) {
    operator fun invoke(): Flow<List<InboxItem>> = inboxRepository.getInboxItems()
}
