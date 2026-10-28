package com.ghdinteractivestudio.codestation.domain

import com.ghdinteractivestudio.codestation.domain.model.Bot
import com.ghdinteractivestudio.codestation.domain.model.ModelInfo
import com.ghdinteractivestudio.codestation.domain.model.ProviderType
import com.ghdinteractivestudio.codestation.domain.repository.AntigravityCatalogState
import com.ghdinteractivestudio.codestation.domain.repository.AntigravityModelRepository
import com.ghdinteractivestudio.codestation.domain.repository.BotRepository
import com.ghdinteractivestudio.codestation.viewmodel.BotEditorViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
class AntigravityDynamicModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test Antigravity baseline models match verified CLI models`() {
        val provider = ProviderType.ANTIGRAVITY
        assertEquals("gemini-4-argon-medium", provider.defaultModel)

        val models = provider.getAvailableModels()
        assertTrue("Models list should not be empty", models.isNotEmpty())

        val modelIds = models.map { it.id }
        assertTrue(modelIds.contains("gemini-4-argon-medium"))
        assertTrue(modelIds.contains("gemini-3.8-flash-high"))
        assertTrue(modelIds.contains("claude-opus-5-5-low"))
        assertTrue(modelIds.contains("claude-sonnet-5-5-medium"))
        assertTrue(modelIds.contains("gpt-oss-120b-medium"))

        // Obsolete hardcoded models must not be present in Antigravity
        assertFalse("gemini-2.5-pro should not be in Antigravity", modelIds.contains("gemini-2.5-pro"))
        assertFalse("gemini-2.0-flash should not be in Antigravity", modelIds.contains("gemini-2.0-flash"))

        // Verified flag
        models.forEach { model ->
            assertTrue("Model ${model.id} should be verified", model.isVerified)
        }
    }

    @Test
    fun `test Fake AntigravityModelRepository caching and fallback retention on error`() = runTest {
        val fakeRepo = FakeAntigravityModelRepository(
            initialModels = ProviderType.ANTIGRAVITY.getAvailableModels()
        )

        assertEquals(13, fakeRepo.getModels().size)
        assertTrue(fakeRepo.isModelVerified("gemini-4-argon-medium"))
        assertFalse(fakeRepo.isModelVerified("unverified-custom-model"))

        // Simulate refresh failure
        fakeRepo.shouldFail = true
        val result = fakeRepo.refreshModels(force = true)
        assertTrue("Refresh should return failure on network error", result.isFailure)

        // Previous models must be retained, not wiped
        val retained = fakeRepo.getModels()
        assertEquals(13, retained.size)
        assertNotNull(fakeRepo.catalogState.value.errorMessage)
        assertTrue(fakeRepo.catalogState.value.errorMessage!!.contains("Erreur"))
    }

    @Test
    fun `test BotEditorViewModel provider change selects verified default and preserves selected model`() = runTest {
        val fakeBotRepo = FakeBotRepository()
        val fakeAntigravityRepo = FakeAntigravityModelRepository(
            initialModels = ProviderType.ANTIGRAVITY.getAvailableModels()
        )

        val viewModel = BotEditorViewModel(
            existingBotId = null,
            botRepository = fakeBotRepo,
            antigravityModelRepository = fakeAntigravityRepo
        )

        // Switch to Antigravity
        viewModel.onProviderChanged(ProviderType.ANTIGRAVITY)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("gemini-4-argon-medium", viewModel.uiState.value.model)
        assertTrue(viewModel.isCurrentModelVerified())

        // Select specific verified model
        viewModel.onModelChanged("claude-sonnet-5-5-high")
        assertEquals("claude-sonnet-5-5-high", viewModel.uiState.value.model)
        assertTrue(viewModel.isCurrentModelVerified())

        // Refresh should retain current selection
        viewModel.refreshAntigravityModels(force = true)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("claude-sonnet-5-5-high", viewModel.uiState.value.model)

        // Set unverified model
        viewModel.onModelChanged("custom-fine-tuned-model")
        assertFalse(viewModel.isCurrentModelVerified())
    }

    private class FakeAntigravityModelRepository(
        initialModels: List<ModelInfo>
    ) : AntigravityModelRepository {
        var shouldFail: Boolean = false
        private val _state = MutableStateFlow(
            AntigravityCatalogState(
                models = initialModels,
                isRefreshing = false,
                lastUpdated = System.currentTimeMillis()
            )
        )
        override val catalogState: StateFlow<AntigravityCatalogState> = _state

        override suspend fun getModels(): List<ModelInfo> = _state.value.models

        override suspend fun refreshModels(force: Boolean): Result<List<ModelInfo>> {
            if (shouldFail) {
                _state.value = _state.value.copy(
                    isRefreshing = false,
                    errorMessage = "Erreur de connexion simulée"
                )
                return Result.failure(RuntimeException("Erreur réseau"))
            }
            return Result.success(_state.value.models)
        }

        override fun isModelVerified(modelId: String): Boolean {
            return _state.value.models.any { it.id == modelId }
        }
    }

    private class FakeBotRepository : BotRepository {
        private val bots = mutableMapOf<String, Bot>()
        override fun getBotsStream() = MutableStateFlow(bots.values.toList())
        override suspend fun getBotById(id: String) = bots[id]
        override suspend fun createBot(bot: Bot): Result<Bot> {
            bots[bot.id] = bot
            return Result.success(bot)
        }
        override suspend fun updateBot(bot: Bot): Result<Bot> {
            bots[bot.id] = bot
            return Result.success(bot)
        }
        override suspend fun saveBot(bot: Bot): Result<Bot> {
            bots[bot.id] = bot
            return Result.success(bot)
        }
        override suspend fun deleteBot(id: String): Result<Unit> {
            bots.remove(id)
            return Result.success(Unit)
        }
        override suspend fun updateStatus(id: String, status: com.ghdinteractivestudio.codestation.domain.model.BotStatus): Result<Unit> = Result.success(Unit)
        override suspend fun stopBotTurn(id: String): Result<Unit> = Result.success(Unit)
    }
}
