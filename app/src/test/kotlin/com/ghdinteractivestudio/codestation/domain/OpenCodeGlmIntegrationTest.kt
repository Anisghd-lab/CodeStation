package com.ghdinteractivestudio.codestation.domain

import com.ghdinteractivestudio.codestation.domain.model.Bot
import com.ghdinteractivestudio.codestation.domain.model.BotStatus
import com.ghdinteractivestudio.codestation.domain.model.ProviderType
import com.ghdinteractivestudio.codestation.domain.repository.BotRepository
import com.ghdinteractivestudio.codestation.domain.repository.ChatRepository
import com.ghdinteractivestudio.codestation.viewmodel.ChatViewModel
import io.mockk.coEvery
import io.mockk.coVerify
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OpenCodeGlmIntegrationTest {

    private val testDispatcher = StandardTestDispatcher()
    private val botRepository: BotRepository = mockk(relaxed = true)
    private val chatRepository: ChatRepository = mockk(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { botRepository.getBotByIdStream(any()) } returns flowOf(
            Bot(
                id = "bot-opencode-glm",
                name = "OpenCode GLM",
                provider = ProviderType.OPENCODE,
                model = "opencode/glm-5.3-flash-free"
            )
        )
        every { chatRepository.getMessagesStream(any()) } returns flowOf(emptyList())
        every { chatRepository.getPendingApprovalsStream(any()) } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test OpenCode provider default model is GLM-5_3-Flash Free`() {
        val opencode = ProviderType.OPENCODE
        assertEquals("OpenCode", opencode.displayName)
        assertEquals("opencode", opencode.backendId)
        assertEquals("opencode/glm-5.3-flash-free", opencode.defaultModel)
    }

    @Test
    fun `test OpenCode available models catalog contains GLM-5_3-Flash Free as default`() {
        val models = ProviderType.OPENCODE.getAvailableModels()
        assertTrue(models.isNotEmpty())

        val defaultModel = models.firstOrNull { it.isDefault }
        assertNotNull(defaultModel)
        assertEquals("opencode/glm-5.3-flash-free", defaultModel?.id)
        assertEquals("GLM-5.3-Flash Free", defaultModel?.displayName)
        assertTrue(defaultModel?.isFree == true)

        // Verify alternative free models exist for fallback recommendations
        val freeModels = models.filter { it.isFree }
        assertTrue("At least 3 free models should be cataloged", freeModels.size >= 3)
        assertTrue(freeModels.any { it.id == "opencode/deepseek-v4.1-flash-free" })
        assertTrue(freeModels.any { it.id == "opencode/beam-free" })
    }

    @Test
    fun `test bot creation with OpenCode GLM configuration`() {
        val bot = Bot(
            id = "bot-glm",
            name = "GLM Free Assistant",
            provider = ProviderType.OPENCODE,
            model = "opencode/glm-5.3-flash-free",
            status = BotStatus.IDLE
        )

        assertEquals("opencode/glm-5.3-flash-free", bot.model)
        assertEquals(ProviderType.OPENCODE, bot.provider)
        assertFalse(bot.isGroup)
    }

    @Test
    fun `test ChatViewModel propagates real transmission error without fake fallback`() = runTest(testDispatcher) {
        val expectedError = "Le serveur a refusé le message (HTTP 401 Unauthorized)"
        coEvery { chatRepository.sendMessage("bot-opencode-glm", "Test prompt", null) } returns
            Result.failure(IllegalStateException(expectedError))

        val viewModel = ChatViewModel("bot-opencode-glm", botRepository, chatRepository)
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect() }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onInputChanged("Test prompt")
        viewModel.sendMessage()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { chatRepository.sendMessage("bot-opencode-glm", "Test prompt", null) }
        assertEquals(expectedError, viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isBotWorking)
    }
}
