package com.ghdinteractivestudio.codestation.util

import android.util.Log
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

        val botInfo = if (botId != null) " | bot=$botId" else ""
        val detailsStr = if (safeDetails.isEmpty()) "" else " | details=$safeDetails"
        Log.i(TAG, "[req:$requestId] event=$event$botInfo$detailsStr")
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
    }
}
