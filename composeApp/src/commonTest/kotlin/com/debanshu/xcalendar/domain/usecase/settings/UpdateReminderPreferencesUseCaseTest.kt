package com.debanshu.xcalendar.domain.usecase.settings

import com.debanshu.xcalendar.domain.model.ReminderPreferences
import com.debanshu.xcalendar.domain.repository.IReminderPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class UpdateReminderPreferencesUseCaseTest {

    private class FakeReminderPreferencesRepository : IReminderPreferencesRepository {
        private val state = MutableStateFlow(ReminderPreferences())
        override val preferences: Flow<ReminderPreferences> = state

        var reducedMotion = true
        var highContrast = false
        var textScale = 1.0f

        override suspend fun setRemindersEnabled(enabled: Boolean) = Unit

        override suspend fun setPrepMinutes(minutes: Int) = Unit

        override suspend fun setTravelBufferMinutes(minutes: Int) = Unit

        override suspend fun setAllDayTime(
            hour: Int,
            minute: Int,
        ) = Unit

        override suspend fun setSummaryEnabled(enabled: Boolean) = Unit

        override suspend fun setSummaryTimes(
            morningHour: Int,
            morningMinute: Int,
            middayHour: Int,
            middayMinute: Int,
        ) = Unit

        override suspend fun setReducedMotionEnabled(enabled: Boolean) {
            reducedMotion = enabled
        }

        override suspend fun setHighContrastEnabled(enabled: Boolean) {
            highContrast = enabled
        }

        override suspend fun setTextScale(scale: Float) {
            textScale = scale
        }
    }

    @Test
    fun accessibilitySetters_forwardToRepository() = runTest {
        val repository = FakeReminderPreferencesRepository()
        val useCase = UpdateReminderPreferencesUseCase(repository)

        useCase.setReducedMotionEnabled(false)
        useCase.setHighContrastEnabled(true)
        useCase.setTextScale(1.2f)

        assertEquals(false, repository.reducedMotion)
        assertEquals(true, repository.highContrast)
        assertEquals(1.2f, repository.textScale)
    }
}
