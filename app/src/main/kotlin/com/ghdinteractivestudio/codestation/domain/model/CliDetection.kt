package com.ghdinteractivestudio.codestation.domain.model

enum class CliHealthStatus(val displayName: String) {
    NOT_INSTALLED("Non installée"),
    NOT_DETECTED("Non détectée"),
    INSTALLED("Installée"),
    AVAILABLE("Disponible"),
    AUTHENTICATED("Authentifiée"),
    NOT_AUTHENTICATED("Non authentifiée"),
    AUTH_REQUIRED("Authentification à vérifier"),
    READY("Prête"),
    BUSY("En cours"),
    ERROR("Erreur");

    companion object {
        fun fromString(str: String?): CliHealthStatus {
            return when (str?.uppercase()) {
                "AUTHENTICATED" -> AUTHENTICATED
                "AVAILABLE" -> AVAILABLE
                "READY" -> READY
                "INSTALLED" -> INSTALLED
                "NOT_AUTHENTICATED" -> NOT_AUTHENTICATED
                "AUTH_REQUIRED" -> AUTH_REQUIRED
                "NOT_INSTALLED" -> NOT_INSTALLED
                "BUSY" -> BUSY
                "ERROR" -> ERROR
                else -> NOT_DETECTED
            }
        }
    }
}

data class DetectedCli(
    val id: String,
    val name: String,
    val binaryName: String,
    val installedPath: String?,
    val version: String?,
    val status: CliHealthStatus,
    val environment: String,
    val isPrimary: Boolean = false,
    val details: String? = null
)
