package com.thomasR.helen.repository

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.thomasR.helen.repository.UserPreferencesRepository.PreferencesKeys.THEME
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.io.IOException

enum class Theme {
    SYSTEM_DEFAULT,
    BRIGHT,
    DARK;

    companion object {
        fun encodeTheme(i: Int): Theme? {
            when (i) {
                0 -> return SYSTEM_DEFAULT
                1 -> return BRIGHT
                2 -> return DARK
                else -> return null
            }
        }
    }
}

data class UserPreferences(
    val theme: Theme
)

class UserPreferencesRepository(private val dataStore: DataStore<Preferences>) {
    private val TAG: String = "UserPreferencesRepo"

    private object PreferencesKeys {
        val THEME = stringPreferencesKey("theme")
    }

    val userPreferencesFlow: Flow<UserPreferences> = dataStore.data
        .catch { exception ->
            // dataStore.data throws an IOException when an error is encountered when reading data
            if (exception is IOException) {
                Log.e(TAG, "Error reading preferences.", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            mapUserPreferences(preferences)
        }

    suspend fun setTheme(theme: Theme) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME] = theme.name
        }
    }

    suspend fun fetchInitialPreferences() = mapUserPreferences(dataStore.data.first().toPreferences())

    private fun mapUserPreferences(preferences: Preferences): UserPreferences {
        // Get the theme from preferences and convert it to a [Theme] object
        val theme =
            Theme.valueOf(
                preferences[PreferencesKeys.THEME] ?: Theme.SYSTEM_DEFAULT.name
            )

        return UserPreferences(theme)
    }
}