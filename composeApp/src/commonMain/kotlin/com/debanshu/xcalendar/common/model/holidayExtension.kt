package com.debanshu.xcalendar.common.model

import com.debanshu.xcalendar.data.localDataSource.model.HolidayEntity
import com.debanshu.xcalendar.data.remoteDataSource.model.holiday.EnricoDate
import com.debanshu.xcalendar.data.remoteDataSource.model.holiday.EnricoHolidayItem
import com.debanshu.xcalendar.domain.model.Holiday
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Converts Enrico date structure to epoch milliseconds.
 */
fun EnricoDate.toEpochMillis(): Long {
    val localDate = LocalDate(year, month, day)
    return localDate.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
}

/**
 * Generates a stable hash-based ID for a holiday.
 * Format: hash of "name-date-countryCode"
 */
fun generateHolidayId(name: String, date: Long, countryCode: String): String {
    val compositeKey = "$name-$date-$countryCode"
    return compositeKey.hashCode().toString()
}

/**
 * Gets the best name for the user's locale.
 * Prefers English, then device locale, then first available.
 */
fun EnricoHolidayItem.getBestName(preferredLang: String = "en"): String {
    return name.find { it.lang == preferredLang }?.text
        ?: name.firstOrNull()?.text
        ?: "Unknown Holiday"
}

/**
 * Builds a translations map from EnricoName list.
 */
fun EnricoHolidayItem.getTranslationsMap(): Map<String, String> {
    return name.associate { it.lang to it.text }
}

fun EnricoHolidayItem.asHoliday(countryCode: String): Holiday {
    val epochMillis = date.toEpochMillis()
    val holidayName = getBestName()
    
    return Holiday(
        id = generateHolidayId(holidayName, epochMillis, countryCode),
        name = holidayName,
        date = epochMillis,
        countryCode = countryCode,
        holidayType = holidayType,
        translations = getTranslationsMap()
    )
}

fun EnricoHolidayItem.asHolidayEntity(countryCode: String): HolidayEntity {
    val epochMillis = date.toEpochMillis()
    val holidayName = getBestName()
    val translationsJson = Json.encodeToString(getTranslationsMap())
    
    return HolidayEntity(
        id = generateHolidayId(holidayName, epochMillis, countryCode),
        name = holidayName,
        date = epochMillis,
        countryCode = countryCode,
        holidayType = holidayType,
        translations = translationsJson
    )
}

fun HolidayEntity.asHoliday(): Holiday {
    val translationsMap = try {
        Json.decodeFromString<Map<String, String>>(translations)
    } catch (e: Exception) {
        emptyMap()
    }
    
    return Holiday(
        id = id,
        name = name,
        date = date,
        countryCode = countryCode,
        holidayType = holidayType,
        translations = translationsMap
    )
}

fun Holiday.asHolidayEntity(): HolidayEntity {
    val translationsJson = Json.encodeToString(translations)
    
    return HolidayEntity(
        id = id,
        name = name,
        date = date,
        countryCode = countryCode,
        holidayType = holidayType,
        translations = translationsJson
    )
}

/**
 * Deduplicates holidays preferring public_holiday over postal_holiday.
 * Groups by date+name, keeps the one with highest priority.
 */
fun List<Holiday>.deduplicateHolidays(): List<Holiday> {
    val priorityOrder = listOf("public_holiday", "postal_holiday", "observance", "other")
    
    return groupBy { "${it.date}-${it.name}" }
        .values
        .map { duplicates ->
            duplicates.minByOrNull { holiday ->
                priorityOrder.indexOf(holiday.holidayType).takeIf { it >= 0 } ?: Int.MAX_VALUE
            } ?: duplicates.first()
        }
}
