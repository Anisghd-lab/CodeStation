package com.ghdinteractivestudio.codestation.domain.model

enum class EnvironmentStatus(val label: String) {
    NOT_INSTALLED("Non installé"),
    STOPPED("Arrêté"),
    CREATING("Création en cours"),
    INSTALLING("Installation en cours"),
    STARTING("Démarrage en cours"),
    READY("Prêt"),
    BUSY("Occupé"),
    ERROR("Erreur"),
    STOPPING("Arrêt en cours");

    companion object {
        fun fromString(value: String?): EnvironmentStatus {
            return when (value?.uppercase()) {
                "READY" -> READY
                "STOPPED" -> STOPPED
                "CREATING" -> CREATING
                "INSTALLING" -> INSTALLING
                "STARTING" -> STARTING
                "BUSY" -> BUSY
                "ERROR" -> ERROR
                "STOPPING" -> STOPPING
                else -> NOT_INSTALLED
            }
        }
    }
}

data class BotEnvironment(
    val botId: String,
    val engine: String,
    val model: String? = null,
    val status: EnvironmentStatus = EnvironmentStatus.NOT_INSTALLED,
    val rootfsPath: String? = null,
    val diskMb: Int = 0,
    val lastError: String? = null
)

data class SystemResources(
    val ramTotalMb: Long = 0,
    val ramAvailableMb: Long = 0,
    val diskTotalGb: Long = 0,
    val diskAvailableGb: Long = 0
)
