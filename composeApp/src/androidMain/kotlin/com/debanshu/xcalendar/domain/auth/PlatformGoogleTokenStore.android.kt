package com.debanshu.xcalendar.domain.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import org.koin.core.annotation.Single
import org.koin.mp.KoinPlatform

@Single(binds = [GoogleTokenStore::class])
actual class PlatformGoogleTokenStore : GoogleTokenStore {
    private val context: Context by lazy { KoinPlatform.getKoin().get() }
    private val prefs by lazy { createEncryptedPreferences(context) }

    override fun saveTokens(accountId: String, tokens: GoogleAuthTokens) {
        prefs.edit()
            .putString(key(accountId, "accessToken"), tokens.accessToken)
            .putString(key(accountId, "refreshToken"), tokens.refreshToken)
            .putString(key(accountId, "idToken"), tokens.idToken)
            .putLong(key(accountId, "expiresAt"), tokens.expiresAt ?: -1L)
            .apply()
    }

    override fun getTokens(accountId: String): GoogleAuthTokens? {
        val accessToken = prefs.getString(key(accountId, "accessToken"), null) ?: return null
        val refreshToken = prefs.getString(key(accountId, "refreshToken"), null)
        val idToken = prefs.getString(key(accountId, "idToken"), null)
        val expiresAt = prefs.getLong(key(accountId, "expiresAt"), -1L).takeIf { it > 0 }
        return GoogleAuthTokens(
            accessToken = accessToken,
            refreshToken = refreshToken,
            idToken = idToken,
            expiresAt = expiresAt,
        )
    }

    override fun clearTokens(accountId: String) {
        prefs.edit()
            .remove(key(accountId, "accessToken"))
            .remove(key(accountId, "refreshToken"))
            .remove(key(accountId, "idToken"))
            .remove(key(accountId, "expiresAt"))
            .apply()
    }

    private fun key(accountId: String, field: String): String = "google.$accountId.$field"

    private fun createEncryptedPreferences(context: Context) =
        EncryptedSharedPreferences.create(
            PREFS_NAME,
            MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )

    companion object {
        private const val PREFS_NAME = "google_oauth_tokens"
    }
}
