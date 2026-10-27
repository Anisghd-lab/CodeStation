package com.ghdinteractivestudio.synapse.domain.repository

import com.ghdinteractivestudio.synapse.domain.model.ServerConfig
import kotlinx.coroutines.flow.Flow

interface ServerRepository {
    fun getServerConfigStream(): Flow<ServerConfig>
    suspend fun getServerConfig(): ServerConfig
    suspend fun updateServerUrl(url: String): Result<Unit>
    suspend fun updatePairingToken(token: String?): Result<Unit>
    suspend fun testConnection(): Result<ServerConfig>
    suspend fun parseAndApplyPairingUri(uriString: String): Result<ServerConfig>
}

interface PreferencesRepository {
    val themeModeStream: Flow<String> // "SYSTEM", "DARK", "LIGHT"
    val showThinkingDetailsStream: Flow<Boolean>
    val hapticFeedbackEnabledStream: Flow<Boolean>
    val onboardingCompletedStream: Flow<Boolean>

    suspend fun setThemeMode(mode: String)
    suspend fun setShowThinkingDetails(show: Boolean)
    suspend fun setHapticFeedbackEnabled(enabled: Boolean)
    suspend fun setOnboardingCompleted(completed: Boolean)
}
