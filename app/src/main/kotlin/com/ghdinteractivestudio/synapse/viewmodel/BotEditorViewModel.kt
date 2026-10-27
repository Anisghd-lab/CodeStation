package com.ghdinteractivestudio.synapse.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ghdinteractivestudio.synapse.domain.model.Bot
import com.ghdinteractivestudio.synapse.domain.model.PermissionPolicy
import com.ghdinteractivestudio.synapse.domain.model.ProviderType
import com.ghdinteractivestudio.synapse.domain.model.WorkspaceMode
import com.ghdinteractivestudio.synapse.domain.repository.BotRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class BotEditorUiState(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val description: String = "",
    val avatarGlyph: String = "circle",
    val avatarColorHex: String = "#00D2FF",
    val provider: ProviderType = ProviderType.CLAUDE_CODE,
    val model: String = ProviderType.CLAUDE_CODE.defaultModel,
    val isFastMode: Boolean = true,
    val workspaceMode: WorkspaceMode = WorkspaceMode.PERSONAL,
    val workspacePath: String = "/root",
    val instructions: String = "",
    val permissionPolicy: PermissionPolicy = PermissionPolicy.ASK_SHELL,
    val isLead: Boolean = false,
    val isPinned: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val isDeleted: Boolean = false,
    val errorMessage: String? = null
)

class BotEditorViewModel(
    private val existingBotId: String?,
    private val botRepository: BotRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BotEditorUiState())
    val uiState = _uiState.asStateFlow()

    init {
        if (!existingBotId.isNullOrBlank()) {
            viewModelScope.launch {
                val bot = botRepository.getBotById(existingBotId)
                if (bot != null) {
                    _uiState.value = BotEditorUiState(
                        id = bot.id,
                        name = bot.name,
                        description = bot.description,
                        avatarGlyph = bot.avatarGlyph,
                        avatarColorHex = bot.avatarColorHex,
                        provider = bot.provider,
                        model = bot.model,
                        isFastMode = bot.isFastMode,
                        workspaceMode = bot.workspaceMode,
                        workspacePath = bot.workspacePath,
                        instructions = bot.instructions,
                        permissionPolicy = bot.permissionPolicy,
                        isLead = bot.isLead,
                        isPinned = bot.isPinned
                    )
                }
            }
        }
    }

    fun onNameChanged(name: String) { _uiState.value = _uiState.value.copy(name = name) }
    fun onDescriptionChanged(desc: String) { _uiState.value = _uiState.value.copy(description = desc) }
    fun onAvatarColorChanged(hex: String) { _uiState.value = _uiState.value.copy(avatarColorHex = hex) }
    fun onAvatarGlyphChanged(glyph: String) { _uiState.value = _uiState.value.copy(avatarGlyph = glyph) }
    fun onProviderChanged(provider: ProviderType) {
        _uiState.value = _uiState.value.copy(provider = provider, model = provider.defaultModel)
    }
    fun onModelChanged(model: String) { _uiState.value = _uiState.value.copy(model = model) }
    fun onFastModeChanged(fast: Boolean) { _uiState.value = _uiState.value.copy(isFastMode = fast) }
    fun onWorkspaceModeChanged(mode: WorkspaceMode) { _uiState.value = _uiState.value.copy(workspaceMode = mode) }
    fun onWorkspacePathChanged(path: String) { _uiState.value = _uiState.value.copy(workspacePath = path) }
    fun onInstructionsChanged(inst: String) { _uiState.value = _uiState.value.copy(instructions = inst) }
    fun onPermissionPolicyChanged(policy: PermissionPolicy) { _uiState.value = _uiState.value.copy(permissionPolicy = policy) }

    fun saveBot() {
        val state = _uiState.value
        if (state.name.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Le nom de l'agent ne peut pas être vide.")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true, errorMessage = null)
            val bot = Bot(
                id = state.id,
                name = state.name.trim(),
                description = state.description.trim(),
                avatarGlyph = state.avatarGlyph,
                avatarColorHex = state.avatarColorHex,
                provider = state.provider,
                model = state.model,
                isFastMode = state.isFastMode,
                workspaceMode = state.workspaceMode,
                workspacePath = state.workspacePath.trim(),
                instructions = state.instructions.trim(),
                permissionPolicy = state.permissionPolicy,
                isLead = state.isLead,
                isPinned = state.isPinned
            )

            val result = if (existingBotId.isNullOrBlank()) {
                botRepository.createBot(bot)
            } else {
                botRepository.updateBot(bot)
            }

            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(isSaving = false, isSaved = true)
            } else {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Erreur de sauvegarde"
                )
            }
        }
    }

    fun deleteBot() {
        val state = _uiState.value
        viewModelScope.launch {
            val result = botRepository.deleteBot(state.id)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(isDeleted = true)
            } else {
                _uiState.value = _uiState.value.copy(errorMessage = result.exceptionOrNull()?.message)
            }
        }
    }

    companion object {
        fun provideFactory(
            botId: String?,
            botRepository: BotRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return BotEditorViewModel(botId, botRepository) as T
            }
        }
    }
}
