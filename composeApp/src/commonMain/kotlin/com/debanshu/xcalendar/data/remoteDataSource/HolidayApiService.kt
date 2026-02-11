package com.debanshu.xcalendar.data.remoteDataSource

import com.debanshu.xcalendar.data.remoteDataSource.error.DataError
import com.debanshu.xcalendar.data.remoteDataSource.model.holiday.EnricoHolidayItem
import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single

@Single
class HolidayApiService(
    client: HttpClient,
    json: Json,
) {
    private val clientWrapper = ClientWrapper(client, json)
    private val baseUrl = "https://kayaposoft.com/enrico/json/v2.0/"

    suspend fun getHolidays(
        countryCode: String,
        region: String,
        year: Int,
    ): Result<List<EnricoHolidayItem>, DataError> =
        clientWrapper.networkGetUsecase<List<EnricoHolidayItem>>(
            baseUrl,
            mapOf(
                "action" to "getHolidaysForYear",
                "year" to year.toString(),
                "country" to countryCode,
                "region" to region,
            ),
        )
}
