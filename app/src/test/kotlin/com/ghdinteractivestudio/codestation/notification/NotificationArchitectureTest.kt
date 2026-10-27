package com.ghdinteractivestudio.codestation.notification

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationArchitectureTest {

    @Test
    fun testNotificationChannelsDefinitions() {
        assertEquals("codestation_live_tasks", CodeStationNotificationManager.CHANNEL_LIVE_TASKS)
        assertEquals("codestation_approvals", CodeStationNotificationManager.CHANNEL_APPROVALS)
        assertEquals("codestation_completed", CodeStationNotificationManager.CHANNEL_COMPLETED)
    }

    @Test
    fun testNotificationIdGeneration() {
        val id1 = CodeStationNotificationManager.getNotificationIdForBot("bot-lead-codestation")
        val id2 = CodeStationNotificationManager.getNotificationIdForBot("bot-lead-codestation")
        val id3 = CodeStationNotificationManager.getNotificationIdForBot("bot-codex-terminal")

        assertTrue("Notification ID must be positive", id1 > 0)
        assertEquals("Same bot ID must yield deterministic notification ID", id1, id2)
        assertNotEquals("Different bots must yield different notification IDs", id1, id3)
    }

    @Test
    fun testTaskNotificationPhasesCompleteness() {
        val phases = TaskNotificationPhase.values().map { it.name }
        assertTrue(phases.contains("QUEUED"))
        assertTrue(phases.contains("WORKING"))
        assertTrue(phases.contains("NEEDS_APPROVAL"))
        assertTrue(phases.contains("COMPLETED"))
        assertTrue(phases.contains("FAILED"))
        assertTrue(phases.contains("STOPPED"))
        assertTrue(phases.contains("DISCONNECTED"))
        assertEquals(7, TaskNotificationPhase.values().size)
    }

    @Test
    fun testServerSentEventBotPayloadParsing() {
        val payload = """
            {
                "type": "bot",
                "rev": 142,
                "bot": {
                    "id": "bot-lead-codestation",
                    "name": "CodeStation Core",
                    "backend": "agy",
                    "status": "working",
                    "activity": "Exécution d'instructions CLI agy",
                    "startedAt": 1700000000000
                }
            }
        """.trimIndent()

        val json = Json { ignoreUnknownKeys = true }
        val root = json.parseToJsonElement(payload).jsonObject

        assertEquals("bot", root["type"]?.jsonPrimitive?.content)
        val bot = root["bot"]?.jsonObject
        assertNotNull(bot)
        assertEquals("bot-lead-codestation", bot!!["id"]?.jsonPrimitive?.content)
        assertEquals("working", bot["status"]?.jsonPrimitive?.content)
        assertEquals("Exécution d'instructions CLI agy", bot["activity"]?.jsonPrimitive?.content)
    }

    @Test
    fun testServerSentEventApprovalPayloadParsing() {
        val payload = """
            {
                "type": "entry",
                "rev": 143,
                "entry": {
                    "id": "card-approval-99",
                    "bot_id": "bot-lead-codestation",
                    "kind": "approval",
                    "turn": 3,
                    "data": {
                        "text": "Autoriser la commande bash",
                        "command": "git status",
                        "approval": true
                    }
                }
            }
        """.trimIndent()

        val json = Json { ignoreUnknownKeys = true }
        val root = json.parseToJsonElement(payload).jsonObject

        assertEquals("entry", root["type"]?.jsonPrimitive?.content)
        val entry = root["entry"]?.jsonObject
        assertNotNull(entry)
        assertEquals("approval", entry!!["kind"]?.jsonPrimitive?.content)
        val data = entry["data"]?.jsonObject
        assertNotNull(data)
        assertEquals("git status", data!!["command"]?.jsonPrimitive?.content)
    }
}
