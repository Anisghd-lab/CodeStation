package com.ghdinteractivestudio.codestation.domain.repository

import com.ghdinteractivestudio.codestation.domain.model.ModelInfo
import kotlinx.coroutines.flow.StateFlow

data class AntigravityCatalogState(
    val models: List<ModelInfo> = emptyList(),
    val isRefreshing: Boolean = false,
    val lastUpdated: Long? = null,
    val errorMessage: String? = null
)

interface AntigravityModelRepository {
    val catalogState: StateFlow<AntigravityCatalogState>
    suspend fun getModels(): List<ModelInfo>
    suspend fun refreshModels(force: Boolean = false): Result<List<ModelInfo>>
    fun isModelVerified(modelId: String): Boolean
}
