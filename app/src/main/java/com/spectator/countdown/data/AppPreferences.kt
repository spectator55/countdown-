package com.spectator.countdown.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.countdownSettings by preferencesDataStore(name = "countdown_settings")

class AppPreferences(private val context: Context) {
    private val autoSyncKey = booleanPreferencesKey("auto_calendar_sync")

    val autoSyncEnabled: Flow<Boolean> = context.countdownSettings.data
        .catch { error ->
            if (error is IOException) emit(androidx.datastore.preferences.core.emptyPreferences())
            else throw error
        }
        .map { preferences -> preferences[autoSyncKey] ?: false }

    suspend fun setAutoSyncEnabled(enabled: Boolean) {
        context.countdownSettings.edit { it[autoSyncKey] = enabled }
    }
}
