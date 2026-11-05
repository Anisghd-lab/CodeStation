package com.ghdinteractivestudio.codestation.data

import com.ghdinteractivestudio.codestation.data.local.CamcsJournalEntity
import com.ghdinteractivestudio.codestation.domain.model.CamcsEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Non-régression Tâche 05 : le journal global doit conserver chaque événement
 * avec un identifiant stable (anti-doublon), sa mission, son agent, son type,
 * sa date, son contenu et son statut.
 */
class CamcsJournalPersistenceTest {

    private val event = CamcsEvent(
        id = 42L,
        missionId = "mission-1",
        agentId = "agent-codebuddy",
        type = "agent.status_changed",
        message = "CodeBuddy analyse le dépôt",
        timestamp = 1_700_000_000.0
    )

    @Test
    fun `le meme evenement rejoue porte le meme identifiant`() {
        val first = CamcsJournalEntity.fromEvent(event)
        val replay = CamcsJournalEntity.fromEvent(event)

        assertEquals("evt-42", first.id)
        assertEquals("Un rejeu SSE doit être ignoré par la clé primaire", first.id, replay.id)
    }

    @Test
    fun `aller retour conserve mission agent type contenu date et statut`() {
        val entity = CamcsJournalEntity.fromEvent(event, agentName = "CodeBuddy Code", status = "RUNNING")
        val restored = entity.toEvent()

        assertEquals("mission-1", entity.missionId)
        assertEquals("agent-codebuddy", entity.agentId)
        assertEquals("CodeBuddy Code", entity.agentName)
        assertEquals("agent.status_changed", entity.eventType)
        assertEquals("CodeBuddy analyse le dépôt", entity.content)
        assertEquals("RUNNING", entity.status)
        assertEquals(1_700_000_000_000L, entity.timestamp)

        assertEquals("mission-1", restored.missionId)
        assertEquals("agent-codebuddy", restored.agentId)
        assertEquals("CodeBuddy Code", restored.agentName)
        assertEquals("agent.status_changed", restored.type)
        assertEquals("CodeBuddy analyse le dépôt", restored.message)
        assertEquals("RUNNING", restored.status)
        assertEquals(1_700_000_000.0, restored.timestamp, 0.001)
    }

    @Test
    fun `un evenement sans id serveur reste deterministe`() {
        val local = event.copy(id = 0L)
        val first = CamcsJournalEntity.fromEvent(local)
        val second = CamcsJournalEntity.fromEvent(local)
        val other = CamcsJournalEntity.fromEvent(local.copy(message = "Autre contenu"))

        assertEquals(first.id, second.id)
        assertNotEquals("Deux contenus différents ne doivent pas fusionner", first.id, other.id)

        // Les identifiants restaurés doivent être identiques pour le même événement local
        assertEquals(first.toEvent().id, second.toEvent().id)
        assertNotEquals(first.toEvent().id, other.toEvent().id)
    }

    @Test
    fun `evenements de plusieurs agents distincts conservent leur identite`() {
        val evCodeBuddy = CamcsEvent(id = 101L, missionId = "m1", agentId = "codebuddy", agentName = "CodeBuddy", type = "task.started", message = "Audit")
        val evOmniRush = CamcsEvent(id = 102L, missionId = "m1", agentId = "omnirush", agentName = "OmniRush", type = "task.started", message = "Refactor")
        val evOpenCode = CamcsEvent(id = 103L, missionId = "m1", agentId = "opencode", agentName = "OpenCode", type = "task.completed", message = "Validation")

        val entities = listOf(evCodeBuddy, evOmniRush, evOpenCode).map { CamcsJournalEntity.fromEvent(it) }

        assertEquals(3, entities.map { it.id }.distinct().size)
        assertEquals(listOf("codebuddy", "omnirush", "opencode"), entities.map { it.agentId })
        assertEquals(listOf("CodeBuddy", "OmniRush", "OpenCode"), entities.map { it.agentName })
    }

    @Test
    fun `deduplication pending vs persisted ne cree aucun doublon`() {
        val ev = CamcsEvent(id = 0L, missionId = "m1", agentId = "codebuddy", type = "task.log", message = "Log message", timestamp = 1700000000.0)
        val entity = CamcsJournalEntity.fromEvent(ev)
        val normalized = entity.toEvent()

        val liveList = mutableListOf(normalized)
        val persistedList = listOf(entity.toEvent())

        val pending = liveList.filter { live ->
            persistedList.none { p ->
                p.id == live.id || (p.timestamp == live.timestamp && p.message == live.message && p.type == live.type)
            }
        }

        assertTrue("L'événement persisté doit être reconnu et exclu des pending", pending.isEmpty())
        val combined = (pending + persistedList).sortedByDescending { it.timestamp }
        assertEquals("Aucun doublon dans la liste finale", 1, combined.size)
    }

    @Test
    fun `le statut est derive du type evenement`() {
        assertEquals("COMPLETED", CamcsJournalEntity.statusFromEvent(event.copy(type = "mission.completed")))
        assertEquals("FAILED", CamcsJournalEntity.statusFromEvent(event.copy(type = "task.failed")))
        assertEquals("CANCELLED", CamcsJournalEntity.statusFromEvent(event.copy(type = "mission.cancelled")))
        assertEquals("RUNNING", CamcsJournalEntity.statusFromEvent(event.copy(type = "task.started")))
        assertEquals("BLOCKED", CamcsJournalEntity.statusFromEvent(event.copy(type = "agent.blocked")))
        assertTrue(CamcsJournalEntity.statusFromEvent(event).isNotBlank())
    }
}
