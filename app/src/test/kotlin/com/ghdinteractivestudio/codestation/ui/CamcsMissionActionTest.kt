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
}
