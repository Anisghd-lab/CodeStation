package com.ghdinteractivestudio.codestation.data.local

import com.ghdinteractivestudio.codestation.data.remote.CodeStationApiService
import com.ghdinteractivestudio.codestation.data.remote.CreateBotRequest
import com.ghdinteractivestudio.codestation.data.remote.DeleteBotRequest
import com.ghdinteractivestudio.codestation.data.remote.StopTurnRequest
import com.ghdinteractivestudio.codestation.data.remote.UpdateBotRequest
import com.ghdinteractivestudio.codestation.domain.model.Bot
import com.ghdinteractivestudio.codestation.domain.model.BotStatus
import com.ghdinteractivestudio.codestation.domain.model.EnvironmentStatus
import com.ghdinteractivestudio.codestation.domain.model.PermissionPolicy
import com.ghdinteractivestudio.codestation.domain.model.ProviderType
import com.ghdinteractivestudio.codestation.domain.model.WorkspaceMode
import com.ghdinteractivestudio.codestation.domain.repository.BotRepository
import com.ghdinteractivestudio.codestation.domain.repository.ServerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class BotRepositoryImpl(
    private val botDao: BotDao,
    private val apiServiceProvider: (suspend () -> CodeStationApiService?)? = null,
    private val serverRepository: ServerRepository? = null
) : BotRepository {

    companion object {
        val OBSOLETE_BOT_IDS = setOf(
            "bot-codex-terminal",
            "bot-claude-architect",
            "bot-lead-codestation",
            "8731af25-da7f-4955-9b37-247f45c60912"
        )
    }

    override fun getBotsStream(): Flow<List<Bot>> = flow {
        if (botDao.getBotCount() == 0) {
            seedInitialBots()
        } else {
            ensureEssentialBots()
        }
        emitAll(botDao.getAllBotsStream().map { entities ->
            entities.filter { it.id !in OBSOLETE_BOT_IDS }.map { it.toDomain() }
        })
    }

    override fun getBotByIdStream(id: String): Flow<Bot?> {
        return botDao.getBotByIdStream(id).map { it?.toDomain() }
    }

    override suspend fun getBotById(id: String): Bot? {
        return botDao.getBotById(id)?.toDomain()
    }

    override suspend fun createBot(bot: Bot): Result<Bot> {
        return runCatching {
            if (apiServiceProvider != null && serverRepository != null) {
                val config = serverRepository.getServerConfig()
                val api = apiServiceProvider.invoke()
                if (api != null && !config.token.isNullOrBlank()) {
                    val auth = "Bearer ${config.token}"
                    val req = CreateBotRequest(
                        id = bot.id,
                        name = bot.name,
                        description = bot.description,
                        kind = if (bot.isGroup) "group" else "agent",
                        backend = bot.provider.backendId,
                        model = bot.model,
                        avatarColor = bot.avatarColorHex,
                        avatarShape = bot.avatarGlyph,
                        prompt = bot.instructions,
                        permission = when (bot.permissionPolicy) {
                            PermissionPolicy.AUTONOMOUS -> "auto"
                            else -> "ask"
                        },
                        fastMode = bot.isFastMode
                    )
                    runCatching {
                        api.createBot(req, authHeader = auth)
                    }
                }
            }
            botDao.insertBot(BotEntity.fromDomain(bot))
            val repo = serverRepository
            if (repo != null) {
                runCatching {
                    repo.createEnvironment(
                        botId = bot.id,
                        engine = bot.provider.backendId,
                        model = bot.model
                    )
                }
            }
            bot
        }
    }

    override suspend fun updateBot(bot: Bot): Result<Bot> {
        return runCatching {
            if (apiServiceProvider != null && serverRepository != null) {
                val config = serverRepository.getServerConfig()
                val api = apiServiceProvider.invoke()
                if (api != null && !config.token.isNullOrBlank()) {
                    val auth = "Bearer ${config.token}"
                    val req = UpdateBotRequest(
                        id = bot.id,
                        name = bot.name,
                        description = bot.description,
                        backend = bot.provider.backendId,
                        model = bot.model,
                        prompt = bot.instructions
                    )
                    runCatching {
                        api.updateBot(req, authHeader = auth)
                    }
                }
            }
            botDao.updateBot(BotEntity.fromDomain(bot))
            bot
        }
    }

    override suspend fun saveBot(bot: Bot): Result<Bot> {
        return runCatching {
            val existing = botDao.getBotById(bot.id)
            if (existing == null) {
                createBot(bot).getOrThrow()
            } else {
                updateBot(bot).getOrThrow()
            }
        }
    }

    override suspend fun deleteBot(id: String): Result<Unit> {
        return runCatching {
            val bot = botDao.getBotById(id)
            if (bot?.isLead == true) {
                error("Le Lead Bot ne peut pas être supprimé.")
            }
            if (apiServiceProvider != null && serverRepository != null) {
                val config = serverRepository.getServerConfig()
                val api = apiServiceProvider.invoke()
                if (api != null && !config.token.isNullOrBlank()) {
                    val auth = "Bearer ${config.token}"
                    runCatching {
                        api.deleteBot(DeleteBotRequest(botId = id), authHeader = auth)
                    }
                }
            }
            botDao.deleteBotById(id)
            val repo = serverRepository
            if (repo != null) {
                runCatching {
                    repo.stopEnvironment(id)
                }
            }
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
            // P1-02: Garantir que la base Room locale contient toujours les bots configurés, même hors ligne
            if (botDao.getBotCount() == 0) {
                seedInitialBots()
            } else {
                ensureEssentialBots()
            }

            // Synchronisation non destructive des statuts d'environnements si le Host est disponible
            val repo = serverRepository
            if (repo != null) {
                runCatching {
                    val envResult = repo.getEnvironments()
                    if (envResult.isSuccess) {
                        val environments = envResult.getOrThrow().first
                        val envMap = environments.associateBy { it.botId }
                        val currentEntities = botDao.getAllBotsStream().first()
                        for (entity in currentEntities) {
                            val env = envMap[entity.id]
                            if (env != null) {
                                val newStatus = when (env.status) {
                                    EnvironmentStatus.BUSY -> BotStatus.BUSY
                                    EnvironmentStatus.STOPPED, EnvironmentStatus.ERROR, EnvironmentStatus.NOT_INSTALLED, EnvironmentStatus.STOPPING -> BotStatus.OFFLINE
                                    EnvironmentStatus.READY -> BotStatus.IDLE
                                    EnvironmentStatus.STARTING, EnvironmentStatus.CREATING, EnvironmentStatus.INSTALLING -> BotStatus.WORKING
                                }
                                if (entity.status != newStatus.name) {
                                    botDao.updateStatus(entity.id, newStatus.name)
                                }
                            }
                        }
                    }
                }
            }

            // Retourner la liste réelle des bots stockés dans Room
            botDao.getAllBotsStream().first()
                .filter { it.id !in OBSOLETE_BOT_IDS }
                .map { it.toDomain() }
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

    suspend fun ensureEssentialBots() {
        val initialBots = getInitialBotsList()
        for (bot in initialBots) {
            if (botDao.getBotById(bot.id) == null) {
                botDao.insertBot(BotEntity.fromDomain(bot))
            }
        }
    }

    suspend fun seedInitialBots() {
        val initialBots = getInitialBotsList()
        botDao.insertAll(initialBots.map { BotEntity.fromDomain(it) })
    }

    fun getInitialBotsList(): List<Bot> {
        return listOf(
            Bot(
                id = "bot-antigravity",
                name = "Google Antigravity",
                description = "Agent Google Antigravity propulsé par le CLI agy officiel",
                avatarGlyph = "diamond",
                avatarColorHex = "#4285F4",
                provider = ProviderType.ANTIGRAVITY,
                model = "gemini-3.8-flash-medium",
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
                id = "bot-codebuddy",
                name = "CodeBuddy Code",
                description = "Agent CodeBuddy Code propulsé par la CLI locale (Hy3 v2.178.1)",
                avatarGlyph = "circle",
                avatarColorHex = "#3B82F6",
                provider = ProviderType.CODEBUDDY,
                model = "Hy3",
                isFastMode = true,
                isLead = false,
                isPinned = true,
                status = BotStatus.IDLE,
                lastMessage = "Prêt. CodeBuddy Code CLI (v2.178.1) opérationnel.",
                instructions = "Tu es un agent de codage CodeBuddy Code expert."
            ),
            Bot(
                id = "bot-omnirush",
                name = "OmniRush",
                description = "Agent OmniRush propulsé par GPT-6 Astra",
                avatarGlyph = "circle",
                avatarColorHex = "#10B981",
                provider = ProviderType.OMNIRUSH,
                model = "gpt-6-astra",
                isFastMode = true,
                isLead = false,
                isPinned = true,
                status = BotStatus.IDLE,
                lastMessage = "Prêt. OmniRush CLI (2.7.8) opérationnel.",
                instructions = "Tu es un agent de développement OmniRush haute performance."
            ),
            Bot(
                id = "bot-clixad",
                name = "Clixad",
                description = "Agent Clixad propulsé par GPT-5 Nano",
                avatarGlyph = "square",
                avatarColorHex = "#EC4899",
                provider = ProviderType.CLIXAD,
                model = "gpt-5-nano",
                isFastMode = true,
                isLead = false,
                isPinned = true,
                status = BotStatus.IDLE,
                lastMessage = "Prêt. Clixad CLI (0.0.1-beta.41) opérationnel.",
                instructions = "Tu es un assistant de codage Clixad agile."
            ),
            Bot(
                id = "bot-freebuff",
                name = "Freebuff",
                description = "Free AI Coding Assistant propulsé par Freebuff CLI",
                avatarGlyph = "hexagon",
                avatarColorHex = "#F59E0B",
                provider = ProviderType.FREEBUFF,
                model = "freebuff/free-auto",
                isFastMode = true,
                isLead = false,
                isPinned = true,
                status = BotStatus.IDLE,
                lastMessage = "Prêt. Freebuff CLI (0.2.35) opérationnel.",
                instructions = "Tu es un assistant d'ingénierie logicielle autonome Freebuff."
            )
        )
    }
}
