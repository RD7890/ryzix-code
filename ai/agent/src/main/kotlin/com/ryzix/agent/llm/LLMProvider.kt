package com.ryzix.agent.llm

/**
 * Abstraction over any LLM backend.
 * Swap Gemini for any other provider by implementing this interface.
 */
interface LLMProvider {

    /** Send a conversation turn; returns the model's raw text response. */
    suspend fun chat(history: List<Message>, tools: List<ToolDefinition>): LLMResponse

    data class Message(
        val role: Role,
        val content: String,
        val toolCalls: List<ToolCall> = emptyList(),
        val toolResults: List<ToolResult> = emptyList(),
    )

    data class ToolDefinition(
        val name: String,
        val description: String,
        val parameters: Map<String, ParameterDef>,
    )

    data class ParameterDef(
        val type: String,       // "string" | "boolean" | "integer"
        val description: String,
        val required: Boolean = true,
    )

    data class ToolCall(
        val id: String,
        val name: String,
        val args: Map<String, String>,
    )

    data class ToolResult(
        val toolCallId: String,
        val output: String,
    )

    sealed class LLMResponse {
        data class Text(val content: String) : LLMResponse()
        data class ToolUse(val calls: List<ToolCall>) : LLMResponse()
        data class Error(val message: String) : LLMResponse()
    }

    enum class Role { USER, ASSISTANT, TOOL }
}
