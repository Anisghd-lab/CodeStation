package com.ghdinteractivestudio.codestation.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import com.ghdinteractivestudio.codestation.domain.model.CamcsEvent
import kotlinx.coroutines.flow.Flow

/**
 * Journal global de mission : chronologie commune à tous les agents CAMCS.
 * Chaque ligne conserve son identifiant unique, la mission, l'agent auteur,
 * le type d'événement, la date, le contenu et le statut.
 */
@Entity(
    tableName = "camcs_journal",
    indices = [
        Index("missionId"),
        Index("agentId"),
        Index("timestamp")
    ]
)
data class CamcsJournalEntity(
    @PrimaryKey val id: String,
    val missionId: String?,
    val agentId: String?,
    val agentName: String?,
    val eventType: String,
    val timestamp: Long,
    val content: String,
    val status: String
) {
    fun toEvent(): CamcsEvent = CamcsEvent(
        id = stableLongId(id),
        missionId = missionId,
        agentId = agentId,
        type = eventType,
        message = content,
        timestamp = timestamp / 1000.0
    )

    companion object {
        fun fromEvent(
            event: CamcsEvent,
            agentName: String? = null,
            status: String = statusFromEvent(event)
        ): CamcsJournalEntity {
            val normalizedTs = normalizeTimestamp(event.timestamp)
            // Identifiant stable : un rejeu SSE (reconnexion) réinsère la même ligne
            // et se heurte à la clé primaire -> aucune duplication.
            val stableId = if (event.id != 0L) {
                "evt-${event.id}"
            } else {
                "evt-local-${event.missionId}-${event.agentId}-${event.type}-$normalizedTs-${event.message.hashCode()}"
            }
            return CamcsJournalEntity(
                id = stableId,
                missionId = event.missionId,
                agentId = event.agentId,
                agentName = agentName,
                eventType = event.type,
                timestamp = normalizedTs,
                content = if (event.message.isBlank()) event.type else event.message,
                status = status.ifBlank { "INFO" }
            )
        }

        fun normalizeTimestamp(raw: Double): Long {
            if (raw <= 0.0) return System.currentTimeMillis()
            return if (raw > 10_000_000_000.0) raw.toLong() else (raw * 1000.0).toLong()
        }

        fun statusFromEvent(event: CamcsEvent): String = when {
            event.type.contains("failed", ignoreCase = true) ||
                event.type.contains("error", ignoreCase = true) -> "FAILED"
            event.type.contains("cancelled", ignoreCase = true) -> "CANCELLED"
            event.type.contains("completed", ignoreCase = true) -> "COMPLETED"
            event.type.contains("started", ignoreCase = true) ||
                event.type.contains("running", ignoreCase = true) -> "RUNNING"
            event.type.contains("blocked", ignoreCase = true) -> "BLOCKED"
            else -> "INFO"
        }

        private fun stableLongId(id: String): Long =
            id.removePrefix("evt-").toLongOrNull()
                ?: (id.hashCode().toLong() and 0xFFFFFFFFL)
    }
}

@Dao
interface CamcsJournalDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(entries: List<CamcsJournalEntity>)

    /** Journal global, du plus récent au plus ancien. */
    @Query("SELECT * FROM camcs_journal ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentStream(limit: Int): Flow<List<CamcsJournalEntity>>

    /** Événements d'un agent donné, sans mélange avec les autres bots. */
    @Query("SELECT * FROM camcs_journal WHERE agentId = :agentId ORDER BY timestamp DESC LIMIT :limit")
    fun getForAgentStream(agentId: String, limit: Int): Flow<List<CamcsJournalEntity>>

    @Query("SELECT COUNT(*) FROM camcs_journal")
    suspend fun count(): Int

    @Query("DELETE FROM camcs_journal")
    suspend fun clear()
}
