package com.ghdinteractivestudio.synapse.viewmodel

import com.ghdinteractivestudio.synapse.domain.model.ApprovalActionType
import com.ghdinteractivestudio.synapse.domain.model.ApprovalRequest
import com.ghdinteractivestudio.synapse.domain.model.ApprovalStatus
import com.ghdinteractivestudio.synapse.domain.model.Bot
import com.ghdinteractivestudio.synapse.domain.model.ChatMessage
import com.ghdinteractivestudio.synapse.domain.model.MessageAuthor
import com.ghdinteractivestudio.synapse.domain.model.MessageAuthorType
import com.ghdinteractivestudio.synapse.domain.repository.BotRepository
import com.ghdinteractivestudio.synapse.domain.repository.ChatRepository
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
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val botRepository: BotRepository = mockk(relaxed = true)
    private val chatRepository: ChatRepository = mockk(relaxed = true)

    private val testBot = Bot(id = "bot-1", name = "Test Bot")
    private val testMessages = listOf(
        ChatMessage(
            id = "msg-1",
            botId = "bot-1",
            author = MessageAuthor(MessageAuthorType.USER, "user", "Moi"),
            text = "Bonjour"
        )
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { botRepository.getBotByIdStream("bot-1") } returns flowOf(testBot)
        every { chatRepository.getMessagesStream("bot-1") } returns flowOf(testMessages)
        every { chatRepository.getPendingApprovalsStream("bot-1") } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test initial state loads bot and messages`() = runTest(testDispatcher) {
        val viewModel = ChatViewModel("bot-1", botRepository, chatRepository)
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect() }
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.bot)
        assertEquals("Test Bot", state.bot?.name)
        assertEquals(1, state.messages.size)
        assertEquals("Bonjour", state.messages.first().text)
    }

    @Test
    fun `test sending message invokes repository`() = runTest(testDispatcher) {
        val viewModel = ChatViewModel("bot-1", botRepository, chatRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onInputChanged("Nouvelle tâche")
        viewModel.sendMessage()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { chatRepository.sendMessage("bot-1", "Nouvelle tâche", null) }
        assertEquals("", viewModel.uiState.value.inputDraft)
    }

    @Test
    fun `test responding approval invokes repository with decision`() = runTest(testDispatcher) {
        val viewModel = ChatViewModel("bot-1", botRepository, chatRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.respondApproval("app-123", ApprovalStatus.APPROVED, true)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { chatRepository.respondApproval("app-123", ApprovalStatus.APPROVED, true) }
    }
}
