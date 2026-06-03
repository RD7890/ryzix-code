package com.ryzix.agent.llm

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.Schema
import com.google.ai.client.generativeai.type.Tool
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.defineFunction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Google Gemini implementation of [LLMProvider].
 * Uses gemini-2.0-flash with function calling for tool use.
 */
class GeminiProvider(private val apiKey: String) : LLMProvider {

    private val modelName = "gemini-2.0-flash"

    override suspend fun chat(
        history: List<LLMProvider.Message>,
        tools: List<LLMProvider.ToolDefinition>,
    ): LLMProvider.LLMResponse = withContext(Dispatchers.IO) {
        try {
            val geminiTools = tools.map { def ->
                Tool(listOf(defineFunction(
                    name = def.name,
                    description = def.description,
                    parameters = def.parameters.map { (name, param) ->
                        when (param.type) {
                            "boolean" -> Schema.boolean(name, param.description)
                            "integer" -> Schema.integer(name, param.description)
                            else      -> Schema.str(name, param.description)
                        }
                    },
                )))
            }

            val model = GenerativeModel(
                modelName = modelName,
                apiKey = apiKey,
                tools = geminiTools,
                systemInstruction = content { text(SYSTEM_PROMPT) },
            )

            val geminiHistory = history.dropLast(1).map { msg ->
                content(role = msg.role.toGeminiRole()) { text(msg.content) }
            }
            val chat = model.startChat(history = geminiHistory)
            val lastMessage = history.last()
            val response = chat.sendMessage(lastMessage.content)

            val functionCalls = response.functionCalls
            if (functionCalls.isNotEmpty()) {
                LLMProvider.LLMResponse.ToolUse(functionCalls.map { fc ->
                    LLMProvider.ToolCall(
                        id = fc.name + "_" + System.currentTimeMillis(),
                        name = fc.name,
                        args = fc.args.mapValues { it.value.toString() },
                    )
                })
            } else {
                LLMProvider.LLMResponse.Text(response.text ?: "")
            }
        } catch (e: Exception) {
            LLMProvider.LLMResponse.Error(e.message ?: "Unknown Gemini error")
        }
    }

    private fun LLMProvider.Role.toGeminiRole(): String = when (this) {
        LLMProvider.Role.USER      -> "user"
        LLMProvider.Role.ASSISTANT -> "model"
        LLMProvider.Role.TOOL      -> "user"
    }

    companion object {
        private val SYSTEM_PROMPT = """
            You are Ryzix, an expert autonomous coding agent running inside an Android IDE.
            
            Your capabilities:
            - Read and write files in the user's project
            - Run terminal commands (shell, gradle, git, etc.)
            - Search and grep across the codebase
            - Edit code with surgical precision
            - Run multi-step autonomous loops to complete tasks
            
            Rules:
            - Always think step-by-step before taking action
            - Prefer reading a file before editing it
            - Use the terminal tool to verify changes (compile, test, run)
            - Never make destructive changes without telling the user
            - Be concise in responses; show code diffs, not full files
            - If a task requires multiple steps, execute them autonomously without asking
        """.trimIndent()
    }
}
