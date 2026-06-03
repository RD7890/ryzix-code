# 2026-06-01 — Fix InProcessToolingRunner Type Mismatches

## What was changed

- `core/app/src/main/java/com/tom/rv2ide/services/builder/GradleBuildService.kt`
  - Changed class declaration from `ToolingServerRunner.Observer` to `InProcessToolingRunner.Observer`
  - Changed `toolingServerRunner` field type from `ToolingServerRunner?` to `InProcessToolingRunner?`
  - Changed import of `OnServerStartListener` to `InProcessToolingRunner.OnServerStartListener`
  - Changed `setServerListener` and `startToolingServer` parameter types accordingly
  - Replaced instantiation of `ToolingServerRunner(...)` with `InProcessToolingRunner(...)`

- `core/app/src/main/java/com/tom/rv2ide/services/builder/InProcessToolingRunner.kt` *(new)*
  - Drop-in replacement for `ToolingServerRunner` that runs the Tooling API in-process (same JVM)
  - Uses coroutines, exposes same `OnServerStartListener` and `Observer` interfaces
  - Calls `InProcessToolingServer.create(client)` instead of spawning a child process

- `tooling/impl/src/main/java/com/tom/rv2ide/tooling/impl/InProcessToolingServer.kt` *(new)*
  - Creates bidirectional `PipedInputStream`/`PipedOutputStream` channels between client and server
  - Eliminates ~150–300 MB RAM for separate JVM process
  - Returns a `Connection` object with `server`, `project`, `errorStream`, `future`

## Why

The original `ToolingServerRunner` spawned a separate `java -jar tooling-api.jar` child process,
consuming an extra 150–300 MB of RAM. To make ACS lighter (single-JVM goal), the tooling API
server is now run in the same JVM process as the Android IDE app.

## How it works

1. `InProcessToolingServer.create(client)` creates two pairs of piped streams.
2. The server side (`ToolingApiServerImpl`) communicates via one pair.
3. The client side (`GradleBuildService` via `ForwardingToolingApiClient`) communicates via the other.
4. LSP4J's `ToolingApiLauncher` wires both sides over these streams.
5. A `Future<Void?>` keeps the coroutine alive until the server shuts down.

## Build errors that were fixed

```
e: GradleBuildService.kt:427:41 Argument type mismatch:
    actual type is 'ToolingServerRunner.OnServerStartListener?',
    but 'InProcessToolingRunner.OnServerStartListener?' was expected.

e: GradleBuildService.kt:752:52 Argument type mismatch:
    actual type is 'ToolingServerRunner.OnServerStartListener?',
    but 'InProcessToolingRunner.OnServerStartListener?' was expected.

e: GradleBuildService.kt:752:62 Argument type mismatch:
    actual type is 'GradleBuildService',
    but 'InProcessToolingRunner.Observer?' was expected.
```

Root cause: the class declaration, field type, and method signatures still referenced
`ToolingServerRunner.*` types after the runner implementation was switched to
`InProcessToolingRunner`.

## Status after this change

- **Build: PASSING** ✅ (GitHub Actions run #26764872749 and #26764867204 — both success)
- Known remaining issues: None identified

## Next agent context

- `ToolingServerRunner.kt` is still present in the codebase but is no longer used.
  It is kept for reference and can be deleted in a future cleanup.
- The `InProcessToolingServer` sets `Main.future` and `Main.client` directly on the
  `tooling:impl` `Main` singleton — this is intentional so the existing shutdown path works.
- The `errorStream` in `InProcessToolingServer.Connection` is always an empty stream
  (no separate process stderr in in-process mode). Log output goes through SLF4J instead.
- Do NOT add `--add-opens` JVM flags for in-process mode (those were for the child process).
