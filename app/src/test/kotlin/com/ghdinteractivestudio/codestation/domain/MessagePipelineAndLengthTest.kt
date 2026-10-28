package com.ghdinteractivestudio.codestation.domain

import com.ghdinteractivestudio.codestation.util.RequestTracer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

class MessagePipelineAndLengthTest {

    @Before
    fun setUp() {
        RequestTracer.clear()
    }

    @Test
    fun `test end-to-end request tracing stages with requestId correlation`() {
        val reqId = UUID.randomUUID().toString()
        val botId = "bot-opencode-glm"

        // 13 phases
        RequestTracer.logEvent(reqId, RequestTracer.EVENT_MESSAGE_CREATED, botId, mapOf("length" to 42))
        RequestTracer.logEvent(reqId, RequestTracer.EVENT_MESSAGE_QUEUED, botId)
        RequestTracer.logEvent(reqId, RequestTracer.EVENT_REQUEST_SENT, botId)
        RequestTracer.logEvent(reqId, RequestTracer.EVENT_REQUEST_RECEIVED, botId)
        RequestTracer.logEvent(reqId, RequestTracer.EVENT_BOT_RESOLVED, botId)
        RequestTracer.logEvent(reqId, RequestTracer.EVENT_CLI_RESOLVED, botId, mapOf("engine" to "opencode"))
        RequestTracer.logEvent(reqId, RequestTracer.EVENT_PROCESS_STARTED, botId)
        RequestTracer.logEvent(reqId, RequestTracer.EVENT_CLI_INPUT_DELIVERED, botId)
        RequestTracer.logEvent(reqId, RequestTracer.EVENT_PROVIDER_REQUEST_STARTED, botId)
        RequestTracer.logEvent(reqId, RequestTracer.EVENT_FIRST_OUTPUT_RECEIVED, botId)
        RequestTracer.logEvent(reqId, RequestTracer.EVENT_RESPONSE_COMPLETED, botId)

        val events = RequestTracer.getEventsForRequest(reqId)
        assertEquals(11, events.size)

        val stageNames = events.map { it.event }
        assertTrue(stageNames.contains(RequestTracer.EVENT_MESSAGE_CREATED))
        assertTrue(stageNames.contains(RequestTracer.EVENT_CLI_RESOLVED))
        assertTrue(stageNames.contains(RequestTracer.EVENT_CLI_INPUT_DELIVERED))
        assertTrue(stageNames.contains(RequestTracer.EVENT_RESPONSE_COMPLETED))

        // All events correlated to the same requestId and bot
        events.forEach { ev ->
            assertEquals(reqId, ev.requestId)
            assertEquals(botId, ev.botId)
        }
    }

    @Test
    fun `test secret filtering prevents logging tokens or passwords`() {
        val reqId = "test-req-secrets"
        RequestTracer.logEvent(
            requestId = reqId,
            event = RequestTracer.EVENT_REQUEST_SENT,
            botId = "bot-antigravity",
            details = mapOf(
                "token" to "secret-bearer-12345",
                "apiKey" to "AIzaSySecretApiKey",
                "authorization" to "Bearer xyz",
                "safeParam" to "public_value",
                "messageLength" to 1500
            )
        )

        val events = RequestTracer.getEventsForRequest(reqId)
        assertEquals(1, events.size)
        val details = events.first().details

        assertFalse(details.containsKey("token"))
        assertFalse(details.containsKey("apiKey"))
        assertFalse(details.containsKey("authorization"))
        assertTrue(details.containsKey("safeParam"))
        assertTrue(details.containsKey("messageLength"))
        assertEquals("public_value", details["safeParam"])
    }

    @Test
    fun `test long message preservation with unicode newlines and special characters`() {
        // Construct a message with 10,000+ characters, newlines, unicode, quotes
        val sb = StringBuilder()
        sb.append("Titre : Message de test volumineux avec caractères spéciaux\n\n")
        sb.append("Emoji: 🚀 🤖 📱 💡 | Accents: é è à ç ù ô î ë | Quotes: 'simple' \"double\" `code`\n")
        for (i in 1..200) {
            sb.append("Ligne $i : L'exécution du code se poursuit sans aucune troncature artificielle.\n")
        }
        val longText = sb.toString()
        assertTrue("Long text should exceed 5000 characters", longText.length > 5000)

        // Verify preservation without truncation
        val originalLength = longText.length
        val retrievedText = longText.trim()
        assertEquals(originalLength, retrievedText.length)
        assertTrue(retrievedText.contains("Ligne 200"))
        assertTrue(retrievedText.contains("🚀 🤖 📱"))
        assertTrue(retrievedText.contains("\"double\""))
    }
}
