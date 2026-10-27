package com.ghdinteractivestudio.synapse.domain.repository

import com.ghdinteractivestudio.synapse.domain.model.Bot
import com.ghdinteractivestudio.synapse.domain.model.BotStatus
import kotlinx.coroutines.flow.Flow

interface BotRepository {
    fun getBotsStream(): Flow<List<Bot>>
    fun getBotByIdStream(id: String): Flow<Bot?>
    suspend fun getBotById(id: String): Bot?
    suspend fun createBot(bot: Bot): Result<Bot>
    suspend fun updateBot(bot: Bot): Result<Bot>
    suspend fun deleteBot(id: String): Result<Unit>
    suspend fun togglePin(id: String): Result<Unit>
    suspend fun updateBotStatus(id: String, status: BotStatus): Result<Unit>
    suspend fun refreshBots(): Result<List<Bot>>
    suspend fun stopBotTurn(botId: String): Result<Unit>
}
