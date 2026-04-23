package com.example.opssync.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "opssync_prefs"
)

class TokenManager(private val context: Context) {

    companion object {
        private val KEY_TOKEN     = stringPreferencesKey("jwt_token")
        private val KEY_USER_ID   = stringPreferencesKey("user_id")
        private val KEY_EMAIL     = stringPreferencesKey("user_email")
        private val KEY_FCM_TOKEN = stringPreferencesKey("fcm_token") // ← new
    }

    suspend fun saveAuthData(token: String, userId: String, email: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_TOKEN]   = token
            prefs[KEY_USER_ID] = userId
            prefs[KEY_EMAIL]   = email
        }
    }

    suspend fun getToken(): String? =
        context.dataStore.data.map { it[KEY_TOKEN] }.first()

    val tokenFlow: Flow<String?> =
        context.dataStore.data.map { it[KEY_TOKEN] }

    suspend fun getUserId(): String? =
        context.dataStore.data.map { it[KEY_USER_ID] }.first()

    // ── FCM Token ─────────────────────────────────────────────
    suspend fun saveFcmToken(fcmToken: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_FCM_TOKEN] = fcmToken
        }
    }

    suspend fun getFcmToken(): String? =
        context.dataStore.data.map { it[KEY_FCM_TOKEN] }.first()

    suspend fun clearAuthData() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_TOKEN)
            prefs.remove(KEY_USER_ID)
            prefs.remove(KEY_EMAIL)
            // Keep FCM token — it's device-bound, not user-bound
        }
    }

    suspend fun isLoggedIn(): Boolean = getToken() != null
}
