package com.ghdinteractivestudio.synapse.data.remote

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

interface SynapseApiService {
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
}
