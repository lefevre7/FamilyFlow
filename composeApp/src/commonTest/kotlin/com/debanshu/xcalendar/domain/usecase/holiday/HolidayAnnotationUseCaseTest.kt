package com.debanshu.xcalendar.domain.usecase.holiday

import com.debanshu.xcalendar.domain.model.HolidayAnnotation
import com.debanshu.xcalendar.domain.repository.IHolidayAnnotationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HolidayAnnotationUseCaseTest {

    // ── Fake repository ───────────────────────────────────────────────────────

    private class FakeHolidayAnnotationRepository : IHolidayAnnotationRepository {
        private val store = MutableStateFlow<Map<String, HolidayAnnotation>>(emptyMap())

        val savedAnnotations: Map<String, HolidayAnnotation> get() = store.value
        val deletedIds = mutableListOf<String>()

        override fun getAnnotation(holidayId: String): Flow<HolidayAnnotation?> =
            store.map { it[holidayId] }

        override suspend fun saveAnnotation(annotation: HolidayAnnotation) {
            store.value = store.value + (annotation.holidayId to annotation)
        }

        override suspend fun deleteAnnotation(holidayId: String) {
            deletedIds += holidayId
            store.value = store.value - holidayId
        }
    }

    // ── GetHolidayAnnotationUseCase ───────────────────────────────────────────

    @Test
    fun getAnnotation_returnsNullWhenNoneExists() = runTest {
        val repo = FakeHolidayAnnotationRepository()
        val useCase = GetHolidayAnnotationUseCase(repo)

        val result = useCase("holiday_1").first()
        assertNull(result)
    }

    @Test
    fun getAnnotation_returnsAnnotationWhenPresent() = runTest {
        val repo = FakeHolidayAnnotationRepository()
        val annotation = HolidayAnnotation(
            holidayId = "holiday_1",
            description = "Family day",
            location = "Home",
            reminderMinutes = 30,
        )
        repo.saveAnnotation(annotation)

        val useCase = GetHolidayAnnotationUseCase(repo)
        val result = useCase("holiday_1").first()

        assertEquals(annotation, result)
    }

    @Test
    fun getAnnotation_doesNotReturnAnnotationsForOtherHolidays() = runTest {
        val repo = FakeHolidayAnnotationRepository()
        repo.saveAnnotation(HolidayAnnotation(holidayId = "holiday_2", description = "Other"))

        val useCase = GetHolidayAnnotationUseCase(repo)
        val result = useCase("holiday_1").first()

        assertNull(result)
    }

    // ── SaveHolidayAnnotationUseCase ──────────────────────────────────────────

    @Test
    fun saveAnnotation_persistsAnnotation() = runTest {
        val repo = FakeHolidayAnnotationRepository()
        val useCase = SaveHolidayAnnotationUseCase(repo)

        val annotation = HolidayAnnotation(
            holidayId = "holiday_1",
            description = "Celebrate!",
            reminderMinutes = 60,
            affectedPersonIds = listOf("person_mom"),
        )
        useCase(annotation)

        assertEquals(annotation, repo.savedAnnotations["holiday_1"])
    }

    @Test
    fun saveAnnotation_overwritesPreviousAnnotation() = runTest {
        val repo = FakeHolidayAnnotationRepository()
        val useCase = SaveHolidayAnnotationUseCase(repo)

        useCase(HolidayAnnotation(holidayId = "holiday_1", description = "First"))
        useCase(HolidayAnnotation(holidayId = "holiday_1", description = "Updated"))

        assertEquals("Updated", repo.savedAnnotations["holiday_1"]?.description)
    }

    // ── DeleteHolidayAnnotationUseCase ────────────────────────────────────────

    @Test
    fun deleteAnnotation_removesAnnotation() = runTest {
        val repo = FakeHolidayAnnotationRepository()
        repo.saveAnnotation(HolidayAnnotation(holidayId = "holiday_1", description = "Notes"))

        val useCase = DeleteHolidayAnnotationUseCase(repo)
        useCase("holiday_1")

        assertTrue(repo.deletedIds.contains("holiday_1"))
        assertNull(repo.savedAnnotations["holiday_1"])
    }

    @Test
    fun deleteAnnotation_doesNothingForMissingHoliday() = runTest {
        val repo = FakeHolidayAnnotationRepository()
        val useCase = DeleteHolidayAnnotationUseCase(repo)

        useCase("holiday_nonexistent")

        assertTrue(repo.deletedIds.contains("holiday_nonexistent"))
    }

    // ── HolidayAnnotation.isEmpty() ───────────────────────────────────────────

    @Test
    fun isEmpty_returnsTrueForDefaultAnnotation() {
        val annotation = HolidayAnnotation(holidayId = "h1")
        assertTrue(annotation.isEmpty())
    }

    @Test
    fun isEmpty_returnsFalseWhenDescriptionSet() {
        val annotation = HolidayAnnotation(holidayId = "h1", description = "Test")
        assertTrue(!annotation.isEmpty())
    }

    @Test
    fun isEmpty_returnsFalseWhenReminderSet() {
        val annotation = HolidayAnnotation(holidayId = "h1", reminderMinutes = 10)
        assertTrue(!annotation.isEmpty())
    }

    @Test
    fun isEmpty_returnsFalseWhenPeopleSet() {
        val annotation = HolidayAnnotation(holidayId = "h1", affectedPersonIds = listOf("p1"))
        assertTrue(!annotation.isEmpty())
    }
}
