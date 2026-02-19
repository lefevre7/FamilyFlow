package com.debanshu.xcalendar.domain.usecase.holiday

import com.debanshu.xcalendar.domain.model.HolidayAnnotation
import com.debanshu.xcalendar.domain.repository.IHolidayAnnotationRepository
import org.koin.core.annotation.Factory

@Factory
class SaveHolidayAnnotationUseCase(
    private val repository: IHolidayAnnotationRepository,
) {
    suspend operator fun invoke(annotation: HolidayAnnotation) =
        repository.saveAnnotation(annotation)
}
