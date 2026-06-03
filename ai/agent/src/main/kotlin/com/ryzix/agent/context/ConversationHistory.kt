package com.ryzix.agent.context

import com.ryzix.agent.llm.LLMProvider

/**
 * Manages the agent conversation history.
 * Trims to a rolling window to avoid exceeding context limits.
 */
class ConversationHistory(private val maxTurns: Int = 40) {

    private val _messages = mutableListOf<LLMProvider.Message>()
    val messages: List<LLMProvider.Message> get() = _messages.toList()

    fun addUser(text: String) {
        _messages.add(LLMProvider.Message(role = LLMProvider.Role.USER, content = text))
        trim()
    }

    fun addAssistant(text: String) {
        _messages.add(LLMProvider.Message(role = LLMProvider.Role.ASSISTANT, content = text))
        trim()
    }

    fun addToolCalls(calls: List<LLMProvider.ToolCall>) {
        _messages.add(LLMProvider.Message(
            role = LLMProvider.Role.ASSISTANT,
            content = "",
            toolCalls = calls,
        ))
    }

    fun addToolResults(results: List<LLMProvider.ToolResult>) {
        _messages.add(LLMProvider.Message(
            role = LLMProvider.Role.TOOL,
            content = results.joinToString("\n\n") { "[${it.toolCallId}]\n${it.output}" },
            toolResults = results,
        ))
        trim()
    }

    fun clear() = _messages.clear()

    private fun trim() {
        while (_messages.size > maxTurns * 2) _messages.removeAt(0)
    }
}
