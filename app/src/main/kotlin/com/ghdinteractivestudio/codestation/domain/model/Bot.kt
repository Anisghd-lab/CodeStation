package com.ghdinteractivestudio.codestation.domain.model

enum class BotStatus {
    IDLE,
    WORKING,
    BUSY,
    OFFLINE
}

enum class ProviderType(val displayName: String, val defaultModel: String, val backendId: String) {
    OMNIRUSH("OmniRush", "gpt-6-astra", "omnirush"),
    CLIXAD("Clixad", "gpt-5-nano", "clixad"),
    FREEBUFF("Freebuff", "freebuff/free-auto", "freebuff"),
    ANTIGRAVITY("Google Antigravity", "gemini-3.8-flash-medium", "antigravity"),
    OPENCODE("OpenCode", "opencode/glm-5.3-flash-free", "opencode"),
    CODEBUDDY("CodeBuddy Code", "Hy3", "codebuddy"),
    KIMI("Kimi Code", "openrouter/nemotron-3-5-lightning", "kimi"),
    OPENROUTER("OpenRouter", "nvidia/nemotron-3.5-lightning:free", "openrouter"),
    XKIRO("xKIRO / Okiro", "mistralai/ministral-14b", "xkiro"),
    VERCEL_AI_GATEWAY("Vercel AI Gateway", "nvidia/nemotron-3.5-lightning-free", "vercel"),
    BYTEZ("Bytez Serverless AI", "Qwen/Qwen2.5-7B-Instruct", "bytez"),
    CLAUDE_CODE("Claude Code", "claude-3-7-sonnet", "claude"),
    OPENAI_CODEX("OpenAI Codex", "gpt-4o", "codex"),
    GEMINI_CLI("Google Gemini CLI", "gemini-2.0-flash", "gemini"),
    ACP_LOCAL("ACP Local Agent", "local-qwen-2.5", "acp"),
    CUSTOM("Custom Agent", "default", "custom");

    fun getAvailableModels(): List<ModelInfo> {
        return when (this) {
            VERCEL_AI_GATEWAY -> listOf(
                ModelInfo("nvidia/nemotron-3.5-lightning-free", "Nemotron 3.5 Lightning Free (1M)", isFree = true, isDefault = true, isVerified = true),
                ModelInfo("reflection/beam-free", "Beam 1.0 Free (1M)", isFree = true, isVerified = true),
                ModelInfo("humansand/ampersand-v0.1", "Ampersand v0.1 Free (1M)", isFree = true, isVerified = true),
                ModelInfo("stealth/glyph-cluster", "Glyph Cluster Free (256k)", isFree = true, isVerified = true),
                ModelInfo("parallel/parallel-decisions-fast", "Decisions Fast Free (32k)", isFree = true, isVerified = true)
            )
            BYTEZ -> listOf(
                ModelInfo("Qwen/Qwen2.5-7B-Instruct", "Qwen 2.5 7B Instruct (Free)", isFree = true, isDefault = true, isVerified = true),
                ModelInfo("microsoft/Phi-3-mini-4k-instruct", "Phi-3 Mini 4K (Free)", isFree = true, isVerified = true),
                ModelInfo("google/gemma-2-2b-it", "Gemma 2 2B IT (Free)", isFree = true, isVerified = true),
                ModelInfo("mistralai/Mistral-7B-Instruct-v0.3", "Mistral 7B Instruct v0.3 (Free)", isFree = true, isVerified = true),
                ModelInfo("meta-llama/Llama-3.2-3B-Instruct", "Llama 3.2 3B Instruct (Free)", isFree = true, isVerified = true),
                ModelInfo("meta-llama/Llama-3.2-1B-Instruct", "Llama 3.2 1B Instruct (Free)", isFree = true, isVerified = true)
            )
            FREEBUFF -> listOf(
                ModelInfo("freebuff/free-auto", "Freebuff Auto (100% Free)", isFree = true, isDefault = true, isVerified = true)
            )
            OMNIRUSH -> listOf(
                ModelInfo("gpt-6-astra", "GPT-6 Astra (Free Tier Included)", isFree = true, isDefault = true, isVerified = true),
                ModelInfo("gpt-6-sol", "GPT-6 Sol", isFree = true, isVerified = true)
            )
            CLIXAD -> listOf(
                ModelInfo("gpt-5-nano", "GPT-5 Nano (Ad-Funded Free)", isFree = true, isDefault = true, isVerified = true)
            )
            CODEBUDDY -> listOf(
                ModelInfo("Hy3", "Hy3 (Free)", isFree = true, isDefault = true, isVerified = true),
                ModelInfo("deepseek-v4.1-flash", "DeepSeek V4.1 Flash (Free)", isFree = true, isVerified = true),
                ModelInfo("glm-5.3-flash", "GLM-5.3 Flash (Free)", isFree = true, isVerified = true)
            )
            KIMI -> listOf(
                ModelInfo("openrouter/nemotron-3-5-lightning", "Nemotron 3.5 Lightning (Free)", isFree = true, isDefault = true, isVerified = true),
                ModelInfo("openrouter/flint-v1", "Flint v1.0 (Free)", isFree = true, isVerified = true),
                ModelInfo("openrouter/nemotron-3-ultra", "Nemotron 3 Ultra (Free)", isFree = true, isVerified = true),
                ModelInfo("openrouter/north-mini-code", "Cohere North Mini Code (Free)", isFree = true, isVerified = true),
                ModelInfo("vercel/nemotron-3-5-lightning-free", "Vercel Nemotron Free", isFree = true, isVerified = true),
                ModelInfo("xkiro/ministral-14b", "xKIRO Ministral Free", isFree = true, isVerified = true),
                ModelInfo("kimi-code/kimi-for-coding", "K2.8 Preview (Free Included)", isFree = true, isVerified = true),
                ModelInfo("kimi-code/k3", "K3 (Free Tier)", isFree = true, isVerified = true)
            )
            OPENROUTER -> listOf(
                ModelInfo("nvidia/nemotron-3.5-lightning:free", "Nemotron 3.5 Lightning (Free 1M)", isFree = true, isDefault = true, isVerified = true),
                ModelInfo("nvidia/nemotron-3-ultra-550b-a55b:free", "Nemotron 3 Ultra 550B (Free 1M)", isFree = true, isVerified = true),
                ModelInfo("nvidia/nemotron-3.5-super-vl:free", "Nemotron 3.5 Super VL (Free 1M)", isFree = true, isVerified = true),
                ModelInfo("cohere/north-mini-code:free", "Cohere North Mini Code (Free)", isFree = true, isVerified = true),
                ModelInfo("dots-studio/dots-3-note-preview:free", "Dots 3 Note Preview (Free)", isFree = true, isVerified = true),
                ModelInfo("baidu/ernie-5.5-flash-preview:free", "ERNIE 5.5 Flash (Free 1M)", isFree = true, isVerified = true),
                ModelInfo("google/gemma-4-26b-a4b-it:free", "Gemma 4 26B (Free)", isFree = true, isVerified = true),
                ModelInfo("google/gemma-4-31b-it:free", "Gemma 4 31B (Free)", isFree = true, isVerified = true),
                ModelInfo("liquid/lfm-3-1.7b:free", "LFM 3 1.7B (Free)", isFree = true, isVerified = true),
                ModelInfo("recursive/flint-v1.0:free", "Flint v1.0 (Free)", isFree = true, isVerified = true),
                ModelInfo("thinkingmachines/inkling-2-small:free", "Inkling 2 Small (Free)", isFree = true, isVerified = true),
                ModelInfo("particle-ai/particle-1:free", "Particle 1 (Free)", isFree = true, isVerified = true),
                ModelInfo("openrouter/free", "OpenRouter Auto Free Router", isFree = true, isVerified = true)
            )
            XKIRO -> listOf(
                ModelInfo("mistralai/ministral-14b", "Ministral 3 14B Free (256k)", isFree = true, isDefault = true, isVerified = true),
                ModelInfo("mistralai/codestral-2508", "Codestral Free (256k)", isFree = true, isVerified = true),
                ModelInfo("mistralai/devstral-medium", "Devstral 2 Free (256k)", isFree = true, isVerified = true),
                ModelInfo("mistralai/mistral-large-2512", "Mistral Large 3 Free (256k)", isFree = true, isVerified = true),
                ModelInfo("cohere/north-mini-code", "North Mini Code Free (256k)", isFree = true, isVerified = true),
                ModelInfo("cohere/command-r-plus-08-2024", "Command R+ Free (128k)", isFree = true, isVerified = true),
                ModelInfo("cohere/command-a", "Command A Free (288k)", isFree = true, isVerified = true),
                ModelInfo("cohere/command-a-plus", "Command A+ Free (436k)", isFree = true, isVerified = true),
                ModelInfo("sensenova/sensenova-6.8-flash-lite", "SenseNova 6.8 Flash-Lite Free", isFree = true, isVerified = true),
                ModelInfo("stealth/big-pickle", "Big Pickle Free (200k)", isFree = true, isVerified = true)
            )
            OPENCODE -> listOf(
                ModelInfo("opencode/glm-5.3-flash-free", "GLM-5.3-Flash Free", isFree = true, isDefault = true),
                ModelInfo("opencode/deepseek-v4.1-flash-free", "DeepSeek V4.1 Flash Free", isFree = true),
                ModelInfo("opencode/beam-free", "Beam Free", isFree = true),
                ModelInfo("openrouter-free/nvidia/nemotron-3.5-lightning:free", "Nemotron 3.5 Lightning Free (1M)", isFree = true),
                ModelInfo("vercel-free/nvidia/nemotron-3.5-lightning-free", "Vercel Nemotron Free (1M)", isFree = true),
                ModelInfo("opencode/flint-40b-free", "Flint 40B Free", isFree = true),
                ModelInfo("opencode/mimo-v2.6-flash-free", "MiMo V2.6 Flash Free", isFree = true),
                ModelInfo("opencode/nemotron-3-ultra-free", "Nemotron 3 Ultra Free", isFree = true),
                ModelInfo("ollama/qwen2.5-coder:3b", "Qwen 2.5 Coder 3B (Local Free)", isFree = true)
            )
            CLAUDE_CODE -> listOf(
                ModelInfo("claude-3-7-sonnet", "Claude 3.7 Sonnet", isFree = false, isDefault = true)
            )
            OPENAI_CODEX -> listOf(
                ModelInfo("gpt-4o-mini", "GPT-4o Mini", isFree = false, isDefault = true)
            )
            GEMINI_CLI -> listOf(
                ModelInfo("gemini-2.0-flash", "Gemini 2.0 Flash", isFree = true, isDefault = true)
            )
            ANTIGRAVITY -> listOf(
                ModelInfo("gemini-3.8-flash-medium", "Gemini 3.8 Flash (Medium)", isFree = true, isDefault = true, isVerified = true),
                ModelInfo("gemini-3.8-flash-high", "Gemini 3.8 Flash (High)", isFree = true, isVerified = true),
                ModelInfo("gemini-3.8-flash-low", "Gemini 3.8 Flash (Low)", isFree = true, isVerified = true),
                ModelInfo("claude-sonnet-4-6", "Claude Sonnet 4.6 (Thinking)", isFree = true, isVerified = true),
                ModelInfo("claude-opus-4-6-thinking", "Claude Opus 4.6 (Thinking)", isFree = true, isVerified = true),
                ModelInfo("gpt-oss-120b-medium", "GPT-OSS 120B (Medium)", isFree = true, isVerified = true)
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
    val provider: ProviderType = ProviderType.OPENCODE,
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
    val memberBotIds: List<String> = emptyList()
) {
    val isGroup: Boolean get() = memberBotIds.isNotEmpty()
}
