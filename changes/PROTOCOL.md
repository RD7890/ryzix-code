# Agent Protocol — Android Code Studio (ACS)

## MANDATORY RULE FOR ALL AGENTS

Every agent that makes changes to this repository **MUST** create a new file in this
`changes/` directory documenting what was done, so the next agent has full context.

---

## How to document your changes

Create a new markdown file:  
`changes/YYYY-MM-DD-short-description.md`

Use this template:

```markdown
# [Date] — [Short Title]

## What was changed
- Bullet list of files modified / created / deleted

## Why
Explain the motivation or bug that was fixed.

## How it works
Brief technical explanation of the approach taken.

## Status after this change
- Build: PASSING / FAILING / UNKNOWN
- Known remaining issues: (list any)

## Next agent context
Any gotchas, warnings, or important context for the next agent working on this repo.
```

---

## Project Context

**Goal:** ACS (Android Code Studio) — a lightweight single-JVM Android IDE with code assist,
modelled after CodeAssist (https://github.com/tyron12233/CodeAssist.git) but lighter.

**Key architectural decision (in-process tooling):**  
Instead of spawning a separate JVM process for the Tooling API server (~150–300 MB RAM),
we run it in-process in the same JVM using `InProcessToolingRunner` +
`InProcessToolingServer`. Communication happens via `PipedInputStream`/`PipedOutputStream`.

**Critical files:**
- `core/app/src/main/java/com/tom/rv2ide/services/builder/GradleBuildService.kt`
- `core/app/src/main/java/com/tom/rv2ide/services/builder/InProcessToolingRunner.kt`
- `tooling/impl/src/main/java/com/tom/rv2ide/tooling/impl/InProcessToolingServer.kt`
- `.github/workflows/asm_build.yml` — triggers on push to `dev`, `main`, `indexing`, `release/**`

**Build command:** `./gradlew :core:app:assembleDebug`

**Branch:** `dev` is the default active branch.

---

## Change Log Index

| Date | File | Summary |
|------|------|---------|
| 2026-06-01 | changes/2026-06-01-inprocess-tooling-fix.md | Fix type mismatches in InProcessToolingRunner integration |
