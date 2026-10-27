package com.ghdinteractivestudio.codestation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ghdinteractivestudio.codestation.domain.model.Bot
import com.ghdinteractivestudio.codestation.domain.model.BotStatus
import com.ghdinteractivestudio.codestation.domain.model.ServerConfig
import com.ghdinteractivestudio.codestation.domain.repository.BotRepository
import com.ghdinteractivestudio.codestation.domain.repository.ServerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
    val serverConfig: ServerConfig = ServerConfig(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val searchQuery: String = "",
    val activeFilter: BotFilter = BotFilter.ALL
) {
    val totalCount: Int get() = (if (leadBot != null) 1 else 0) + pinnedBots.size + otherBots.size
}

class RosterViewModel(
    private val botRepository: BotRepository,
    private val serverRepository: ServerRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _activeFilter = MutableStateFlow(BotFilter.ALL)
    private val _isRefreshing = MutableStateFlow(false)

    val uiState: StateFlow<RosterUiState> = combine(
        botRepository.getBotsStream(),
        serverRepository.getServerConfigStream(),
        _searchQuery,
        _activeFilter,
        _isRefreshing
    ) { bots, serverConfig, query, filter, refreshing ->
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
            isRefreshing = refreshing,
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

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            botRepository.refreshBots()
            serverRepository.testConnection()
            _isRefreshing.value = false
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
            serverRepository: ServerRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return RosterViewModel(botRepository, serverRepository) as T
            }
        }
    }
}
