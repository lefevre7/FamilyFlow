package com.debanshu.xcalendar.domain.repository

import com.debanshu.xcalendar.data.localDataSource.HolidayAnnotationDao
import com.debanshu.xcalendar.data.localDataSource.model.HolidayAnnotationEntity
import com.debanshu.xcalendar.domain.model.HolidayAnnotation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single

@Single(binds = [IHolidayAnnotationRepository::class])
class HolidayAnnotationRepository(
    private val dao: HolidayAnnotationDao,
) : IHolidayAnnotationRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override fun getAnnotation(holidayId: String): Flow<HolidayAnnotation?> =
        dao.getAnnotation(holidayId).map { entity -> entity?.toDomain() }

    override suspend fun saveAnnotation(annotation: HolidayAnnotation) {
        dao.upsertAnnotation(annotation.toEntity())
    }

    override suspend fun deleteAnnotation(holidayId: String) {
        dao.deleteAnnotation(holidayId)
    }

    // ── Mapping ──────────────────────────────────────────────────────────────

    private fun HolidayAnnotationEntity.toDomain(): HolidayAnnotation =
        HolidayAnnotation(
            holidayId = holidayId,
            description = description,
            location = location,
            reminderMinutes = reminderMinutes,
            affectedPersonIds = decodePersonIds(affectedPersonIds),
            updatedAt = updatedAt,
        )

    private fun HolidayAnnotation.toEntity(): HolidayAnnotationEntity =
        HolidayAnnotationEntity(
            holidayId = holidayId,
            description = description,
            location = location,
            reminderMinutes = reminderMinutes,
            affectedPersonIds = encodePersonIds(affectedPersonIds),
            updatedAt = updatedAt,
        )

    private fun encodePersonIds(ids: List<String>): String =
        runCatching { json.encodeToString(ids) }.getOrDefault("[]")

    private fun decodePersonIds(raw: String): List<String> =
        runCatching { json.decodeFromString<List<String>>(raw) }.getOrDefault(emptyList())
}
