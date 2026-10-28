package com.ghdinteractivestudio.codestation.domain.repository

import com.ghdinteractivestudio.codestation.domain.model.BotEnvironment
import com.ghdinteractivestudio.codestation.domain.model.ServerConfig
import com.ghdinteractivestudio.codestation.domain.model.SystemResources
import kotlinx.coroutines.flow.Flow

interface ServerRepository {
    fun getServerConfigStream(): Flow<ServerConfig>
    suspend fun getServerConfig(): ServerConfig
    suspend fun updateServerUrl(url: String): Result<Unit>
    suspend fun updatePairingToken(token: String?): Result<Unit>
    suspend fun testConnection(): Result<ServerConfig>
    suspend fun parseAndApplyPairingUri(uriString: String): Result<ServerConfig>
    suspend fun getEnvironments(): Result<Pair<List<BotEnvironment>, SystemResources>>
    suspend fun startEnvironment(botId: String): Result<Unit>
    suspend fun stopEnvironment(botId: String): Result<Unit>
    suspend fun createEnvironment(botId: String, engine: String, model: String?): Result<Unit>
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
