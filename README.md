# Git For Android

A dual-mode Android Git client featuring both a GUI and a CLI terminal interface, powered by JGit — no native git binary required.

## Features

- **GUI Mode** — Jetpack Compose + Material 3 screens for common Git operations: status, commit, log, branches, push/pull
- **CLI Terminal Mode** — Full git command-line REPL with parsing of standard git commands (`git status`, `git commit -m "msg"`, `git push`, etc.)
- **Git Engine** — Built on JGit 6.10, runs entirely on-device with no external dependencies
- **Repository Management** — Clone, list, and switch between repositories; repos added in either mode are instantly visible in the other
- **SSH & Credentials** — Support for SSH key authentication and username/password credentials
- **Room Database** — Local storage for repo metadata and settings

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

# Build release APK (signed)
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
