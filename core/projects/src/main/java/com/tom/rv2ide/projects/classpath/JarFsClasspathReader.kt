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
   *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   *  GNU General Public License for more details.
   *
   *  You should have received a copy of the GNU General Public License
   *   along with Ryzix Code.  If not, see <https://www.gnu.org/licenses/>.
   */

  package com.tom.rv2ide.projects.classpath

  import com.google.common.collect.ImmutableSet
  import java.io.File
  import java.util.jar.JarFile

  /**
   * Reads class names from JAR files using the standard [JarFile] API.
   * (The previous implementation used internal javac file-system classes that
   * have been removed from this stripped build.)
   *
   * @author Akash Yadav
   */
  class JarFsClasspathReader : IClasspathReader {

    override fun listClasses(files: Collection<File>): ImmutableSet<ClassInfo> {
      val builder = ImmutableSet.builder<ClassInfo>()
      for (file in files) {
        if (!file.exists()) continue
        try {
          JarFile(file).use { jar ->
            jar.entries().asSequence().forEach { entry ->
              var name = entry.name
              if (!name.endsWith(".class") || name.endsWith("/package-info.class")) {
                return@forEach
              }
              name = name.substringBeforeLast(".class")
              if (name.isBlank()) return@forEach
              if (name.startsWith('/')) name = name.substring(1)
              name = name.replace('/', '.')
              ClassInfo.create(name)?.also { builder.add(it) }
            }
          }
        } catch (_: Exception) {
          // Skip unreadable or malformed JARs silently
        }
      }
      return builder.build()
    }
  }
  