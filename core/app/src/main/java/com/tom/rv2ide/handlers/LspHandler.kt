package com.tom.rv2ide.handlers

import android.content.Context
import com.tom.rv2ide.lsp.api.ILanguageClient
import com.tom.rv2ide.lsp.api.ILanguageServerRegistry

/** @author Akash Yadav */
object LspHandler {

  fun registerLanguageServers(context: Context) {
    // ACS Optimization: LSP servers disabled for better performance on low-end devices (Redmi 12C etc.)
    // Java + XML language servers (code completion, diagnostics) are NOT started.
    // Saves ~80-120 MB RAM at runtime and removes editor startup lag.
    //
    // To re-enable code completion, uncomment the lines below:
    // import com.tom.rv2ide.lsp.java.JavaLanguageServer
    // import com.tom.rv2ide.lsp.xml.XMLLanguageServer
    //
    // ILanguageServerRegistry.getDefault().apply {
    //   getServer(JavaLanguageServer.SERVER_ID) ?: register(JavaLanguageServer())
    //   getServer(XMLLanguageServer.SERVER_ID)  ?: register(XMLLanguageServer())
    // }
  }

  fun connectClient(client: ILanguageClient) {
    ILanguageServerRegistry.getDefault().connectClient(client)
  }

  fun destroyLanguageServers(isConfigurationChange: Boolean) {
    if (isConfigurationChange) {
      return
    }
    ILanguageServerRegistry.getDefault().destroy()
  }
}
