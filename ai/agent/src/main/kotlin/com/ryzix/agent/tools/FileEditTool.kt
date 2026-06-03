package com.ryzix.agent.tools

import com.ryzix.agent.llm.LLMProvider
import java.io.File

/**
 * Surgically replaces a block of text in a file.
 * Safer than FileWriteTool for targeted edits.
 */
class FileEditTool(private val projectRoot: File) : AgentTool {

    override val name = "file_edit"
    override val description = "Replace an exact string in a file with new content. Use this for targeted edits instead of rewriting the whole file."
    override val parameters = mapOf(
        "path"       to LLMProvider.ParameterDef("string", "Relative path to the file from project root"),
        "oldString"  to LLMProvider.ParameterDef("string", "The exact text to find and replace (must be unique in the file)"),
        "newString"  to LLMProvider.ParameterDef("string", "The replacement text"),
    )

    override suspend fun execute(args: Map<String, String>): ToolOutput {
        val path      = args["path"]      ?: return ToolOutput("Error: 'path' is required",      isError = true)
        val oldString = args["oldString"] ?: return ToolOutput("Error: 'oldString' is required", isError = true)
        val newString = args["newString"] ?: return ToolOutput("Error: 'newString' is required", isError = true)

        val file = File(projectRoot, path)
        if (!file.exists()) return ToolOutput("Error: file '$path' does not exist", isError = true)

        val original = file.readText()
        val count = original.split(oldString).size - 1
        if (count == 0) return ToolOutput("Error: oldString not found in '$path'", isError = true)
        if (count > 1)  return ToolOutput("Error: oldString matches $count times in '$path' — provide more context to make it unique", isError = true)

        val updated = original.replace(oldString, newString)
        file.writeText(updated)
        return ToolOutput("Replaced 1 occurrence in $path")
    }
}
