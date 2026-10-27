package com.ghdinteractivestudio.synapse.data.local

import com.ghdinteractivestudio.synapse.domain.model.Bot
import com.ghdinteractivestudio.synapse.domain.model.BotStatus
import com.ghdinteractivestudio.synapse.domain.model.PermissionPolicy
import com.ghdinteractivestudio.synapse.domain.model.ProviderType
import com.ghdinteractivestudio.synapse.domain.model.WorkspaceMode
import com.ghdinteractivestudio.synapse.domain.repository.BotRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BotRepositoryImpl(
    private val botDao: BotDao
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
            // Initial seed if empty
            if (botDao.getBotCount() == 0) {
                seedInitialBots()
            }
            val entities = botDao.getAllBotsStream()
            emptyList()
        }
    }

    override suspend fun stopBotTurn(botId: String): Result<Unit> {
        return runCatching {
            botDao.updateStatus(botId, BotStatus.IDLE.name)
        }
    }

    suspend fun seedInitialBots() {
        val initialBots = listOf(
            Bot(
                id = "bot-lead-synapse",
                name = "Synapse Core",
                description = "Agent principal autonome — Check-ins proactifs & coordination",
                avatarGlyph = "diamond",
                avatarColorHex = "#FFB703", // Gold Star
                provider = ProviderType.ACP_LOCAL,
                model = "synapse-neural-core",
                isFastMode = true,
                isLead = true,
                isPinned = true,
                status = BotStatus.IDLE,
                lastMessage = "Système opérationnel. Tout est prêt pour orchestrer vos tâches.",
                instructions = "Tu es l'agent principal Synapse. Tu accueilles l'utilisateur, vérifies l'état des projets et coordonnes les autres agents."
            ),
            Bot(
                id = "bot-claude-architect",
                name = "Claude Architect",
                description = "Spécialiste conception système & refactoring Kotlin/Rust",
                avatarGlyph = "polygon",
                avatarColorHex = "#00D2FF",
                provider = ProviderType.CLAUDE_CODE,
                model = "claude-3-7-sonnet",
                isFastMode = true,
                isLead = false,
                isPinned = true,
                status = BotStatus.IDLE,
                lastMessage = "Architecture MVVM validée. Aucune fuite de mémoire détectée.",
                instructions = "Expert senior en architecture logicielle Android Jetpack Compose et protocoles distribués."
            ),
            Bot(
                id = "bot-codex-terminal",
                name = "Codex Terminal",
                description = "Exécution de scripts, diagnostics bash et git",
                avatarGlyph = "square",
                avatarColorHex = "#00E676",
                provider = ProviderType.OPENAI_CODEX,
                model = "gpt-4o",
                isFastMode = false,
                isLead = false,
                isPinned = false,
                status = BotStatus.IDLE,
                lastMessage = "Tests unitaires passés avec succès (100% couverture).",
                permissionPolicy = PermissionPolicy.ASK_SHELL,
                instructions = "Assistant de commandes bash et d'exécution d'outils système sécurisés."
            )
        )
        botDao.insertAll(initialBots.map { BotEntity.fromDomain(it) })
    }
}
