package com.ghdinteractivestudio.codestation.data.local

import com.ghdinteractivestudio.codestation.data.remote.RespondPermissionRequest
import com.ghdinteractivestudio.codestation.data.remote.SendMessageRequest
import com.ghdinteractivestudio.codestation.data.remote.StopTurnRequest
import com.ghdinteractivestudio.codestation.data.remote.CodeStationApiService
import com.ghdinteractivestudio.codestation.domain.model.ApprovalRequest
import com.ghdinteractivestudio.codestation.domain.model.ApprovalStatus
import com.ghdinteractivestudio.codestation.domain.model.ChatMessage
import com.ghdinteractivestudio.codestation.domain.model.MessageAuthor
import com.ghdinteractivestudio.codestation.domain.model.MessageAuthorType
import com.ghdinteractivestudio.codestation.domain.model.QuotedReply
import com.ghdinteractivestudio.codestation.domain.repository.ChatRepository
import com.ghdinteractivestudio.codestation.domain.repository.ServerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class ChatRepositoryImpl(
    private val messageDao: MessageDao,
    private val approvalDao: ApprovalDao,
    private val botDao: BotDao,
    private val apiServiceProvider: (suspend () -> CodeStationApiService?)? = null,
    private val serverRepository: ServerRepository? = null,
    private val context: android.content.Context? = null
) : ChatRepository {

    override fun getMessagesStream(botId: String): Flow<List<ChatMessage>> {
        return messageDao.getMessagesByBotIdStream(botId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getPendingApprovalsStream(botId: String?): Flow<List<ApprovalRequest>> {
        val stream = if (botId == null) {
            approvalDao.getAllPendingApprovalsStream()
        } else {
            approvalDao.getPendingApprovalsForBotStream(botId)
        }
        return stream.map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun sendMessage(
        botId: String,
        text: String,
        replyTo: QuotedReply?,
        attachments: List<com.ghdinteractivestudio.codestation.domain.model.FileAttachment>
    ): Result<ChatMessage> {
        return runCatching {
            val now = System.currentTimeMillis()
            val recentMsg = messageDao.findRecentUserMessage(botId, text)
            val isRetry = recentMsg != null && (now - recentMsg.timestamp < 120_000L)

            val userMsg = if (isRetry && recentMsg != null) {
                recentMsg.toDomain()
            } else {
                val newMsg = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    botId = botId,
                    author = MessageAuthor(MessageAuthorType.USER, "user", "Moi"),
                    text = text,
                    timestamp = now,
                    isFinal = true,
                    replyTo = replyTo,
                    attachments = attachments
                )
                messageDao.insertMessage(ChatMessageEntity.fromDomain(newMsg))
                newMsg
            }

            val previewText = text.ifBlank { if (attachments.isNotEmpty()) "[Pièce jointe: ${attachments.first().name}]" else "" }
            botDao.updateLastMessage(botId, previewText, userMsg.timestamp)

            // Envoi réseau vers apiService si connecté
            if (apiServiceProvider != null && serverRepository != null) {
                val config = serverRepository.getServerConfig()
                val api = apiServiceProvider.invoke()
                if (config.token.isNullOrBlank()) {
                    throw IllegalStateException("Authentification absente : hôte non appairé. Lancez './start_code_station.sh pair' dans Termux ou saisissez le jeton dans les Paramètres.")
                }
                if (api == null) {
                    throw IllegalStateException("Serveur inaccessible sur ${config.url}. Vérifiez que CodeStation Host est démarré.")
                }
                val auth = "Bearer ${config.token}"

                // Téléversement préalable obligatoire de chaque pièce jointe vers l'hôte CodeStation
                for (att in attachments) {
                    val localUriStr = att.localUri ?: continue
                    val uri = android.net.Uri.parse(localUriStr)
                    val fileBytes = try {
                        context?.contentResolver?.openInputStream(uri)?.use { it.readBytes() }
                            ?: java.io.File(uri.path ?: "").readBytes()
                    } catch (e: Exception) {
                        throw IllegalStateException("Impossible de lire la pièce jointe '${att.name}' : ${e.localizedMessage}")
                    }

                    val totalSize = fileBytes.size.toLong()
                    val chunkSize = 256 * 1024 // 256 Ko par fragment
                    var offset = 0L

                    if (fileBytes.isEmpty()) {
                        val req = com.ghdinteractivestudio.codestation.data.remote.UploadChunkRequest(
                            botId = botId,
                            uploadId = att.id,
                            name = att.name,
                            data = "",
                            offset = 0L,
                            done = true
                        )
                        val uploadRes = api.uploadChunk(req, authHeader = auth)
                        if (!uploadRes.isSuccessful) {
                            throw IllegalStateException("Échec de l'envoi de '${att.name}' (HTTP ${uploadRes.code()})")
                        }
                    } else {
                        while (offset < totalSize) {
                            val chunkLen = ((totalSize - offset).toInt()).coerceAtMost(chunkSize)
                            val chunkSlice = fileBytes.copyOfRange(offset.toInt(), offset.toInt() + chunkLen)
                            val base64Data = android.util.Base64.encodeToString(chunkSlice, android.util.Base64.NO_WRAP)
                            val isDone = (offset + chunkLen >= totalSize)

                            val req = com.ghdinteractivestudio.codestation.data.remote.UploadChunkRequest(
                                botId = botId,
                                uploadId = att.id,
                                name = att.name,
                                data = base64Data,
                                offset = offset,
                                done = isDone
                            )
                            val uploadRes = api.uploadChunk(req, authHeader = auth)
                            if (!uploadRes.isSuccessful) {
                                val errBody = uploadRes.errorBody()?.string().orEmpty()
                                throw IllegalStateException("Échec de l'envoi de la pièce jointe '${att.name}' (HTTP ${uploadRes.code()}: $errBody)")
                            }
                            offset += chunkLen
                        }
                    }
                }

                val response = try {
                    api.sendMessage(
                        SendMessageRequest(
                            botId = botId,
                            text = text,
                            clientNonce = userMsg.id,
                            replyTo = replyTo?.messageId,
                            attachments = attachments.map { it.id }
                        ),
                        authHeader = auth
                    )
                } catch (e: java.net.ConnectException) {
                    throw IllegalStateException("Serveur inaccessible (${config.url}). Vérifiez que CodeStation Host est démarré.")
                } catch (e: java.net.SocketTimeoutException) {
                    throw IllegalStateException("Délai de connexion dépassé lors de l'envoi au serveur.")
                } catch (e: Exception) {
                    throw IllegalStateException("Transmission impossible : ${e.localizedMessage ?: "erreur réseau"}")
                }
                if (!response.isSuccessful) {
                    val code = response.code()
                    val err = response.errorBody()?.string().orEmpty()
                    when (code) {
                        401, 403 -> throw IllegalStateException("Authentification refusée (HTTP $code) : jeton invalide ou non autorisé.")
                        404 -> throw IllegalStateException("Route inconnue ou bot '$botId' non trouvé sur le serveur.")
                        502, 503 -> throw IllegalStateException("Fournisseur de modèle indisponible (HTTP $code).")
                        else -> throw IllegalStateException("Le serveur a refusé la requête (HTTP $code: $err)")
                    }
                }
            }

            userMsg
        }
    }

    override suspend fun respondApproval(
        approvalId: String,
        status: ApprovalStatus,
        alwaysApprove: Boolean
    ): Result<Unit> {
        return runCatching {
            approvalDao.updateStatus(approvalId, status.name, alwaysApprove)
            if (apiServiceProvider != null && serverRepository != null) {
                val config = serverRepository.getServerConfig()
                val api = apiServiceProvider.invoke()
                if (api != null && !config.token.isNullOrBlank()) {
                    val auth = "Bearer ${config.token}"
                    val optionId = when (status) {
                        ApprovalStatus.APPROVED -> if (alwaysApprove) "allow_always" else "allow_once"
                        ApprovalStatus.REJECTED -> if (alwaysApprove) "reject_always" else "reject_once"
                        else -> "allow_once"
                    }
                    val resp = api.respondPermission(
                        RespondPermissionRequest(entryId = approvalId, optionId = optionId),
                        authHeader = auth
                    )
                    if (!resp.isSuccessful) {
                        val err = resp.errorBody()?.string() ?: "Erreur HTTP ${resp.code()}"
                        throw IllegalStateException("Échec de transmission de l'approbation ($err)")
                    }
                }
            }
        }
    }

    suspend fun insertNotice(
        botId: String,
        text: String
    ): Result<ChatMessage> {
        return runCatching {
            val noticeMsg = ChatMessage(
                id = UUID.randomUUID().toString(),
                botId = botId,
                author = MessageAuthor(MessageAuthorType.SYSTEM, "system", "Système"),
                text = text,
                timestamp = System.currentTimeMillis(),
                isFinal = true
            )
            messageDao.insertMessage(ChatMessageEntity.fromDomain(noticeMsg))
            botDao.updateLastMessage(botId, text, noticeMsg.timestamp)
            noticeMsg
        }
    }

    override suspend fun clearHistory(botId: String): Result<Unit> {
        return runCatching {
            messageDao.deleteMessagesForBot(botId)
        }
    }

    override suspend fun markChatRead(botId: String): Result<Unit> {
        return runCatching {
            // Marquage local
        }
    }

    suspend fun insertAgentReply(
        botId: String,
        botName: String,
        text: String,
        traceSteps: List<com.ghdinteractivestudio.codestation.domain.model.ExecutionStep> = emptyList()
    ): Result<ChatMessage> {
        return runCatching {
            val agentMsg = ChatMessage(
                id = UUID.randomUUID().toString(),
                botId = botId,
                author = MessageAuthor(MessageAuthorType.BOT, botId, botName),
                text = text,
                timestamp = System.currentTimeMillis(),
                isFinal = true
            )
            messageDao.insertMessage(ChatMessageEntity.fromDomain(agentMsg))
            botDao.updateLastMessage(botId, text, agentMsg.timestamp)
            agentMsg
        }
    }

    suspend fun createApprovalRequest(
        botId: String,
        botName: String,
        command: String,
        diff: String? = null
    ): Result<ApprovalRequest> {
        return runCatching {
            val approval = ApprovalRequest(
                id = UUID.randomUUID().toString(),
                botId = botId,
                botName = botName,
                actionType = com.ghdinteractivestudio.codestation.domain.model.ApprovalActionType.SHELL_COMMAND,
                commandOrPath = command,
                diffPreview = diff,
                timestamp = System.currentTimeMillis(),
                status = ApprovalStatus.PENDING
            )
            approvalDao.insertApproval(ApprovalEntity.fromDomain(approval))
            approval
        }
    }
}
