package com.ghdinteractivestudio.codestation.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ghdinteractivestudio.codestation.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "codestation_preferences")

class PreferencesRepositoryImpl(
    private val context: Context
) : PreferencesRepository {

    private object PreferencesKeys {
        val SERVER_URL = stringPreferencesKey("server_url")
        val PAIRING_TOKEN = stringPreferencesKey("pairing_token")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val SHOW_THINKING_DETAILS = booleanPreferencesKey("show_thinking_details")
        val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val DYNAMIC_ISLAND_OVERLAY = booleanPreferencesKey("dynamic_island_overlay_enabled")
    }

    val serverUrlStream: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SERVER_URL] ?: "http://127.0.0.1:19222"
    }

    val pairingTokenStream: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.PAIRING_TOKEN]
    }

    override val themeModeStream: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.THEME_MODE] ?: "SYSTEM"
    }

    override val showThinkingDetailsStream: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SHOW_THINKING_DETAILS] ?: true
    }

    override val hapticFeedbackEnabledStream: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.HAPTIC_FEEDBACK] ?: true
    }

    override val onboardingCompletedStream: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.ONBOARDING_COMPLETED] ?: false
    }

    override val dynamicIslandOverlayEnabledStream: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.DYNAMIC_ISLAND_OVERLAY] ?: false
    }

    suspend fun setServerUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SERVER_URL] = url
        }
    }

    suspend fun setPairingToken(token: String?) {
        context.dataStore.edit { preferences ->
            if (token != null) {
                preferences[PreferencesKeys.PAIRING_TOKEN] = token
            } else {
                preferences.remove(PreferencesKeys.PAIRING_TOKEN)
            }
        }
    }

    override suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode
        }
    }

    override suspend fun setShowThinkingDetails(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_THINKING_DETAILS] = show
        }
    }

    override suspend fun setHapticFeedbackEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HAPTIC_FEEDBACK] = enabled
        }
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ONBOARDING_COMPLETED] = completed
        }
    }

    override suspend fun setDynamicIslandOverlayEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DYNAMIC_ISLAND_OVERLAY] = enabled
        }
    }
}
