package com.ghdinteractivestudio.codestation.data

import com.ghdinteractivestudio.codestation.data.local.ServerRepositoryImpl
import com.ghdinteractivestudio.codestation.data.preferences.PreferencesRepositoryImpl
import com.ghdinteractivestudio.codestation.data.remote.CodeStationApiService
import com.ghdinteractivestudio.codestation.data.remote.DetectedCliDto
import com.ghdinteractivestudio.codestation.data.remote.EnvironmentDto
import com.ghdinteractivestudio.codestation.data.remote.EnvironmentsResponse
import com.ghdinteractivestudio.codestation.data.remote.SystemResourcesDto
import com.ghdinteractivestudio.codestation.domain.model.CliHealthStatus
import com.ghdinteractivestudio.codestation.domain.model.EnvironmentStatus
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class ContainerSyncAndCliTest {

    private val preferencesRepository: PreferencesRepositoryImpl = mockk(relaxed = true)
    private val apiService: CodeStationApiService = mockk(relaxed = true)

    @Before
    fun setUp() {
        every { preferencesRepository.serverUrlStream } returns flowOf("http://127.0.0.1:19222")
        every { preferencesRepository.pairingTokenStream } returns flowOf("test-token-abc")
    }

    @Test
    fun `test environments and CLIs parsed and emitted via environmentsStream`() = runTest {
        val repo = ServerRepositoryImpl(preferencesRepository) { apiService }

        val mockResponse = EnvironmentsResponse(
            environments = listOf(
                EnvironmentDto(
                    botId = "bot-antigravity",
                    engine = "antigravity",
                    model = "gemini-4-argon-medium",
                    status = "READY",
                    rootfs = "/root/CodeStation/environments/bot-antigravity/rootfs",
                    diskMb = 360
                ),
                EnvironmentDto(
                    botId = "bot-opencode-glm",
                    engine = "opencode",
                    model = "opencode/glm-5.3-flash-free",
                    status = "READY",
                    rootfs = "/root/CodeStation/environments/bot-opencode-glm/rootfs",
                    diskMb = 360
                )
            ),
            resources = SystemResourcesDto(
                ramTotalMb = 11170,
                ramAvailableMb = 2620,
                diskTotalGb = 222,
                diskAvailableGb = 48
            ),
            clis = listOf(
                DetectedCliDto(
                    id = "antigravity",
                    name = "Google Antigravity",
                    binary = "agy",
                    path = "/root/.local/bin/agy",
                    version = "1.4.10",
                    status = "READY",
                    isPrimary = true,
                    environment = "bot-antigravity"
                ),
                DetectedCliDto(
                    id = "opencode",
                    name = "OpenCode GLM",
                    binary = "opencode",
                    path = "/usr/local/bin/opencode",
                    version = "v2.2.1",
                    status = "READY",
                    isPrimary = true,
                    environment = "bot-opencode-glm"
                )
            )
        )

        coEvery { apiService.getEnvironments(any(), "Bearer test-token-abc") } returns Response.success(mockResponse)

        val result = repo.refreshEnvironments(force = true)
        assertTrue(result.isSuccess)

        val data = repo.environmentsStream.first { it.environments.isNotEmpty() }
        assertEquals(2, data.environments.size)
        assertEquals("bot-antigravity", data.environments[0].botId)
        assertEquals(EnvironmentStatus.READY, data.environments[0].status)

        assertEquals(2, data.clis.size)
        assertTrue(data.clis.any { it.id == "antigravity" && it.status == CliHealthStatus.READY && it.isPrimary })
        assertTrue(data.clis.any { it.id == "opencode" && it.status == CliHealthStatus.READY && it.isPrimary })

        assertEquals(11170L, data.resources.ramTotalMb)
        assertEquals(2620L, data.resources.ramAvailableMb)
        assertEquals(222L, data.resources.diskTotalGb)
        assertEquals(48L, data.resources.diskAvailableGb)
    }

    @Test
    fun `test network failure retains previous valid environments`() = runTest {
        val repo = ServerRepositoryImpl(preferencesRepository) { apiService }

        val mockResponse = EnvironmentsResponse(
            environments = listOf(
                EnvironmentDto(
                    botId = "bot-antigravity",
                    engine = "antigravity",
                    status = "READY",
                    diskMb = 360
                )
            ),
            resources = SystemResourcesDto(ramTotalMb = 8000, ramAvailableMb = 4000)
        )

        // 1. Initial successful refresh
        coEvery { apiService.getEnvironments(any(), any()) } returns Response.success(mockResponse)
        repo.refreshEnvironments(force = true)

        val initialData = repo.environmentsStream.first { it.environments.isNotEmpty() }
        assertEquals(1, initialData.environments.size)

        // 2. Network error on second refresh
        coEvery { apiService.getEnvironments(any(), any()) } throws java.io.IOException("Connection refused")
        val errorResult = repo.refreshEnvironments(force = true)

        // Result should succeed returning fallback cached data
        assertTrue(errorResult.isSuccess)
        val afterErrorData = repo.environmentsStream.first()

        // Cache must NOT be cleared!
        assertEquals(1, afterErrorData.environments.size)
        assertEquals("bot-antigravity", afterErrorData.environments[0].botId)
        assertNotNull(afterErrorData.errorMessage)
        assertTrue(afterErrorData.errorMessage!!.contains("injoignable"))
    }
}
