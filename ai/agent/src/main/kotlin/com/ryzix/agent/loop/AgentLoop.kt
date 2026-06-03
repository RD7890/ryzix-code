package com.ryzix.agent.loop

import com.ryzix.agent.context.ConversationHistory
import com.ryzix.agent.context.ProjectContext
import com.ryzix.agent.llm.LLMProvider
import com.ryzix.agent.tools.AgentTool
import com.ryzix.agent.tools.ToolOutput
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * The autonomous agent loop — the heart of Ryzix.
 *
 * Modelled after the ReAct pattern:
 *   Think → Act (tool call) → Observe (tool result) → repeat until done.
 *
 * The loop runs until:
 *   - The model returns a plain text response (no tool calls) → done
 *   - [maxIterations] tool calls have been made → safety stop
 */
class AgentLoop(
    private val llm: LLMProvider,
    private val tools: List<AgentTool>,
    private val history: ConversationHistory,
    private val projectRoot: java.io.File,
    private val maxIterations: Int = 20,
) {
    sealed class Event {
        data class Thinking(val text: String)                        : Event()
        data class ToolCall(val name: String, val args: Map<String,String>) : Event()
        data class ToolResult(val name: String, val output: ToolOutput)      : Event()
        data class FinalAnswer(val text: String)                     : Event()
        data class AgentError(val message: String)                   : Event()
    }

    private val toolMap = tools.associateBy { it.name }
    private val toolDefs = tools.map { it.toDefinition() }

    /**
     * Run the agent loop for [userMessage], emitting [Event]s as it works.
     */
    fun run(userMessage: String): Flow<Event> = flow {
        history.addUser(buildContextualMessage(userMessage))

        var iterations = 0
        while (iterations < maxIterations) {
            iterations++

            emit(Event.Thinking("Thinking… (step $iterations)"))

            when (val response = llm.chat(history.messages, toolDefs)) {
                is LLMProvider.LLMResponse.Text -> {
                    history.addAssistant(response.content)
                    emit(Event.FinalAnswer(response.content))
                    return@flow
                }

                is LLMProvider.LLMResponse.ToolUse -> {
                    history.addToolCalls(response.calls)

                    val results = mutableListOf<LLMProvider.ToolResult>()
                    for (call in response.calls) {
                        emit(Event.ToolCall(call.name, call.args))

                        val tool = toolMap[call.name]
                        val output = if (tool != null) {
                            tool.execute(call.args)
                        } else {
                            ToolOutput("Error: unknown tool '${call.name}'", isError = true)
                        }

                        emit(Event.ToolResult(call.name, output))
                        results.add(LLMProvider.ToolResult(call.id, output.output))
                    }

                    history.addToolResults(results)
                }

                is LLMProvider.LLMResponse.Error -> {
                    emit(Event.AgentError(response.message))
                    return@flow
                }
            }
        }

        emit(Event.AgentError("Reached max iterations ($maxIterations). Stopping."))
    }

    private fun buildContextualMessage(userMessage: String): String {
        val ctx = ProjectContext.build(projectRoot)
        return buildString {
            appendLine(ProjectContext.toPromptString(ctx))
            appendLine("---")
            appendLine(userMessage)
        }
    }
}
