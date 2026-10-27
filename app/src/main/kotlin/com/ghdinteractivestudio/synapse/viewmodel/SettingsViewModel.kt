package com.ghdinteractivestudio.synapse.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ghdinteractivestudio.synapse.domain.model.ServerConfig
import com.ghdinteractivestudio.synapse.domain.repository.PreferencesRepository
import com.ghdinteractivestudio.synapse.domain.repository.ServerRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val serverConfig: ServerConfig = ServerConfig(),
    val themeMode: String = "SYSTEM",
    val showThinkingDetails: Boolean = true,
    val hapticFeedback: Boolean = true,
    val appVersion: String = "1.0.0 (Build 1)"
)

class SettingsViewModel(
    private val serverRepository: ServerRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        serverRepository.getServerConfigStream(),
        preferencesRepository.themeModeStream,
        preferencesRepository.showThinkingDetailsStream,
        preferencesRepository.hapticFeedbackEnabledStream
    ) { config, theme, showThinking, haptic ->
        SettingsUiState(
            serverConfig = config,
            themeMode = theme,
            showThinkingDetails = showThinking,
            hapticFeedback = haptic
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            preferencesRepository.setThemeMode(mode)
        }
    }

    fun setShowThinkingDetails(show: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setShowThinkingDetails(show)
        }
    }

    fun setHapticFeedback(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setHapticFeedbackEnabled(enabled)
        }
    }

    fun resetPreferences() {
        viewModelScope.launch {
            preferencesRepository.setThemeMode("SYSTEM")
            preferencesRepository.setShowThinkingDetails(true)
            preferencesRepository.setHapticFeedbackEnabled(true)
            preferencesRepository.setOnboardingCompleted(false)
        }
    }

    companion object {
        fun provideFactory(
            serverRepository: ServerRepository,
            preferencesRepository: PreferencesRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(serverRepository, preferencesRepository) as T
            }
        }
    }
}
