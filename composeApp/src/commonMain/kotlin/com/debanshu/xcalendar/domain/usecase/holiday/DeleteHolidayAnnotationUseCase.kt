package com.debanshu.xcalendar.domain.usecase.holiday

import com.debanshu.xcalendar.domain.repository.IHolidayAnnotationRepository
import org.koin.core.annotation.Factory

@Factory
class DeleteHolidayAnnotationUseCase(
    private val repository: IHolidayAnnotationRepository,
) {
    suspend operator fun invoke(holidayId: String) =
        repository.deleteAnnotation(holidayId)
}
