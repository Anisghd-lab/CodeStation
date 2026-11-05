package com.ghdinteractivestudio.codestation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ghdinteractivestudio.codestation.domain.model.Bot
import com.ghdinteractivestudio.codestation.domain.model.CollectiveMemoryStatus
import com.ghdinteractivestudio.codestation.domain.model.MemoryExperience
import com.ghdinteractivestudio.codestation.domain.model.MemoryKnowledge
import com.ghdinteractivestudio.codestation.domain.model.MemorySearchResultItem
import com.ghdinteractivestudio.codestation.domain.model.MemorySyncState
import com.ghdinteractivestudio.codestation.domain.repository.BotRepository
import com.ghdinteractivestudio.codestation.domain.repository.MemoryRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MemoryUiState(
    val status: CollectiveMemoryStatus = CollectiveMemoryStatus(),
    val searchQuery: String = "",
    val selectedFilter: String = "ALL", // "ALL" | "VALIDATED" | "EXPERIENCES" | "KNOWLEDGE"
    val items: List<MemorySearchResultItem> = emptyList(),
    val allExperiences: List<MemoryExperience> = emptyList(),
    val allKnowledge: List<MemoryKnowledge> = emptyList(),
    val bots: List<Bot> = emptyList(),
    val selectedExperience: MemoryExperience? = null,
    val selectedKnowledge: MemoryKnowledge? = null,
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val syncFeedbackMessage: String? = null,
    val errorMessage: String? = null
) {
    /**
     * Exact count of stored experiences (either from remote sync status or local deduplicated items).
     */
    val realExperiencesCount: Int
        get() = if (status.totalExperiences > 0) status.totalExperiences else allExperiences.size

    /**
     * Exact count of stored knowledge items.
     */
    val realKnowledgeCount: Int
        get() = if (status.totalKnowledge > 0) status.totalKnowledge else allKnowledge.size

    /**
     * Exact count of mobilized agents in memory (from status, active bots, or distinct experience authors).
     */
    val realAgentsCount: Int
        get() {
            val authors = allExperiences.map { it.agent }.filter { it.isNotBlank() }.distinct()
            return maxOf(status.totalAgents, bots.size, authors.size)
        }

    /**
     * Effective relations count reflecting real connections in memory:
     * Core to agents, inter-agent collaboration, experience-to-agent, experience-to-project,
     * knowledge-to-experience and knowledge-to-agent links.
     */
    val realRelationsCount: Int
        get() {
            var calculated = 0
            val agents = realAgentsCount
            if (agents > 0) {
                calculated += agents // Core to each agent
                if (agents > 1) calculated += agents // Inter-agent highways
            }
            for (exp in allExperiences) {
                if (exp.agent.isNotBlank()) calculated++
                if (exp.project.isNotBlank()) calculated++
            }
            for (knw in allKnowledge) {
                calculated += knw.derivedFromExperiences.size
                calculated += knw.appliesToProjects.size
                calculated++ // Knowledge associated with author/agent
            }
            return maxOf(status.totalRelations, calculated)
        }

    /**
     * Accurate total node count in the collective neural graph:
     * CodeStation Core Nucleus (1) + Real Agents + Experiences + Knowledge.
     */
    val realNodesCount: Int
        get() {
            val hasData = allExperiences.isNotEmpty() || allKnowledge.isNotEmpty() || realAgentsCount > 0
            val baseCore = if (hasData) 1 else 0
            val calculated = baseCore + realAgentsCount + realExperiencesCount + realKnowledgeCount
            return maxOf(status.totalNodes, calculated)
        }
}

class MemoryViewModel(
    private val memoryRepository: MemoryRepository,
    private val botRepository: BotRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(MemoryUiState())
    val uiState: StateFlow<MemoryUiState> = _uiState.asStateFlow()
    private var loadJob: Job? = null

    init {
        // Observe repository memoryStatus
        viewModelScope.launch {
            memoryRepository.memoryStatus.collect { status ->
                _uiState.update { it.copy(status = status) }
            }
        }
        // Observe active/installed bots dynamically
        viewModelScope.launch {
            botRepository?.getBotsStream()?.collect { botList ->
                _uiState.update { it.copy(bots = botList) }
            }
        }
        loadAll()
    }

    fun loadAll() {
        if (loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                memoryRepository.refreshStatus()
                val expResult = memoryRepository.getExperiences(50, 0)
                val knwResult = memoryRepository.getKnowledge(50, 0)

                // Tâche 03 : Déduplication stricte des expériences et connaissances par identifiant
                val exps = expResult.getOrDefault(emptyList()).distinctBy { it.id }
                val knws = knwResult.getOrDefault(emptyList()).distinctBy { it.id }

                _uiState.update { current ->
                    current.copy(
                        allExperiences = exps,
                        allKnowledge = knws,
                        isLoading = false
                    )
                }
                applyFilterAndSearch()
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFilterAndSearch()
    }

    fun onFilterSelected(filter: String) {
        _uiState.update { it.copy(selectedFilter = filter) }
        applyFilterAndSearch()
    }

    fun selectExperience(exp: MemoryExperience?) {
        _uiState.update { it.copy(selectedExperience = exp, selectedKnowledge = null) }
    }

    fun selectKnowledge(knw: MemoryKnowledge?) {
        _uiState.update { it.copy(selectedKnowledge = knw, selectedExperience = null) }
    }

    fun selectResultItem(item: MemorySearchResultItem) {
        if (item.itemType == "experience") {
            val found = _uiState.value.allExperiences.find { it.id == item.id }
            selectExperience(found ?: MemoryExperience(
                id = item.id,
                title = item.title,
                agent = item.agent ?: "",
                project = item.project ?: "",
                context = "",
                problem = item.summary,
                rootCause = "",
                solution = "",
                outcome = "",
                impact = "",
                validationStatus = item.validationStatus,
                tags = item.tags,
                createdAt = ""
            ))
        } else {
            val found = _uiState.value.allKnowledge.find { it.id == item.id }
            selectKnowledge(found ?: MemoryKnowledge(
                id = item.id,
                title = item.title,
                category = "General",
                content = item.summary,
                validationStatus = item.validationStatus,
                tags = item.tags,
                createdAt = ""
            ))
        }
    }

    fun triggerSync() {
        if (_uiState.value.isSyncing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, syncFeedbackMessage = "Synchronisation avec GitHub en cours…") }
            try {
                val updatedStatus = memoryRepository.triggerSync()
                val isSuccess = updatedStatus.isSuccess
                val msg = if (isSuccess) {
                    "Mémoire collective synchronisée avec succès avec GitHub."
                } else {
                    "Échec de synchronisation : ${updatedStatus.exceptionOrNull()?.message}"
                }
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        syncFeedbackMessage = msg
                    )
                }
                loadAll()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        syncFeedbackMessage = "Erreur : ${e.message}"
                    )
                }
            }
        }
    }

    private fun applyFilterAndSearch() {
        val q = _uiState.value.searchQuery.trim().lowercase()
        val filter = _uiState.value.selectedFilter

        val expList = _uiState.value.allExperiences
        val knwList = _uiState.value.allKnowledge

        val results = mutableListOf<MemorySearchResultItem>()
        val seenIds = mutableSetOf<String>()

        // 1. Filter Knowledge
        if (filter == "ALL" || filter == "KNOWLEDGE" || filter == "VALIDATED") {
            for (k in knwList) {
                if (filter == "VALIDATED" && k.validationStatus != "validated_solution") continue
                if (!seenIds.add(k.id)) continue
                if (q.isNotEmpty()) {
                    val matchTitle = k.title.lowercase().contains(q)
                    val matchContent = k.content.lowercase().contains(q)
                    val matchTags = k.tags.any { it.lowercase().contains(q) }
                    if (!matchTitle && !matchContent && !matchTags) continue
                }
                results.add(
                    MemorySearchResultItem(
                        id = k.id,
                        itemType = "knowledge",
                        title = k.title,
                        summary = k.content.take(160),
                        validationStatus = k.validationStatus,
                        tags = k.tags
                    )
                )
            }
        }

        // 2. Filter Experiences
        if (filter == "ALL" || filter == "EXPERIENCES" || filter == "VALIDATED") {
            for (e in expList) {
                if (filter == "VALIDATED" && e.validationStatus != "validated_solution") continue
                if (!seenIds.add(e.id)) continue
                if (q.isNotEmpty()) {
                    val matchTitle = e.title.lowercase().contains(q)
                    val matchProb = e.problem.lowercase().contains(q)
                    val matchSol = e.solution.lowercase().contains(q)
                    val matchTags = e.tags.any { it.lowercase().contains(q) }
                    if (!matchTitle && !matchProb && !matchSol && !matchTags) continue
                }
                results.add(
                    MemorySearchResultItem(
                        id = e.id,
                        itemType = "experience",
                        title = e.title,
                        summary = "${e.problem} -> ${e.solution}".take(160),
                        agent = e.agent,
                        project = e.project,
                        validationStatus = e.validationStatus,
                        tags = e.tags
                    )
                )
            }
        }

        _uiState.update { it.copy(items = results) }
    }

    companion object {
        fun provideFactory(
            memoryRepository: MemoryRepository,
            botRepository: BotRepository? = null
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return MemoryViewModel(memoryRepository, botRepository) as T
                }
            }
    }
}
