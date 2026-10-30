package com.ghdinteractivestudio.codestation

import android.content.Context
import com.ghdinteractivestudio.codestation.data.local.AntigravityModelRepositoryImpl
import com.ghdinteractivestudio.codestation.data.local.BotRepositoryImpl
import com.ghdinteractivestudio.codestation.data.local.ChatRepositoryImpl
import com.ghdinteractivestudio.codestation.data.local.ServerRepositoryImpl
import com.ghdinteractivestudio.codestation.data.local.CodeStationDatabase
import com.ghdinteractivestudio.codestation.data.preferences.PreferencesRepositoryImpl
import com.ghdinteractivestudio.codestation.data.remote.CodeStationApiService
import com.ghdinteractivestudio.codestation.domain.repository.AntigravityModelRepository
import com.ghdinteractivestudio.codestation.domain.repository.BotRepository
import com.ghdinteractivestudio.codestation.domain.repository.ChatRepository
import com.ghdinteractivestudio.codestation.domain.repository.PreferencesRepository
import com.ghdinteractivestudio.codestation.domain.repository.ServerRepository
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

interface AppContainer {
    val context: Context
    val botRepository: BotRepository
    val chatRepository: ChatRepository
    val serverRepository: ServerRepository
    val preferencesRepository: PreferencesRepository
    val antigravityModelRepository: AntigravityModelRepository
    val eventStreamClient: com.ghdinteractivestudio.codestation.data.remote.CodeStationEventStreamClient
    val approvalDao: com.ghdinteractivestudio.codestation.data.local.ApprovalDao
}

class CodeStationContainer(override val context: Context) : AppContainer {

    private val database: CodeStationDatabase by lazy {
        CodeStationDatabase.getDatabase(context)
    }

    override val approvalDao: com.ghdinteractivestudio.codestation.data.local.ApprovalDao by lazy {
        database.approvalDao()
    }

    override val preferencesRepository: PreferencesRepositoryImpl by lazy {
        PreferencesRepositoryImpl(context)
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectionPool(okhttp3.ConnectionPool(8, 5, TimeUnit.MINUTES))
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        coerceInputValues = true
    }

    private val apiServiceCache = java.util.concurrent.ConcurrentHashMap<String, CodeStationApiService>()

    private fun createApiService(baseUrl: String): CodeStationApiService? {
        val normalized = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        apiServiceCache[normalized]?.let { return it }
        return try {
            val contentType = "application/json".toMediaType()
            val service = Retrofit.Builder()
                .baseUrl(normalized)
                .client(okHttpClient)
                .addConverterFactory(json.asConverterFactory(contentType))
                .build()
                .create(CodeStationApiService::class.java)
            apiServiceCache[normalized] = service
            service
        } catch (_: Exception) {
            null
        }
    }

    override val serverRepository: ServerRepository by lazy {
        ServerRepositoryImpl(
            preferencesRepository = preferencesRepository,
            apiServiceProvider = { url -> createApiService(url) }
        )
    }

    override val botRepository: BotRepository by lazy {
        BotRepositoryImpl(
            botDao = database.botDao(),
            apiServiceProvider = {
                val config = serverRepository.getServerConfig()
                createApiService(config.url)
            },
            serverRepository = serverRepository
        )
    }

    override val chatRepository: ChatRepository by lazy {
        ChatRepositoryImpl(
            messageDao = database.messageDao(),
            approvalDao = database.approvalDao(),
            botDao = database.botDao(),
            apiServiceProvider = {
                val config = serverRepository.getServerConfig()
                createApiService(config.url)
            },
            serverRepository = serverRepository,
            context = context
        )
    }

    override val antigravityModelRepository: AntigravityModelRepository by lazy {
        AntigravityModelRepositoryImpl(
            context = context,
            serverRepository = serverRepository,
            apiServiceProvider = {
                val config = serverRepository.getServerConfig()
                createApiService(config.url)
            }
        )
    }

    override val eventStreamClient: com.ghdinteractivestudio.codestation.data.remote.CodeStationEventStreamClient by lazy {
        com.ghdinteractivestudio.codestation.data.remote.CodeStationEventStreamClient(
            context = context,
            serverRepository = serverRepository,
            botDao = database.botDao(),
            messageDao = database.messageDao(),
            approvalDao = database.approvalDao()
        )
    }
}
