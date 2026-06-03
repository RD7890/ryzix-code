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

  package com.tom.rv2ide.editor.language.treesitter

  import android.content.Context
  import com.tom.rv2ide.editor.language.treesitter.TreeSitterLanguage.Factory
  import com.tom.rv2ide.lsp.api.ILanguageServer
  import com.tom.rv2ide.treesitter.kotlin.TSLanguageKotlin
  import io.github.rosemoe.sora.lang.Language.INTERRUPTION_LEVEL_NONE
  import io.github.rosemoe.sora.util.MyCharacter

  /**
   * [TreeSitterLanguage] implementation for Kotlin.
   * Language server (Kotlin LSP) has been removed from this stripped build.
   *
   * @author Akash Yadav
   */
  open class KotlinLanguage(context: Context) :
      TreeSitterLanguage(context, TSLanguageKotlin.getInstance(), TS_TYPE_KT) {

    companion object {

      val FACTORY = Factory { KotlinLanguage(it) }
      const val TS_TYPE_KT = "kt"
      const val TS_TYPE_KTS = "kts"
    }

    override val languageServer: ILanguageServer?
      get() = null

    override fun checkIsCompletionChar(c: Char): Boolean {
      return MyCharacter.isJavaIdentifierPart(c) || c == '.'
    }

    override fun getInterruptionLevel(): Int {
      return INTERRUPTION_LEVEL_NONE
    }
  }
  