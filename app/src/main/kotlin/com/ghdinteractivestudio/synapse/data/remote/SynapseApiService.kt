package com.ghdinteractivestudio.synapse.data.remote

import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

@Serializable
data class ServerInfoResponse(
    val status: String,
    val version: String,
    val hostId: String? = null,
    val activeBotsCount: Int = 0
)

@Serializable
data class SendMessageRequest(
    val botId: String,
    val text: String,
    val replyToId: String? = null
)

@Serializable
data class SendMessageResponse(
    val messageId: String,
    val replyText: String? = null,
    val isComplete: Boolean = true
)

@Serializable
data class ApproveRequest(
    val approvalId: String,
    val decision: String, // "APPROVE", "REJECT"
    val alwaysApprove: Boolean = false
)

@Serializable
data class ApiResponse(
    val success: Boolean,
    val message: String? = null
)

interface SynapseApiService {
    @GET("api/info")
    suspend fun getServerInfo(
        @Header("Authorization") authHeader: String? = null
    ): Response<ServerInfoResponse>

    @POST("api/send")
    suspend fun sendMessage(
        @Body request: SendMessageRequest,
        @Header("Authorization") authHeader: String? = null
    ): Response<SendMessageResponse>

    @POST("api/approve")
    suspend fun respondApproval(
        @Body request: ApproveRequest,
        @Header("Authorization") authHeader: String? = null
    ): Response<ApiResponse>

    @POST("api/stop")
    suspend fun stopTurn(
        @Body botId: String,
        @Header("Authorization") authHeader: String? = null
    ): Response<ApiResponse>
}
