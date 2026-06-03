package com.ryzix.agent.tools

import com.ryzix.agent.llm.LLMProvider
import java.io.File

/**
 * Reads a file from the project directory.
 * Supports optional line range: startLine..endLine.
 */
class FileReadTool(private val projectRoot: File) : AgentTool {

    override val name = "file_read"
    override val description = "Read the contents of a file in the project. Optionally specify startLine and endLine to read a range."
    override val parameters = mapOf(
        "path"      to LLMProvider.ParameterDef("string",  "Relative path to the file from project root"),
        "startLine" to LLMProvider.ParameterDef("integer", "First line to read (1-indexed, optional)", required = false),
        "endLine"   to LLMProvider.ParameterDef("integer", "Last line to read (inclusive, optional)",  required = false),
    )

    override suspend fun execute(args: Map<String, String>): ToolOutput {
        val path = args["path"] ?: return ToolOutput("Error: 'path' is required", isError = true)
        val file = File(projectRoot, path)
        if (!file.exists()) return ToolOutput("Error: file '$path' does not exist", isError = true)
        if (!file.isFile)   return ToolOutput("Error: '$path' is a directory, not a file", isError = true)

        val lines = file.readLines()
        val start = args["startLine"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
        val end   = args["endLine"]?.toIntOrNull()?.coerceAtMost(lines.size) ?: lines.size

        val slice = lines.subList(start - 1, end)
            .mapIndexed { i, line -> "${start + i}\t$line" }
            .joinToString("\n")

        return ToolOutput("File: $path (lines $start-$end of ${lines.size})\n\n$slice")
    }
}
