package com.debanshu.xcalendar.domain.usecase.routine

import com.debanshu.xcalendar.domain.model.Routine
import com.debanshu.xcalendar.domain.repository.IRoutineRepository
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Factory

@Factory
class GetRoutinesUseCase(
    private val routineRepository: IRoutineRepository,
) {
    operator fun invoke(): Flow<List<Routine>> = routineRepository.getRoutines()
}
