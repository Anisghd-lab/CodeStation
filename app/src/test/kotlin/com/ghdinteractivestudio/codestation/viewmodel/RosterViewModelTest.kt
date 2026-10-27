package com.ghdinteractivestudio.codestation.viewmodel

import com.ghdinteractivestudio.codestation.domain.model.Bot
import com.ghdinteractivestudio.codestation.domain.model.BotStatus
import com.ghdinteractivestudio.codestation.domain.model.ProviderType
import com.ghdinteractivestudio.codestation.domain.model.ServerConfig
import com.ghdinteractivestudio.codestation.domain.model.ServerConnectionStatus
import com.ghdinteractivestudio.codestation.domain.repository.BotRepository
import com.ghdinteractivestudio.codestation.domain.repository.ServerRepository
import io.mockk.coEvery
import io.mockk.every
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
