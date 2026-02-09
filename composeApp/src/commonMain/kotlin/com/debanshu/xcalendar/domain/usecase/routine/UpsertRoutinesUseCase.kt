package com.debanshu.xcalendar.domain.usecase.routine

import com.debanshu.xcalendar.domain.model.Routine
import com.debanshu.xcalendar.domain.repository.IRoutineRepository
import org.koin.core.annotation.Factory

@Factory
class UpsertRoutinesUseCase(
    private val routineRepository: IRoutineRepository,
) {
    suspend operator fun invoke(routine: Routine) {
        routineRepository.upsertRoutine(routine)
    }

    suspend operator fun invoke(routines: List<Routine>) {
        if (routines.isEmpty()) return
        routineRepository.upsertRoutines(routines)
    }
}
