/*
 *  This file is part of Ryzix Code.
 *
 *  Ryzix Code is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Ryzix Code is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Ryzix Code. If not, see <https://www.gnu.org/licenses/>.
 */

@file:Suppress("UnstableApiUsage")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
  includeBuild("composite-builds/build-logic") {
    name = "build-logic"
  }

  repositories {
    gradlePluginPortal()
    google()
    mavenCentral()
  }
}

dependencyResolutionManagement {
  val dependencySubstitutions = mapOf(
    "build-deps" to arrayOf(
      "appintro",
      "fuzzysearch",
      "google-java-format",
      "java-compiler",
      "javac",
      "javapoet",
      "jaxp",
      "jdk-compiler",
      "jdk-jdeps",
      "jdt",
      "layoutlib-api",
      "logback-core"
    ),
    "build-deps-common" to arrayOf(
      "desugaring-core"
    )
  )

  for ((build, modules) in dependencySubstitutions) {
    includeBuild("composite-builds/${build}") {
      this.name = build
      dependencySubstitution {
        for (module in modules) {
          substitute(module("com.tom.rv2ide.build:${module}"))
            .using(project(":${module}"))
        }
      }
    }
  }

  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    mavenLocal()
    google()
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
    maven { url = uri("https://repo.gradle.org/gradle/libs-releases") }
  }
}

gradle.rootProject {
  val appMainVersion = System.getenv("MAIN_VERSION") ?: "1.0.0"
  val revision = "r${System.getenv("REVISION_NUM") ?: "01"}"
  val baseVersion = "$appMainVersion+gh.$revision"
  println("Ryzix Code version: $baseVersion")
  project.setProperty("version", baseVersion)
}

rootProject.name = "RyzixCode"

include(
  // Annotation processing
  ":annotation:annotations",
  ":annotation:processors",
  ":annotation:processors-ksp",

  // External utilities
  ":external:acsprovider",
  ":external:atc",
  ":external:logwire",

  // Core modules
  ":core:actions",
  ":core:app",
  ":core:common",
  ":core:projects",
  ":core:projectdata",
  ":core:resources",

  // IDE configurations
  ":ideconfigurations",

  // Code Editor (sora-editor based)
  ":editor:api",
  ":editor:impl",
  ":editor:lexers",
  ":editor:treesitter",

  // Event bus
  ":event:eventbus",
  ":event:eventbus-android",
  ":event:eventbus-events",

  // Logging
  ":logging:idestats",
  ":logging:logger",
  ":logging:logsender",

  // Terminal emulator (Termux-based — used by AI agent for shell execution)
  ":termux:application",
  ":termux:emulator",
  ":termux:shared",
  ":termux:view",

  // Utilities
  ":utilities:build-info",
  ":utilities:flashbar",
  ":utilities:framework-stubs",
  ":utilities:lookup",
  ":utilities:preferences",
  ":utilities:shared",
  ":utilities:templates-api",
  ":utilities:templates-impl",
  ":utilities:treeview",

  // XML (basic parsing/utils only — no LSP)
  ":xml:dom",
  ":xml:resources-api",
  ":xml:utils",

  // AI Agent — Cursor-style autonomous coding assistant
  ":ai:agent",
)
