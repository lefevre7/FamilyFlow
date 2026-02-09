package com.debanshu.xcalendar.domain.repository

import com.debanshu.xcalendar.common.model.asRoutine
import com.debanshu.xcalendar.common.model.asRoutineEntity
import com.debanshu.xcalendar.data.localDataSource.RoutineDao
import com.debanshu.xcalendar.domain.model.Routine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

@Single(binds = [IRoutineRepository::class])
class RoutineRepository(
    private val routineDao: RoutineDao,
) : BaseRepository(), IRoutineRepository {
    override fun getRoutines(): Flow<List<Routine>> =
        safeFlow(
            flowName = "getRoutines",
            defaultValue = emptyList(),
            flow = routineDao.getActiveRoutines().map { entities -> entities.map { it.asRoutine() } },
        )

    override suspend fun getRoutineById(routineId: String): Routine? =
        safeCallOrThrow("getRoutineById($routineId)") {
            routineDao.getRoutineById(routineId)?.asRoutine()
        }

    override suspend fun upsertRoutine(routine: Routine) =
        safeCallOrThrow("upsertRoutine(${routine.id})") {
            routineDao.upsertRoutine(routine.asRoutineEntity())
        }

    override suspend fun upsertRoutines(routines: List<Routine>) =
        safeCallOrThrow("upsertRoutines(${routines.size})") {
            routineDao.upsertRoutines(routines.map { it.asRoutineEntity() })
        }

    override suspend fun deleteRoutine(routine: Routine) =
        safeCallOrThrow("deleteRoutine(${routine.id})") {
            routineDao.deleteRoutine(routine.asRoutineEntity())
        }
}
