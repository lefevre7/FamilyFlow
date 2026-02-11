package com.debanshu.xcalendar.data.remoteDataSource.model.holiday

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EnricoName(
    @SerialName("lang")
    val lang: String,
    @SerialName("text")
    val text: String
)
