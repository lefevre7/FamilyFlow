package com.debanshu.xcalendar.domain.usecase.holiday

import com.debanshu.xcalendar.domain.model.HolidayAnnotation
import com.debanshu.xcalendar.domain.repository.IHolidayAnnotationRepository
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Factory

@Factory
class GetHolidayAnnotationUseCase(
    private val repository: IHolidayAnnotationRepository,
) {
    operator fun invoke(holidayId: String): Flow<HolidayAnnotation?> =
        repository.getAnnotation(holidayId)
}
