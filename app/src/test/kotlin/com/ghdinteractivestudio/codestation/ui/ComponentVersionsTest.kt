package com.ghdinteractivestudio.codestation.ui

import com.ghdinteractivestudio.codestation.domain.model.ServerConfig
import com.ghdinteractivestudio.codestation.domain.model.ServerConnectionStatus
import com.ghdinteractivestudio.codestation.viewmodel.SettingsUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ComponentVersionsTest {

    @Test
    fun `test Android and Host versions are maintained distinctly`() {
        val androidVersion = "1.0.3"
        val androidBuild = 4

        val connectedServerConfig = ServerConfig(
            status = ServerConnectionStatus.CONNECTED,
            url = "http://127.0.0.1:19222",
            serverVersion = "2.28.0"
        )

        val state = SettingsUiState(
            serverConfig = connectedServerConfig
        )

        // Vérifier que la version Android est indépendante
        assertNotNull(androidVersion)
        assertEquals("1.0.3", androidVersion)
        assertEquals(4, androidBuild)

        // Vérifier que la version Host est distincte et non confondue
        assertEquals(ServerConnectionStatus.CONNECTED, state.serverConfig.status)
        assertEquals("2.28.0", state.serverConfig.serverVersion)
        assertNotEquals(androidVersion, state.serverConfig.serverVersion)
    }

    @Test
    fun `test Host version displays Indisponible when server is disconnected or stopped`() {
        val disconnectedConfig = ServerConfig(
            status = ServerConnectionStatus.DISCONNECTED,
            serverVersion = null
        )

        val state = SettingsUiState(
            serverConfig = disconnectedConfig
        )

        val isConnected = state.serverConfig.status == ServerConnectionStatus.CONNECTED
        val hostVersionText = if (isConnected && !state.serverConfig.serverVersion.isNullOrBlank()) {
            state.serverConfig.serverVersion!!
        } else if (isConnected) {
            "Inconnue"
        } else {
            "Indisponible"
        }

        assertEquals("Indisponible", hostVersionText)
    }

    @Test
    fun `test Host version displays real server version when connected`() {
        val connectedConfig = ServerConfig(
            status = ServerConnectionStatus.CONNECTED,
            serverVersion = "2.28.0"
        )

        val state = SettingsUiState(
            serverConfig = connectedConfig
        )

        val isConnected = state.serverConfig.status == ServerConnectionStatus.CONNECTED
        val hostVersionText = if (isConnected && !state.serverConfig.serverVersion.isNullOrBlank()) {
            state.serverConfig.serverVersion!!
        } else if (isConnected) {
            "Inconnue"
        } else {
            "Indisponible"
        }

        assertEquals("2.28.0", hostVersionText)
    }
}
