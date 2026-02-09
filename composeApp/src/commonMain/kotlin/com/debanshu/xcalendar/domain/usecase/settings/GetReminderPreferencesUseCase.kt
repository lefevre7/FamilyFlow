package com.debanshu.xcalendar.domain.usecase.settings

import com.debanshu.xcalendar.domain.model.ReminderPreferences
import com.debanshu.xcalendar.domain.repository.IReminderPreferencesRepository
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Factory

@Factory
class GetReminderPreferencesUseCase(
    private val repository: IReminderPreferencesRepository,
) {
    operator fun invoke(): Flow<ReminderPreferences> = repository.preferences
}
