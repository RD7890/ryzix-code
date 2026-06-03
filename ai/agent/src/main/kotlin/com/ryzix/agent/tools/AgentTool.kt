package com.ryzix.agent.tools

import com.ryzix.agent.llm.LLMProvider

/**
 * Base interface for all agent tools.
 * Each tool maps to a function the LLM can call.
 */
interface AgentTool {
    val name: String
    val description: String
    val parameters: Map<String, LLMProvider.ParameterDef>

    suspend fun execute(args: Map<String, String>): ToolOutput

    fun toDefinition() = LLMProvider.ToolDefinition(
        name = name,
        description = description,
        parameters = parameters,
    )
}

data class ToolOutput(
    val output: String,
    val isError: Boolean = false,
)
