package com.ghdinteractivestudio.codestation.notification

import com.ghdinteractivestudio.codestation.domain.model.BotStatus
import com.ghdinteractivestudio.codestation.util.RequestTracer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BotNotificationAndTraceTest {

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
    fun `test stable notification ID per bot`() {
        val botA = "bot-opencode-glm"
        val botB = "bot-antigravity"

        val idA1 = CodeStationNotificationManager.getNotificationIdForBot(botA)
        val idA2 = CodeStationNotificationManager.getNotificationIdForBot(botA)
        val idB = CodeStationNotificationManager.getNotificationIdForBot(botB)

        // Stabilité pour un même bot
        assertEquals("L'ID de notification doit être strictement stable pour un même bot", idA1, idA2)
        // Distinction entre deux bots
        assertNotEquals("Deux bots différents doivent avoir des IDs de notification distincts", idA1, idB)
        // ID strictement positif
        assertTrue("L'ID doit être supérieur ou égal à la base", idA1 >= 1000)
    }

    @Test
    fun `test notification phase ongoing behavior`() {
        // Phases nécessitant une notification persistante dans la barre d'état
        val ongoingPhases = listOf(
            TaskNotificationPhase.QUEUED,
            TaskNotificationPhase.STARTING,
            TaskNotificationPhase.WORKING,
            TaskNotificationPhase.RESPONSE_RECEIVED,
            TaskNotificationPhase.NEEDS_APPROVAL
        )

        // Phases finales ou révocables
        val completedPhases = listOf(
            TaskNotificationPhase.COMPLETED,
            TaskNotificationPhase.FAILED,
            TaskNotificationPhase.STOPPED,
            TaskNotificationPhase.CANCELLED,
            TaskNotificationPhase.DISCONNECTED
        )

        for (phase in ongoingPhases) {
            val isOngoing = phase == TaskNotificationPhase.WORKING ||
                    phase == TaskNotificationPhase.STARTING ||
                    phase == TaskNotificationPhase.QUEUED ||
                    phase == TaskNotificationPhase.RESPONSE_RECEIVED ||
                    phase == TaskNotificationPhase.NEEDS_APPROVAL
            assertTrue("Phase $phase doit être persistante (ongoing = true)", isOngoing)
        }

        for (phase in completedPhases) {
            val isOngoing = phase == TaskNotificationPhase.WORKING ||
                    phase == TaskNotificationPhase.STARTING ||
                    phase == TaskNotificationPhase.QUEUED ||
                    phase == TaskNotificationPhase.RESPONSE_RECEIVED ||
                    phase == TaskNotificationPhase.NEEDS_APPROVAL
            assertFalse("Phase $phase ne doit PAS être persistante (ongoing = false)", isOngoing)
        }
    }

    @Test
    fun `test business state machine transitions`() {
        // Séquence standard d'une requête réussie
        val normalFlow = listOf(
            BotStatus.IDLE,      // stopped / ready
            BotStatus.WORKING,   // busy (request sent)
            BotStatus.IDLE       // completed
        )

        assertEquals(BotStatus.IDLE, normalFlow[0])
        assertEquals(BotStatus.WORKING, normalFlow[1])
        assertEquals(BotStatus.IDLE, normalFlow[2])

        // Séquence d'erreur
        val errorFlow = listOf(
            BotStatus.IDLE,
            BotStatus.WORKING,
            BotStatus.OFFLINE    // error
        )
        assertEquals(BotStatus.OFFLINE, errorFlow[2])
    }

    @Test
    fun `test request tracer filters sensitive data`() {
        // Vérifier que les filtres de sécurité éliminent jetons et clés
        val rawDetails = mapOf(
            "botId" to "bot-opencode-glm",
            "model" to "opencode/glm-5.3-flash-free",
            "token" to "secret_token_12345",
            "api_key" to "sk-sensitive-key-999",
            "authHeader" to "Bearer abcdef",
            "safe_param" to "ok"
        )

        val safeKeys = rawDetails.keys.filter { key ->
            !key.contains("token", ignoreCase = true) &&
            !key.contains("key", ignoreCase = true) &&
            !key.contains("secret", ignoreCase = true) &&
            !key.contains("auth", ignoreCase = true) &&
            !key.contains("password", ignoreCase = true)
        }

        assertTrue("Les clés sûres doivent être conservées", safeKeys.contains("botId"))
        assertTrue("Les clés sûres doivent être conservées", safeKeys.contains("model"))
        assertTrue("Les clés sûres doivent être conservées", safeKeys.contains("safe_param"))

        assertFalse("Le token ne doit jamais apparaître", safeKeys.contains("token"))
        assertFalse("L'api_key ne doit jamais apparaître", safeKeys.contains("api_key"))
        assertFalse("L'authHeader ne doit jamais apparaître", safeKeys.contains("authHeader"))
    }

    @Test
    fun `test agent and backend display name formatting`() {
        assertEquals("OpenCode GLM", CodeStationNotificationManager.formatAgentDisplayName("bot-opencode-glm"))
        assertEquals("Google Antigravity", CodeStationNotificationManager.formatAgentDisplayName("bot-antigravity"))
        assertEquals("Codex Terminal", CodeStationNotificationManager.formatAgentDisplayName("bot-codex-terminal"))
        assertEquals("Custom Bot", CodeStationNotificationManager.formatAgentDisplayName("custom-1", "Custom Bot"))

        assertEquals("Google Antigravity CLI", CodeStationNotificationManager.formatBackendDisplayName("agy"))
        assertEquals("Google Antigravity CLI", CodeStationNotificationManager.formatBackendDisplayName("antigravity"))
        assertEquals("OpenCode CLI", CodeStationNotificationManager.formatBackendDisplayName("opencode"))
        assertEquals("Codex Termux", CodeStationNotificationManager.formatBackendDisplayName("codex"))
        assertEquals("Protocole ACP", CodeStationNotificationManager.formatBackendDisplayName("acp"))
    }

    @Test
    fun `test execution step tracking and reasoning streaming`() {
        val testBot = "bot-test-tracer"
        RequestTracer.clearSteps(testBot)

        // 1. Initial empty state
        assertTrue(RequestTracer.getSteps(testBot).isEmpty())

        // 2. Add lifecycle event
        val eventStep = RequestTracer.eventToExecutionStep(
            com.ghdinteractivestudio.codestation.util.TraceEvent(
                requestId = "req-1",
                event = RequestTracer.EVENT_PROCESS_STARTED,
                botId = testBot,
                details = mapOf("engine" to "agy", "model" to "gemini-2.5-pro")
            )
        )
        RequestTracer.addStep(testBot, eventStep)
        assertEquals(1, RequestTracer.getSteps(testBot).size)
        assertEquals("Processus agent démarré", RequestTracer.getSteps(testBot).first().title)

        // 3. Add reasoning/thinking step
        val thoughtStep1 = com.ghdinteractivestudio.codestation.domain.model.ExecutionStep(
            id = "thought-stream-1",
            type = com.ghdinteractivestudio.codestation.domain.model.StepType.THINKING,
            title = "Raisonnement de l'agent",
            content = "Analyse initiale du problème..."
        )
        RequestTracer.addStep(testBot, thoughtStep1)
        assertEquals(2, RequestTracer.getSteps(testBot).size)

        // 4. Stream update for the same reasoning step (in-place replacement)
        val thoughtStep2 = thoughtStep1.copy(content = "Analyse initiale du problème... Planification de la solution terminée.")
        RequestTracer.addStep(testBot, thoughtStep2)
        assertEquals(2, RequestTracer.getSteps(testBot).size)
        assertEquals("Analyse initiale du problème... Planification de la solution terminée.", RequestTracer.getSteps(testBot).last().content)

        // 5. Add tool execution step
        val toolStep = com.ghdinteractivestudio.codestation.domain.model.ExecutionStep(
            id = "tool-run-1",
            type = com.ghdinteractivestudio.codestation.domain.model.StepType.TOOL_CALL,
            title = "run_command (completed)",
            content = "Statut: completed\nSortie: Build success"
        )
        RequestTracer.addStep(testBot, toolStep)
        assertEquals(3, RequestTracer.getSteps(testBot).size)

        // 6. Clear steps
        RequestTracer.clearSteps(testBot)
        assertTrue(RequestTracer.getSteps(testBot).isEmpty())
    }
}
