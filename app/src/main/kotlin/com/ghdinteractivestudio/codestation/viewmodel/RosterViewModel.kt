package com.ghdinteractivestudio.codestation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ghdinteractivestudio.codestation.domain.model.Bot
import com.ghdinteractivestudio.codestation.domain.model.BotStatus
import com.ghdinteractivestudio.codestation.domain.model.CollectiveMemoryStatus
import com.ghdinteractivestudio.codestation.domain.model.ServerConfig
import com.ghdinteractivestudio.codestation.domain.repository.BotRepository
import com.ghdinteractivestudio.codestation.domain.repository.CamcsRepository
import com.ghdinteractivestudio.codestation.domain.repository.MemoryRepository
import com.ghdinteractivestudio.codestation.domain.repository.ServerRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class BotFilter {
    ALL,
    CONNECTED,
    OFFLINE
}

data class RosterUiState(
    val leadBot: Bot? = null,
    val pinnedBots: List<Bot> = emptyList(),
    val otherBots: List<Bot> = emptyList(),
    val memoryStatus: CollectiveMemoryStatus = CollectiveMemoryStatus(),
    val serverConfig: ServerConfig = ServerConfig(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val refreshError: String? = null,
    val searchQuery: String = "",
    val activeFilter: BotFilter = BotFilter.ALL
) {
    val totalCount: Int get() = (if (leadBot != null) 1 else 0) + pinnedBots.size + otherBots.size
}

/** État interne du Pull-to-Refresh : actualisation en cours + dernière erreur réelle remontée. */
data class RefreshState(
    val isRefreshing: Boolean = false,
    val error: String? = null
)

class RosterViewModel(
    private val botRepository: BotRepository,
    private val serverRepository: ServerRepository,
    private val memoryRepository: MemoryRepository? = null,
    private val camcsRepository: CamcsRepository? = null
) : ViewModel() {

    private var refreshJob: Job? = null
    private val _searchQuery = MutableStateFlow("")
    private val _activeFilter = MutableStateFlow(BotFilter.ALL)
    // État du Pull-to-Refresh : indicateur + dernière erreur réelle (un seul flux pour combine)
    private val _refreshState = MutableStateFlow(RefreshState())
    private val _memoryStatus = MutableStateFlow(CollectiveMemoryStatus())
    val memoryStatus: StateFlow<CollectiveMemoryStatus> = _memoryStatus.asStateFlow()

    init {
        viewModelScope.launch {
            memoryRepository?.refreshStatus()
        }
        viewModelScope.launch {
            memoryRepository?.memoryStatus?.collect { status ->
                _memoryStatus.value = status
            }
        }
    }

    val uiState: StateFlow<RosterUiState> = combine(
        botRepository.getBotsStream(),
        serverRepository.getServerConfigStream(),
        _searchQuery,
        _activeFilter,
        _refreshState
    ) { bots, serverConfig, query, filter, refresh ->
        // Filtrage par texte de recherche
        val filtered = bots.filter { bot ->
            query.isBlank() || bot.name.contains(query, ignoreCase = true) || bot.description.contains(query, ignoreCase = true)
        }.filter { bot ->
            when (filter) {
                BotFilter.ALL -> true
                BotFilter.CONNECTED -> bot.status != BotStatus.OFFLINE
                BotFilter.OFFLINE -> bot.status == BotStatus.OFFLINE
            }
        }

        val lead = filtered.firstOrNull { it.isLead }
        val pinned = filtered.filter { it.isPinned && !it.isLead }
        val others = filtered.filter { !it.isPinned && !it.isLead }

        RosterUiState(
            leadBot = lead,
            pinnedBots = pinned,
            otherBots = others,
            serverConfig = serverConfig,
            isLoading = false,
            isRefreshing = refresh.isRefreshing,
            refreshError = refresh.error,
            searchQuery = query,
            activeFilter = filter
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RosterUiState(isLoading = true)
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onFilterSelected(filter: BotFilter) {
        _activeFilter.value = filter
    }

    /**
     * Actualisation réelle du Master : agents, connexion hôte, mémoire collective et missions CAMCS.
     * Un seul rafraîchissement peut être en vol à la fois afin d'éviter toute requête dupliquée
     * (geste de pull-to-refresh répété, tap sur « Tester / Reconnecter », etc.).
     */
    fun refresh() {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            _refreshState.value = RefreshState(isRefreshing = true, error = null)
            val errors = mutableListOf<String>()
            try {
                safely("Bots", errors) { botRepository.refreshBots() }
                safely("Hôte", errors) { serverRepository.testConnection() }
                memoryRepository?.let { repo -> safely("Mémoire", errors) { repo.refreshStatus() } }
                camcsRepository?.let { repo -> safely("CAMCS", errors) { repo.refreshMissions() } }
            } finally {
                _refreshState.value = RefreshState(
                    isRefreshing = false,
                    error = errors.takeIf { it.isNotEmpty() }?.joinToString(" · ")
                )
            }
        }
    }

    /** Masque le bandeau d'erreur d'actualisation (action explicite de l'utilisateur). */
    fun dismissRefreshError() {
        _refreshState.value = _refreshState.value.copy(error = null)
    }

    /**
     * Isole chaque source : une erreur réseau (hôte absent, timeout) ne doit ni interrompre
     * l'actualisation des autres sources, ni remonter jusqu'au scope du ViewModel.
     * Les échecs (exception OU `Result.failure`) sont collectés dans [errors] pour être affichés :
     * les données déjà affichées sont conservées, mais l'erreur n'est jamais masquée.
     */
    private suspend fun safely(
        source: String,
        errors: MutableList<String>,
        block: suspend () -> Result<*>
    ) {
        try {
            block().onFailure { errors += "$source : ${it.message ?: it::class.simpleName}" }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            errors += "$source : ${e.message ?: e::class.simpleName}"
        }
    }

    fun togglePin(botId: String) {
        viewModelScope.launch {
            botRepository.togglePin(botId)
        }
    }

    fun deleteBot(botId: String) {
        viewModelScope.launch {
            botRepository.deleteBot(botId)
        }
    }

    fun stopBot(botId: String) {
        viewModelScope.launch {
            botRepository.stopBotTurn(botId)
        }
    }

    companion object {
        fun provideFactory(
            botRepository: BotRepository,
            serverRepository: ServerRepository,
            memoryRepository: MemoryRepository? = null,
            camcsRepository: CamcsRepository? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return RosterViewModel(botRepository, serverRepository, memoryRepository, camcsRepository) as T
            }
        }
    }
}
