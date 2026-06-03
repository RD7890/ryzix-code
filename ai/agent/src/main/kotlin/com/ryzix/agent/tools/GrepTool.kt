package com.ryzix.agent.tools

import com.ryzix.agent.llm.LLMProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Searches for a pattern across all files in the project.
 * Returns file paths with matching line numbers and content.
 */
class GrepTool(private val projectRoot: File) : AgentTool {

    override val name = "grep"
    override val description = "Search for a text pattern across project files. Returns file paths, line numbers, and matching lines."
    override val parameters = mapOf(
        "pattern"   to LLMProvider.ParameterDef("string",  "The search pattern (regex supported)"),
        "glob"      to LLMProvider.ParameterDef("string",  "File glob pattern to restrict search (e.g. '*.kt', '*.xml')", required = false),
        "recursive" to LLMProvider.ParameterDef("boolean", "Search subdirectories recursively (default: true)", required = false),
    )

    override suspend fun execute(args: Map<String, String>): ToolOutput = withContext(Dispatchers.IO) {
        val pattern   = args["pattern"] ?: return@withContext ToolOutput("Error: 'pattern' is required", isError = true)
        val glob      = args["glob"]
        val recursive = args["recursive"]?.toBooleanStrictOrNull() ?: true

        val results = mutableListOf<String>()
        val sequence = if (recursive) projectRoot.walkTopDown() else projectRoot.walk().maxDepth(1)

        sequence
            .filter { it.isFile }
            .filter { glob == null || matchesGlob(it.name, glob) }
            .filter { !it.path.contains("/.git/") && !it.path.contains("/build/") }
            .forEach { file ->
                file.runCatching {
                    readLines().forEachIndexed { idx, line ->
                        if (line.contains(Regex(pattern))) {
                            val rel = file.relativeTo(projectRoot).path
                            results.add("$rel:${idx + 1}: $line")
                        }
                    }
                }
            }

        if (results.isEmpty()) return@withContext ToolOutput("No matches found for '$pattern'")
        ToolOutput(results.take(100).joinToString("\n") + if (results.size > 100) "\n...${results.size - 100} more results" else "")
    }

    private fun matchesGlob(name: String, glob: String): Boolean {
        val regex = glob.replace(".", "\\.").replace("*", ".*").replace("?", ".")
        return name.matches(Regex(regex))
    }
}
