/*
 *  This file is part of AndroidCodeStudio.
 *
 *  AndroidCodeStudio is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  AndroidCodeStudio is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *   along with AndroidCodeStudio.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.tom.rv2ide.tooling.impl

import com.tom.rv2ide.tooling.api.IProject
import com.tom.rv2ide.tooling.api.IToolingApiClient
import com.tom.rv2ide.tooling.api.IToolingApiServer
import com.tom.rv2ide.tooling.api.util.ToolingApiLauncher
import com.tom.rv2ide.tooling.impl.internal.ProjectImpl
import java.io.InputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.util.concurrent.Future
import org.slf4j.LoggerFactory

/**
 * Provides an in-process connection to the Tooling API server, eliminating the need for a
 * separate JVM process. Uses PipedInputStream/PipedOutputStream to create a bidirectional
 * channel between the client (GradleBuildService) and the server (ToolingApiServerImpl)
 * within the same JVM, saving ~150-300MB of RAM on low-end devices.
 */
object InProcessToolingServer {

  private val log = LoggerFactory.getLogger(InProcessToolingServer::class.java)
  private const val PIPE_BUFFER_SIZE = 65536

  /**
   * Creates an in-process Tooling API server connected to the given client.
   * Both client and server run in the same JVM process.
   */
  fun create(client: IToolingApiClient): Connection {
    log.info("Creating in-process Tooling API server (no separate JVM)...")

    // client→server pipe: client writes to clientOut, server reads from serverIn
    val serverIn = PipedInputStream(PIPE_BUFFER_SIZE)
    val clientOut = PipedOutputStream(serverIn)

    // server→client pipe: server writes to serverOut, client reads from clientIn
    val clientIn = PipedInputStream(PIPE_BUFFER_SIZE)
    val serverOut = PipedOutputStream(clientIn)

    val project = ProjectImpl()
    val server = ToolingApiServerImpl(project)

    // Wire server side: server reads from serverIn, writes to serverOut
    val serverLauncher = ToolingApiLauncher.newServerLauncher(server, project, serverIn, serverOut)
    val serverFuture = serverLauncher.startListening()
    val clientProxy = serverLauncher.remoteProxy as IToolingApiClient
    server.connect(clientProxy)

    // Set globals so ToolingApiServerImpl.shutdown() can cancel the future
    Main.future = serverFuture
    Main.client = clientProxy

    // Wire client side: client reads from clientIn, writes to clientOut
    val clientLauncher = ToolingApiLauncher.newClientLauncher(client, clientIn, clientOut)
    clientLauncher.startListening()

    log.info("In-process Tooling API server started successfully")

    return Connection(
      server = clientLauncher.remoteProxy as IToolingApiServer,
      project = clientLauncher.remoteProxy as IProject,
      errorStream = object : InputStream() { override fun read() = -1 },
      future = serverFuture,
    )
  }

  /** Holds the in-process server connection objects. */
  data class Connection(
    val server: IToolingApiServer,
    val project: IProject,
    /** Empty stream — no separate process stderr in in-process mode. */
    val errorStream: InputStream,
    val future: Future<Void?>,
  )
}
