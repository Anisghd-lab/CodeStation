package com.ghdinteractivestudio.codestation.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

@Serializable
data class HealthResponse(
    val ok: Boolean = false,
    val version: String = "2.28.0",
    val hostId: String? = null,
    val computerId: String? = null,
    val environment: String? = null,
    val busy: Boolean? = false
)

@Serializable
data class BackendInfo(
    val id: String,
    val name: String,
    val installed: Boolean = false,
    val available: Boolean = false,
    val description: String? = null
)

@Serializable
data class HelloResponse(
    val hostId: String? = null,
    val computerId: String? = null,
    val name: String? = null,
    val version: String? = null,
    val backends: List<BackendInfo> = emptyList(),
    val rev: Long? = 0
)

@Serializable
data class SendMessageRequest(
    val botId: String,
    val text: String,
    val clientNonce: String? = null,
    val replyTo: String? = null,
    val attachments: List<String> = emptyList()
)

@Serializable
data class StopTurnRequest(
    val botId: String
)

@Serializable
data class RespondPermissionRequest(
    val entryId: String,
    val optionId: String? = null
)

@Serializable
data class GenericApiResponse(
    val ok: Boolean = true,
    val error: String? = null
)

@Serializable
data class EnvironmentDto(
    @SerialName("bot_id") val botId: String = "",
    @SerialName("engine") val engine: String = "opencode",
    @SerialName("model") val model: String? = null,
    @SerialName("status") val status: String? = "READY",
    @SerialName("rootfs") val rootfs: String? = null,
    @SerialName("disk_mb") val diskMb: Int = 0,
    @SerialName("last_error") val lastError: String? = null
)

@Serializable
data class SystemResourcesDto(
    @SerialName("ram_total_mb") val ramTotalMb: Long = 0,
    @SerialName("ram_available_mb") val ramAvailableMb: Long = 0,
    @SerialName("disk_total_gb") val diskTotalGb: Long = 0,
    @SerialName("disk_available_gb") val diskAvailableGb: Long = 0
)

@Serializable
data class DetectedCliDto(
    @SerialName("id") val id: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("binary") val binary: String = "",
    @SerialName("path") val path: String? = null,
    @SerialName("version") val version: String? = null,
    @SerialName("status") val status: String = "NOT_DETECTED",
    @SerialName("is_primary") val isPrimary: Boolean = false,
    @SerialName("environment") val environment: String = "host",
    @SerialName("details") val details: String? = null
)

@Serializable
data class EnvironmentsResponse(
    @SerialName("environments") val environments: List<EnvironmentDto> = emptyList(),
    @SerialName("resources") val resources: SystemResourcesDto = SystemResourcesDto(),
    @SerialName("clis") val clis: List<DetectedCliDto> = emptyList()
)

@Serializable
data class EnvironmentActionRequest(
    @SerialName("botId") val botId: String
)

@Serializable
data class EnvironmentCreateRequest(
    @SerialName("botId") val botId: String,
    @SerialName("engine") val engine: String,
    @SerialName("model") val model: String? = null
)

interface CodeStationApiService {
    /** `GET /health`, open on loopback without token. */
    @GET("health")
    suspend fun getHealth(): Response<HealthResponse>

    /** `POST /api/hello`, loopback bearer authenticated. */
    @POST("api/hello")
    suspend fun hello(
        @Header("Authorization") authHeader: String? = null
    ): Response<HelloResponse>

    /** `POST /api/send`, post a message to a bot. */
    @POST("api/send")
    suspend fun sendMessage(
        @Body request: SendMessageRequest,
        @Header("Authorization") authHeader: String? = null
    ): Response<GenericApiResponse>

    /** `POST /api/stop`, stop turn for a bot. */
    @POST("api/stop")
    suspend fun stopTurn(
        @Body request: StopTurnRequest,
        @Header("Authorization") authHeader: String? = null
    ): Response<GenericApiResponse>

    /** `POST /api/respondPermission`, answer an approval card. */
    @POST("api/respondPermission")
    suspend fun respondPermission(
        @Body request: RespondPermissionRequest,
        @Header("Authorization") authHeader: String? = null
    ): Response<GenericApiResponse>

    /** `POST /api/environments`, list all isolated PRoot environments. */
    @POST("api/environments")
    suspend fun getEnvironments(
        @Body empty: Map<String, String> = emptyMap(),
        @Header("Authorization") authHeader: String? = null
    ): Response<EnvironmentsResponse>

    /** `POST /api/environmentStart`, start a bot's PRoot environment. */
    @POST("api/environmentStart")
    suspend fun startEnvironment(
        @Body request: EnvironmentActionRequest,
        @Header("Authorization") authHeader: String? = null
    ): Response<GenericApiResponse>

    /** `POST /api/environmentStop`, stop a bot's PRoot environment. */
    @POST("api/environmentStop")
    suspend fun stopEnvironment(
        @Body request: EnvironmentActionRequest,
        @Header("Authorization") authHeader: String? = null
    ): Response<GenericApiResponse>

    /** `POST /api/environmentCreate`, create a bot's PRoot environment. */
    @POST("api/environmentCreate")
    suspend fun createEnvironment(
        @Body request: EnvironmentCreateRequest,
        @Header("Authorization") authHeader: String? = null
    ): Response<GenericApiResponse>

    /** `POST /api/agentModels`, discover supported models for a backend. */
    @POST("api/agentModels")
    suspend fun getAgentModels(
        @Body request: AgentModelsRequest,
        @Header("Authorization") authHeader: String? = null
    ): Response<AgentModelsResponse>

    /** `POST /api/upload`, upload a file attachment chunk. */
    @POST("api/upload")
    suspend fun uploadChunk(
        @Body request: UploadChunkRequest,
        @Header("Authorization") authHeader: String? = null
    ): Response<UploadChunkResponse>

    /** `POST /api/memoryStatus`, get memory engine status. */
    @POST("api/memoryStatus")
    suspend fun getMemoryStatus(
        @Body empty: Map<String, String> = emptyMap(),
        @Header("Authorization") authHeader: String? = null
    ): Response<MemoryStatusDto>

    /** `POST /api/memoryExperiences`, get experiences list. */
    @POST("api/memoryExperiences")
    suspend fun getMemoryExperiences(
        @Body request: MemoryPaginationRequest = MemoryPaginationRequest(),
        @Header("Authorization") authHeader: String? = null
    ): Response<MemoryExperiencesResponse>

    /** `POST /api/memoryKnowledge`, get knowledge list. */
    @POST("api/memoryKnowledge")
    suspend fun getMemoryKnowledge(
        @Body request: MemoryPaginationRequest = MemoryPaginationRequest(),
        @Header("Authorization") authHeader: String? = null
    ): Response<MemoryKnowledgeResponse>

    /** `POST /api/memorySearch`, search memory. */
    @POST("api/memorySearch")
    suspend fun searchMemory(
        @Body request: MemorySearchRequest,
        @Header("Authorization") authHeader: String? = null
    ): Response<MemorySearchResponse>

    /** `POST /api/memorySync`, trigger git sync with GitHub. */
    @POST("api/memorySync")
    suspend fun syncMemory(
        @Body empty: Map<String, String> = emptyMap(),
        @Header("Authorization") authHeader: String? = null
    ): Response<MemorySyncResponse>
}

@Serializable
data class MemoryStatusDto(
    @SerialName("sync_status") val syncStatus: String = "not_initialized",
    @SerialName("last_sync_at") val lastSyncAt: String? = null,
    @SerialName("experiences_count") val experiencesCount: Int = 0,
    @SerialName("knowledge_count") val knowledgeCount: Int = 0,
    @SerialName("agents_count") val agentsCount: Int = 0,
    @SerialName("relations_count") val relationsCount: Int = 0,
    @SerialName("github_repo") val repoUrl: String = "",
    @SerialName("local_cache_path") val localCachePath: String? = null,
    @SerialName("error_message") val errorMessage: String? = null
)

@Serializable
data class MemoryPaginationRequest(
    @SerialName("limit") val limit: Int = 50,
    @SerialName("offset") val offset: Int = 0
)

@Serializable
data class MemoryExperienceDto(
    @SerialName("id") val id: String = "",
    @SerialName("title") val title: String = "",
    @SerialName("project_id") val projectId: String = "",
    @SerialName("author_agent") val authorAgent: String = "",
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("problem") val problem: String = "",
    @SerialName("context") val context: String = "",
    @SerialName("attempts") val attempts: List<String> = emptyList(),
    @SerialName("observed_results") val observedResults: List<String> = emptyList(),
    @SerialName("errors_encountered") val errorsEncountered: List<String> = emptyList(),
    @SerialName("solution_proposed") val solutionProposed: String? = null,
    @SerialName("validation_status") val validationStatus: String = "experimental",
    @SerialName("tags") val tags: List<String> = emptyList(),
    @SerialName("relations") val relations: List<String> = emptyList()
)

@Serializable
data class MemoryExperiencesResponse(
    @SerialName("experiences") val experiences: List<MemoryExperienceDto> = emptyList(),
    @SerialName("count") val count: Int = 0
)

@Serializable
data class MemoryKnowledgeDto(
    @SerialName("id") val id: String = "",
    @SerialName("type") val type: String = "solution",
    @SerialName("title") val title: String = "",
    @SerialName("content") val content: String = "",
    @SerialName("project_id") val projectId: String = "",
    @SerialName("source") val source: String = "",
    @SerialName("author") val author: String = "",
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("tags") val tags: List<String> = emptyList(),
    @SerialName("confidence") val confidence: Double = 0.8,
    @SerialName("validation_status") val validationStatus: String = "draft",
    @SerialName("relations") val relations: List<String> = emptyList(),
    @SerialName("version") val version: Int = 1
)

@Serializable
data class MemoryKnowledgeResponse(
    @SerialName("knowledge") val knowledge: List<MemoryKnowledgeDto> = emptyList(),
    @SerialName("count") val count: Int = 0
)

@Serializable
data class MemorySearchRequest(
    @SerialName("query") val query: String = "",
    @SerialName("project") val project: String? = null,
    @SerialName("agent") val agent: String? = null,
    @SerialName("type") val type: String? = null,
    @SerialName("status") val status: String? = null,
    @SerialName("limit") val limit: Int = 30
)

@Serializable
data class MemorySearchResultDto(
    @SerialName("id") val id: String = "",
    @SerialName("item_type") val itemType: String = "",
    @SerialName("title") val title: String = "",
    @SerialName("snippet") val snippet: String = "",
    @SerialName("validation_status") val validationStatus: String = "",
    @SerialName("tags") val tags: List<String> = emptyList(),
    @SerialName("author") val author: String? = null,
    @SerialName("score") val score: Float = 1.0f
)

@Serializable
data class MemorySearchResponse(
    @SerialName("results") val results: List<MemorySearchResultDto> = emptyList(),
    @SerialName("count") val count: Int = 0
)

@Serializable
data class MemorySyncResponse(
    @SerialName("success") val success: Boolean = false,
    @SerialName("message") val message: String = "",
    @SerialName("status") val status: MemoryStatusDto? = null
)

@Serializable
data class UploadChunkRequest(
    @SerialName("botId") val botId: String,
    @SerialName("uploadId") val uploadId: String,
    @SerialName("name") val name: String,
    @SerialName("data") val data: String,
    @SerialName("offset") val offset: Long,
    @SerialName("done") val done: Boolean
)

@Serializable
data class UploadAttachmentDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("size") val size: Long? = 0
)

@Serializable
data class UploadChunkResponse(
    @SerialName("attachment") val attachment: UploadAttachmentDto? = null
)

@Serializable
data class AgentModelsRequest(
    @SerialName("backend") val backend: String
)

@Serializable
data class AgentModelsResponse(
    @SerialName("models") val models: List<AgentModelItem> = emptyList(),
    @SerialName("currentModelId") val currentModelId: String? = null,
    @SerialName("fastMode") val fastMode: Boolean = false
)

@Serializable
data class AgentModelItem(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("description") val description: String? = null
)
