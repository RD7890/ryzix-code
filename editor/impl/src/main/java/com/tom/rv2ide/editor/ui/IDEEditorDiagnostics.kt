package com.tom.rv2ide.editor.ui

  import com.tom.rv2ide.lsp.models.DiagnosticItem
  import com.tom.rv2ide.models.Position
  import com.tom.rv2ide.models.Range
  import org.slf4j.LoggerFactory

  /** Diagnostic handling extensions for IDEEditor */
  private val log = LoggerFactory.getLogger("IDEEditorDiagnostics")

  private const val DIAGNOSTIC_HANDLER_KEY = "diagnostic_handler"

  private class DiagnosticHandler {
    private val diagnosticsByLine = mutableMapOf<Int, MutableList<DiagnosticItem>>()

    fun updateDiagnostics(diagnostics: List<DiagnosticItem>) {
      diagnosticsByLine.clear()
      diagnostics.forEach { diagnostic ->
        val line = diagnostic.range.start.line
        diagnosticsByLine.getOrPut(line) { mutableListOf() }.add(diagnostic)
      }
      log.info("Updated diagnostics for {} lines", diagnosticsByLine.size)
    }

    fun getDiagnosticsAtLine(line: Int): List<DiagnosticItem> {
      return diagnosticsByLine[line] ?: emptyList()
    }

    fun getDiagnosticAt(line: Int, column: Int): DiagnosticItem? {
      return diagnosticsByLine[line]?.firstOrNull { diagnostic ->
        val range = diagnostic.range
        line == range.start.line && column >= range.start.column && column <= range.end.column
      }
    }

    fun clear() {
      diagnosticsByLine.clear()
    }
  }

  private fun IDEEditor.getDiagnosticHandler(): DiagnosticHandler {
    var handler = getTag(DIAGNOSTIC_HANDLER_KEY.hashCode()) as? DiagnosticHandler
    if (handler == null) {
      handler = DiagnosticHandler()
      setTag(DIAGNOSTIC_HANDLER_KEY.hashCode(), handler)
    }
    return handler
  }

  fun IDEEditor.initDiagnosticHandling() {
    getDiagnosticHandler()
    log.info("Diagnostic handling initialized for editor")
  }

  fun IDEEditor.updateEditorDiagnostics(diagnostics: List<DiagnosticItem>) {
    getDiagnosticHandler().updateDiagnostics(diagnostics)
  }

  /**
   * Import fix at cursor — stubbed out: KotlinLanguageServer has been removed
   * from this stripped build. Always returns false.
   */
  fun IDEEditor.applyImportFixAtCursor(): Boolean = false

  fun IDEEditor.clearDiagnostics() {
    getDiagnosticHandler().clear()
  }
  