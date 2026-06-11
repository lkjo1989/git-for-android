# Git For Android

A dual-mode Android Git client featuring both a GUI and a CLI terminal interface, powered by JGit — no native git binary required.

> 简体中文：[README_CN.md](README_CN.md)

## Features

- **GUI Mode** — Jetpack Compose + Material 3 screens for common Git operations: status, commit, log, branches, push/pull
- **CLI Terminal Mode** — Full git command-line REPL: init, clone, status, add, commit, push/pull, log, branch, checkout, merge, diff, stash, and more. Works without pre-selecting repos for init/clone.
- **Git Engine** — Built on JGit 6.10, runs entirely on-device with no external dependencies
- **Repository Management** — Clone, list, and switch between repositories; repos added in either mode are instantly visible in the other
- **SSH & Credentials** — Support for SSH key authentication and username/password credentials
- **Room Database** — Local storage for repo metadata and settings

## Terminal Mode

The terminal provides a git command-line REPL (Read-Eval-Print Loop). It parses standard git commands and executes them via JGit — no native git binary needed.

### Getting started

1. Open the app and switch to the **Terminal** tab
2. Create your first repo: `git init myproject` or `git clone https://github.com/user/repo.git`
3. The new repo is automatically selected — `git status`, `git add`, `git commit` all work immediately
4. Use `repo list` and `repo use <id>` to switch between repos

### Command dispatch logic

```
User Input
  ├── "" (empty) → no-op
  ├── "help" → show command reference
  ├── "clear" / "cls" → clear terminal
  ├── "pwd" → show current repo path
  ├── "repo <sub>" → repo management (no repoId needed)
  │   ├── "repo list" → show all repos from database
  │   ├── "repo use <id>" → select a repo by ID
  │   └── "repo current" → show selected repo info + path
  ├── "git init [name]" → create new repo (no repoId needed!) → auto-selects
  ├── "git clone <url> [path]" → clone repo (no repoId needed!) → auto-selects
  ├── "git config <key> [value]" → set/view author (no repoId needed)
  └── "git <command>" → requires repo selected (auto-selects if only 1 exists)
      ├── status, add, commit, push, pull, fetch
      ├── log, branch, checkout, merge, diff, stash
      └── remote
```

### Key behaviors

- **`git init` and `git clone`** work without selecting a repo first. The newly created repo is auto-selected so subsequent commands work immediately.
- **Auto-select**: If you have exactly one repo in the database and type a git command without `repo use`, it auto-selects that repo.
- **Dual-mode sync**: Repos cloned in the GUI (Repos tab) appear instantly in the terminal's `repo list`. Repos created via terminal `git init`/`git clone` appear instantly in the GUI repo list.
- **History**: Up/Down arrows navigate command history (max 500 entries). Your draft input is preserved — pressing Down past the end of history restores what you were typing.
- **Progress feedback**: Clone operations show real-time progress messages in the terminal.

### Built-in commands

| Command | Description |
|---------|-------------|
| `help` | Show full command reference |
| `clear` / `cls` | Clear terminal output |
| `pwd` | Show current repository filesystem path |
| `repo list` | List all repositories (id and name) |
| `repo use <id>` | Select a repository by its ID |
| `repo current` | Show the currently selected repository |

### Git commands

| Command | Notes |
|---------|-------|
| `git init [name]` | Create a new repo. Defaults to `my-repo` if no name given. Repos are created under the app's files directory. |
| `git clone <url> [path]` | Clone a remote repo. Shows progress during transfer. Auto-selects on completion. |
| `git config user.name <name>` | Set commit author name |
| `git config user.email <email>` | Set commit author email |
| `git config user.name` | View current author name |
| `git config user.email` | View current author email |
| `git status` | Show branch, staged/unstaged/untracked changes |
| `git add <files...>` | Stage files (defaults to `.` if no files given) |
| `git commit -m <message>` | Commit staged changes |
| `git push [remote] [branch]` | Push to remote (defaults to `origin`) |
| `git pull [remote] [branch]` | Pull from remote |
| `git fetch [remote]` | Fetch from remote |
| `git log [-n <count>] [--oneline]` | Show commit history |
| `git branch [-a] [-d <name>]` | List branches, or create/delete |
| `git checkout <branch> [-b]` | Switch branches (supports remote-tracking branches) |
| `git merge <branch>` | Merge a branch into current |
| `git diff [--staged] [path]` | Show working tree changes |
| `git stash [pop\|list]` | Stash changes, pop stash, or list stashes |

### Usage flow examples

**Scenario 1: First-time user (zero repos)**
```
git init hello-world
  → "Initialized empty Git repository in .../repos/hello-world"
  → "Auto-selected repo 'hello-world' [id=1]"
git status
  → "On branch main\nnothing to commit, working tree clean"
```

**Scenario 2: User with one repo (GUI-cloned or terminal-created)**
```
git status
  → "Auto-selected repo 'my-repo' [id=1]"
  → "On branch main\nChanges:\n  modified: README.md"
```

**Scenario 3: User with multiple repos**
```
git status
  → "No repository selected. Use 'repo list' and 'repo use <id>' to select one."
repo list
  → "  [1] project-a\n  [2] project-b"
repo use 2
  → "Switched to repo: project-b [id=2]"
```

**Scenario 4: Set author and commit**
```
git config user.name "Alice"
  → "Set user.name = Alice"
git config user.email "alice@example.com"
  → "Set user.email = alice@example.com"
git add -A
git commit -m "feat: initial implementation"
  → "Committed: a1b2c3d - feat: initial implementation"
```

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Kotlin 1.9.24 |
| UI Framework | Jetpack Compose + Material 3 |
| Build System | Gradle 8.8 + AGP 8.4.1 |
| DI | Hilt 2.51.1 (KSP) |
| Git Engine | JGit 6.10.0 |
| Database | Room 2.6.1 |
| Architecture | MVVM + Clean Architecture (UI → Domain → Data) |

## Requirements

- Android 8.0+ (API 26)
- JDK 17

## Building

```bash
# Set JDK 17 on PATH
source set-jdk17.sh

# Or use the wrapper script (JDK 17 + Gradle + Android SDK)
./use-gradle.sh

# Build debug APK
./gradlew assembleDebug --no-daemon

# Build release APK (signed) — one-step script
./build-release.sh
# Output: GitForAndroid-release.apk
# Install: adb install GitForAndroid-release.apk

# Or manually:
./gradlew assembleRelease --no-daemon
```

## Running Tests

```bash
# All unit tests
./gradlew test --no-daemon

# Single test class
./gradlew test --tests "com.gitforandroid.domain.parser.GitCliParserTest" --no-daemon
```

## Project Structure

```
app/src/main/java/com/gitforandroid/
├── ui/                    # Compose screens + ViewModels
│   ├── gui/               # GUI mode screens (Status, Commit, Log, etc.)
│   ├── cli/               # CLI terminal screens + ViewModel
│   ├── common/            # Shared UI components
│   └── theme/             # Material 3 theme
├── domain/                # Use cases + CLI parser (no Android deps)
│   ├── parser/            # GitCliParser → CommandAST
│   └── usecase/           # Business logic use cases
├── data/
│   ├── git/               # GitService interface + JGit implementation
│   ├── local/             # Room database, DAOs, entities
│   └── repository/        # AppRepository (mediates DB + GitService)
├── di/                    # Hilt dependency injection module
├── navigation/            # Compose navigation graph
├── util/                  # Utility classes (FileUtils, SSH, permissions, logging)
├── GitApp.kt              # Application class
└── MainActivity.kt        # Single Activity host
```

## License

MIT

## Author

GitForAndroid
