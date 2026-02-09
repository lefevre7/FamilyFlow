package com.debanshu.xcalendar.domain.repository

import com.debanshu.xcalendar.domain.model.Routine
import kotlinx.coroutines.flow.Flow

interface IRoutineRepository {
    fun getRoutines(): Flow<List<Routine>>
    suspend fun getRoutineById(routineId: String): Routine?
    suspend fun upsertRoutine(routine: Routine)
    suspend fun upsertRoutines(routines: List<Routine>)
    suspend fun deleteRoutine(routine: Routine)
}
