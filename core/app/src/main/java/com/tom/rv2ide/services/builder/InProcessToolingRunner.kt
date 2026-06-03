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

package com.tom.rv2ide.services.builder

import com.tom.rv2ide.tooling.api.IProject
import com.tom.rv2ide.tooling.api.IToolingApiClient
import com.tom.rv2ide.tooling.api.IToolingApiServer
import com.tom.rv2ide.tooling.impl.InProcessToolingServer
import java.io.InputStream
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory

/**
 * Drop-in replacement for [ToolingServerRunner] that runs the Tooling API server
 * in-process (same JVM) rather than spawning a separate Java process.
 *
 * Benefits:
 * - Eliminates ~150-300MB RAM for the separate JVM process
 * - Faster startup: no process spawn + IPC handshake overhead
 * - Direct piped-stream communication instead of OS-level process I/O
 */
internal class InProcessToolingRunner(
  private var listener: OnServerStartListener?,
  private var observer: Observer?,
) {

  companion object {
    private val log = LoggerFactory.getLogger(InProcessToolingRunner::class.java)
  }

  internal var pid: Int? = null
  private var _job: Job? = null
  private val _isStarted = AtomicBoolean(false)

  var isStarted: Boolean
    get() = _isStarted.get()
    private set(value) {
      _isStarted.set(value)
    }

  private val runnerScope =
    CoroutineScope(Dispatchers.IO + CoroutineName("InProcessToolingRunner"))

  fun setListener(listener: OnServerStartListener?) {
    this.listener = listener
  }

  fun startAsync(envs: Map<String, String>) {
    _job =
      runnerScope.launch {
        try {
          log.info("Starting in-process Tooling API server...")

          val client = observer!!.getClient()
          val connection = withContext(Dispatchers.IO) { InProcessToolingServer.create(client) }

          observer?.onListenerStarted(
            server = connection.server,
            projectProxy = connection.project,
            errorStream = connection.errorStream,
          )

          pid = android.os.Process.myPid()
          isStarted = true
          listener?.onServerStarted(pid!!)
          listener = null

          log.info("In-process Tooling API server running (no separate JVM process)")

          // Keep coroutine alive until server future completes or we are cancelled
          withContext(Dispatchers.IO) {
            try {
              connection.future.get()
            } catch (_: Exception) {}
          }
        } catch (e: CancellationException) {
          // expected on release()
        } catch (e: Exception) {
          log.error("Failed to start in-process tooling server", e)
        }
      }
        .also { _job = it }
  }

  fun release() {
    this.listener = null
    this.observer = null
    _job?.cancel(CancellationException("Cancellation was requested"))
  }

  interface Observer {
    fun onListenerStarted(
      server: IToolingApiServer,
      projectProxy: IProject,
      errorStream: InputStream,
    )

    fun onServerExited(exitCode: Int)

    fun getClient(): IToolingApiClient
  }

  fun interface OnServerStartListener {
    fun onServerStarted(pid: Int)
  }
}
