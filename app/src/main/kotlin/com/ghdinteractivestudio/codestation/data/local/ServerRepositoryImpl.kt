package com.ghdinteractivestudio.codestation.data.local

import java.net.URI
import java.net.URLDecoder
import com.ghdinteractivestudio.codestation.data.preferences.PreferencesRepositoryImpl
import com.ghdinteractivestudio.codestation.data.remote.CodeStationApiService
import com.ghdinteractivestudio.codestation.domain.model.ServerConfig
import com.ghdinteractivestudio.codestation.domain.model.ServerConnectionStatus
import com.ghdinteractivestudio.codestation.domain.model.ServerMode
import com.ghdinteractivestudio.codestation.domain.repository.ServerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

class ServerRepositoryImpl(
    private val preferencesRepository: PreferencesRepositoryImpl,
    private val apiServiceProvider: (String) -> CodeStationApiService?
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
                    errorMessage = "Serveur inaccessible (${e.javaClass.simpleName}: ${e.message ?: "Connexion refusée"}). Vérifiez que CodeStation est démarré sur ${current.url}."
                )
                _serverState.value = errorConfig
                return@runCatching errorConfig
            }

            val duration = System.currentTimeMillis() - startTime
            if (!healthResponse.isSuccessful || healthResponse.body() == null) {
                val errorConfig = current.copy(
                    status = ServerConnectionStatus.ERROR,
                    errorMessage = "Erreur HTTP de l'hôte (${healthResponse.code()} ${healthResponse.message()})."
                )
                _serverState.value = errorConfig
                return@runCatching errorConfig
            }

            val health = healthResponse.body()!!
            var activeBots = 0
            var verifiedVersion = health.version

            // 2. Vérification rigoureuse de l'authentification avec le jeton
            if (current.token.isNullOrBlank()) {
                val errorConfig = current.copy(
                    status = ServerConnectionStatus.ERROR,
                    latencyMs = duration,
                    serverVersion = verifiedVersion,
                    errorMessage = "Serveur en ligne (v$verifiedVersion), mais jeton d'authentification manquant. Saisissez le jeton local (~/.codestation/token) ou générez le lien d'appairage."
                )
                _serverState.value = errorConfig
                return@runCatching errorConfig
            }

            val helloResponse = try {
                api.hello("Bearer ${current.token}")
            } catch (e: Exception) {
                val errorConfig = current.copy(
                    status = ServerConnectionStatus.ERROR,
                    latencyMs = duration,
                    serverVersion = verifiedVersion,
                    errorMessage = "Erreur de communication lors de l'authentification (${e.javaClass.simpleName}: ${e.message})."
                )
                _serverState.value = errorConfig
                return@runCatching errorConfig
            }

            if (helloResponse.code() in listOf(401, 403)) {
                val errorConfig = current.copy(
                    status = ServerConnectionStatus.ERROR,
                    latencyMs = duration,
                    serverVersion = verifiedVersion,
                    errorMessage = "Erreur d'authentification (${helloResponse.code()}) : jeton d'appairage invalide ou non autorisé."
                )
                _serverState.value = errorConfig
                return@runCatching errorConfig
            }

            if (!helloResponse.isSuccessful || helloResponse.body() == null) {
                val errorConfig = current.copy(
                    status = ServerConnectionStatus.ERROR,
                    latencyMs = duration,
                    serverVersion = verifiedVersion,
                    errorMessage = "Erreur HTTP du serveur lors de la validation (${helloResponse.code()} ${helloResponse.message()})."
                )
                _serverState.value = errorConfig
                return@runCatching errorConfig
            }

            val hello = helloResponse.body()!!
            verifiedVersion = hello.version ?: verifiedVersion
            activeBots = hello.backends?.size ?: 0

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
            if (scheme != "codestation" && scheme != "codestation-dev" && scheme != "synapse" && scheme != "codync") {
                error("Schéma d'URI non reconnu ($scheme). Attendu: codestation://pair")
            }

            val query = parsedUri?.rawQuery ?: trimmed.substringAfter("?", "")
            val queryParams = query.split("&")
                .filter { it.contains("=") }
                .associate {
                    val parts = it.split("=", limit = 2)
                    parts[0] to runCatching { URLDecoder.decode(parts[1], "UTF-8") }.getOrDefault(parts[1])
                }

            val token = queryParams["token"] ?: queryParams["code"]
            val rawUrls = queryParams["urls"] ?: queryParams["url"] ?: queryParams["host"]
            val host = rawUrls?.split(",")?.firstOrNull() ?: "http://127.0.0.1:19222"

            updateServerUrl(host)
            if (token != null) {
                updatePairingToken(token)
            }

            testConnection().getOrThrow()
        }
    }
}
