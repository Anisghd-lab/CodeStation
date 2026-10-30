package com.ghdinteractivestudio.codestation.domain.repository

import com.ghdinteractivestudio.codestation.domain.model.CollectiveMemoryStatus
import com.ghdinteractivestudio.codestation.domain.model.MemoryExperience
import com.ghdinteractivestudio.codestation.domain.model.MemoryKnowledge
import com.ghdinteractivestudio.codestation.domain.model.MemorySearchResultItem
import kotlinx.coroutines.flow.StateFlow

interface MemoryRepository {
    val memoryStatus: StateFlow<CollectiveMemoryStatus>

    suspend fun refreshStatus(): Result<CollectiveMemoryStatus>

    suspend fun getExperiences(limit: Int = 50, offset: Int = 0): Result<List<MemoryExperience>>

    suspend fun getKnowledge(limit: Int = 50, offset: Int = 0): Result<List<MemoryKnowledge>>

    suspend fun search(
        query: String,
        project: String? = null,
        agent: String? = null,
        type: String? = null,
        status: String? = null,
        limit: Int = 30
    ): Result<List<MemorySearchResultItem>>

    suspend fun triggerSync(): Result<CollectiveMemoryStatus>
}
