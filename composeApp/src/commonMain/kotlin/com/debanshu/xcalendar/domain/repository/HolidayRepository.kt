package com.debanshu.xcalendar.domain.repository

import com.debanshu.xcalendar.data.store.HolidayKey
import com.debanshu.xcalendar.domain.model.Holiday
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single
import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.StoreReadResponse

/**
 * Repository for holiday data using Store5.
 * 
 * Store5 automatically handles:
 * - Caching to Room database
 * - Network fetching with automatic retry
 * - Request deduplication (multiple requesters share the same network call)
 * - Offline-first: serves cached data while refreshing from network
 */
@Single(binds = [IHolidayRepository::class])
class HolidayRepository(
    private val holidayStore: Store<HolidayKey, List<Holiday>>,
) : BaseRepository(), IHolidayRepository {

    /**
     * Refreshes holidays from the network.
     * Store5 handles caching automatically after a successful fetch.
     */
    override suspend fun updateHolidays(
        countryCode: String,
        region: String,
        year: Int,
    ): Unit = safeCallOrThrow("updateHolidays($countryCode, $region, $year)") {
        val key = HolidayKey(countryCode, region, year)
        // Force a fresh fetch from the network
        // Use filterIsInstance + first() to exit after the first successful data emission
        // This prevents hanging since Store5's stream() returns a hot Flow
        holidayStore.stream(StoreReadRequest.fresh(key))
            .filterIsInstance<StoreReadResponse.Data<List<Holiday>>>()
            .first()
            .also { response ->
                logDebug { "Successfully refreshed holidays for $countryCode/$region, $year: ${response.value.size} holidays" }
            }
        Unit
    }

    /**
     * Gets holidays for a specific year and country/region.
     * 
     * Store5 automatically:
     * - Returns cached data immediately if available
     * - Fetches from network in the background
     * - Updates the cache and emits new data
     */
    override fun getHolidaysForYear(
        countryCode: String,
        region: String,
        year: Int,
    ): Flow<List<Holiday>> {
        val key = HolidayKey(countryCode, region, year)
        
        return safeFlow(
            flowName = "getHolidaysForYear($countryCode, $region, $year)",
            defaultValue = emptyList(),
            flow = holidayStore.stream(StoreReadRequest.cached(key, refresh = true))
                .filterIsInstance<StoreReadResponse.Data<List<Holiday>>>()
                .map { it.value }
        )
    }
}
