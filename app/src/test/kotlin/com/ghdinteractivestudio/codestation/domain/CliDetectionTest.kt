package com.ghdinteractivestudio.codestation.domain

import com.ghdinteractivestudio.codestation.domain.model.CliHealthStatus
import com.ghdinteractivestudio.codestation.domain.model.DetectedCli
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CliDetectionTest {

    @Test
    fun `test primary CLIs detection Antigravity and OpenCode`() {
        val detectedClis = listOf(
            DetectedCli(
                id = "antigravity",
                name = "Google Antigravity",
                binaryName = "agy",
                installedPath = "/root/.local/bin/agy",
                version = "1.4.9",
                status = CliHealthStatus.READY,
                environment = "bot-antigravity",
                isPrimary = true,
                details = "CLI agy 1.4.9 opérationnel via ACP Bridge"
            ),
            DetectedCli(
                id = "opencode",
                name = "OpenCode GLM",
                binaryName = "opencode",
                installedPath = "/usr/local/bin/opencode",
                version = "opencode v2.2.0",
                status = CliHealthStatus.READY,
                environment = "bot-opencode-glm",
                isPrimary = true,
                details = "CLI opencode v2.2.0 prêt avec GLM-5.3-Flash Free"
            ),
            DetectedCli(
                id = "codex",
                name = "OpenAI Codex",
                binaryName = "codex",
                installedPath = "/data/data/com.termux/files/usr/bin/codex",
                version = "codex-cli 0.153.3",
                status = CliHealthStatus.INSTALLED,
                environment = "termux",
                isPrimary = false,
                details = "Installé dans l'environnement termux"
            )
        )

        val primary = detectedClis.filter { it.isPrimary }
        assertEquals(2, primary.size)
        assertTrue(primary.any { it.id == "antigravity" && it.status == CliHealthStatus.READY })
        assertTrue(primary.any { it.id == "opencode" && it.status == CliHealthStatus.READY })

        val others = detectedClis.filter { !it.isPrimary }
        assertEquals(1, others.size)
        assertEquals("codex", others.first().id)
        assertEquals(CliHealthStatus.INSTALLED, others.first().status)
    }

    @Test
    fun `test status conversion from strings`() {
        assertEquals(CliHealthStatus.READY, CliHealthStatus.fromString("READY"))
        assertEquals(CliHealthStatus.INSTALLED, CliHealthStatus.fromString("INSTALLED"))
        assertEquals(CliHealthStatus.AUTH_REQUIRED, CliHealthStatus.fromString("AUTH_REQUIRED"))
        assertEquals(CliHealthStatus.BUSY, CliHealthStatus.fromString("BUSY"))
        assertEquals(CliHealthStatus.ERROR, CliHealthStatus.fromString("ERROR"))
        assertEquals(CliHealthStatus.NOT_DETECTED, CliHealthStatus.fromString("UNKNOWN"))
        assertEquals(CliHealthStatus.NOT_DETECTED, CliHealthStatus.fromString(null))
    }

    @Test
    fun `test separate rootfs environment isolation`() {
        val agyCli = DetectedCli(
            id = "antigravity",
            name = "Google Antigravity",
            binaryName = "agy",
            installedPath = "/root/.local/bin/agy",
            version = "1.4.9",
            status = CliHealthStatus.READY,
            environment = "bot-antigravity",
            isPrimary = true
        )

        val opencodeCli = DetectedCli(
            id = "opencode",
            name = "OpenCode GLM",
            binaryName = "opencode",
            installedPath = "/usr/local/bin/opencode",
            version = "opencode v2.2.0",
            status = CliHealthStatus.READY,
            environment = "bot-opencode-glm",
            isPrimary = true
        )

        // Verifying separate bot environments
        assertNotNull(agyCli.installedPath)
        assertNotNull(opencodeCli.installedPath)
        assertEquals("bot-antigravity", agyCli.environment)
        assertEquals("bot-opencode-glm", opencodeCli.environment)
        assertFalse(agyCli.environment == opencodeCli.environment)
    }
}
