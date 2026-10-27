package com.ghdinteractivestudio.codestation.domain.model

enum class BotStatus {
    IDLE,
    WORKING,
    BUSY,
    OFFLINE
}

enum class ProviderType(val displayName: String, val defaultModel: String) {
    CLAUDE_CODE("Claude Code", "claude-3-7-sonnet"),
    OPENAI_CODEX("OpenAI Codex", "gpt-4o"),
    GEMINI_CLI("Google Gemini CLI", "gemini-2.0-flash"),
    OPENCODE("OpenCode", "deepseek-coder-v2"),
    ACP_LOCAL("ACP Local Agent", "local-qwen-2.5"),
    CUSTOM("Custom Agent", "default")
}

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
