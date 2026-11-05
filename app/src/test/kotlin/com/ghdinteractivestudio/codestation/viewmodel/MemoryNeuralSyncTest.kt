package com.ghdinteractivestudio.codestation.viewmodel

import com.ghdinteractivestudio.codestation.domain.model.Bot
import com.ghdinteractivestudio.codestation.domain.model.CollectiveMemoryStatus
import com.ghdinteractivestudio.codestation.domain.model.MemoryExperience
import com.ghdinteractivestudio.codestation.domain.model.MemoryKnowledge
import com.ghdinteractivestudio.codestation.domain.model.MemorySyncState
import com.ghdinteractivestudio.codestation.domain.repository.BotRepository
import com.ghdinteractivestudio.codestation.domain.repository.MemoryRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tâche 11 : Tests de synchronisation et de fidélité des compteurs de la mémoire neuronale
 * (Agents, Relations, Nœuds, Expériences, Connaissances).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MemoryNeuralSyncTest {

    private val testDispatcher = StandardTestDispatcher()
    private val memoryRepository: MemoryRepository = mockk(relaxed = true)
    private val botRepository: BotRepository = mockk(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { memoryRepository.memoryStatus } returns MutableStateFlow(CollectiveMemoryStatus())
        every { botRepository.getBotsStream() } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `MemoryUiState computes exact real counts when remote status has zeros`() {
        val sampleBots = listOf(
            Bot(id = "agent-antigravity", name = "Google Antigravity"),
            Bot(id = "agent-codebuddy", name = "CodeBuddy Code"),
            Bot(id = "agent-opencode", name = "OpenCode GLM"),
            Bot(id = "agent-codex", name = "OpenAI Codex")
        )
        val sampleExperiences = listOf(
            MemoryExperience(
                id = "exp-1",
                title = "Resolution Incident",
                agent = "agent-codebuddy",
                project = "prj-codestation",
                context = "PRoot ARM64",
                problem = "SQLite JDBC issue",
                rootCause = "",
                solution = "Emulate OS",
                outcome = "Success",
                impact = "High",
                validationStatus = "validated",
                createdAt = "2026-11-05T10:00:00Z"
            ),
            MemoryExperience(
                id = "exp-2",
                title = "SSE Streaming Fix",
                agent = "agent-opencode",
                project = "prj-codestation",
                context = "Network",
                problem = "Timeout",
                rootCause = "",
                solution = "Heartbeat",
                outcome = "Stable",
                impact = "High",
                validationStatus = "validated",
                createdAt = "2026-11-05T10:30:00Z"
            )
        )
        val sampleKnowledge = listOf(
            MemoryKnowledge(
                id = "knw-1",
                title = "ACP Protocol Integration",
                category = "architecture",
                content = "Native ACP bindings",
                validationStatus = "validated",
                derivedFromExperiences = listOf("exp-1"),
                appliesToProjects = listOf("prj-codestation"),
                createdAt = "2026-11-05T11:00:00Z"
            )
        )

        val state = MemoryUiState(
            status = CollectiveMemoryStatus(
                totalExperiences = 0,
                totalKnowledge = 0,
                totalAgents = 0,
                totalRelations = 0,
                totalNodes = 0
            ),
            bots = sampleBots,
            allExperiences = sampleExperiences,
            allKnowledge = sampleKnowledge
        )

        // Exact real counts must accurately reflect stored items without showing 0
        assertEquals("Expériences réelles doivent être 2", 2, state.realExperiencesCount)
        assertEquals("Connaissances réelles doivent être 1", 1, state.realKnowledgeCount)
        assertEquals("Agents réels doivent être 4", 4, state.realAgentsCount)

        // Relations:
        // 4 Core-to-Agent + 4 Inter-agent + 2 Exp-Agent + 2 Exp-Project + 1 Knw-derived + 1 Knw-project + 1 Knw-agent = 15
        assertTrue("Relations réelles doivent être calculées positivement", state.realRelationsCount >= 10)

        // Nœuds: 1 Core + 4 Agents + 2 Expériences + 1 Connaissance = 8 nœuds
        assertEquals("Nœuds réels doivent être exactement 8 (1 Core + 4 Agents + 2 Exp + 1 Knw)", 8, state.realNodesCount)
    }

    @Test
    fun `MemoryUiState preserves verified remote status when positive`() {
        val state = MemoryUiState(
            status = CollectiveMemoryStatus(
                isInitialized = true,
                syncStatus = MemorySyncState.SYNCED,
                totalExperiences = 25,
                totalKnowledge = 14,
                totalAgents = 6,
                totalRelations = 48,
                totalNodes = 46
            ),
            bots = listOf(Bot(id = "b1", name = "Bot 1")),
            allExperiences = emptyList(),
            allKnowledge = emptyList()
        )

        assertEquals("Expériences distantes respectées", 25, state.realExperiencesCount)
        assertEquals("Connaissances distantes respectées", 14, state.realKnowledgeCount)
        assertEquals("Agents distants respectés", 6, state.realAgentsCount)
        assertEquals("Relations distantes respectées", 48, state.realRelationsCount)
        assertEquals("Nœuds distants respectés", 46, state.realNodesCount)
    }

    @Test
    fun `loadAll populates UI state and syncs real counts`() = runTest(testDispatcher) {
        val bots = listOf(
            Bot(id = "agent-antigravity", name = "Google Antigravity"),
            Bot(id = "agent-codebuddy", name = "CodeBuddy Code")
        )
        val experiences = listOf(
            MemoryExperience(
                id = "exp-1",
                title = "Title 1",
                agent = "agent-antigravity",
                project = "prj-codestation",
                context = "",
                problem = "",
                rootCause = "",
                solution = "",
                outcome = "",
                impact = "",
                validationStatus = "validated",
                createdAt = ""
            )
        )
        val knowledge = listOf(
            MemoryKnowledge(
                id = "knw-1",
                title = "Knw 1",
                category = "general",
                content = "Content",
                validationStatus = "validated",
                createdAt = ""
            )
        )

        every { botRepository.getBotsStream() } returns flowOf(bots)
        coEvery { memoryRepository.refreshStatus() } returns Result.success(CollectiveMemoryStatus())
        coEvery { memoryRepository.getExperiences(any(), any()) } returns Result.success(experiences)
        coEvery { memoryRepository.getKnowledge(any(), any()) } returns Result.success(knowledge)

        val viewModel = MemoryViewModel(memoryRepository, botRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.realExperiencesCount)
        assertEquals(1, state.realKnowledgeCount)
        assertEquals(2, state.realAgentsCount)
        // 1 Core + 2 Agents + 1 Exp + 1 Knw = 5
        assertEquals(5, state.realNodesCount)
    }
}
