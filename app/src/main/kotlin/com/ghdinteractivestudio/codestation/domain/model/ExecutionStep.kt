package com.ghdinteractivestudio.codestation.domain.model

enum class StepType {
    THOUGHT,
    THINKING,
    TOOL_CALL,
    TOOL_RESULT,
    PLANNING,
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
) {
    val description: String get() = content
}
