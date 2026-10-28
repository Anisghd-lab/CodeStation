package com.ghdinteractivestudio.codestation.domain.model

enum class BotStatus {
    IDLE,
    WORKING,
    BUSY,
    OFFLINE
}

enum class ProviderType(val displayName: String, val defaultModel: String, val backendId: String) {
    CLAUDE_CODE("Claude Code", "claude-3-7-sonnet", "claude"),
    OPENAI_CODEX("OpenAI Codex", "gpt-4o", "codex"),
    GEMINI_CLI("Google Gemini CLI", "gemini-2.0-flash", "gemini"),
    ANTIGRAVITY("Google Antigravity", "gemini-4-argon-medium", "antigravity"),
    OPENCODE("OpenCode", "opencode/glm-5.3-flash-free", "opencode"),
    ACP_LOCAL("ACP Local Agent", "local-qwen-2.5", "acp"),
    CUSTOM("Custom Agent", "default", "custom");

    fun getAvailableModels(): List<ModelInfo> {
        return when (this) {
            OPENCODE -> listOf(
                ModelInfo("opencode/glm-5.3-flash-free", "GLM-5.3-Flash Free", isFree = true, isDefault = true),
                ModelInfo("opencode/deepseek-v4.1-flash-free", "DeepSeek V4.1 Flash Free", isFree = true),
                ModelInfo("opencode/beam-free", "Beam Free", isFree = true),
                ModelInfo("opencode/flint-40b-free", "Flint 40B Free", isFree = true),
                ModelInfo("opencode/mimo-v2.6-flash-free", "MiMo V2.6 Flash Free", isFree = true),
                ModelInfo("opencode/nemotron-3-ultra-free", "Nemotron 3 Ultra Free", isFree = true),
                ModelInfo("deepseek/deepseek-v4-pro", "DeepSeek V4 Pro", isFree = false)
            )
            CLAUDE_CODE -> listOf(
                ModelInfo("claude-3-7-sonnet", "Claude 3.7 Sonnet", isFree = false, isDefault = true),
                ModelInfo("claude-3-5-sonnet", "Claude 3.5 Sonnet", isFree = false),
                ModelInfo("claude-3-5-haiku", "Claude 3.5 Haiku", isFree = false)
            )
            OPENAI_CODEX -> listOf(
                ModelInfo("gpt-4o", "GPT-4o", isFree = false, isDefault = true),
                ModelInfo("gpt-4o-mini", "GPT-4o Mini", isFree = false),
                ModelInfo("o3-mini", "o3-mini", isFree = false)
            )
            GEMINI_CLI -> listOf(
                ModelInfo("gemini-2.0-flash", "Gemini 2.0 Flash", isFree = false, isDefault = true),
                ModelInfo("gemini-2.5-pro", "Gemini 2.5 Pro", isFree = false)
            )
            ANTIGRAVITY -> listOf(
                ModelInfo("gemini-4-argon-medium", "Gemini 4 Argon (Medium)", isFree = false, isDefault = true, isVerified = true),
                ModelInfo("gemini-4-argon-high", "Gemini 4 Argon (High)", isFree = false, isVerified = true),
                ModelInfo("gemini-4-argon-low", "Gemini 4 Argon (Low)", isFree = false, isVerified = true),
                ModelInfo("gemini-3.8-flash-high", "Gemini 3.8 Flash (High)", isFree = false, isVerified = true),
                ModelInfo("gemini-3.8-flash-medium", "Gemini 3.8 Flash (Medium)", isFree = false, isVerified = true),
                ModelInfo("gemini-3.8-flash-low", "Gemini 3.8 Flash (Low)", isFree = false, isVerified = true),
                ModelInfo("claude-opus-5-5-low", "Claude Opus 5.5 (Low)", isFree = false, isVerified = true),
                ModelInfo("claude-opus-5-5-medium", "Claude Opus 5.5 (Medium)", isFree = false, isVerified = true),
                ModelInfo("claude-opus-5-5-high", "Claude Opus 5.5 (High)", isFree = false, isVerified = true),
                ModelInfo("claude-sonnet-5-5-low", "Claude Sonnet 5.5 (Low)", isFree = false, isVerified = true),
                ModelInfo("claude-sonnet-5-5-medium", "Claude Sonnet 5.5 (Medium)", isFree = false, isVerified = true),
                ModelInfo("claude-sonnet-5-5-high", "Claude Sonnet 5.5 (High)", isFree = false, isVerified = true),
                ModelInfo("gpt-oss-120b-medium", "GPT-OSS 120B (Medium)", isFree = false, isVerified = true)
            )
            ACP_LOCAL -> listOf(
                ModelInfo("local-qwen-2.5", "Qwen 2.5 (Local)", isFree = true, isDefault = true)
            )
            CUSTOM -> listOf(
                ModelInfo("default", "Modèle Personnalisé", isFree = true, isDefault = true)
            )
        }
    }
}

data class ModelInfo(
    val id: String,
    val displayName: String,
    val isFree: Boolean = false,
    val isDefault: Boolean = false,
    val isVerified: Boolean = true
)

enum class WorkspaceMode(val displayName: String) {
    PERSONAL("Bac à sable personnel"),
    PROJECT_FOLDER("Répertoire de projet")
}

enum class PermissionPolicy(val displayName: String) {
    AUTONOMOUS("Autonome (Approbation auto)"),
    ASK_SHELL("Demander pour les commandes shell"),
    ASK_FILES("Demander pour l'écriture de fichiers"),
    READ_ONLY("Lecture seule (Strict)")
}

data class Bot(
    val id: String,
    val name: String,
    val description: String = "",
    val avatarGlyph: String = "circle",
    val avatarColorHex: String = "#00D2FF",
    val provider: ProviderType = ProviderType.CLAUDE_CODE,
    val model: String = provider.defaultModel,
    val isFastMode: Boolean = true,
    val workspaceMode: WorkspaceMode = WorkspaceMode.PERSONAL,
    val workspacePath: String = "/root",
    val instructions: String = "",
    val permissionPolicy: PermissionPolicy = PermissionPolicy.ASK_SHELL,
    val isLead: Boolean = false,
    val isPinned: Boolean = false,
    val status: BotStatus = BotStatus.IDLE,
    val lastMessage: String? = null,
    val lastActivityAt: Long = System.currentTimeMillis(),
    val unreadCount: Int = 0,
    val pendingApprovalsCount: Int = 0,
    val memberBotIds: List<String> = emptyList() // Pour les salons multi-bots
) {
    val isGroup: Boolean get() = memberBotIds.isNotEmpty()
}
