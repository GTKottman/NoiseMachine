package com.noisemachine.sleep.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appPrefs by preferencesDataStore(name = "sleep_noise_prefs")

class AppSettingsStore(
    private val context: Context
) {
    val settings: Flow<AppSettings> = context.appPrefs.data.map { prefs ->
        AppSettings(
            masterVolume = prefs[MASTER_VOLUME] ?: 0.72f,
            napMinutes = prefs[NAP_MINUTES] ?: 25,
            sleepMinutes = prefs[SLEEP_MINUTES] ?: 90,
            deepSleepMinutes = prefs[DEEP_SLEEP_MINUTES] ?: 480,
            sleepModeDimEnabled = prefs[SLEEP_MODE_DIM] ?: true
        )
    }

    suspend fun setMasterVolume(value: Float) {
        context.appPrefs.edit { prefs -> prefs[MASTER_VOLUME] = value.coerceIn(0f, 1f) }
    }

    suspend fun setSleepModeDimEnabled(enabled: Boolean) {
        context.appPrefs.edit { prefs -> prefs[SLEEP_MODE_DIM] = enabled }
    }

    companion object {
        private val MASTER_VOLUME = floatPreferencesKey("master_volume")
        private val NAP_MINUTES = intPreferencesKey("nap_minutes")
        private val SLEEP_MINUTES = intPreferencesKey("sleep_minutes")
        private val DEEP_SLEEP_MINUTES = intPreferencesKey("deep_sleep_minutes")
        private val SLEEP_MODE_DIM = booleanPreferencesKey("sleep_mode_dim")
    }
}

data class AppSettings(
    val masterVolume: Float,
    val napMinutes: Int,
    val sleepMinutes: Int,
    val deepSleepMinutes: Int,
    val sleepModeDimEnabled: Boolean
)
