package com.ryzix.agent.tools

import com.ryzix.agent.llm.LLMProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

/**
 * Executes a shell command inside the project root and returns stdout+stderr.
 * This is the core tool that makes Ryzix agentic — the AI can run:
 *   - gradle tasks
 *   - git commands
 *   - shell scripts
 *   - file system operations
 */
class TerminalTool(private val projectRoot: File) : AgentTool {

    override val name = "terminal"
    override val description = "Run a shell command in the project root directory. Returns stdout and stderr. Supports gradle, git, shell scripts, etc. Timeout: 60s."
    override val parameters = mapOf(
        "command"    to LLMProvider.ParameterDef("string",  "The shell command to execute"),
        "workingDir" to LLMProvider.ParameterDef("string",  "Subdirectory to run from (relative to project root, optional)", required = false),
    )

    override suspend fun execute(args: Map<String, String>): ToolOutput = withContext(Dispatchers.IO) {
        val command    = args["command"]    ?: return@withContext ToolOutput("Error: 'command' is required", isError = true)
        val subDir     = args["workingDir"]
        val workingDir = if (subDir != null) File(projectRoot, subDir) else projectRoot

        if (!workingDir.exists()) {
            return@withContext ToolOutput("Error: working directory '$subDir' does not exist", isError = true)
        }

        val result = withTimeoutOrNull(60_000L) {
            val process = ProcessBuilder("/bin/sh", "-c", command)
                .directory(workingDir)
                .redirectErrorStream(true)
                .start()

            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            Pair(output.take(8000), exitCode)
        }

        if (result == null) {
            return@withContext ToolOutput("Error: command timed out after 60 seconds", isError = true)
        }

        val (output, exitCode) = result
        val prefix = if (exitCode == 0) "Exit 0 (success)" else "Exit $exitCode (error)"
        ToolOutput("$prefix\n\n$output", isError = exitCode != 0)
    }
}
