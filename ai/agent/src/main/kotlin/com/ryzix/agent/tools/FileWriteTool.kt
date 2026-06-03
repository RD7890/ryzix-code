package com.ryzix.agent.tools

import com.ryzix.agent.llm.LLMProvider
import java.io.File

/**
 * Writes or overwrites a file in the project directory.
 * Creates parent directories automatically.
 */
class FileWriteTool(private val projectRoot: File) : AgentTool {

    override val name = "file_write"
    override val description = "Write content to a file in the project. Creates the file and parent directories if they don't exist. Overwrites existing content."
    override val parameters = mapOf(
        "path"    to LLMProvider.ParameterDef("string", "Relative path to the file from project root"),
        "content" to LLMProvider.ParameterDef("string", "Full content to write to the file"),
    )

    override suspend fun execute(args: Map<String, String>): ToolOutput {
        val path    = args["path"]    ?: return ToolOutput("Error: 'path' is required",    isError = true)
        val content = args["content"] ?: return ToolOutput("Error: 'content' is required", isError = true)

        val file = File(projectRoot, path)
        file.parentFile?.mkdirs()
        file.writeText(content)

        val lines = content.lines().size
        return ToolOutput("Written $lines lines to $path")
    }
}
