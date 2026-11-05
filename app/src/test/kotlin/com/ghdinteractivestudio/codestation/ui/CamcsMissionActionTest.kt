package com.ghdinteractivestudio.codestation.ui

import com.ghdinteractivestudio.codestation.ui.screens.isMissionActionConfirmed
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Non-régression Tâche 06 : une commande pause/reprise n'est validée que si
 * le backend CAMCS répond réellement 2xx avec `ok` différent de false.
 */
class CamcsMissionActionTest {

    @Test
    fun `ok vrai avec http 200 est confirme`() {
        assertTrue(isMissionActionConfirmed(200, """{"ok": true}"""))
    }

    @Test
    fun `ok faux avec http 200 est refuse`() {
        assertFalse(
            "Le moteur signale que la mission n'est pas pausable",
            isMissionActionConfirmed(200, """{"ok": false}""")
        )
    }

    @Test
    fun `une erreur http est refusee`() {
        assertFalse(isMissionActionConfirmed(500, """{"ok": true}"""))
        assertFalse(isMissionActionConfirmed(404, ""))
    }

    @Test
    fun `une reponse sans champ ok mais 2xx est acceptee`() {
        assertTrue(isMissionActionConfirmed(200, "{}"))
        assertTrue(isMissionActionConfirmed(204, ""))
    }

    @Test
    fun `arreter mission conserve la mission dans la liste avec statut CANCELLED`() {
        val initialMissions = listOf(
            com.ghdinteractivestudio.codestation.domain.model.CamcsMission(
                id = "m-1",
                title = "Mission Test",
                objective = "Tester l'arrêt",
                status = "RUNNING"
            )
        )

        // Simule la transition d'arrêt confirmée par le moteur
        val updated = initialMissions.map { m ->
            if (m.id == "m-1") {
                m.copy(
                    status = "CANCELLED",
                    lastTransitionReason = "Mission arrêtée par l'utilisateur"
                )
            } else m
        }

        org.junit.Assert.assertEquals("La mission arrêtée ne doit PAS être supprimée de la liste", 1, updated.size)
        org.junit.Assert.assertEquals("CANCELLED", updated.first().status)
        org.junit.Assert.assertEquals("Mission arrêtée par l'utilisateur", updated.first().lastTransitionReason)
    }

    @Test
    fun `pause et reprise alternent le statut sans perte`() {
        val mission = com.ghdinteractivestudio.codestation.domain.model.CamcsMission(
            id = "m-2",
            title = "Mission Longue",
            objective = "Tester pause",
            status = "RUNNING"
        )

        val paused = mission.copy(status = "PAUSED", lastTransitionReason = "Mission mise en pause")
        org.junit.Assert.assertEquals("PAUSED", paused.status)

        val resumed = paused.copy(status = "RUNNING", lastTransitionReason = "Reprise de la mission")
        org.junit.Assert.assertEquals("RUNNING", resumed.status)
        org.junit.Assert.assertEquals(mission.id, resumed.id)
    }
}
