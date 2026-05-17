package com.galaxyjoy.cpuinfo.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

class UserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    private object PreferencesKeys {
        val SORTING_APPS = booleanPreferencesKey("sorting_apps")
        val LANGUAGE_PICKED = booleanPreferencesKey("language_picked")
        val EXPORT_FORMAT = stringPreferencesKey("export_format")
    }

    @Suppress("unused")
    val userPreferencesFlow: Flow<UserPreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Timber.e(exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            mapUserPreferences(preferences)
        }

    @Suppress("unused")
    suspend fun setApplicationsSortingOrder(isAscending: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SORTING_APPS] = isAscending
        }
    }

    /** First-launch gate: true once the user has explicitly picked a language. */
    val hasPickedLanguageFlow: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Timber.e(exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { it[PreferencesKeys.LANGUAGE_PICKED] == true }

    suspend fun setLanguagePicked() {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.LANGUAGE_PICKED] = true
        }
    }

    /** Last picked export format name (matches [SystemInfoExporter.Format.name]). */
    val exportFormatFlow: Flow<String?> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Timber.e(exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { it[PreferencesKeys.EXPORT_FORMAT] }

    suspend fun setExportFormat(name: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.EXPORT_FORMAT] = name
        }
    }

    private fun mapUserPreferences(preferences: Preferences): UserPreferences {
        val isApplicationsSortingAscending = preferences[PreferencesKeys.SORTING_APPS] != false
        return UserPreferences(
            isApplicationsSortingAscending = isApplicationsSortingAscending
        )
    }
}

data class UserPreferences(
    val isApplicationsSortingAscending: Boolean
)
