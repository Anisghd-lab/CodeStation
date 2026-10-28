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
data class EnvironmentsResponse(
    @SerialName("environments") val environments: List<EnvironmentDto> = emptyList(),
    @SerialName("resources") val resources: SystemResourcesDto = SystemResourcesDto()
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
}
