package com.debanshu.xcalendar.data.remoteDataSource.model.holiday

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EnricoDate(
    @SerialName("day")
    val day: Int,
    @SerialName("month")
    val month: Int,
    @SerialName("year")
    val year: Int,
    @SerialName("dayOfWeek")
    val dayOfWeek: Int
)
