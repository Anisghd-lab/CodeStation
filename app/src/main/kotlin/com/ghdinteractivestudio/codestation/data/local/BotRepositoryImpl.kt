package com.ghdinteractivestudio.codestation.data.local

import com.ghdinteractivestudio.codestation.data.remote.CodeStationApiService
import com.ghdinteractivestudio.codestation.data.remote.StopTurnRequest
import com.ghdinteractivestudio.codestation.domain.model.Bot
import com.ghdinteractivestudio.codestation.domain.model.BotStatus
import com.ghdinteractivestudio.codestation.domain.model.PermissionPolicy
import com.ghdinteractivestudio.codestation.domain.model.ProviderType
import com.ghdinteractivestudio.codestation.domain.model.WorkspaceMode
import com.ghdinteractivestudio.codestation.domain.repository.BotRepository
import com.ghdinteractivestudio.codestation.domain.repository.ServerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BotRepositoryImpl(
    private val botDao: BotDao,
    private val apiServiceProvider: (suspend () -> CodeStationApiService?)? = null,
    private val serverRepository: ServerRepository? = null
) : BotRepository {

    override fun getBotsStream(): Flow<List<Bot>> {
        return botDao.getAllBotsStream().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getBotByIdStream(id: String): Flow<Bot?> {
        return botDao.getBotByIdStream(id).map { it?.toDomain() }
    }

    override suspend fun getBotById(id: String): Bot? {
        return botDao.getBotById(id)?.toDomain()
    }

    override suspend fun createBot(bot: Bot): Result<Bot> {
        return runCatching {
            botDao.insertBot(BotEntity.fromDomain(bot))
            bot
        }
    }

    override suspend fun updateBot(bot: Bot): Result<Bot> {
        return runCatching {
            botDao.updateBot(BotEntity.fromDomain(bot))
            bot
        }
    }

    override suspend fun saveBot(bot: Bot): Result<Bot> {
        return runCatching {
            val existing = botDao.getBotById(bot.id)
            if (existing == null) {
                botDao.insertBot(BotEntity.fromDomain(bot))
            } else {
                botDao.updateBot(BotEntity.fromDomain(bot))
            }
            bot
        }
    }

    override suspend fun deleteBot(id: String): Result<Unit> {
        return runCatching {
            val bot = botDao.getBotById(id)
            if (bot?.isLead == true) {
                error("Le Lead Bot ne peut pas être supprimé.")
            }
            botDao.deleteBotById(id)
        }
    }

    override suspend fun togglePin(id: String): Result<Unit> {
        return runCatching {
            val bot = botDao.getBotById(id)
            if (bot?.isLead == true) {
                error("Le Lead Bot est toujours épinglé en tête.")
            }
            botDao.togglePin(id)
        }
    }

    override suspend fun updateBotStatus(id: String, status: BotStatus): Result<Unit> {
        return runCatching {
            botDao.updateStatus(id, status.name)
        }
    }

    override suspend fun refreshBots(): Result<List<Bot>> {
        return runCatching {
            // S'assurer que les agents prioritaires Antigravity et OpenCode GLM sont toujours enregistrés
            if (botDao.getBotById("bot-antigravity") == null) {
                botDao.insertBot(
                    BotEntity.fromDomain(
                        Bot(
                            id = "bot-antigravity",
                            name = "Google Antigravity",
                            description = "Agent Google Antigravity propulsé par le CLI agy officiel",
                            avatarGlyph = "diamond",
                            avatarColorHex = "#6366F1",
                            provider = ProviderType.ANTIGRAVITY,
                            model = "gemini-4-argon-medium",
                            isFastMode = true,
                            isLead = true,
                            isPinned = true,
                            status = BotStatus.IDLE,
                            lastMessage = "Prêt. Google Antigravity CLI (agy) opérationnel.",
                            instructions = "Tu es un agent de codage Google Antigravity expert."
                        )
                    )
                )
            }
            if (botDao.getBotById("bot-opencode-glm") == null) {
                botDao.insertBot(
                    BotEntity.fromDomain(
                        Bot(
                            id = "bot-opencode-glm",
                            name = "OpenCode GLM",
                            description = "Agent OpenCode propulsé par GLM-5.3-Flash Free",
                            avatarGlyph = "hexagon",
                            avatarColorHex = "#A855F7",
                            provider = ProviderType.OPENCODE,
                            model = "opencode/glm-5.3-flash-free",
                            isFastMode = true,
                            isLead = false,
                            isPinned = true,
                            status = BotStatus.IDLE,
                            lastMessage = "Prêt. Modèle gratuit GLM-5.3-Flash actif via OpenCode CLI.",
                            instructions = "Tu es un agent de codage propulsé par OpenCode et le modèle gratuit GLM-5.3-Flash."
                        )
                    )
                )
            }
            if (botDao.getBotCount() == 0) {
                seedInitialBots()
            }
            emptyList()
        }
    }

    override suspend fun stopBotTurn(botId: String): Result<Unit> {
        return runCatching {
            botDao.updateStatus(botId, BotStatus.IDLE.name)
            if (apiServiceProvider != null && serverRepository != null) {
                val config = serverRepository.getServerConfig()
                val api = apiServiceProvider.invoke()
                if (api != null && !config.token.isNullOrBlank()) {
                    val auth = "Bearer ${config.token}"
                    api.stopTurn(
                        StopTurnRequest(botId = botId),
                        authHeader = auth
                    )
                }
            }
        }
    }

    suspend fun seedInitialBots() {
        val initialBots = listOf(
            Bot(
                id = "bot-antigravity",
                name = "Google Antigravity",
                description = "Agent Google Antigravity propulsé par le CLI agy officiel",
                avatarGlyph = "diamond",
                avatarColorHex = "#6366F1", // Indigo
                provider = ProviderType.ANTIGRAVITY,
                model = "gemini-4-argon-medium",
                isFastMode = true,
                isLead = true,
                isPinned = true,
                status = BotStatus.IDLE,
                lastMessage = "Prêt. Google Antigravity CLI (agy) opérationnel.",
                instructions = "Tu es un agent de codage Google Antigravity expert."
            ),
            Bot(
                id = "bot-opencode-glm",
                name = "OpenCode GLM",
                description = "Agent OpenCode propulsé par GLM-5.3-Flash Free",
                avatarGlyph = "hexagon",
                avatarColorHex = "#A855F7",
                provider = ProviderType.OPENCODE,
                model = "opencode/glm-5.3-flash-free",
                isFastMode = true,
                isLead = false,
                isPinned = true,
                status = BotStatus.IDLE,
                lastMessage = "Prêt. Modèle gratuit GLM-5.3-Flash actif via OpenCode CLI.",
                instructions = "Tu es un agent de codage propulsé par OpenCode et le modèle gratuit GLM-5.3-Flash."
            ),
            Bot(
                id = "bot-codex-terminal",
                name = "OpenAI Codex",
                description = "Exécution de scripts, diagnostics bash et git (Termux)",
                avatarGlyph = "square",
                avatarColorHex = "#00E676",
                provider = ProviderType.OPENAI_CODEX,
                model = "gpt-4o",
                isFastMode = false,
                isLead = false,
                isPinned = false,
                status = BotStatus.IDLE,
                lastMessage = "Disponible dans Termux.",
                permissionPolicy = PermissionPolicy.ASK_SHELL,
                instructions = "Assistant de commandes bash et d'exécution d'outils système sécurisés."
            )
        )
        botDao.insertAll(initialBots.map { BotEntity.fromDomain(it) })
    }
}
