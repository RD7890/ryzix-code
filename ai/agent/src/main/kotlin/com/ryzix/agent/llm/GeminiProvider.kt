package com.ryzix.agent.llm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Google Gemini implementation of [LLMProvider].
 *
 * Calls the Gemini REST API directly via OkHttp + org.json (built-in Android).
 * This avoids any dependency on the Gemini Android SDK whose function-calling
 * API changes frequently between minor versions.
 *
 * Endpoint: POST /v1beta/models/gemini-2.0-flash:generateContent
 */
class GeminiProvider(private val apiKey: String) : LLMProvider {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val JSON = "application/json; charset=utf-8".toMediaType()
    private val BASE = "https://generativelanguage.googleapis.com/v1beta/models"
    private val MODEL = "gemini-2.0-flash"

    override suspend fun chat(
        history: List<LLMProvider.Message>,
        tools: List<LLMProvider.ToolDefinition>,
    ): LLMProvider.LLMResponse = withContext(Dispatchers.IO) {
        try {
            val body = buildRequestJson(history, tools)
            val request = Request.Builder()
                .url("$BASE/$MODEL:generateContent?key=$apiKey")
                .post(body.toString().toRequestBody(JSON))
                .build()

            val response = client.newCall(request).execute()
            val responseText = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext LLMProvider.LLMResponse.Error(
                    "Gemini API error ${response.code}: $responseText"
                )
            }

            parseResponse(responseText)
        } catch (e: Exception) {
            LLMProvider.LLMResponse.Error(e.message ?: "Unknown error")
        }
    }

    // ── Request builder ────────────────────────────────────────────────────

    private fun buildRequestJson(
        history: List<LLMProvider.Message>,
        tools: List<LLMProvider.ToolDefinition>,
    ): JSONObject {
        val contents = JSONArray()
        for (msg in history) {
            val role = when (msg.role) {
                LLMProvider.Role.USER      -> "user"
                LLMProvider.Role.ASSISTANT -> "model"
                LLMProvider.Role.TOOL      -> "user"
            }
            val parts = JSONArray()
            if (msg.content.isNotEmpty()) {
                parts.put(JSONObject().put("text", msg.content))
            }
            // Encode outgoing tool calls as model parts
            for (tc in msg.toolCalls) {
                val argsJson = JSONObject()
                tc.args.forEach { (k, v) -> argsJson.put(k, v) }
                parts.put(JSONObject().put("functionCall",
                    JSONObject().put("name", tc.name).put("args", argsJson)))
            }
            // Encode tool results as function response parts
            for (tr in msg.toolResults) {
                parts.put(JSONObject().put("functionResponse",
                    JSONObject()
                        .put("name", tr.toolCallId.substringBefore("_"))
                        .put("response", JSONObject().put("output", tr.output))
                ))
            }
            if (parts.length() > 0) {
                contents.put(JSONObject().put("role", role).put("parts", parts))
            }
        }

        val root = JSONObject()
            .put("contents", contents)
            .put("systemInstruction", JSONObject().put("parts",
                JSONArray().put(JSONObject().put("text", SYSTEM_PROMPT))))

        if (tools.isNotEmpty()) {
            val funcDeclarations = JSONArray()
            for (def in tools) {
                val props = JSONObject()
                val required = JSONArray()
                for ((pName, pDef) in def.parameters) {
                    val typeStr = when (pDef.type) {
                        "boolean" -> "BOOLEAN"
                        "integer" -> "INTEGER"
                        else      -> "STRING"
                    }
                    props.put(pName, JSONObject()
                        .put("type", typeStr)
                        .put("description", pDef.description))
                    if (pDef.required) required.put(pName)
                }
                val params = JSONObject()
                    .put("type", "OBJECT")
                    .put("properties", props)
                if (required.length() > 0) params.put("required", required)

                funcDeclarations.put(JSONObject()
                    .put("name", def.name)
                    .put("description", def.description)
                    .put("parameters", params))
            }
            root.put("tools", JSONArray().put(
                JSONObject().put("function_declarations", funcDeclarations)))
        }

        return root
    }

    // ── Response parser ────────────────────────────────────────────────────

    private fun parseResponse(raw: String): LLMProvider.LLMResponse {
        val root = JSONObject(raw)
        val candidates = root.optJSONArray("candidates") ?: return LLMProvider.LLMResponse.Error("No candidates in response")
        val first = candidates.optJSONObject(0) ?: return LLMProvider.LLMResponse.Error("Empty candidate")
        val content = first.optJSONObject("content") ?: return LLMProvider.LLMResponse.Error("No content")
        val parts = content.optJSONArray("parts") ?: return LLMProvider.LLMResponse.Error("No parts")

        val toolCalls = mutableListOf<LLMProvider.ToolCall>()
        val textParts = mutableListOf<String>()

        for (i in 0 until parts.length()) {
            val part = parts.getJSONObject(i)
            when {
                part.has("functionCall") -> {
                    val fc = part.getJSONObject("functionCall")
                    val name = fc.getString("name")
                    val args = mutableMapOf<String, String>()
                    val argsJson = fc.optJSONObject("args")
                    argsJson?.keys()?.forEach { k -> args[k] = argsJson.opt(k)?.toString() ?: "" }
                    toolCalls.add(LLMProvider.ToolCall(
                        id = "${name}_${System.currentTimeMillis()}",
                        name = name,
                        args = args,
                    ))
                }
                part.has("text") -> textParts.add(part.getString("text"))
            }
        }

        return when {
            toolCalls.isNotEmpty() -> LLMProvider.LLMResponse.ToolUse(toolCalls)
            textParts.isNotEmpty() -> LLMProvider.LLMResponse.Text(textParts.joinToString("\n"))
            else -> LLMProvider.LLMResponse.Error("Empty response from Gemini")
        }
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
