package com.ghdinteractivestudio.synapse.data.local

import java.net.URI
import java.net.URLDecoder
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
            _serverState.value = current.copy(status = ServerConnectionStatus.CONNECTING, errorMessage = null)

            val startTime = System.currentTimeMillis()
            val api = apiServiceProvider(current.url)
                ?: error("Client HTTP non initialisable pour l'URL: ${current.url}")

            // 1. Appel du endpoint réel GET /health (ouvert sur loopback)
            val healthResponse = try {
                api.getHealth()
            } catch (e: Exception) {
                val errorConfig = current.copy(
                    status = ServerConnectionStatus.ERROR,
                    errorMessage = "Impossible de joindre l'hôte (${e.message ?: "Connexion refusée"}). Vérifiez que le serveur écoute sur ${current.url}."
                )
                _serverState.value = errorConfig
                return@runCatching errorConfig
            }

            val duration = System.currentTimeMillis() - startTime
            if (!healthResponse.isSuccessful || healthResponse.body() == null) {
                val errorConfig = current.copy(
                    status = ServerConnectionStatus.ERROR,
                    errorMessage = "Réponse HTTP invalide de l'hôte (${healthResponse.code()})"
                )
                _serverState.value = errorConfig
                return@runCatching errorConfig
            }

            val health = healthResponse.body()!!
            var activeBots = 0
            var verifiedVersion = health.version

            // 2. Si un token d'appairage est fourni, valider POST /api/hello
            if (!current.token.isNullOrBlank()) {
                try {
                    val helloResponse = api.hello("Bearer ${current.token}")
                    if (helloResponse.isSuccessful && helloResponse.body() != null) {
                        val hello = helloResponse.body()!!
                        verifiedVersion = hello.version ?: verifiedVersion
                    } else if (helloResponse.code() == 401) {
                        val errorConfig = current.copy(
                            status = ServerConnectionStatus.ERROR,
                            latencyMs = duration,
                            serverVersion = verifiedVersion,
                            errorMessage = "Jeton d'appairage invalide (401 Unauthorized)."
                        )
                        _serverState.value = errorConfig
                        return@runCatching errorConfig
                    }
                } catch (_: Exception) {
                    // Si hello échoue pour une raison mineure alors que health a réussi
                }
            }

            val connected = current.copy(
                status = ServerConnectionStatus.CONNECTED,
                latencyMs = duration,
                serverVersion = verifiedVersion,
                activeBotCount = activeBots,
                errorMessage = null,
                lastConnectedAt = System.currentTimeMillis()
            )
            _serverState.value = connected
            connected
        }
    }

    override suspend fun parseAndApplyPairingUri(uriString: String): Result<ServerConfig> {
        return runCatching {
            val trimmed = uriString.trim()
            val parsedUri = runCatching { URI.create(trimmed) }.getOrNull()
            val scheme = (parsedUri?.scheme ?: trimmed.substringBefore("://", "")).lowercase()
            if (scheme != "synapse" && scheme != "codestation" && scheme != "codync") {
                error("Schéma d'URI non reconnu ($scheme). Attendu: synapse://pair")
            }

            val query = parsedUri?.rawQuery ?: trimmed.substringAfter("?", "")
            val queryParams = query.split("&")
                .filter { it.contains("=") }
                .associate {
                    val parts = it.split("=", limit = 2)
                    parts[0] to runCatching { URLDecoder.decode(parts[1], "UTF-8") }.getOrDefault(parts[1])
                }

            val token = queryParams["token"] ?: queryParams["v"]
            val host = queryParams["host"] ?: queryParams["url"] ?: "http://127.0.0.1:19222"

            updateServerUrl(host)
            if (token != null) {
                updatePairingToken(token)
            }

            testConnection().getOrThrow()
        }
    }
}
