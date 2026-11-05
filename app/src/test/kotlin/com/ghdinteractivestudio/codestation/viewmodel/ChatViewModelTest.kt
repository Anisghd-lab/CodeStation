package com.ghdinteractivestudio.codestation.viewmodel

import com.ghdinteractivestudio.codestation.domain.model.ApprovalActionType
import com.ghdinteractivestudio.codestation.domain.model.ApprovalRequest
import com.ghdinteractivestudio.codestation.domain.model.ApprovalStatus
import com.ghdinteractivestudio.codestation.domain.model.Bot
import com.ghdinteractivestudio.codestation.domain.model.ChatMessage
import com.ghdinteractivestudio.codestation.domain.model.Conversation
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
        every { chatRepository.getConversationsStream(any()) } returns flowOf(emptyList())
        every { chatRepository.getMessagesByConversationStream(any(), any()) } returns flowOf(testMessages)
        coEvery { chatRepository.createConversation(any(), any()) } returns Result.success(Conversation(id = "conv-1", botId = "bot-1", title = "Discussion"))
        coEvery { chatRepository.createConversation(any(), any(), any(), any()) } returns Result.success(Conversation(id = "conv-1", botId = "bot-1", title = "Discussion"))
        coEvery { chatRepository.createConversation(any(), any(), any(), any(), any()) } returns Result.success(Conversation(id = "conv-1", botId = "bot-1", title = "Discussion"))
        coEvery { chatRepository.getConversationById(any()) } returns null
        coEvery { chatRepository.renameConversation(any(), any()) } returns Result.success(Unit)
        coEvery { chatRepository.sendMessage(any(), any(), any(), any(), any()) } returns Result.success(testMessages.first())
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

        coVerify { chatRepository.sendMessage("bot-1", "Nouvelle tâche", any(), any(), any()) }
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

        coVerify { chatRepository.sendMessage("bot-1", "Bonjour modifié", any(), any(), any()) }
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
        every { chatRepository.getMessagesByConversationStream(any(), any()) } returns flowOf(listOf(testMessages.first(), botReply))

        val viewModel = ChatViewModel("bot-1", botRepository, chatRepository)
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect() }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.regenerateResponse("bot-msg-1")
        testDispatcher.scheduler.advanceUntilIdle()

        // Verifies the user prompt was retrieved and re-sent
        coVerify { chatRepository.sendMessage("bot-1", "Bonjour", any(), any(), any()) }
        // Verify deleteMessage was NOT called (preserving history)
        coVerify(exactly = 0) { chatRepository.deleteMessage("bot-msg-1") }
    }

    @Test
    fun `test generateConversationTitle cleans commands, markdown and limits length`() {
        val title1 = ChatViewModel.generateConversationTitle("/task Peux-tu analyser ce code ?")
        assertEquals("Peux-tu analyser ce code ?", title1)

        val title2 = ChatViewModel.generateConversationTitle("@codebuddy # Fix **critical** issue")
        assertEquals("Fix critical issue", title2)

        val longText = "Ceci est une très longue question portant sur l'architecture globale des microservices et la synchronisation des données distantes"
        val title3 = ChatViewModel.generateConversationTitle(longText)
        org.junit.Assert.assertTrue("Title should be truncated with ellipsis", title3.endsWith("…"))
        org.junit.Assert.assertTrue("Title length should be bounded", title3.length <= 45)

        val fileTitle = ChatViewModel.generateConversationTitle("", listOf("capture_ecran.png"))
        assertEquals("Fichier : capture_ecran.png", fileTitle)
    }

    @Test
    fun `test isGenericConversationTitle detects placeholders accurately`() {
        org.junit.Assert.assertTrue(ChatViewModel.isGenericConversationTitle(null))
        org.junit.Assert.assertTrue(ChatViewModel.isGenericConversationTitle(""))
        org.junit.Assert.assertTrue(ChatViewModel.isGenericConversationTitle("Discussion"))
        org.junit.Assert.assertTrue(ChatViewModel.isGenericConversationTitle("Nouvelle discussion"))
        org.junit.Assert.assertTrue(ChatViewModel.isGenericConversationTitle("Discussion sans titre"))
        org.junit.Assert.assertTrue(ChatViewModel.isGenericConversationTitle("new chat"))

        org.junit.Assert.assertFalse(ChatViewModel.isGenericConversationTitle("Refactoring OAuth2"))
        org.junit.Assert.assertFalse(ChatViewModel.isGenericConversationTitle("Architecture Système"))
    }

    @Test
    fun `test sendMessage generates meaningful conversation title on first message`() = runTest(testDispatcher) {
        val viewModel = ChatViewModel("bot-1", botRepository, chatRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onInputChanged("Refactoriser l'authentification OAuth2")
        viewModel.sendMessage()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            chatRepository.createConversation(
                botId = "bot-1",
                title = "Refactoriser l'authentification OAuth2"
            )
        }
    }

    @Test
    fun `test sendMessage renames existing generic conversation on first message`() = runTest(testDispatcher) {
        val genericConv = Conversation(id = "conv-gen-1", botId = "bot-1", title = "Nouvelle discussion", messageCount = 0)
        coEvery { chatRepository.getConversationById("conv-gen-1") } returns genericConv

        val viewModel = ChatViewModel("bot-1", botRepository, chatRepository, initialConversationId = "conv-gen-1")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onInputChanged("Optimiser la base de données")
        viewModel.sendMessage()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { chatRepository.renameConversation("conv-gen-1", "Optimiser la base de données") }
    }

    @Test
    fun `test sendMessage preserves custom non-generic conversation title`() = runTest(testDispatcher) {
        val customConv = Conversation(id = "conv-custom-1", botId = "bot-1", title = "Architecture Microservices", messageCount = 2)
        coEvery { chatRepository.getConversationById("conv-custom-1") } returns customConv

        val viewModel = ChatViewModel("bot-1", botRepository, chatRepository, initialConversationId = "conv-custom-1")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onInputChanged("Deuxième message dans la session")
        viewModel.sendMessage()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { chatRepository.renameConversation("conv-custom-1", any()) }
    }
}
