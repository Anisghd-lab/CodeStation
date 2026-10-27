package com.ghdinteractivestudio.synapse

import android.content.Context
import com.ghdinteractivestudio.synapse.data.local.BotRepositoryImpl
import com.ghdinteractivestudio.synapse.data.local.ChatRepositoryImpl
import com.ghdinteractivestudio.synapse.data.local.ServerRepositoryImpl
import com.ghdinteractivestudio.synapse.data.local.SynapseDatabase
import com.ghdinteractivestudio.synapse.data.preferences.PreferencesRepositoryImpl
import com.ghdinteractivestudio.synapse.data.remote.SynapseApiService
import com.ghdinteractivestudio.synapse.domain.repository.BotRepository
import com.ghdinteractivestudio.synapse.domain.repository.ChatRepository
import com.ghdinteractivestudio.synapse.domain.repository.PreferencesRepository
import com.ghdinteractivestudio.synapse.domain.repository.ServerRepository
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

interface AppContainer {
    val botRepository: BotRepository
    val chatRepository: ChatRepository
    val serverRepository: ServerRepository
    val preferencesRepository: PreferencesRepository
}

class SynapseContainer(private val context: Context) : AppContainer {

    private val database: SynapseDatabase by lazy {
        SynapseDatabase.getDatabase(context)
    }

    override val preferencesRepository: PreferencesRepositoryImpl by lazy {
        PreferencesRepositoryImpl(context)
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    private fun createApiService(baseUrl: String): SynapseApiService? {
        return try {
            Retrofit.Builder()
                .baseUrl(if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/")
                .client(okHttpClient)
                .build()
                .create(SynapseApiService::class.java)
        } catch (e: Exception) {
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
        BotRepositoryImpl(database.botDao())
    }

    override val chatRepository: ChatRepository by lazy {
        ChatRepositoryImpl(
            messageDao = database.messageDao(),
            approvalDao = database.approvalDao(),
            botDao = database.botDao(),
            apiService = null
        )
    }
}
