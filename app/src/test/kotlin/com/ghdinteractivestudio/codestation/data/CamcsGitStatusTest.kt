package com.ghdinteractivestudio.codestation.data

import com.ghdinteractivestudio.codestation.domain.model.CamcsGitLastCommit
import com.ghdinteractivestudio.codestation.domain.model.CamcsGitStatus
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Non-régression Tâche 08 : traçabilité Git réelle sans simulation.
 * Validation de la désérialisation du contrat JSON émis par /api/camcs/git/status,
 * de la gestion des erreurs si le dépôt est inaccessible, et du comptage des fichiers modifiés.
 */
class CamcsGitStatusTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @Test
    fun `deserialisation complete du statut Git reel`() {
        val payload = """
            {
              "ok": true,
              "path": "/root/CodeStation",
              "branch": "feature/codestation-proot-adaptation",
              "upstream": "origin/feature/codestation-proot-adaptation",
              "ahead": 1,
              "behind": 0,
              "modified": ["app/src/main/kotlin/RosterScreen.kt"],
              "added": ["scripts/camcs_git_status.py"],
              "deleted": ["old_file.txt"],
              "renamed": [],
              "lastCommit": {
                "hash": "821bb0f4846d52d7b9c727df410728052da066e0",
                "shortHash": "821bb0f",
                "subject": "fix: audit CAMCS",
                "date": "2026-11-05T13:47:45+00:00"
              },
              "clean": false,
              "remoteAccessible": true,
              "lastSyncAt": 1793880751.5,
              "generatedAt": 1793886494.7
            }
        """.trimIndent()

        val status = json.decodeFromString<CamcsGitStatus>(payload)

        assertTrue(status.ok)
        assertEquals("/root/CodeStation", status.path)
        assertEquals("feature/codestation-proot-adaptation", status.branch)
        assertEquals("origin/feature/codestation-proot-adaptation", status.upstream)
        assertEquals(1, status.ahead)
        assertEquals(0, status.behind)
        assertEquals(listOf("app/src/main/kotlin/RosterScreen.kt"), status.modified)
        assertEquals(listOf("scripts/camcs_git_status.py"), status.added)
        assertEquals(listOf("old_file.txt"), status.deleted)
        assertFalse(status.clean)
        assertTrue(status.remoteAccessible)

        assertNotNull(status.lastCommit)
        val commit = status.lastCommit!!
        assertEquals("821bb0f4846d52d7b9c727df410728052da066e0", commit.hash)
        assertEquals("821bb0f", commit.shortHash)
        assertEquals("fix: audit CAMCS", commit.subject)
    }

    @Test
    fun `gestion explicite de l erreur si depot inaccessible`() {
        val errorPayload = """
            {
              "ok": false,
              "path": "/invalid/path",
              "error": "Dépôt introuvable"
            }
        """.trimIndent()

        val status = json.decodeFromString<CamcsGitStatus>(errorPayload)

        assertFalse("Le statut doit être marqué en échec", status.ok)
        assertEquals("Dépôt introuvable", status.error)
        assertTrue(status.modified.isEmpty())
        assertFalse(status.remoteAccessible)
    }

    @Test
    fun `un depot propre ne contient aucun fichier en attente`() {
        val cleanStatus = CamcsGitStatus(
            ok = true,
            path = "/root/CodeStation",
            branch = "main",
            clean = true
        )

        val totalChanged = cleanStatus.modified.size + cleanStatus.added.size +
            cleanStatus.deleted.size + cleanStatus.renamed.size

        assertEquals(0, totalChanged)
        assertTrue(cleanStatus.clean)
    }
}
