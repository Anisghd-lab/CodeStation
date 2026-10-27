package com.ghdinteractivestudio.synapse.data

import com.ghdinteractivestudio.synapse.data.local.ServerRepositoryImpl
import com.ghdinteractivestudio.synapse.data.preferences.PreferencesRepositoryImpl
import com.ghdinteractivestudio.synapse.data.remote.HealthResponse
import com.ghdinteractivestudio.synapse.data.remote.HelloResponse
import com.ghdinteractivestudio.synapse.data.remote.SynapseApiService
import com.ghdinteractivestudio.synapse.domain.model.ServerConnectionStatus
import com.ghdinteractivestudio.synapse.domain.model.ServerMode
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class ServerConnectionTest {

    private val preferencesRepository: PreferencesRepositoryImpl = mockk(relaxed = true)
    private val apiService: SynapseApiService = mockk(relaxed = true)

    @Before
    fun setUp() {
        every { preferencesRepository.serverUrlStream } returns flowOf("http://127.0.0.1:19222")
        every { preferencesRepository.pairingTokenStream } returns flowOf("test-token-xyz")
    }

    @Test
    fun `test server mode correctly identifies loopback, LAN, and remote`() = runTest {
        val repo = ServerRepositoryImpl(preferencesRepository) { apiService }

        val config = repo.getServerConfigStream().first()
        assertEquals("http://127.0.0.1:19222", config.url)
        assertEquals(ServerMode.LOCAL_LOOPBACK, config.mode)
        assertEquals("test-token-xyz", config.token)
    }

    @Test
    fun `test successful connection to health endpoint`() = runTest {
        val repo = ServerRepositoryImpl(preferencesRepository) { apiService }

        coEvery { apiService.getHealth() } returns Response.success(
            HealthResponse(ok = true, version = "2.28.0", busy = false)
        )
        coEvery { apiService.hello("Bearer test-token-xyz") } returns Response.success(
            HelloResponse(name = "termux-dev", version = "2.28.0")
        )

        val result = repo.testConnection()
        assertTrue(result.isSuccess)

        val connectedConfig = result.getOrThrow()
        assertEquals(ServerConnectionStatus.CONNECTED, connectedConfig.status)
        assertEquals("2.28.0", connectedConfig.serverVersion)
        assertTrue((connectedConfig.latencyMs ?: 0L) >= 0)
    }

    @Test
    fun `test connection failure on 401 unauthorized`() = runTest {
        val repo = ServerRepositoryImpl(preferencesRepository) { apiService }

        coEvery { apiService.getHealth() } returns Response.success(
            HealthResponse(ok = true, version = "2.28.0", busy = false)
        )
        coEvery { apiService.hello("Bearer test-token-xyz") } returns Response.error(
            401,
            "{}".toResponseBody()
        )

        val result = repo.testConnection()
        assertTrue(result.isSuccess)

        val errorConfig = result.getOrThrow()
        assertEquals(ServerConnectionStatus.ERROR, errorConfig.status)
        assertNotNull(errorConfig.errorMessage)
        assertTrue(errorConfig.errorMessage!!.contains("401"))
    }
}
