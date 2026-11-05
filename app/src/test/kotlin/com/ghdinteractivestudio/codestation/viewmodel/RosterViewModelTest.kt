package com.ghdinteractivestudio.codestation.viewmodel

import com.ghdinteractivestudio.codestation.domain.model.Bot
import com.ghdinteractivestudio.codestation.domain.model.BotStatus
import com.ghdinteractivestudio.codestation.domain.model.CollectiveMemoryStatus
import com.ghdinteractivestudio.codestation.domain.model.ProviderType
import com.ghdinteractivestudio.codestation.domain.model.ServerConfig
import com.ghdinteractivestudio.codestation.domain.model.ServerConnectionStatus
import com.ghdinteractivestudio.codestation.domain.repository.BotRepository
import com.ghdinteractivestudio.codestation.domain.repository.CamcsRepository
import com.ghdinteractivestudio.codestation.domain.repository.MemoryRepository
import com.ghdinteractivestudio.codestation.domain.repository.ServerRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import kotlinx.coroutines.delay
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RosterViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val botRepository: BotRepository = mockk(relaxed = true)
    private val serverRepository: ServerRepository = mockk(relaxed = true)

    private val sampleBots = listOf(
        Bot(id = "1", name = "Z-Bot", isLead = false, isPinned = false, lastActivityAt = 100L),
        Bot(id = "2", name = "Lead Bot", isLead = true, isPinned = true, lastActivityAt = 200L),
        Bot(id = "3", name = "A-Pinned", isLead = false, isPinned = true, lastActivityAt = 300L)
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { botRepository.getBotsStream() } returns flowOf(sampleBots)
        every { serverRepository.getServerConfigStream() } returns flowOf(
            ServerConfig(url = "http://127.0.0.1:19222", status = ServerConnectionStatus.CONNECTED)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test roster sorts lead bot on top followed by pinned bots`() = runTest(testDispatcher) {
        val viewModel = RosterViewModel(botRepository, serverRepository)
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect() }
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull("Lead bot doit être présent", state.leadBot)
        assertEquals("Lead Bot", state.leadBot?.name)
        assertTrue(state.leadBot?.isLead == true)

        assertEquals(1, state.pinnedBots.size)
        assertEquals("A-Pinned", state.pinnedBots.first().name)

        assertEquals(1, state.otherBots.size)
        assertEquals("Z-Bot", state.otherBots.first().name)
    }

    @Test
    fun `pull to refresh actualise agents memoire et missions`() = runTest(testDispatcher) {
        val memoryRepository: MemoryRepository = mockk(relaxed = true)
        every { memoryRepository.memoryStatus } returns kotlinx.coroutines.flow.MutableStateFlow(CollectiveMemoryStatus())
        val camcsRepository: CamcsRepository = mockk(relaxed = true)
        coEvery { botRepository.refreshBots() } returns Result.success(sampleBots)
        coEvery { serverRepository.testConnection() } returns Result.success(ServerConfig())
        coEvery { memoryRepository.refreshStatus() } returns Result.success(CollectiveMemoryStatus())
        coEvery { camcsRepository.refreshMissions() } returns Result.success(Unit)

        val viewModel = RosterViewModel(botRepository, serverRepository, memoryRepository, camcsRepository)
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect() }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.refresh()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { botRepository.refreshBots() }
        coVerify(exactly = 1) { serverRepository.testConnection() }
        coVerify(exactly = 1) { camcsRepository.refreshMissions() }
        coVerify(atLeast = 1) { memoryRepository.refreshStatus() }
        val finalState = viewModel.uiState.value
        assertFalse("L'indicateur de rafraîchissement doit retomber", finalState.isRefreshing)
        org.junit.Assert.assertNull("Aucune erreur après rafraîchissement réussi", finalState.refreshError)
        assertEquals("Tous les bots sont toujours affichés", 3, finalState.totalCount)
    }

    @Test
    fun `pull to refresh repete ne lance pas de requetes dupliquees`() = runTest(testDispatcher) {
        val camcsRepository: CamcsRepository = mockk(relaxed = true)
        coEvery { botRepository.refreshBots() } coAnswers {
            delay(500)
            Result.success(sampleBots)
        }
        coEvery { camcsRepository.refreshMissions() } returns Result.success(Unit)

        val viewModel = RosterViewModel(botRepository, serverRepository, camcsRepository = camcsRepository)
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect() }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.refresh()
        viewModel.refresh()
        viewModel.refresh()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { botRepository.refreshBots() }
        assertFalse(viewModel.uiState.value.isRefreshing)
    }

    @Test
    fun `un echec du backend camcs ne fait pas planter le refresh et remonte l'erreur sans effacer les donnees`() = runTest(testDispatcher) {
        val camcsRepository: CamcsRepository = mockk(relaxed = true)
        coEvery { botRepository.refreshBots() } returns Result.success(sampleBots)
        coEvery { serverRepository.testConnection() } returns Result.success(ServerConfig())
        coEvery { camcsRepository.refreshMissions() } returns Result.failure(java.net.ConnectException("Connection refused"))

        val viewModel = RosterViewModel(botRepository, serverRepository, camcsRepository = camcsRepository)
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect() }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.refresh()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { camcsRepository.refreshMissions() }
        val state = viewModel.uiState.value
        assertFalse("L'indicateur doit retomber malgré l'erreur réseau", state.isRefreshing)
        assertNotNull("L'erreur réelle doit être exposée dans l'UI", state.refreshError)
        assertTrue("L'erreur doit mentionner CAMCS", state.refreshError!!.contains("CAMCS"))
        assertEquals("Les bots existants ne sont pas perdus", 3, state.totalCount)

        viewModel.dismissRefreshError()
        testDispatcher.scheduler.advanceUntilIdle()
        org.junit.Assert.assertNull("L'erreur est effacée après dismiss", viewModel.uiState.value.refreshError)
    }

    @Test
    fun `test roster search query filters bot list by name`() = runTest(testDispatcher) {
        val viewModel = RosterViewModel(botRepository, serverRepository)
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect() }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onSearchQueryChanged("Pinned")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(null, state.leadBot)
        assertEquals(1, state.pinnedBots.size)
        assertEquals(0, state.otherBots.size)
    }
}
