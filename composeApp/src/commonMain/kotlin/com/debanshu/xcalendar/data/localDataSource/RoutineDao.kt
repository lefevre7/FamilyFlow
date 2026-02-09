package com.debanshu.xcalendar.data.localDataSource

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.debanshu.xcalendar.data.localDataSource.model.RoutineEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {
    @Query("SELECT * FROM routines WHERE isActive = 1 ORDER BY sortOrder ASC, title ASC")
    fun getActiveRoutines(): Flow<List<RoutineEntity>>

    @Query("SELECT * FROM routines WHERE id = :routineId")
    suspend fun getRoutineById(routineId: String): RoutineEntity?

    @Upsert
    suspend fun upsertRoutine(routine: RoutineEntity)

    @Upsert
    suspend fun upsertRoutines(routines: List<RoutineEntity>)

    @Delete
    suspend fun deleteRoutine(routine: RoutineEntity)
}
