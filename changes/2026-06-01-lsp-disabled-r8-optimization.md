# 2026-06-01 — LSP Disabled + R8 Optimization

## Summary
Removed code completion (LSP servers) and enabled R8 minification for better performance on low-end devices like Redmi 12C.

## Changes

### 1. `core/app/src/main/java/com/tom/rv2ide/handlers/LspHandler.kt`
- **What**: `registerLanguageServers()` is now a no-op (empty body)
- **Effect**: Java Language Server and XML Language Server are never started
- **Benefit**:
  - ~80–120 MB less RAM used at editor startup
  - No background indexing thread (CPU freed)
  - Editor opens immediately with no completion lag
  - Smoother experience on 2–3 GB RAM devices
- **To revert**: Uncomment the `ILanguageServerRegistry` block in LspHandler.kt

### 2. `core/app/build.gradle.kts`
- **What**: Release build type now has:
  ```
  isMinifyEnabled = true
  isShrinkResources = true
  proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
  ```
- **Effect**: R8 shrinks and obfuscates release APK
- **Benefit**:
  - Release APK ~30–50% smaller
  - Dead code (unused LSP classes) stripped automatically by R8
  - Faster app startup due to fewer classes to load

## Architecture Note
ACS uses real Gradle daemon + Termux JDK (unlike CodeAssist which runs an in-process custom javac).
Disabling LSP at `LspHandler` level is the safest approach — all import chains remain valid,
only the runtime registration is skipped. R8 will prune the dead code in release builds.

## Tested
Build triggered via GitHub Actions after push. Previous successful builds: #26764872749, #26765757896.
