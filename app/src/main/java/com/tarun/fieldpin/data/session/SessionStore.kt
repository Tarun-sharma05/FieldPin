package com.tarun.fieldpin.data.session

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.tarun.fieldpin.domain.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface SessionStore {
    val userFlow: Flow<User?>
    suspend fun saveSession(user: User)
    suspend fun clearSession()
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_session")

class DataStoreSessionStore(private val context: Context) : SessionStore {

    private object PreferencesKeys {
        val USER_ID = stringPreferencesKey("user_id")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_TOKEN = stringPreferencesKey("user_token")
    }

    override val userFlow: Flow<User?> = context.dataStore.data.map { preferences ->
        val id = preferences[PreferencesKeys.USER_ID]
        val email = preferences[PreferencesKeys.USER_EMAIL]
        val name = preferences[PreferencesKeys.USER_NAME]
        val token = preferences[PreferencesKeys.USER_TOKEN]

        if (id != null && email != null && name != null && token != null) {
            User(id = id, email = email, name = name, token = token)
        } else {
            null
        }
    }

    override suspend fun saveSession(user: User) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USER_ID] = user.id
            preferences[PreferencesKeys.USER_EMAIL] = user.email
            preferences[PreferencesKeys.USER_NAME] = user.name
            preferences[PreferencesKeys.USER_TOKEN] = user.token
        }
    }

    override suspend fun clearSession() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
