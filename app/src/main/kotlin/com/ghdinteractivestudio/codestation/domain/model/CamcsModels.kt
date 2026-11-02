package com.ghdinteractivestudio.codestation.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class CamcsAgentStatus {
    INITIALIZING,
    ANALYZING,
    PLANNING,
    WORKING,
    COMMUNICATING,
    VALIDATING,
    COMPLETED,
    FAILED,
    BLOCKED
}

@Serializable
enum class CamcsSubtaskStatus {
    PENDING,
    WORKING,
    COMPLETED,
    BLOCKED,
    FAILED
}

@Serializable
data class CamcsSubtask(
    @SerialName("id") val id: String,
    @SerialName("title") val title: String,
    @SerialName("status") val status: String = "PENDING",
    @SerialName("weight") val weight: Int = 25,
    @SerialName("criteria") val criteria: String? = null,
    @SerialName("agentId") val agentId: String? = null,
    @SerialName("pid") val pid: Int? = null,
    @SerialName("startedAt") val startedAt: Double? = null,
    @SerialName("completedAt") val completedAt: Double? = null
)

@Serializable
data class CamcsAgent(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("role") val role: String,
    @SerialName("shape") val shape: String = "squircle",
    @SerialName("color") val color: String = "green",
    @SerialName("cli") val cliId: String = "antigravity",
    @SerialName("provider") val providerId: String = "openrouter",
    @SerialName("model") val modelId: String = "cohere/north-mini-code:free",
    @SerialName("objective") val objective: String,
    @SerialName("currentSubtask") val currentSubtask: String = "Initialisation",
    @SerialName("progress") val progress: Int = 0,
    @SerialName("status") val status: String = "INITIALIZING",
    @SerialName("pid") val pid: Int? = null,
    @SerialName("startedAt") val startedAt: Double? = null,
    @SerialName("lastActivityAt") val lastActivityAt: Double? = null,
    @SerialName("lastOutput") val lastOutput: String? = null,
    @SerialName("activeTaskId") val activeTaskId: String? = null,
    @SerialName("tasks") val tasks: List<CamcsSubtask> = emptyList()
)

@Serializable
enum class CamcsGovernanceMode(
    val title: String,
    val description: String,
    val badge: String? = null
) {
    @SerialName("AUTONOMOUS")
    AUTONOMOUS(
        title = "Orchestration autonome",
        description = "CAMCS planifie, délègue et coordonne librement les sous-agents."
    ),

    @SerialName("HUMAN_VALIDATION")
    HUMAN_VALIDATION(
        title = "Validation humaine requise",
        description = "Demande une confirmation humaine avant toute modification de fichier ou exécution de commande CLI."
    ),

    @SerialName("HYBRID")
    HYBRID(
        title = "Mode hybride",
        description = "Fonctionnement autonome pour les tâches courantes.\nValidation humaine obligatoire pour les opérations sensibles.",
        badge = "Recommandé"
    );

    companion object {
        fun fromId(id: String?): CamcsGovernanceMode {
            return entries.firstOrNull { it.name.equals(id, ignoreCase = true) } ?: HYBRID
        }
    }
}

@Serializable
data class CamcsMission(
    @SerialName("id") val id: String,
    @SerialName("title") val title: String,
    @SerialName("objective") val objective: String,
    @SerialName("governanceMode") val governanceMode: String = "HYBRID",
    @SerialName("status") val status: String = "INITIALIZING",
    @SerialName("progress") val progress: Int = 0,
    @SerialName("orchestrationPhase") val orchestrationPhase: String = "PLANNING",
    @SerialName("createdAt") val createdAt: Double = 0.0,
    @SerialName("startedAt") val startedAt: Double? = null,
    @SerialName("completedAt") val completedAt: Double? = null,
    @SerialName("lastTransitionReason") val lastTransitionReason: String? = null,
    @SerialName("lastTransitionTime") val lastTransitionTime: Double? = null,
    @SerialName("blockedReason") val blockedReason: String? = null,
    @SerialName("agents") val agents: List<CamcsAgent> = emptyList()
)

@Serializable
data class CamcsStopTaskRequest(
    @SerialName("missionId") val missionId: String,
    @SerialName("taskId") val taskId: String? = null,
    @SerialName("agentId") val agentId: String? = null
)

@Serializable
data class CamcsStateTransition(
    @SerialName("id") val id: Long = 0,
    @SerialName("agentId") val agentId: String? = null,
    @SerialName("taskId") val taskId: String? = null,
    @SerialName("previousState") val previousState: String = "",
    @SerialName("currentState") val currentState: String = "",
    @SerialName("eventSource") val eventSource: String = "",
    @SerialName("reason") val reason: String = "",
    @SerialName("durationMs") val durationMs: Long = 0,
    @SerialName("createdAt") val createdAt: Double = 0.0
)

@Serializable
data class CamcsTransitionsResponse(
    @SerialName("ok") val ok: Boolean = true,
    @SerialName("missionId") val missionId: String = "",
    @SerialName("transitions") val transitions: List<CamcsStateTransition> = emptyList()
)

@Serializable
data class CamcsCliStatus(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("path") val path: String,
    @SerialName("version") val version: String,
    @SerialName("status") val status: String = "AVAILABLE", // ACTIVE, AVAILABLE, NOT_INSTALLED
    @SerialName("isInstalled") val isInstalled: Boolean = true,
    @SerialName("activeAgent") val activeAgent: String? = null,
    @SerialName("accessibleModels") val accessibleModels: List<String> = emptyList(),
    @SerialName("activeSessions") val activeSessions: Int = 0,
    @SerialName("capabilities") val capabilities: List<String> = emptyList()
)

@Serializable
data class CamcsEvent(
    @SerialName("id") val id: Long,
    @SerialName("missionId") val missionId: String? = null,
    @SerialName("agentId") val agentId: String? = null,
    @SerialName("type") val type: String,
    @SerialName("message") val message: String,
    @SerialName("timestamp") val timestamp: Double = 0.0
)

@Serializable
data class CamcsProviderStatus(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("status") val status: String = "OPERATIONAL",
    @SerialName("modelsCount") val modelsCount: Int = 0,
    @SerialName("freeModelsCount") val freeModelsCount: Int = 0,
    @SerialName("activeModel") val activeModel: String = "",
    @SerialName("totalRequests") val totalRequests: Int = 0,
    @SerialName("failedRequests") val failedRequests: Int = 0,
    @SerialName("averageLatencyMs") val averageLatencyMs: Double = 0.0,
    @SerialName("lastChecked") val lastChecked: Double = 0.0
)

@Serializable
data class CamcsMissionsResponse(
    @SerialName("missions") val missions: List<CamcsMission> = emptyList()
)

@Serializable
data class CamcsProvidersResponse(
    @SerialName("providers") val providers: List<CamcsProviderStatus> = emptyList(),
    @SerialName("clis") val clis: List<CamcsCliStatus> = emptyList()
)

@Serializable
data class CamcsClisResponse(
    @SerialName("clis") val clis: List<CamcsCliStatus> = emptyList()
)

@Serializable
data class CamcsCreateMissionRequest(
    @SerialName("title") val title: String,
    @SerialName("objective") val objective: String,
    @SerialName("governanceMode") val governanceMode: String = "HYBRID"
)

@Serializable
data class CamcsActionRequest(
    @SerialName("missionId") val missionId: String
)

@Serializable
data class CamcsCreateMissionResponse(
    @SerialName("ok") val ok: Boolean,
    @SerialName("missionId") val missionId: String? = null,
    @SerialName("status") val status: String? = null,
    @SerialName("agentsCount") val agentsCount: Int = 0
)

@Serializable
data class CamcsDeliverable(
    @SerialName("id") val id: String,
    @SerialName("missionId") val missionId: String,
    @SerialName("taskId") val taskId: String? = null,
    @SerialName("agentId") val agentId: String? = null,
    @SerialName("agentName") val agentName: String? = null,
    @SerialName("filePath") val filePath: String,
    @SerialName("fileName") val fileName: String,
    @SerialName("docType") val docType: String = "source_code",
    @SerialName("description") val description: String? = null,
    @SerialName("sha256") val sha256: String,
    @SerialName("sizeBytes") val sizeBytes: Long = 0L,
    @SerialName("validationStatus") val validationStatus: String = "VERIFIED",
    @SerialName("validationDetails") val validationDetails: String? = null,
    @SerialName("createdAt") val createdAt: Double = 0.0,
    @SerialName("updatedAt") val updatedAt: Double = 0.0
)

@Serializable
data class CamcsCommunication(
    @SerialName("id") val id: String,
    @SerialName("missionId") val missionId: String,
    @SerialName("senderAgentId") val senderAgentId: String? = null,
    @SerialName("senderName") val senderName: String? = null,
    @SerialName("receiverAgentId") val receiverAgentId: String? = null,
    @SerialName("receiverName") val receiverName: String? = null,
    @SerialName("messageType") val messageType: String,
    @SerialName("content") val content: String,
    @SerialName("status") val status: String = "SENT",
    @SerialName("impactOnWorkflow") val impactOnWorkflow: String? = null,
    @SerialName("createdAt") val createdAt: Double = 0.0
)

@Serializable
data class CamcsProof(
    @SerialName("id") val id: String,
    @SerialName("missionId") val missionId: String,
    @SerialName("taskId") val taskId: String? = null,
    @SerialName("agentId") val agentId: String? = null,
    @SerialName("agentName") val agentName: String? = null,
    @SerialName("cliCommand") val cliCommand: String? = null,
    @SerialName("workingDir") val workingDir: String? = null,
    @SerialName("exitCode") val exitCode: Int? = null,
    @SerialName("stdoutSummary") val stdoutSummary: String? = null,
    @SerialName("stderrSummary") val stderrSummary: String? = null,
    @SerialName("executionType") val executionType: String? = null,
    @SerialName("providerId") val providerId: String? = null,
    @SerialName("modelId") val modelId: String? = null,
    @SerialName("durationMs") val durationMs: Long = 0L,
    @SerialName("filesCreated") val filesCreated: List<String> = emptyList(),
    @SerialName("filesModified") val filesModified: List<String> = emptyList(),
    @SerialName("gitDiff") val gitDiff: String? = null,
    @SerialName("verifiedByAuditor") val verifiedByAuditor: Int = 1,
    @SerialName("auditorVerdict") val auditorVerdict: String? = null,
    @SerialName("createdAt") val createdAt: Double = 0.0
)

@Serializable
data class CamcsReport(
    @SerialName("id") val id: String? = null,
    @SerialName("missionId") val missionId: String,
    @SerialName("title") val title: String = "",
    @SerialName("initialObjective") val initialObjective: String = "",
    @SerialName("independentValidationSummary") val independentValidationSummary: String? = null,
    @SerialName("finalState") val finalState: String? = null,
    @SerialName("reportMarkdown") val reportMarkdown: String = "",
    @SerialName("createdAt") val createdAt: Double = 0.0
)

@Serializable
data class CamcsDeliverablesResponse(
    @SerialName("ok") val ok: Boolean = true,
    @SerialName("missionId") val missionId: String = "",
    @SerialName("deliverables") val deliverables: List<CamcsDeliverable> = emptyList()
)

@Serializable
data class CamcsCommunicationsResponse(
    @SerialName("ok") val ok: Boolean = true,
    @SerialName("missionId") val missionId: String = "",
    @SerialName("communications") val communications: List<CamcsCommunication> = emptyList()
)

@Serializable
data class CamcsProofsResponse(
    @SerialName("ok") val ok: Boolean = true,
    @SerialName("missionId") val missionId: String = "",
    @SerialName("proofs") val proofs: List<CamcsProof> = emptyList()
)

@Serializable
data class CamcsReportResponse(
    @SerialName("ok") val ok: Boolean = true,
    @SerialName("missionId") val missionId: String = "",
    @SerialName("report") val report: CamcsReport? = null
)

@Serializable
data class CamcsDiffResponse(
    @SerialName("ok") val ok: Boolean = true,
    @SerialName("missionId") val missionId: String = "",
    @SerialName("diff") val diff: String = ""
)

@Serializable
data class CamcsFileResponse(
    @SerialName("ok") val ok: Boolean = true,
    @SerialName("path") val path: String = "",
    @SerialName("fileName") val fileName: String = "",
    @SerialName("content") val content: String = "",
    @SerialName("sizeBytes") val sizeBytes: Long = 0L,
    @SerialName("sha256") val sha256: String = ""
)
