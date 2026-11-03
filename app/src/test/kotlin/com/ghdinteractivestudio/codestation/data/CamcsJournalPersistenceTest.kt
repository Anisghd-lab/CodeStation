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
        val entity = CamcsJournalEntity.fromEvent(event, agentName = "CodeBuddy Code")
        val restored = entity.toEvent()

        assertEquals("mission-1", entity.missionId)
        assertEquals("agent-codebuddy", entity.agentId)
        assertEquals("CodeBuddy Code", entity.agentName)
        assertEquals("agent.status_changed", entity.eventType)
        assertEquals("CodeBuddy analyse le dépôt", entity.content)
        assertEquals("INFO", entity.status)
        assertEquals(1_700_000_000_000L, entity.timestamp)

        assertEquals("mission-1", restored.missionId)
        assertEquals("agent-codebuddy", restored.agentId)
        assertEquals("agent.status_changed", restored.type)
        assertEquals("CodeBuddy analyse le dépôt", restored.message)
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
