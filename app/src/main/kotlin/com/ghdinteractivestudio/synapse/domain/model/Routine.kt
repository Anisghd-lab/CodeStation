package com.ghdinteractivestudio.synapse.domain.model

data class Routine(
    val id: String,
    val botId: String,
    val name: String,
    val instruction: String,
    val scheduleType: String = "DAILY", // HOURLY, DAILY, WEEKDAYS, WEEKLY, CRON
    val cronExpression: String = "0 9 * * *",
    val isEnabled: Boolean = true,
    val nextRunDescription: String = "Aujourd'hui à 09:00",
    val lastRunAt: Long? = null,
    val lastResultSummary: String? = null
)

data class MemoryFact(
    val id: String,
    val botId: String,
    val content: String,
    val category: String = "Général",
    val isShared: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
)

data class ExtensionItem(
    val id: String,
    val name: String,
    val description: String,
    val category: String, // MCP, SHELL, GIT, SEARCH
    val isInstalled: Boolean,
    val isConfigured: Boolean,
    val isAvailableLocally: Boolean = true
)
