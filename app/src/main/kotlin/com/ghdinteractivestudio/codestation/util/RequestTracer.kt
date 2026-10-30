package com.ghdinteractivestudio.codestation.util

import android.util.Log
import com.ghdinteractivestudio.codestation.domain.model.ExecutionStep
import com.ghdinteractivestudio.codestation.domain.model.StepType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Traceur d'événements structurés pour le cycle de vie complet d'une requête agent.
 * Assure la corrélation par requestId sans exposer de secrets, jetons ni clés API.
 */
data class TraceEvent(
    val requestId: String,
    val event: String,
    val timestamp: Long = System.currentTimeMillis(),
    val botId: String? = null,
    val details: Map<String, Any?> = emptyMap()
)

object RequestTracer {

    private const val TAG = "CodeStationTrace"

    // 13 étapes requises par le protocole d'audit complet
    const val EVENT_MESSAGE_CREATED = "message_created"
    const val EVENT_MESSAGE_QUEUED = "message_queued"
    const val EVENT_REQUEST_SENT = "request_sent"
    const val EVENT_REQUEST_RECEIVED = "request_received"
    const val EVENT_BOT_RESOLVED = "bot_resolved"
    const val EVENT_CLI_RESOLVED = "cli_resolved"
    const val EVENT_PROCESS_STARTED = "process_started"
    const val EVENT_CLI_INPUT_DELIVERED = "cli_input_delivered"
    const val EVENT_PROVIDER_REQUEST_STARTED = "provider_request_started"
    const val EVENT_FIRST_OUTPUT_RECEIVED = "first_output_received"
    const val EVENT_RESPONSE_COMPLETED = "response_completed"
    const val EVENT_REQUEST_FAILED = "request_failed"
    const val EVENT_REQUEST_CANCELLED = "request_cancelled"

    // Mémoire tampon circulaire des 500 derniers événements pour audit et tests
    private val traceLog = CopyOnWriteArrayList<TraceEvent>()
    private val requestIndices = ConcurrentHashMap<String, CopyOnWriteArrayList<TraceEvent>>()

    // Étapes d'exécution et de raisonnement (ExecutionStep) indexées par botId
    private val botStepsMap = ConcurrentHashMap<String, CopyOnWriteArrayList<ExecutionStep>>()
    private val _stepsFlow = MutableStateFlow<Map<String, List<ExecutionStep>>>(emptyMap())

    fun addStep(botId: String, step: ExecutionStep) {
        val list = botStepsMap.computeIfAbsent(botId) { CopyOnWriteArrayList() }
        val existingIndex = list.indexOfFirst { it.id == step.id }
        if (existingIndex >= 0) {
            list[existingIndex] = step
        } else {
            list.add(step)
            if (list.size > 200) {
                list.removeAt(0)
            }
        }
        val currentMap = _stepsFlow.value.toMutableMap()
        currentMap[botId] = list.toList()
        _stepsFlow.value = currentMap
    }

    fun getSteps(botId: String): List<ExecutionStep> {
        return botStepsMap[botId]?.toList() ?: emptyList()
    }

    fun getStepsFlow(botId: String): Flow<List<ExecutionStep>> {
        return _stepsFlow.map { it[botId] ?: emptyList() }
    }

    fun clearSteps(botId: String) {
        botStepsMap[botId]?.clear()
        val currentMap = _stepsFlow.value.toMutableMap()
        currentMap.remove(botId)
        _stepsFlow.value = currentMap
    }

    fun eventToExecutionStep(event: TraceEvent): ExecutionStep {
        val title = when (event.event) {
            EVENT_MESSAGE_CREATED -> "Création du message"
            EVENT_MESSAGE_QUEUED -> "Mise en file d'attente"
            EVENT_REQUEST_SENT -> "Envoi vers l'hôte CodeStation"
            EVENT_REQUEST_RECEIVED -> "Requête reçue"
            EVENT_BOT_RESOLVED -> "Agent sélectionné"
            EVENT_CLI_RESOLVED -> "CLI et modèle résolus"
            EVENT_PROCESS_STARTED -> "Processus agent démarré"
            EVENT_CLI_INPUT_DELIVERED -> "Entrée ACP délivrée"
            EVENT_PROVIDER_REQUEST_STARTED -> "Requête modèle initiée"
            EVENT_FIRST_OUTPUT_RECEIVED -> "Premier flux de réponse reçu"
            EVENT_RESPONSE_COMPLETED -> "Génération terminée"
            EVENT_REQUEST_FAILED -> "Échec d'exécution"
            EVENT_REQUEST_CANCELLED -> "Génération interrompue"
            else -> event.event.replace('_', ' ').replaceFirstChar { it.uppercase() }
        }

        val content = if (event.details.isNotEmpty()) {
            event.details.entries.joinToString(" • ") { "${it.key}: ${it.value}" }
        } else {
            "Étape validée avec succès"
        }

        return ExecutionStep(
            id = "${event.requestId}-${event.event}-${event.timestamp}",
            type = if (event.event == EVENT_PROCESS_STARTED || event.event == EVENT_PROVIDER_REQUEST_STARTED) StepType.THINKING else StepType.NARRATION,
            title = title,
            content = content,
            isError = (event.event == EVENT_REQUEST_FAILED),
            timestamp = event.timestamp
        )
    }

    fun logEvent(
        requestId: String,
        event: String,
        botId: String? = null,
        details: Map<String, Any?> = emptyMap()
    ) {
        val safeDetails = details.filterKeys { key ->
            !key.contains("token", ignoreCase = true) &&
            !key.contains("key", ignoreCase = true) &&
            !key.contains("secret", ignoreCase = true) &&
            !key.contains("auth", ignoreCase = true) &&
            !key.contains("password", ignoreCase = true)
        }

        val traceEvent = TraceEvent(
            requestId = requestId,
            event = event,
            timestamp = System.currentTimeMillis(),
            botId = botId,
            details = safeDetails
        )

        traceLog.add(traceEvent)
        if (traceLog.size > 500) {
            traceLog.removeAt(0)
        }

        requestIndices.computeIfAbsent(requestId) { CopyOnWriteArrayList() }.add(traceEvent)

        val effectiveBotId = botId ?: details["botId"] as? String
        if (effectiveBotId != null) {
            val step = eventToExecutionStep(traceEvent)
            addStep(effectiveBotId, step)
        }

        val botInfo = if (effectiveBotId != null) " | bot=$effectiveBotId" else ""
        val detailsStr = if (safeDetails.isEmpty()) "" else " | details=$safeDetails"
        try {
            Log.i(TAG, "[req:$requestId] event=$event$botInfo$detailsStr")
        } catch (_: Throwable) {
            println("$TAG: [req:$requestId] event=$event$botInfo$detailsStr")
        }
    }

    fun getEventsForRequest(requestId: String): List<TraceEvent> {
        return requestIndices[requestId]?.toList() ?: emptyList()
    }

    fun getAllEvents(): List<TraceEvent> {
        return traceLog.toList()
    }

    fun clear() {
        traceLog.clear()
        requestIndices.clear()
        botStepsMap.clear()
        _stepsFlow.value = emptyMap()
    }
}
