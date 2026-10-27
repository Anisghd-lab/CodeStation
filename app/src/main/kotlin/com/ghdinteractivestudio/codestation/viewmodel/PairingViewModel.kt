package com.ghdinteractivestudio.codestation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ghdinteractivestudio.codestation.domain.model.ServerConfig
import com.ghdinteractivestudio.codestation.domain.model.ServerConnectionStatus
import com.ghdinteractivestudio.codestation.domain.model.ServerMode
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
    val isPairingSuccess: Boolean = false
) {
    val serverUrl: String get() = urlInput
    val pairingToken: String? get() = tokenInput.takeIf { it.isNotEmpty() }
    val status: ServerConnectionStatus get() = connectionStatus
}

class PairingViewModel(
    private val serverRepository: ServerRepository
) : ViewModel() {

    private val _urlInput = MutableStateFlow("http://127.0.0.1:19222")
    private val _tokenInput = MutableStateFlow("")
    private val _isTesting = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<PairingUiState> = combine(
        serverRepository.getServerConfigStream(),
        _urlInput,
        _tokenInput,
        _isTesting,
        _errorMessage
    ) { config, url, token, testing, error ->
        PairingUiState(
            urlInput = url,
            tokenInput = token,
            serverMode = config.mode,
            connectionStatus = config.status,
            latencyMs = config.latencyMs,
            serverVersion = config.serverVersion,
            isTesting = testing || config.status == ServerConnectionStatus.CONNECTING,
            errorMessage = error ?: config.errorMessage,
            isPairingSuccess = config.status == ServerConnectionStatus.CONNECTED
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PairingUiState()
    )

    init {
        viewModelScope.launch {
            val config = serverRepository.getServerConfig()
            _urlInput.value = config.url
            _tokenInput.value = config.token ?: ""
        }
    }

    fun onUrlChanged(url: String) {
        _urlInput.value = url
        _errorMessage.value = null
    }

    fun onServerUrlChanged(url: String) = onUrlChanged(url)

    fun onTokenChanged(token: String) {
        _tokenInput.value = token
        _errorMessage.value = null
    }

    fun onPairingTokenChanged(token: String) = onTokenChanged(token)

    fun setServerMode(mode: ServerMode) {
        val suggestedUrl = when (mode) {
            ServerMode.LOCAL_LOOPBACK -> "http://127.0.0.1:19222"
            ServerMode.LAN_HOST -> "http://192.168.1.100:19222"
            ServerMode.REMOTE_GATEWAY -> "http://127.0.0.1:19222"
        }
        _urlInput.value = suggestedUrl
    }

    fun testConnection() {
        viewModelScope.launch {
            _isTesting.value = true
            _errorMessage.value = null

            serverRepository.updateServerUrl(_urlInput.value)
            serverRepository.updatePairingToken(_tokenInput.value.takeIf { it.isNotBlank() })

            val result = serverRepository.testConnection()
            _isTesting.value = false

            if (result.isFailure) {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Impossible de joindre le serveur hôte."
            }
        }
    }

    fun applyPairingUri(uriString: String) {
        viewModelScope.launch {
            _isTesting.value = true
            _errorMessage.value = null
            val result = serverRepository.parseAndApplyPairingUri(uriString)
            _isTesting.value = false
            if (result.isFailure) {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "URI d'appairage invalide."
            }
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
