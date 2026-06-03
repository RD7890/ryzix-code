<p align="left">
  <img src="https://avatars.githubusercontent.com/u/234419170?s=400&u=43571ebf481969baafb8399813ad57f46c19eb95&v=4" alt="Ryzix Code" width="80" height="80"/>
</p>

<h2 align="left"><b>Ryzix Code</b></h2>
<p align="left">A lightweight, agentic code editor for Android — Cursor-style AI that reads, writes, and runs code autonomously.</p>

<p align="left">
  <img src="https://img.shields.io/badge/Status-Alpha-orange" alt="Alpha"/>
  <img src="https://img.shields.io/badge/License-GPLv3-blue.svg" alt="GPLv3"/>
  <img src="https://img.shields.io/badge/AI-Gemini%202.0-4285F4?logo=google" alt="Gemini"/>
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?logo=android" alt="Android"/>
</p>

---

## What is Ryzix Code?

Ryzix Code is a **stripped-down, agentic Android IDE** — think Cursor AI, but running natively on your Android device.

It's based on [Android Code Studio](https://github.com/RD7890/android-code-studio), but with all the heavy infrastructure removed and replaced by a **Gemini-powered autonomous agent** that can:

- 📄 Read and understand your code
- ✏️ Write and surgically edit files
- 🖥️ Run terminal commands autonomously (gradle, git, shell)
- 🔍 Search and grep across your codebase
- 🔁 Loop autonomously until the task is complete — without you lifting a finger

---

## What was stripped vs what was kept

| Removed (too heavy) | Kept |
|---|---|
| Java Language Server | ✅ Code editor (sora-editor) |
| Kotlin Language Server | ✅ Terminal emulator (Termux) |
| XML Language Server | ✅ File manager |
| Visual UI Designer | ✅ Git integration |
| SDK / NDK Manager | ✅ Basic Gradle support |
| XML inflater & layout editor | ✅ Event bus, logging |
| Heavy indexing engine | ✅ **AI Agent (new)** |

---

## AI Agent — How it works

The agent is built on the **ReAct loop** (Reason → Act → Observe):

```
User: "Add dark mode support to MainActivity"
 │
 ├─ 🔍 list_dir(".")              → finds project structure
 ├─ 📄 file_read("MainActivity.kt") → reads the file
 ├─ 🔍 grep("setTheme|DayNight") → finds existing theme code
 ├─ ✏️ file_edit(...)             → adds dark mode toggle
 ├─ 🖥️ terminal("./gradlew assembleDebug") → builds to verify
 └─ ✅ "Done! Dark mode added and build passes."
```

### Agent Tools

| Tool | What it does |
|---|---|
| `file_read` | Read any file, with optional line range |
| `file_write` | Write/create a file |
| `file_edit` | Surgically replace a string in a file |
| `terminal` | Run any shell command (60s timeout) |
| `grep` | Search across all files with regex |
| `list_dir` | List files and folders |

---

## Stack

- **Language**: Kotlin
- **Editor core**: [sora-editor](https://github.com/Rosemoe/sora-editor)
- **Terminal**: Termux emulator
- **AI**: Google Gemini 2.0 Flash (function calling)
- **Build**: Gradle KTS + Android AGP

---

## Getting Started

### 1. Prerequisites

- Android device running Android 8.0+ (API 26+)
- Gemini API key — get one free at [aistudio.google.com](https://aistudio.google.com)

### 2. Build

```bash
git clone https://github.com/RD7890/ryzix-code.git
cd ryzix-code
./gradlew assembleDebug
```

### 3. Configure your API key

In the app → Settings → paste your Gemini API key.

---

## Architecture

```
ryzix-code/
├── ai/
│   └── agent/                   # 🤖 AI Agent module
│       └── src/main/kotlin/com/ryzix/agent/
│           ├── llm/             # LLM providers (Gemini, extensible)
│           ├── tools/           # File, terminal, search tools
│           ├── loop/            # Autonomous ReAct loop
│           ├── context/         # Project context + conversation history
│           └── ui/              # Agent panel Fragment + ViewModel
├── editor/                      # Code editor (sora-editor)
├── termux/                      # Terminal emulator
├── core/                        # Core app logic
├── event/                       # Event bus
├── logging/                     # Logging
└── utilities/                   # Shared utilities
```

---

## Roadmap

- [ ] Diff view — show agent edits as a unified diff before applying
- [ ] Multi-file context — agent reads all open files automatically
- [ ] Voice input — speak tasks to the agent
- [ ] Plugin system — add custom agent tools
- [ ] Offline mode — local LLM via llama.cpp

---

## License

GPLv3 — see [LICENSE](./LICENSE)

Based on [Android Code Studio](https://github.com/RD7890/android-code-studio) by RD7890, which is a fork of [AndroidIDE](https://github.com/AndroidIDEOfficial/AndroidIDE).
