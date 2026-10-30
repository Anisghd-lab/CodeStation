package com.ghdinteractivestudio.codestation.domain.model

enum class MemorySyncState {
    SYNCED,
    SYNCING,
    PENDING_CHANGES,
    OFFLINE,
    SYNC_ERROR,
    NOT_INITIALIZED;

    val displayLabel: String
        get() = when (this) {
            SYNCED -> "Synchronisée avec GitHub"
            SYNCING -> "Synchronisation en cours…"
            PENDING_CHANGES -> "Modifications en attente"
            OFFLINE -> "Hors ligne"
            SYNC_ERROR -> "Erreur de synchronisation"
            NOT_INITIALIZED -> "Mémoire non initialisée"
        }

    val dotColorHex: String
        get() = when (this) {
            SYNCED -> "#10B981"         // Vert émeraude
            SYNCING -> "#2DD4BF"        // Turquoise cyan
            PENDING_CHANGES -> "#F59E0B"// Ambre orange
            OFFLINE -> "#6B7280"        // Gris
            SYNC_ERROR -> "#EF4444"     // Rouge erreur
            NOT_INITIALIZED -> "#9CA3AF"// Gris clair
        }
}

data class CollectiveMemoryStatus(
    val isInitialized: Boolean = false,
    val syncStatus: MemorySyncState = MemorySyncState.NOT_INITIALIZED,
    val lastSyncAt: String? = null,
    val totalExperiences: Int = 0,
    val totalKnowledge: Int = 0,
    val totalRelations: Int = 0,
    val totalAgents: Int = 0,
    val repoUrl: String = "https://github.com/Anisghd-lab/CodeStation-Memory",
    val branch: String = "main",
    val latestCommitHash: String? = null,
    val errorMessage: String? = null
)

data class MemoryExperience(
    val id: String,
    val title: String,
    val agent: String,
    val project: String,
    val context: String,
    val problem: String,
    val rootCause: String,
    val solution: String,
    val outcome: String,
    val impact: String,
    val validationStatus: String,
    val tags: List<String> = emptyList(),
    val createdAt: String,
    val filesChanged: List<String> = emptyList()
)

data class MemoryKnowledge(
    val id: String,
    val title: String,
    val category: String,
    val content: String,
    val validationStatus: String,
    val version: Int = 1,
    val derivedFromExperiences: List<String> = emptyList(),
    val appliesToProjects: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val createdAt: String,
    val updatedAt: String? = null
)

data class MemorySearchResultItem(
    val id: String,
    val itemType: String, // "experience" | "knowledge"
    val title: String,
    val summary: String,
    val agent: String? = null,
    val project: String? = null,
    val validationStatus: String,
    val relevanceScore: Float = 1.0f,
    val tags: List<String> = emptyList()
)
