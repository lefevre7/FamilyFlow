package com.debanshu.xcalendar.domain.repository

import com.debanshu.xcalendar.domain.model.HolidayAnnotation
import kotlinx.coroutines.flow.Flow

interface IHolidayAnnotationRepository {
    /** Emits the annotation for [holidayId], or null if none has been saved. */
    fun getAnnotation(holidayId: String): Flow<HolidayAnnotation?>

    /** Upserts the annotation. */
    suspend fun saveAnnotation(annotation: HolidayAnnotation)

    /** Clears all user annotations for [holidayId]. */
    suspend fun deleteAnnotation(holidayId: String)
}
