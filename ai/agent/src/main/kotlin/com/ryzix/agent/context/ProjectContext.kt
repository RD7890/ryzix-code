package com.ryzix.agent.context

import java.io.File

/**
 * Builds a compact summary of the project structure to inject
 * into the agent's system context — so it understands the codebase
 * without needing to explore it manually every time.
 */
object ProjectContext {

    data class Summary(
        val projectName: String,
        val rootPath: String,
        val modules: List<String>,
        val keyFiles: List<String>,
        val compactMap: String,
    )

    fun build(projectRoot: File): Summary {
        val modules = findModules(projectRoot)
        val keyFiles = findKeyFiles(projectRoot)
        val tree = buildCompactTree(projectRoot, depth = 2)
        return Summary(
            projectName = projectRoot.name,
            rootPath    = projectRoot.absolutePath,
            modules     = modules,
            keyFiles    = keyFiles,
            compactMap  = tree,
        )
    }

    fun toPromptString(summary: Summary): String = buildString {
        appendLine("## Project: ${summary.projectName}")
        appendLine("Root: ${summary.rootPath}")
        appendLine()
        appendLine("### Modules")
        summary.modules.forEach { appendLine("  - $it") }
        appendLine()
        appendLine("### Key Files")
        summary.keyFiles.forEach { appendLine("  - $it") }
        appendLine()
        appendLine("### Structure (2 levels)")
        appendLine(summary.compactMap)
    }

    private fun findModules(root: File): List<String> {
        val settings = File(root, "settings.gradle.kts").takeIf { it.exists() }
            ?: File(root, "settings.gradle").takeIf { it.exists() }
            ?: return emptyList()
        return settings.readLines()
            .filter { it.trimStart().startsWith("\":") }
            .map { it.trim().removeSurrounding("\"").removeSurrounding(",") }
    }

    private fun findKeyFiles(root: File): List<String> {
        val patterns = listOf("build.gradle.kts", "AndroidManifest.xml", "proguard-rules.pro", "gradle.properties")
        return root.walkTopDown()
            .filter { f -> f.isFile && patterns.any { f.name == it } && !f.path.contains("/build/") }
            .map { it.relativeTo(root).path }
            .take(20)
            .toList()
    }

    private fun buildCompactTree(dir: File, depth: Int, prefix: String = ""): String = buildString {
        if (depth < 0) return@buildString
        dir.listFiles()
            ?.filter { !it.name.startsWith(".") && it.name != "build" }
            ?.sortedWith(compareBy({ !it.isDirectory }, { it.name }))
            ?.forEach { child ->
                appendLine("$prefix${if (child.isDirectory) "📁" else "📄"} ${child.name}")
                if (child.isDirectory) append(buildCompactTree(child, depth - 1, "$prefix  "))
            }
    }
}
