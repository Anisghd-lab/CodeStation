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
    @SerialName("completedAt") val completedAt: Double? = null,
    @SerialName("errorMessage") val errorMessage: String? = null,
    @SerialName("fallbackCli") val fallbackCli: String? = null,
    @SerialName("canRetry") val canRetry: Boolean = false
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
    @SerialName("waitingForTaskId") val waitingForTaskId: String? = null,
    @SerialName("waitingForTaskTitle") val waitingForTaskTitle: String? = null,
    @SerialName("waitingForAgentName") val waitingForAgentName: String? = null,
    @SerialName("dependencyStatus") val dependencyStatus: String? = null,
    @SerialName("waitStartedAt") val waitStartedAt: Double? = null,
    @SerialName("blockingReason") val blockingReason: String? = null,
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
    @SerialName("agents") val agents: List<CamcsAgent> = emptyList(),
    @SerialName("externalActions") val externalActions: List<CamcsExternalAction> = emptyList()
)

@Serializable
data class CamcsExternalAction(
    @SerialName("id") val id: String = "",
    @SerialName("action_type") val actionType: String = "",
    @SerialName("target_service") val targetService: String = "",
    @SerialName("target_resource") val targetResource: String = "",
    @SerialName("status") val status: String = "PLANNED",
    @SerialName("is_mandatory") val isMandatory: Boolean = true,
    @SerialName("verification_method") val verificationMethod: String = "",
    @SerialName("verified_at") val verifiedAt: Double? = null,
    @SerialName("error_message") val errorMessage: String? = null,
    @SerialName("account") val account: String? = null,
    @SerialName("scopes") val scopes: List<String> = emptyList(),
    @SerialName("auth_duration_ms") val authDurationMs: Double? = null,
    @SerialName("last_event") val lastEvent: String? = null,
    @SerialName("verified_url") val verifiedUrl: String? = null,
    @SerialName("owner") val owner: String? = null,
    @SerialName("repo_name") val repoName: String? = null,
    @SerialName("current_step") val currentStep: String? = null,
    @SerialName("completed_steps") val completedSteps: List<String> = emptyList()
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
    @SerialName("status") val status: String = "AVAILABLE", // ACTIVE, AVAILABLE, NOT_INSTALLED, BLOCKED
    @SerialName("isInstalled") val isInstalled: Boolean = true,
    @SerialName("activeAgent") val activeAgent: String? = null,
    @SerialName("accessibleModels") val accessibleModels: List<String> = emptyList(),
    @SerialName("activeSessions") val activeSessions: Int = 0,
    @SerialName("capabilities") val capabilities: List<String> = emptyList(),
    @SerialName("healthStatus") val healthStatus: String? = null,
    @SerialName("healthDetails") val healthDetails: String? = null,
    @SerialName("isOperational") val isOperational: Boolean = true,
    @SerialName("latencyMs") val latencyMs: Long? = null
)

@Serializable
data class CamcsEvent(
    @SerialName("id") val id: Long = 0L,
    @SerialName("missionId") val missionId: String? = null,
    @SerialName("agentId") val agentId: String? = null,
    @SerialName("agentName") val agentName: String? = null,
    @SerialName("type") val type: String,
    @SerialName("message") val message: String = "",
    @SerialName("status") val status: String? = null,
    @SerialName("timestamp") val timestamp: Double = 0.0
)

@Serializable
data class CamcsGitLastCommit(
    @SerialName("hash") val hash: String = "",
    @SerialName("shortHash") val shortHash: String = "",
    @SerialName("subject") val subject: String = "",
    @SerialName("date") val date: String = ""
)

@Serializable
data class CamcsGitStatus(
    @SerialName("ok") val ok: Boolean = false,
    @SerialName("path") val path: String = "",
    @SerialName("branch") val branch: String = "",
    @SerialName("upstream") val upstream: String = "",
    @SerialName("ahead") val ahead: Int = 0,
    @SerialName("behind") val behind: Int = 0,
    @SerialName("modified") val modified: List<String> = emptyList(),
    @SerialName("added") val added: List<String> = emptyList(),
    @SerialName("deleted") val deleted: List<String> = emptyList(),
    @SerialName("renamed") val renamed: List<String> = emptyList(),
    @SerialName("lastCommit") val lastCommit: CamcsGitLastCommit? = null,
    @SerialName("clean") val clean: Boolean = true,
    @SerialName("remoteAccessible") val remoteAccessible: Boolean = false,
    @SerialName("lastSyncAt") val lastSyncAt: Double = 0.0,
    @SerialName("generatedAt") val generatedAt: Double = 0.0,
    @SerialName("error") val error: String? = null
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

@Serializable
data class CamcsExternalService(
    @SerialName("service_id") val serviceId: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("endpoint") val endpoint: String = "",
    @SerialName("auth_type") val authType: String = "",
    @SerialName("status") val status: String = "NOT_TESTED",
    @SerialName("is_authenticated") val isAuthenticated: Boolean = false,
    @SerialName("detected_scopes") val detectedScopes: List<String> = emptyList(),
    @SerialName("rate_limit_remaining") val rateLimitRemaining: Int? = null,
    @SerialName("rate_limit_limit") val rateLimitLimit: Int? = null,
    @SerialName("latency_ms") val latencyMs: Double = 0.0,
    @SerialName("last_checked") val lastChecked: Double = 0.0,
    @SerialName("last_error") val lastError: String? = null,
    @SerialName("capabilities") val capabilities: List<String> = emptyList(),
    @SerialName("accessible_free_models") val accessibleFreeModels: List<String> = emptyList()
)

@Serializable
data class CamcsServicesResponse(
    @SerialName("ok") val ok: Boolean = true,
    @SerialName("services") val services: List<CamcsExternalService> = emptyList()
)
