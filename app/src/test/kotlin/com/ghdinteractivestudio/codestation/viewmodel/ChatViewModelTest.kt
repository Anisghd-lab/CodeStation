package com.ghdinteractivestudio.codestation.viewmodel

import com.ghdinteractivestudio.codestation.domain.model.ApprovalActionType
import com.ghdinteractivestudio.codestation.domain.model.ApprovalRequest
import com.ghdinteractivestudio.codestation.domain.model.ApprovalStatus
import com.ghdinteractivestudio.codestation.domain.model.Bot
import com.ghdinteractivestudio.codestation.domain.model.ChatMessage
import com.ghdinteractivestudio.codestation.domain.model.MessageAuthor
import com.ghdinteractivestudio.codestation.domain.model.MessageAuthorType
import com.ghdinteractivestudio.codestation.domain.repository.BotRepository
import com.ghdinteractivestudio.codestation.domain.repository.ChatRepository
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
        coEvery { botRepository.getBotById("bot-1") } returns testBot
        coEvery { botRepository.updateBotStatus(any(), any()) } returns Result.success(Unit)
        every { chatRepository.getMessagesStream("bot-1") } returns flowOf(testMessages)
        every { chatRepository.getPendingApprovalsStream("bot-1") } returns flowOf(emptyList())
        coEvery { chatRepository.sendMessage(any(), any(), any(), any()) } returns Result.success(testMessages.first())
        coEvery { chatRepository.respondApproval(any(), any(), any()) } returns Result.success(Unit)
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

        coVerify { chatRepository.sendMessage("bot-1", "Nouvelle tâche", null, any()) }
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

    @Test
    fun `test delete message invokes repository`() = runTest(testDispatcher) {
        val viewModel = ChatViewModel("bot-1", botRepository, chatRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.deleteMessage("msg-1")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { chatRepository.deleteMessage("msg-1") }
    }

    @Test
    fun `test startEditingMessage updates state with message text and reference`() = runTest(testDispatcher) {
        val viewModel = ChatViewModel("bot-1", botRepository, chatRepository)
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect() }
        testDispatcher.scheduler.advanceUntilIdle()

        val msg = testMessages.first()
        viewModel.startEditingMessage(msg)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Bonjour", state.inputDraft)
        assertEquals(msg.id, state.editingMessage?.id)
    }

    @Test
    fun `test cancelEditingMessage resets editing state and input draft`() = runTest(testDispatcher) {
        val viewModel = ChatViewModel("bot-1", botRepository, chatRepository)
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect() }
        testDispatcher.scheduler.advanceUntilIdle()

        val msg = testMessages.first()
        viewModel.startEditingMessage(msg)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.cancelEditingMessage()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("", state.inputDraft)
        assertEquals(null, state.editingMessage)
    }

    @Test
    fun `test confirmEditAndSend sends updated text and clears editing message`() = runTest(testDispatcher) {
        val viewModel = ChatViewModel("bot-1", botRepository, chatRepository)
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect() }
        testDispatcher.scheduler.advanceUntilIdle()

        val msg = testMessages.first()
        viewModel.startEditingMessage(msg)
        viewModel.confirmEditAndSend("Bonjour modifié")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { chatRepository.sendMessage("bot-1", "Bonjour modifié", null, any()) }
        assertEquals(null, viewModel.uiState.value.editingMessage)
        assertEquals("", viewModel.uiState.value.inputDraft)
    }

    @Test
    fun `test regenerateResponse finds preceding user prompt and triggers generation without deleting old message`() = runTest(testDispatcher) {
        val botReply = ChatMessage(
            id = "bot-msg-1",
            botId = "bot-1",
            author = MessageAuthor(MessageAuthorType.BOT, "bot-1", "Test Bot"),
            text = "Voici la réponse originale",
            isFinal = true
        )
        every { chatRepository.getMessagesStream("bot-1") } returns flowOf(listOf(testMessages.first(), botReply))

        val viewModel = ChatViewModel("bot-1", botRepository, chatRepository)
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect() }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.regenerateResponse("bot-msg-1")
        testDispatcher.scheduler.advanceUntilIdle()

        // Verifies the user prompt was retrieved and re-sent
        coVerify { chatRepository.sendMessage("bot-1", "Bonjour", null, any()) }
        // Verify deleteMessage was NOT called (preserving history)
        coVerify(exactly = 0) { chatRepository.deleteMessage("bot-msg-1") }
    }
}
