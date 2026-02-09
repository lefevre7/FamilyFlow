package com.debanshu.xcalendar.domain.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.debanshu.xcalendar.domain.model.FamilyLens
import com.debanshu.xcalendar.domain.model.FamilyLensSelection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single
import org.koin.mp.KoinPlatform

private val Context.lensPreferencesDataStore by preferencesDataStore(name = "lens_preferences")

@Single(binds = [ILensPreferencesRepository::class])
class LensPreferencesRepository : ILensPreferencesRepository {
    private val context: Context by lazy { KoinPlatform.getKoin().get() }
    private val dataStore by lazy { context.lensPreferencesDataStore }

    override val selection: Flow<FamilyLensSelection> =
        dataStore.data.map { prefs ->
            val lens =
                runCatching { FamilyLens.valueOf(prefs[LENS_KEY] ?: FamilyLens.MOM.name) }
                    .getOrDefault(FamilyLens.MOM)
            val personId = prefs[PERSON_ID_KEY]
            FamilyLensSelection(lens = lens, personId = personId)
        }

    override suspend fun updateSelection(selection: FamilyLensSelection) {
        dataStore.edit { prefs ->
            prefs[LENS_KEY] = selection.lens.name
            val personId = selection.personId
            if (personId.isNullOrBlank()) {
                prefs.remove(PERSON_ID_KEY)
            } else {
                prefs[PERSON_ID_KEY] = personId
            }
        }
    }

    private companion object {
        val LENS_KEY = stringPreferencesKey("selected_family_lens")
        val PERSON_ID_KEY = stringPreferencesKey("selected_family_lens_person_id")
    }
}
