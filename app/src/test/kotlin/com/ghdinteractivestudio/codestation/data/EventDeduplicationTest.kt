package com.ghdinteractivestudio.codestation.data

import com.ghdinteractivestudio.codestation.data.remote.CodeStationEventStreamClient
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Non-régression Tâche 03 : un événement SSE rejoué après reconnexion ne doit pas
 * être traité deux fois (doublons CodeBuddy / messages / approbations / traces).
 */
class EventDeduplicationTest {

    @Test
    fun `la meme entree rejouee est ignoree`() {
        val cache = CodeStationEventStreamClient.BoundedEntryIdCache()
        val fingerprint = "entry-1|agent|false|12|123456"

        assertTrue("Première réception = nouvelle entrée", cache.markIfNew(fingerprint))
        assertFalse("Rejeu SSE = doublon ignoré", cache.markIfNew(fingerprint))
    }

    @Test
    fun `une mise a jour reelle du meme id reste traitee`() {
        val cache = CodeStationEventStreamClient.BoundedEntryIdCache()
        val firstChunk = "entry-1|agent|false|12|123456"
        val finalEntry = "entry-1|agent|true|48|654321"

        assertTrue(cache.markIfNew(firstChunk))
        assertTrue("Le contenu a changé, ce n'est pas un doublon", cache.markIfNew(finalEntry))
    }

    @Test
    fun `le cache reste borne et accepte les nouvelles entrees apres eviction`() {
        val cache = CodeStationEventStreamClient.BoundedEntryIdCache(capacity = 2)
        assertTrue(cache.markIfNew("a"))
        assertTrue(cache.markIfNew("b"))
        assertTrue(cache.markIfNew("c"))
        assertTrue("L'entrée la plus ancienne a été évincée", cache.markIfNew("a"))
        assertFalse(cache.markIfNew("c"))
    }

    @Test
    fun `une empreinte vide ne bloque jamais le traitement`() {
        val cache = CodeStationEventStreamClient.BoundedEntryIdCache()
        assertTrue(cache.markIfNew(""))
        assertTrue(cache.markIfNew(""))
    }
}
