package com.wargart.data.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.wargart.domain.model.UserSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "wargart_prefs")

class SessionPreferences(private val context: Context, private val defaultBaseUrl: String) {
    private object Keys {
        val TOKEN = stringPreferencesKey("jwt_token")
        val USER_ID = longPreferencesKey("user_id")
        val USERNAME = stringPreferencesKey("username")
        val EMAIL = stringPreferencesKey("email")
        val PHONE = stringPreferencesKey("phone")
        val FULL_NAME = stringPreferencesKey("full_name")
        val ROLE_CODE = stringPreferencesKey("role_code")
        val ROLE_NAME = stringPreferencesKey("role_name")
        val RT_ID = longPreferencesKey("rt_id")
        val RW_ID = longPreferencesKey("rw_id")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val API_BASE_URL = stringPreferencesKey("api_base_url")
    }

    val sessionFlow: Flow<UserSession?> = context.dataStore.data.map { prefs ->
        val token = prefs[Keys.TOKEN] ?: return@map null
        val uid = prefs[Keys.USER_ID] ?: return@map null
        UserSession(
            userId = uid,
            username = prefs[Keys.USERNAME] ?: "",
            email = prefs[Keys.EMAIL] ?: "",
            phone = prefs[Keys.PHONE],
            fullName = prefs[Keys.FULL_NAME] ?: "",
            roleCode = prefs[Keys.ROLE_CODE] ?: "WARGA",
            roleName = prefs[Keys.ROLE_NAME] ?: "Warga",
            rtId = prefs[Keys.RT_ID],
            rwId = prefs[Keys.RW_ID],
            token = token
        )
    }

    val darkModeFlow: Flow<Boolean> = context.dataStore.data.map { it[Keys.DARK_MODE] ?: false }
    val apiBaseUrlFlow: Flow<String> = context.dataStore.data.map { it[Keys.API_BASE_URL] ?: defaultBaseUrl }

    suspend fun getToken(): String? = context.dataStore.data.first()[Keys.TOKEN]
    suspend fun getApiBaseUrl(): String = context.dataStore.data.first()[Keys.API_BASE_URL] ?: defaultBaseUrl

    suspend fun saveSession(session: UserSession) {
        context.dataStore.edit { prefs ->
            prefs[Keys.TOKEN] = session.token
            prefs[Keys.USER_ID] = session.userId
            prefs[Keys.USERNAME] = session.username
            prefs[Keys.EMAIL] = session.email
            session.phone?.let { prefs[Keys.PHONE] = it }
            prefs[Keys.FULL_NAME] = session.fullName
            prefs[Keys.ROLE_CODE] = session.roleCode
            prefs[Keys.ROLE_NAME] = session.roleName
            session.rtId?.let { prefs[Keys.RT_ID] = it }
            session.rwId?.let { prefs[Keys.RW_ID] = it }
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.TOKEN)
            prefs.remove(Keys.USER_ID)
            prefs.remove(Keys.ROLE_CODE)
        }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DARK_MODE] = enabled }
    }

    suspend fun setApiBaseUrl(url: String) {
        context.dataStore.edit { it[Keys.API_BASE_URL] = url.trimEnd('/') + "/" }
    }
}
