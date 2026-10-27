package com.ghdinteractivestudio.synapse.domain.model

enum class StepType {
    THOUGHT,
    TOOL_CALL,
    TOOL_RESULT,
    NARRATION
}

data class ExecutionStep(
    val id: String,
    val type: StepType,
    val title: String,
    val content: String,
    val durationMs: Long? = null,
    val isError: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
