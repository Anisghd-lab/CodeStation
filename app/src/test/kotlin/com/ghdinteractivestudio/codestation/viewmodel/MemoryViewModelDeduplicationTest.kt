package com.ghdinteractivestudio.codestation.viewmodel

import com.ghdinteractivestudio.codestation.domain.model.CollectiveMemoryStatus
import com.ghdinteractivestudio.codestation.domain.model.MemoryExperience
import com.ghdinteractivestudio.codestation.domain.model.MemoryKnowledge
import com.ghdinteractivestudio.codestation.domain.repository.MemoryRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

/**
 * Tâche 03 : Vérification de la non-duplication et de l'idempotence des interactions
 * de mémoire dans [MemoryViewModel].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MemoryViewModelDeduplicationTest {

    private val testDispatcher = StandardTestDispatcher()
    private val memoryRepository: MemoryRepository = mockk(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { memoryRepository.memoryStatus } returns MutableStateFlow(CollectiveMemoryStatus())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadAll eliminates duplicate experiences and knowledge with identical ids`() = runTest(testDispatcher) {
        val dupExp = listOf(
            MemoryExperience(id = "exp-1", title = "Interaction CodeBuddy", agent = "bot-codebuddy", project = "p", context = "c", problem = "prob", rootCause = "", solution = "sol", outcome = "", impact = "", validationStatus = "validated", tags = listOf("codebuddy"), createdAt = ""),
            MemoryExperience(id = "exp-1", title = "Interaction CodeBuddy Duplicate", agent = "bot-codebuddy", project = "p", context = "c", problem = "prob", rootCause = "", solution = "sol", outcome = "", impact = "", validationStatus = "validated", tags = listOf("codebuddy"), createdAt = ""),
            MemoryExperience(id = "exp-2", title = "Experience Unique", agent = "bot-codebuddy", project = "p", context = "c", problem = "prob2", rootCause = "", solution = "sol2", outcome = "", impact = "", validationStatus = "validated", tags = emptyList(), createdAt = "")
        )
        val dupKnw = listOf(
            MemoryKnowledge(id = "knw-1", title = "Knowledge CodeBuddy", category = "solution", content = "text", validationStatus = "validated", version = 1, derivedFromExperiences = emptyList(), appliesToProjects = emptyList(), tags = emptyList(), createdAt = "", updatedAt = ""),
            MemoryKnowledge(id = "knw-1", title = "Knowledge CodeBuddy Duplicate", category = "solution", content = "text", validationStatus = "validated", version = 1, derivedFromExperiences = emptyList(), appliesToProjects = emptyList(), tags = emptyList(), createdAt = "", updatedAt = "")
        )

        coEvery { memoryRepository.refreshStatus() } returns Result.success(CollectiveMemoryStatus())
        coEvery { memoryRepository.getExperiences(any(), any()) } returns Result.success(dupExp)
        coEvery { memoryRepository.getKnowledge(any(), any()) } returns Result.success(dupKnw)

        val viewModel = MemoryViewModel(memoryRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Les doublons d'expériences doivent être éliminés", 2, state.allExperiences.size)
        assertEquals("Les doublons de connaissances doivent être éliminés", 1, state.allKnowledge.size)
        assertEquals("La liste filtrée ne doit pas contenir de doublons", 3, state.items.size)
    }

    @Test
    fun `concurrent loadAll calls do not trigger redundant requests`() = runTest(testDispatcher) {
        coEvery { memoryRepository.refreshStatus() } coAnswers {
            delay(500)
            Result.success(CollectiveMemoryStatus())
        }
        coEvery { memoryRepository.getExperiences(any(), any()) } returns Result.success(emptyList())
        coEvery { memoryRepository.getKnowledge(any(), any()) } returns Result.success(emptyList())

        val viewModel = MemoryViewModel(memoryRepository)
        // loadAll is invoked in init, try calling it again while active
        viewModel.loadAll()
        viewModel.loadAll()
        testDispatcher.scheduler.advanceUntilIdle()

        // Init triggered 1 call, concurrent calls were ignored by the job guard
        coVerify(exactly = 1) { memoryRepository.refreshStatus() }
        assertFalse(viewModel.uiState.value.isLoading)
    }
}
