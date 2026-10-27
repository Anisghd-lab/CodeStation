package com.ghdinteractivestudio.synapse.domain.model

enum class ServerMode(val displayName: String) {
    LOCAL_LOOPBACK("Boucle Locale Termux (127.0.0.1)"),
    LAN_HOST("Hôte Réseau Local (Wi-Fi)"),
    REMOTE_GATEWAY("Passerelle Distante Sécurisée")
}

enum class ServerConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

data class ServerConfig(
    val url: String = "http://127.0.0.1:19222",
    val token: String? = null,
    val mode: ServerMode = ServerMode.LOCAL_LOOPBACK,
    val status: ServerConnectionStatus = ServerConnectionStatus.DISCONNECTED,
    val latencyMs: Long? = null,
    val serverVersion: String? = null,
    val activeBotCount: Int = 0,
    val errorMessage: String? = null,
    val lastConnectedAt: Long? = null
)
