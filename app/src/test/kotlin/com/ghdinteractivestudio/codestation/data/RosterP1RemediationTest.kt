package com.ghdinteractivestudio.codestation.data

import com.ghdinteractivestudio.codestation.data.local.BotDao
import com.ghdinteractivestudio.codestation.data.local.BotEntity
import com.ghdinteractivestudio.codestation.data.local.BotRepositoryImpl
import com.ghdinteractivestudio.codestation.data.local.ServerRepositoryImpl
import com.ghdinteractivestudio.codestation.data.preferences.PreferencesRepositoryImpl
import com.ghdinteractivestudio.codestation.data.remote.BackendInfo
import com.ghdinteractivestudio.codestation.data.remote.CodeStationApiService
import com.ghdinteractivestudio.codestation.data.remote.HealthResponse
import com.ghdinteractivestudio.codestation.data.remote.HelloResponse
import com.ghdinteractivestudio.codestation.domain.model.Bot
import com.ghdinteractivestudio.codestation.domain.model.BotStatus
import com.ghdinteractivestudio.codestation.domain.model.ProviderType
import com.ghdinteractivestudio.codestation.domain.model.ServerConnectionStatus
import com.ghdinteractivestudio.codestation.domain.repository.ServerRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class RosterP1RemediationTest {

    private val preferencesRepository: PreferencesRepositoryImpl = mockk(relaxed = true)
    private val apiService: CodeStationApiService = mockk(relaxed = true)
    private val botDao: BotDao = mockk(relaxed = true)

    @Before
    fun setUp() {
        every { preferencesRepository.serverUrlStream } returns flowOf("http://127.0.0.1:19222")
        every { preferencesRepository.pairingTokenStream } returns flowOf("test-token-xyz")
    }

    // ==========================================
    // P1-01: Calcul exact du nombre de bots actifs
    // ==========================================

    @Test
    fun `P1-01 testConnection calculates activeBotCount from Room BotDao, not from catalog backends`() = runTest {
        // Le catalogue du backend hôte expose ~46 adaptateurs CLI
        val hostCatalogBackends = (1..46).map { idx ->
            BackendInfo(id = "backend-$idx", name = "Adapter $idx", installed = true, available = true)
        }

        coEvery { apiService.getHealth() } returns Response.success(
            HealthResponse(ok = true, version = "2.28.0", busy = false)
        )
        coEvery { apiService.hello("Bearer test-token-xyz") } returns Response.success(
            HelloResponse(name = "termux-dev", version = "2.28.0", backends = hostCatalogBackends)
        )

        // Room contient exactement 7 bots configurés, tous actifs (IDLE / WORKING)
        coEvery { botDao.getActiveBotCount() } returns 7
        coEvery { botDao.getBotCount() } returns 7

        val repo = ServerRepositoryImpl(
            preferencesRepository = preferencesRepository,
            botDaoProvider = { botDao },
            apiServiceProvider = { apiService }
        )

        val result = repo.testConnection()
        assertTrue(result.isSuccess)

        val connectedConfig = result.getOrThrow()
        assertEquals(ServerConnectionStatus.CONNECTED, connectedConfig.status)
        // Vérification P1-01 : le compte de bots actifs doit être 7 (issu de Room), et JAMAIS 46 (backends du catalog)
        assertEquals(7, connectedConfig.activeBotCount)
        assertFalse("Le compte ne doit JAMAIS être le catalogue global (46)", connectedConfig.activeBotCount == 46)
    }

    @Test
    fun `P1-01 testConnection reflects zero bots when Room database has no configured bots`() = runTest {
        val hostCatalogBackends = (1..46).map { idx ->
            BackendInfo(id = "backend-$idx", name = "Adapter $idx")
        }

        coEvery { apiService.getHealth() } returns Response.success(
            HealthResponse(ok = true, version = "2.28.0", busy = false)
        )
        coEvery { apiService.hello("Bearer test-token-xyz") } returns Response.success(
            HelloResponse(name = "termux-dev", version = "2.28.0", backends = hostCatalogBackends)
        )

        coEvery { botDao.getActiveBotCount() } returns 0
        coEvery { botDao.getBotCount() } returns 0

        val repo = ServerRepositoryImpl(
            preferencesRepository = preferencesRepository,
            botDaoProvider = { botDao },
            apiServiceProvider = { apiService }
        )

        val result = repo.testConnection()
        assertTrue(result.isSuccess)

        val connectedConfig = result.getOrThrow()
        assertEquals(0, connectedConfig.activeBotCount)
    }

    @Test
    fun `P1-01 testConnection falls back to total bot count if all configured bots are temporarily offline`() = runTest {
        coEvery { apiService.getHealth() } returns Response.success(
            HealthResponse(ok = true, version = "2.28.0", busy = false)
        )
        coEvery { apiService.hello("Bearer test-token-xyz") } returns Response.success(
            HelloResponse(name = "termux-dev", version = "2.28.0", backends = emptyList())
        )

        // 7 bots configurés mais temporairement offline avant premier refresh
        coEvery { botDao.getActiveBotCount() } returns 0
        coEvery { botDao.getBotCount() } returns 7

        val repo = ServerRepositoryImpl(
            preferencesRepository = preferencesRepository,
            botDaoProvider = { botDao },
            apiServiceProvider = { apiService }
        )

        val result = repo.testConnection()
        assertTrue(result.isSuccess)
        val connectedConfig = result.getOrThrow()
        assertEquals(7, connectedConfig.activeBotCount)
    }

    // ==========================================
    // P1-02: Amorçage initial et Roster hors ligne
    // ==========================================

    @Test
    fun `P1-02 getBotsStream seeds initial 6 bots when Room database is completely empty`() = runTest {
        val insertedBots = mutableListOf<BotEntity>()
        val botFlow = MutableStateFlow<List<BotEntity>>(emptyList())

        // Premier appel : DB vide (count = 0)
        coEvery { botDao.getBotCount() } answers { insertedBots.size }
        coEvery { botDao.getAllBotsStream() } returns botFlow
        coEvery { botDao.insertAll(any()) } answers {
            val list = firstArg<List<BotEntity>>()
            insertedBots.addAll(list)
            botFlow.value = insertedBots.toList()
        }

        val botRepository = BotRepositoryImpl(botDao = botDao)

        // Collecter le premier flux émis
        val bots = botRepository.getBotsStream().first()

        // Vérification P1-02 : le seed automatique doit injecter exactement 6 bots
        assertEquals(6, bots.size)
        assertEquals(6, insertedBots.size)

        // Vérifier la présence des 6 CLI réelles configurées
        val botIds = bots.map { it.id }.toSet()
        assertTrue(botIds.contains("bot-antigravity"))
        assertTrue(botIds.contains("bot-opencode-glm"))
        assertTrue(botIds.contains("bot-codebuddy"))
        assertTrue(botIds.contains("bot-omnirush"))
        assertTrue(botIds.contains("bot-clixad"))
        assertTrue(botIds.contains("bot-freebuff"))
    }

    @Test
    fun `P1-02 getBotsStream works fully offline without server connection or network`() = runTest {
        val existingEntities = listOf(
            BotEntity.fromDomain(Bot(id = "bot-antigravity", name = "Google Antigravity", isLead = true)),
            BotEntity.fromDomain(Bot(id = "bot-opencode-glm", name = "OpenCode GLM", isLead = false)),
            BotEntity.fromDomain(Bot(id = "bot-codebuddy", name = "CodeBuddy Code", isLead = false))
        )

        coEvery { botDao.getBotCount() } returns existingEntities.size
        coEvery { botDao.getAllBotsStream() } returns flowOf(existingEntities)

        // Aucun serveur connecté (null)
        val botRepository = BotRepositoryImpl(
            botDao = botDao,
            serverRepository = null
        )

        val bots = botRepository.getBotsStream().first()
        assertEquals(3, bots.size)
        assertEquals("Google Antigravity", bots[0].name)
        // Aucune tentative d'insertion destructive
        coVerify(exactly = 0) { botDao.insertAll(any()) }
    }

    @Test
    fun `P1-02 refreshBots returns non-empty list and preserves custom bot modifications`() = runTest {
        val existingCustomBot = BotEntity(
            id = "bot-antigravity",
            name = "Google Antigravity (Customized)",
            description = "Customized by user",
            avatarGlyph = "diamond",
            avatarColorHex = "#6366F1",
            provider = "ANTIGRAVITY",
            model = "gemini-4-argon-high",
            isFastMode = true,
            workspaceMode = "PERSONAL",
            workspacePath = "/root/custom",
            instructions = "Custom expert instructions",
            permissionPolicy = "AUTONOMOUS",
            isLead = true,
            isPinned = true,
            status = "IDLE",
            lastMessage = "Custom last message",
            lastActivityAt = 123456L,
            unreadCount = 0,
            pendingApprovalsCount = 0
        )

        val entities = mutableListOf(existingCustomBot)
        val entitiesFlow = MutableStateFlow<List<BotEntity>>(entities)

        coEvery { botDao.getBotCount() } answers { entities.size }
        coEvery { botDao.getBotById("bot-antigravity") } returns existingCustomBot
        coEvery { botDao.getBotById(neq("bot-antigravity")) } returns null
        coEvery { botDao.getAllBotsStream() } returns entitiesFlow
        coEvery { botDao.insertBot(any()) } answers {
            val entity = firstArg<BotEntity>()
            entities.add(entity)
            entitiesFlow.value = entities.toList()
        }

        val botRepository = BotRepositoryImpl(botDao = botDao)

        val result = botRepository.refreshBots()
        assertTrue(result.isSuccess)

        val bots = result.getOrThrow()
        // Vérification P1-02 : refreshBots ne retourne plus emptyList()
        assertFalse("refreshBots ne doit JAMAIS retourner une liste vide", bots.isEmpty())
        assertEquals(6, bots.size)

        // Le bot préexistant modifié n'a pas été écrasé
        val antigravity = bots.first { it.id == "bot-antigravity" }
        assertEquals("Google Antigravity (Customized)", antigravity.name)
        assertEquals("Custom last message", antigravity.lastMessage)
    }

    // ==========================================
    // Détection & Filtrage Roster des 6 Réelles CLI
    // ==========================================

    @Test
    fun `Roster filters out obsolete bots and exposes only installed CLIs`() = runTest {
        val mixedEntities = listOf(
            BotEntity.fromDomain(Bot(id = "bot-antigravity", name = "Google Antigravity", isLead = true)),
            BotEntity.fromDomain(Bot(id = "bot-opencode-glm", name = "OpenCode GLM", isLead = false)),
            BotEntity.fromDomain(Bot(id = "bot-codebuddy", name = "CodeBuddy Code", isLead = false)),
            BotEntity.fromDomain(Bot(id = "bot-omnirush", name = "OmniRush", isLead = false)),
            BotEntity.fromDomain(Bot(id = "bot-clixad", name = "Clixad", isLead = false)),
            BotEntity.fromDomain(Bot(id = "bot-freebuff", name = "Freebuff", isLead = false)),
            // Obsolete bots that might reside in Room DB history
            BotEntity.fromDomain(Bot(id = "bot-codex-terminal", name = "OpenAI Codex", isLead = false)),
            BotEntity.fromDomain(Bot(id = "bot-claude-architect", name = "Claude Architect", isLead = false)),
            BotEntity.fromDomain(Bot(id = "bot-lead-codestation", name = "CodeStation Core", isLead = false)),
            BotEntity.fromDomain(Bot(id = "8731af25-da7f-4955-9b37-247f45c60912", name = "Codync", isLead = false))
        )

        coEvery { botDao.getBotCount() } returns mixedEntities.size
        coEvery { botDao.getAllBotsStream() } returns flowOf(mixedEntities)

        val botRepository = BotRepositoryImpl(botDao = botDao)
        val activeRoster = botRepository.getBotsStream().first()

        // Exactement 6 bots actifs (les 6 CLI réellement installées)
        assertEquals(6, activeRoster.size)
        val activeIds = activeRoster.map { it.id }.toSet()
        assertEquals(
            setOf("bot-antigravity", "bot-opencode-glm", "bot-codebuddy", "bot-omnirush", "bot-clixad", "bot-freebuff"),
            activeIds
        )

        // Les anciens bots obsolètes ne sont PAS affichés dans le Roster
        assertFalse(activeIds.contains("bot-codex-terminal"))
        assertFalse(activeIds.contains("bot-claude-architect"))
        assertFalse(activeIds.contains("bot-lead-codestation"))
        assertFalse(activeIds.contains("8731af25-da7f-4955-9b37-247f45c60912"))
    }
}
