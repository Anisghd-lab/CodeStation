package com.ghdinteractivestudio.synapse.domain.model

enum class ApprovalActionType(val label: String) {
    SHELL_COMMAND("Commande Shell (Bash)"),
    FILE_WRITE("Écriture / Modification de fichier"),
    NETWORK_ACCESS("Accès réseau externe")
}

enum class ApprovalStatus {
    PENDING,
    APPROVED,
    REJECTED
}

data class ApprovalRequest(
    val id: String,
    val botId: String,
    val botName: String,
    val actionType: ApprovalActionType,
    val commandOrPath: String,
    val diffPreview: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val status: ApprovalStatus = ApprovalStatus.PENDING,
    val isAlwaysApproved: Boolean = false
)
