package com.ghdinteractivestudio.codestation.data.local

import android.content.Context
import android.content.SharedPreferences
import com.ghdinteractivestudio.codestation.data.remote.AgentModelsRequest
import com.ghdinteractivestudio.codestation.data.remote.CodeStationApiService
import com.ghdinteractivestudio.codestation.domain.model.ModelInfo
import com.ghdinteractivestudio.codestation.domain.model.ProviderType
import com.ghdinteractivestudio.codestation.domain.repository.AntigravityCatalogState
import com.ghdinteractivestudio.codestation.domain.repository.AntigravityModelRepository
import com.ghdinteractivestudio.codestation.domain.repository.ServerRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class CachedModelDto(
    val id: String,
    val displayName: String,
    val isFree: Boolean = false,
    val isDefault: Boolean = false,
    val isVerified: Boolean = true
)

class AntigravityModelRepositoryImpl(
    private val context: Context,
    private val serverRepository: ServerRepository,
    private val apiServiceProvider: suspend () -> CodeStationApiService?,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AntigravityModelRepository {

    companion object {
        private const val PREFS_NAME = "codestation_antigravity_catalog"
        private const val KEY_MODELS_JSON = "antigravity_models_json"
        private const val KEY_LAST_UPDATED = "antigravity_models_last_updated"
        private const val CACHE_TTL_MS = 15 * 60 * 1000L // 15 minutes TTL
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val _catalogState = MutableStateFlow(loadInitialState())
    override val catalogState: StateFlow<AntigravityCatalogState> = _catalogState.asStateFlow()

    private fun loadInitialState(): AntigravityCatalogState {
        val lastUpdated = prefs.getLong(KEY_LAST_UPDATED, 0L).takeIf { it > 0 }
        val rawJson = prefs.getString(KEY_MODELS_JSON, null)
        if (!rawJson.isNullOrBlank()) {
            try {
                val dtos = json.decodeFromString<List<CachedModelDto>>(rawJson)
                if (dtos.isNotEmpty()) {
                    val models = dtos.map {
                        ModelInfo(
                            id = it.id,
                            displayName = it.displayName,
                            isFree = it.isFree,
                            isDefault = it.isDefault,
                            isVerified = it.isVerified
                        )
                    }
                    return AntigravityCatalogState(
                        models = models,
                        isRefreshing = false,
                        lastUpdated = lastUpdated,
                        errorMessage = null
                    )
                }
            } catch (_: Exception) {
                // Ignore parse error and fall back to baseline
            }
        }

        // Baseline verified models if no cache yet
        val baseline = ProviderType.ANTIGRAVITY.getAvailableModels()
        return AntigravityCatalogState(
            models = baseline,
            isRefreshing = false,
            lastUpdated = null,
            errorMessage = null
        )
    }

    private fun saveToCache(models: List<ModelInfo>, timestamp: Long) {
        try {
            val dtos = models.map {
                CachedModelDto(
                    id = it.id,
                    displayName = it.displayName,
                    isFree = it.isFree,
                    isDefault = it.isDefault,
                    isVerified = it.isVerified
                )
            }
            val raw = json.encodeToString(dtos)
            prefs.edit()
                .putString(KEY_MODELS_JSON, raw)
                .putLong(KEY_LAST_UPDATED, timestamp)
                .apply()
        } catch (_: Exception) {
            // Cache persistence failure is non-fatal
        }
    }

    override suspend fun getModels(): List<ModelInfo> {
        val current = _catalogState.value
        val now = System.currentTimeMillis()
        if (current.models.isNotEmpty() && current.lastUpdated != null && (now - current.lastUpdated < CACHE_TTL_MS)) {
            return current.models
        }
        refreshModels(force = false)
        return _catalogState.value.models
    }

    override suspend fun refreshModels(force: Boolean): Result<List<ModelInfo>> = withContext(ioDispatcher) {
        val current = _catalogState.value
        val now = System.currentTimeMillis()

        if (!force && current.models.isNotEmpty() && current.lastUpdated != null && (now - current.lastUpdated < CACHE_TTL_MS)) {
            return@withContext Result.success(current.models)
        }

        _catalogState.value = current.copy(isRefreshing = true, errorMessage = null)

        try {
            val server = serverRepository.getServerConfig()
            val api = apiServiceProvider()
                ?: error("Service API CodeStation indisponible pour l'URL ${server.url}")

            val authHeader = server.token?.let { "Bearer $it" }
            val response = api.getAgentModels(
                request = AgentModelsRequest(backend = "antigravity"),
                authHeader = authHeader
            )

            if (!response.isSuccessful || response.body() == null) {
                val code = response.code()
                val msg = response.errorBody()?.string()?.takeIf { it.isNotBlank() }
                    ?: response.message().takeIf { it.isNotBlank() }
                    ?: "Erreur HTTP $code"
                throw IllegalStateException(msg)
            }

            val body = response.body()!!
            val remoteModels = body.models
            if (remoteModels.isEmpty()) {
                // Retain cached list with informational warning
                _catalogState.value = current.copy(
                    isRefreshing = false,
                    errorMessage = "Aucun modèle retourné par le serveur. Catalogue en cache conservé."
                )
                return@withContext Result.success(current.models)
            }

            val newModels = remoteModels.map { item ->
                ModelInfo(
                    id = item.id,
                    displayName = item.name.ifBlank { item.id },
                    isFree = false,
                    isDefault = (item.id == body.currentModelId),
                    isVerified = true
                )
            }

            _catalogState.value = AntigravityCatalogState(
                models = newModels,
                isRefreshing = false,
                lastUpdated = now,
                errorMessage = null
            )
            saveToCache(newModels, now)

            Result.success(newModels)
        } catch (e: Exception) {
            // Keep existing models, do NOT clear cache!
            val userMsg = "Actualisation impossible (${e.message ?: "serveur inaccessible"}). Catalogue en cache conservé."
            _catalogState.value = current.copy(
                isRefreshing = false,
                errorMessage = userMsg
            )
            Result.failure(e)
        }
    }

    override fun isModelVerified(modelId: String): Boolean {
        return _catalogState.value.models.any { it.id == modelId }
    }
}
