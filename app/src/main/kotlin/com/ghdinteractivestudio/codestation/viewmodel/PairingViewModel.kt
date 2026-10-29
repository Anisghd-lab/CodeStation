package com.ghdinteractivestudio.codestation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ghdinteractivestudio.codestation.domain.model.BotEnvironment
import com.ghdinteractivestudio.codestation.domain.model.DetectedCli
import com.ghdinteractivestudio.codestation.domain.model.ServerConfig
import com.ghdinteractivestudio.codestation.domain.model.ServerConnectionStatus
import com.ghdinteractivestudio.codestation.domain.model.ServerMode
import com.ghdinteractivestudio.codestation.domain.model.SystemResources
import com.ghdinteractivestudio.codestation.domain.repository.ServerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PairingUiState(
    val urlInput: String = "http://127.0.0.1:19222",
    val tokenInput: String = "",
    val serverMode: ServerMode = ServerMode.LOCAL_LOOPBACK,
    val connectionStatus: ServerConnectionStatus = ServerConnectionStatus.DISCONNECTED,
    val latencyMs: Long? = null,
    val serverVersion: String? = null,
    val isTesting: Boolean = false,
    val errorMessage: String? = null,
    val isPairingSuccess: Boolean = false,
    val environments: List<BotEnvironment> = emptyList(),
    val resources: SystemResources = SystemResources(),
    val clis: List<DetectedCli> = emptyList(),
    val isLoadingEnvironments: Boolean = false,
    val environmentMessage: String? = null,
    val lastRefreshedAt: Long = 0L
) {
    val serverUrl: String get() = urlInput
    val pairingToken: String? get() = tokenInput.takeIf { it.isNotEmpty() }
    val status: ServerConnectionStatus get() = connectionStatus
}

private data class ServerFormState(
    val url: String = "http://127.0.0.1:19222",
    val token: String = "",
    val isTesting: Boolean = false,
    val errorMessage: String? = null
)

class PairingViewModel(
    private val serverRepository: ServerRepository
) : ViewModel() {

    private val _formState = MutableStateFlow(ServerFormState())
    private val _actionMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<PairingUiState> = combine(
        serverRepository.getServerConfigStream(),
        _formState,
        serverRepository.environmentsStream,
        _actionMessage
    ) { config, form, envData, actionMsg ->
        PairingUiState(
            urlInput = form.url,
            tokenInput = form.token,
            serverMode = config.mode,
            connectionStatus = config.status,
            latencyMs = config.latencyMs,
            serverVersion = config.serverVersion,
            isTesting = form.isTesting || config.status == ServerConnectionStatus.CONNECTING,
            errorMessage = form.errorMessage ?: config.errorMessage,
            isPairingSuccess = config.status == ServerConnectionStatus.CONNECTED,
            environments = envData.environments,
            resources = envData.resources,
            clis = envData.clis,
            isLoadingEnvironments = envData.isRefreshing,
            environmentMessage = actionMsg ?: envData.errorMessage,
            lastRefreshedAt = envData.lastRefreshedAt
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PairingUiState()
    )

    init {
        viewModelScope.launch {
            val config = serverRepository.getServerConfig()
            _formState.value = _formState.value.copy(
                url = config.url,
                token = config.token ?: ""
            )
            // Trigger light initial refresh if connected
            if (config.status == ServerConnectionStatus.CONNECTED) {
                serverRepository.refreshEnvironments(force = false)
            }
        }
    }

    fun onUrlChanged(url: String) {
        _formState.value = _formState.value.copy(url = url, errorMessage = null)
    }

    fun onServerUrlChanged(url: String) = onUrlChanged(url)

    fun onTokenChanged(token: String) {
        _formState.value = _formState.value.copy(token = token, errorMessage = null)
    }

    fun onPairingTokenChanged(token: String) = onTokenChanged(token)

    fun setServerMode(mode: ServerMode) {
        val suggestedUrl = when (mode) {
            ServerMode.LOCAL_LOOPBACK -> "http://127.0.0.1:19222"
            ServerMode.LAN_HOST -> "http://192.168.1.100:19222"
            ServerMode.REMOTE_GATEWAY -> "http://127.0.0.1:19222"
        }
        _formState.value = _formState.value.copy(url = suggestedUrl)
    }

    fun testConnection() {
        viewModelScope.launch {
            _formState.value = _formState.value.copy(isTesting = true, errorMessage = null)

            serverRepository.updateServerUrl(_formState.value.url)
            serverRepository.updatePairingToken(_formState.value.token.takeIf { it.isNotBlank() })

            val result = serverRepository.testConnection()
            _formState.value = _formState.value.copy(isTesting = false)

            if (result.isFailure) {
                _formState.value = _formState.value.copy(
                    errorMessage = result.exceptionOrNull()?.message ?: "Impossible de joindre le serveur hôte."
                )
            } else {
                refreshEnvironments(force = true)
            }
        }
    }

    fun applyPairingUri(uriString: String) {
        viewModelScope.launch {
            _formState.value = _formState.value.copy(isTesting = true, errorMessage = null)
            val result = serverRepository.parseAndApplyPairingUri(uriString)
            _formState.value = _formState.value.copy(isTesting = false)
            if (result.isFailure) {
                _formState.value = _formState.value.copy(
                    errorMessage = result.exceptionOrNull()?.message ?: "URI d'appairage invalide."
                )
            } else {
                refreshEnvironments(force = true)
            }
        }
    }

    fun refreshEnvironments(force: Boolean = true) {
        viewModelScope.launch {
            _actionMessage.value = null
            serverRepository.refreshEnvironments(force = force)
        }
    }

    fun startEnvironment(botId: String) {
        viewModelScope.launch {
            _actionMessage.value = "Démarrage de $botId..."
            val result = serverRepository.startEnvironment(botId)
            if (result.isSuccess) {
                _actionMessage.value = "Environnement $botId prêt."
            } else {
                _actionMessage.value = "Échec du démarrage ($botId): ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun stopEnvironment(botId: String) {
        viewModelScope.launch {
            _actionMessage.value = "Arrêt de $botId..."
            val result = serverRepository.stopEnvironment(botId)
            if (result.isSuccess) {
                _actionMessage.value = "Environnement $botId arrêté."
            } else {
                _actionMessage.value = "Échec de l'arrêt ($botId): ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun startAllEnvironments() {
        viewModelScope.launch {
            _actionMessage.value = "Démarrage de tous les environnements..."
            val list = uiState.value.environments
            for (env in list) {
                serverRepository.startEnvironment(env.botId)
            }
            refreshEnvironments(force = true)
            _actionMessage.value = "Tous les environnements ont été lancés."
        }
    }

    companion object {
        fun provideFactory(
            serverRepository: ServerRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return PairingViewModel(serverRepository) as T
            }
        }
    }
}
