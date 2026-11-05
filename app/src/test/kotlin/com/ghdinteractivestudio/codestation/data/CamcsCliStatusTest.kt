package com.ghdinteractivestudio.codestation.data

import com.ghdinteractivestudio.codestation.domain.model.CamcsCliStatus
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CamcsCliStatusTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Test
    fun `test CamcsCliStatus deserialization with latency and health details`() {
        val payload = """
            {
                "id": "omnirush",
                "name": "OmniRush",
                "path": "/usr/bin/omnirush",
                "version": "2.8.0",
                "status": "AVAILABLE",
                "isInstalled": true,
                "activeAgent": "Agent-Alpha",
                "accessibleModels": ["gpt-6-astra", "claude-opus-5-5"],
                "activeSessions": 1,
                "capabilities": ["high_complexity_coding"],
                "healthStatus": "IMPLEMENTED_AND_VERIFIED",
                "healthDetails": "OmniRush opérationnel et authentifié",
                "isOperational": true,
                "latencyMs": 42
            }
        """.trimIndent()

        val parsed = json.decodeFromString<CamcsCliStatus>(payload)
        assertEquals("omnirush", parsed.id)
        assertEquals("OmniRush", parsed.name)
        assertEquals("/usr/bin/omnirush", parsed.path)
        assertEquals("2.8.0", parsed.version)
        assertEquals("AVAILABLE", parsed.status)
        assertTrue(parsed.isInstalled)
        assertTrue(parsed.isOperational)
        assertEquals(42L, parsed.latencyMs)
        assertEquals("IMPLEMENTED_AND_VERIFIED", parsed.healthStatus)
        assertEquals("OmniRush opérationnel et authentifié", parsed.healthDetails)
    }

    @Test
    fun `test CamcsCliStatus backward compatibility with legacy payloads missing new fields`() {
        val legacyPayload = """
            {
                "id": "agy",
                "name": "Google Antigravity",
                "path": "/root/.local/bin/agy",
                "version": "1.5.1"
            }
        """.trimIndent()

        val parsed = json.decodeFromString<CamcsCliStatus>(legacyPayload)
        assertEquals("agy", parsed.id)
        assertEquals("1.5.1", parsed.version)
        assertEquals("AVAILABLE", parsed.status)
        assertTrue(parsed.isInstalled)
        assertTrue(parsed.isOperational)
        assertEquals(null, parsed.latencyMs)
        assertEquals(null, parsed.healthStatus)
        assertEquals(null, parsed.healthDetails)
    }

    @Test
    fun `test CamcsCliStatus with blocked and non-operational cli`() {
        val freebuffPayload = """
            {
                "id": "freebuff",
                "name": "Freebuff",
                "path": "/usr/bin/freebuff",
                "version": "0.2.36",
                "status": "BLOCKED",
                "isInstalled": true,
                "healthStatus": "PARTIALLY_IMPLEMENTED",
                "healthDetails": "Freebuff v0.2.36 limité au mode TUI interactif",
                "isOperational": false,
                "latencyMs": 5
            }
        """.trimIndent()

        val parsed = json.decodeFromString<CamcsCliStatus>(freebuffPayload)
        assertEquals("freebuff", parsed.id)
        assertEquals("BLOCKED", parsed.status)
        assertTrue(parsed.isInstalled)
        assertFalse(parsed.isOperational)
        assertEquals("PARTIALLY_IMPLEMENTED", parsed.healthStatus)
        assertEquals(5L, parsed.latencyMs)
    }
}
