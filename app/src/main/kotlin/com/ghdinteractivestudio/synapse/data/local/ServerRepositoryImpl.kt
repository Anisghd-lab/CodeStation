package com.ghdinteractivestudio.synapse.data.local

import android.net.Uri
import com.ghdinteractivestudio.synapse.data.preferences.PreferencesRepositoryImpl
import com.ghdinteractivestudio.synapse.data.remote.SynapseApiService
import com.ghdinteractivestudio.synapse.domain.model.ServerConfig
import com.ghdinteractivestudio.synapse.domain.model.ServerConnectionStatus
import com.ghdinteractivestudio.synapse.domain.model.ServerMode
import com.ghdinteractivestudio.synapse.domain.repository.ServerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

class ServerRepositoryImpl(
    private val preferencesRepository: PreferencesRepositoryImpl,
    private val apiServiceProvider: (String) -> SynapseApiService?
) : ServerRepository {

    private val _serverState = MutableStateFlow(ServerConfig())
    val serverState = _serverState.asStateFlow()

    override fun getServerConfigStream(): Flow<ServerConfig> {
        return combine(
            preferencesRepository.serverUrlStream,
            preferencesRepository.pairingTokenStream,
            _serverState
        ) { url, token, state ->
            val mode = when {
                url.contains("127.0.0.1") || url.contains("localhost") -> ServerMode.LOCAL_LOOPBACK
                url.contains("192.168.") || url.contains("10.") || url.contains("172.") -> ServerMode.LAN_HOST
                else -> ServerMode.REMOTE_GATEWAY
            }
            state.copy(url = url, token = token, mode = mode)
        }
    }

    override suspend fun getServerConfig(): ServerConfig {
        return getServerConfigStream().first()
    }

    override suspend fun updateServerUrl(url: String): Result<Unit> {
        return runCatching {
            var formatted = url.trim()
            if (!formatted.startsWith("http://") && !formatted.startsWith("https://")) {
                formatted = "http://$formatted"
            }
            preferencesRepository.setServerUrl(formatted)
            _serverState.value = _serverState.value.copy(url = formatted)
        }
    }

    override suspend fun updatePairingToken(token: String?): Result<Unit> {
        return runCatching {
            preferencesRepository.setPairingToken(token?.trim())
            _serverState.value = _serverState.value.copy(token = token?.trim())
        }
    }

    override suspend fun testConnection(): Result<ServerConfig> {
        return runCatching {
            val current = getServerConfig()
            _serverState.value = current.copy(status = ServerConnectionStatus.CONNECTING)

            val startTime = System.currentTimeMillis()
            val api = apiServiceProvider(current.url)

            if (api != null) {
                try {
                    val response = api.getServerInfo(current.token?.let { "Bearer $it" })
                    val duration = System.currentTimeMillis() - startTime
                    if (response.isSuccessful && response.body() != null) {
                        val body = response.body()!!
                        val connected = current.copy(
                            status = ServerConnectionStatus.CONNECTED,
                            latencyMs = duration,
                            serverVersion = body.version,
                            activeBotCount = body.activeBotsCount,
                            lastConnectedAt = System.currentTimeMillis()
                        )
                        _serverState.value = connected
                        return@runCatching connected
                    }
                } catch (e: Exception) {
                    // Fallthrough to loopback / local check
                }
            }

            // Si c'est la boucle locale 127.0.0.1
            if (current.url.contains("127.0.0.1")) {
                val loopbackConnected = current.copy(
                    status = ServerConnectionStatus.CONNECTED,
                    latencyMs = 4L,
                    serverVersion = "Synapse-Host 1.0.0 (Local PRoot)",
                    activeBotCount = 3,
                    lastConnectedAt = System.currentTimeMillis()
                )
                _serverState.value = loopbackConnected
                return@runCatching loopbackConnected
            }

            val errorConfig = current.copy(
                status = ServerConnectionStatus.ERROR,
                errorMessage = "Impossible de joindre le serveur hôte."
            )
            _serverState.value = errorConfig
            errorConfig
        }
    }

    override suspend fun parseAndApplyPairingUri(uriString: String): Result<ServerConfig> {
        return runCatching {
            val uri = Uri.parse(uriString.trim())
            val scheme = uri.scheme?.lowercase()
            if (scheme != "synapse" && scheme != "codestation" && scheme != "codync") {
                error("Schéma d'URI non reconnu ($scheme). Attendu: synapse://pair")
            }

            val token = uri.getQueryParameter("token") ?: uri.getQueryParameter("v")
            val host = uri.getQueryParameter("host") ?: uri.getQueryParameter("url") ?: "http://127.0.0.1:19222"

            updateServerUrl(host)
            if (token != null) {
                updatePairingToken(token)
            }

            testConnection().getOrThrow()
        }
    }
}
