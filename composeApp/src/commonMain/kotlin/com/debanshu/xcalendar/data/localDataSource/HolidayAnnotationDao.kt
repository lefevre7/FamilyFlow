package com.debanshu.xcalendar.data.localDataSource

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.debanshu.xcalendar.data.localDataSource.model.HolidayAnnotationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HolidayAnnotationDao {

    @Query("SELECT * FROM holiday_annotations WHERE holidayId = :holidayId")
    fun getAnnotation(holidayId: String): Flow<HolidayAnnotationEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAnnotation(annotation: HolidayAnnotationEntity)

    @Query("DELETE FROM holiday_annotations WHERE holidayId = :holidayId")
    suspend fun deleteAnnotation(holidayId: String)
}
