package com.debanshu.xcalendar.data.remoteDataSource.model.holiday

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EnricoHolidayItem(
    @SerialName("date")
    val date: EnricoDate,
    @SerialName("name")
    val name: List<EnricoName>,
    @SerialName("holidayType")
    val holidayType: String
)
