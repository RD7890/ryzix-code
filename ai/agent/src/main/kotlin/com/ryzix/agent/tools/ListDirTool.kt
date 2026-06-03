package com.ryzix.agent.tools

import com.ryzix.agent.llm.LLMProvider
import java.io.File

/**
 * Lists files and directories at a given path.
 */
class ListDirTool(private val projectRoot: File) : AgentTool {

    override val name = "list_dir"
    override val description = "List files and directories at a path in the project. Shows names, sizes, and types."
    override val parameters = mapOf(
        "path" to LLMProvider.ParameterDef("string", "Relative path to list (use '.' for project root)", required = false),
    )

    override suspend fun execute(args: Map<String, String>): ToolOutput {
        val rel  = args["path"] ?: "."
        val dir  = File(projectRoot, rel)
        if (!dir.exists())    return ToolOutput("Error: path '$rel' does not exist", isError = true)
        if (!dir.isDirectory) return ToolOutput("Error: '$rel' is a file, not a directory", isError = true)

        val entries = dir.listFiles()
            ?.sortedWith(compareBy({ !it.isDirectory }, { it.name }))
            ?.map { f ->
                val type = if (f.isDirectory) "DIR " else "FILE"
                val size = if (f.isFile) " (${f.length()} bytes)" else ""
                "$type  ${f.name}$size"
            } ?: emptyList()

        if (entries.isEmpty()) return ToolOutput("Directory '$rel' is empty")
        return ToolOutput("Contents of $rel:\n\n${entries.joinToString("\n")}")
    }
}
