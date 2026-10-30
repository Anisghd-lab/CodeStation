package com.ghdinteractivestudio.codestation.data.local

import com.ghdinteractivestudio.codestation.data.remote.CodeStationApiService
import com.ghdinteractivestudio.codestation.data.remote.MemoryPaginationRequest
import com.ghdinteractivestudio.codestation.data.remote.MemorySearchRequest
import com.ghdinteractivestudio.codestation.data.remote.MemoryStatusDto
import com.ghdinteractivestudio.codestation.domain.model.CollectiveMemoryStatus
import com.ghdinteractivestudio.codestation.domain.model.MemoryExperience
import com.ghdinteractivestudio.codestation.domain.model.MemoryKnowledge
import com.ghdinteractivestudio.codestation.domain.model.MemorySearchResultItem
import com.ghdinteractivestudio.codestation.domain.model.MemorySyncState
import com.ghdinteractivestudio.codestation.domain.repository.MemoryRepository
import com.ghdinteractivestudio.codestation.domain.repository.ServerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MemoryRepositoryImpl(
    private val apiServiceProvider: suspend () -> CodeStationApiService?,
    private val serverRepository: ServerRepository
) : MemoryRepository {

    private val _memoryStatus = MutableStateFlow(CollectiveMemoryStatus())
    override val memoryStatus: StateFlow<CollectiveMemoryStatus> = _memoryStatus.asStateFlow()

    private suspend fun getAuthHeader(): String? {
        val config = serverRepository.getServerConfig()
        val token = config.token
        return if (!token.isNullOrBlank()) "Bearer $token" else null
    }

    override suspend fun refreshStatus(): Result<CollectiveMemoryStatus> = runCatching {
        val api = apiServiceProvider() ?: throw IllegalStateException("API service indisponible")
        val auth = getAuthHeader()
        val response = api.getMemoryStatus(authHeader = auth)
        if (!response.isSuccessful || response.body() == null) {
            val err = "Erreur HTTP ${response.code()}: ${response.errorBody()?.string()}"
            val fallback = _memoryStatus.value.copy(
                syncStatus = MemorySyncState.OFFLINE,
                errorMessage = err
            )
            _memoryStatus.value = fallback
            return@runCatching fallback
        }

        val dto = response.body()!!
        val syncState = parseSyncState(dto.syncStatus)
        val status = CollectiveMemoryStatus(
            isInitialized = dto.experiencesCount > 0 || dto.knowledgeCount > 0,
            syncStatus = syncState,
            lastSyncAt = dto.lastSyncAt,
            totalExperiences = dto.experiencesCount,
            totalKnowledge = dto.knowledgeCount,
            totalRelations = dto.relationsCount,
            totalAgents = dto.agentsCount,
            repoUrl = dto.repoUrl.ifEmpty { "https://github.com/Anisghd-lab/CodeStation-Memory" },
            branch = "main",
            latestCommitHash = null,
            errorMessage = dto.errorMessage
        )
        _memoryStatus.value = status
        status
    }

    override suspend fun getExperiences(limit: Int, offset: Int): Result<List<MemoryExperience>> = runCatching {
        val api = apiServiceProvider() ?: throw IllegalStateException("API service indisponible")
        val auth = getAuthHeader()
        val response = api.getMemoryExperiences(MemoryPaginationRequest(limit, offset), auth)
        if (!response.isSuccessful || response.body() == null) {
            throw IllegalStateException("Erreur HTTP ${response.code()}")
        }
        response.body()!!.experiences.map { dto ->
            MemoryExperience(
                id = dto.id,
                title = dto.title,
                agent = dto.authorAgent,
                project = dto.projectId,
                context = dto.context,
                problem = dto.problem,
                rootCause = dto.errorsEncountered.firstOrNull() ?: "",
                solution = dto.solutionProposed ?: "",
                outcome = dto.observedResults.firstOrNull() ?: "",
                impact = "",
                validationStatus = dto.validationStatus,
                tags = dto.tags,
                createdAt = dto.createdAt,
                filesChanged = emptyList()
            )
        }
    }

    override suspend fun getKnowledge(limit: Int, offset: Int): Result<List<MemoryKnowledge>> = runCatching {
        val api = apiServiceProvider() ?: throw IllegalStateException("API service indisponible")
        val auth = getAuthHeader()
        val response = api.getMemoryKnowledge(MemoryPaginationRequest(limit, offset), auth)
        if (!response.isSuccessful || response.body() == null) {
            throw IllegalStateException("Erreur HTTP ${response.code()}")
        }
        response.body()!!.knowledge.map { dto ->
            MemoryKnowledge(
                id = dto.id,
                title = dto.title,
                category = dto.type,
                content = dto.content,
                validationStatus = dto.validationStatus,
                version = dto.version,
                derivedFromExperiences = dto.relations,
                appliesToProjects = listOf(dto.projectId),
                tags = dto.tags,
                createdAt = dto.createdAt,
                updatedAt = dto.updatedAt
            )
        }
    }

    override suspend fun search(
        query: String,
        project: String?,
        agent: String?,
        type: String?,
        status: String?,
        limit: Int
    ): Result<List<MemorySearchResultItem>> = runCatching {
        val api = apiServiceProvider() ?: throw IllegalStateException("API service indisponible")
        val auth = getAuthHeader()
        val response = api.searchMemory(
            MemorySearchRequest(
                query = query,
                project = project,
                agent = agent,
                type = type,
                status = status,
                limit = limit
            ),
            auth
        )
        if (!response.isSuccessful || response.body() == null) {
            throw IllegalStateException("Erreur HTTP ${response.code()}")
        }
        response.body()!!.results.map { dto ->
            MemorySearchResultItem(
                id = dto.id,
                itemType = dto.itemType,
                title = dto.title,
                summary = dto.snippet,
                agent = dto.author,
                project = null,
                validationStatus = dto.validationStatus,
                relevanceScore = dto.score,
                tags = dto.tags
            )
        }
    }

    override suspend fun triggerSync(): Result<CollectiveMemoryStatus> = runCatching {
        val api = apiServiceProvider() ?: throw IllegalStateException("API service indisponible")
        val auth = getAuthHeader()

        // Temporarily set status to SYNCING
        _memoryStatus.value = _memoryStatus.value.copy(syncStatus = MemorySyncState.SYNCING)

        val response = api.syncMemory(authHeader = auth)
        if (!response.isSuccessful || response.body() == null) {
            _memoryStatus.value = _memoryStatus.value.copy(
                syncStatus = MemorySyncState.SYNC_ERROR,
                errorMessage = "Erreur HTTP ${response.code()}"
            )
            throw IllegalStateException("Erreur de synchronisation")
        }

        val body = response.body()!!
        val statusDto = body.status
        val newStatus = if (statusDto != null) {
            CollectiveMemoryStatus(
                isInitialized = statusDto.experiencesCount > 0 || statusDto.knowledgeCount > 0,
                syncStatus = parseSyncState(statusDto.syncStatus),
                lastSyncAt = statusDto.lastSyncAt,
                totalExperiences = statusDto.experiencesCount,
                totalKnowledge = statusDto.knowledgeCount,
                totalRelations = statusDto.relationsCount,
                totalAgents = statusDto.agentsCount,
                repoUrl = statusDto.repoUrl,
                branch = "main",
                latestCommitHash = null,
                errorMessage = statusDto.errorMessage
            )
        } else {
            _memoryStatus.value.copy(syncStatus = MemorySyncState.SYNCED)
        }

        _memoryStatus.value = newStatus
        newStatus
    }

    private fun parseSyncState(raw: String): MemorySyncState {
        return when (raw.lowercase()) {
            "synced", "synchronized" -> MemorySyncState.SYNCED
            "syncing" -> MemorySyncState.SYNCING
            "pending_changes" -> MemorySyncState.PENDING_CHANGES
            "offline" -> MemorySyncState.OFFLINE
            "sync_error", "error" -> MemorySyncState.SYNC_ERROR
            else -> MemorySyncState.NOT_INITIALIZED
        }
    }
}
