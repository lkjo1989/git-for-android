# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Environment

- **JDK 8** at `/usr/local/jdk8u412-full/` — global default, do not modify or remove
- **JDK 17** at `.jdk17/` (Temurin 17.0.19) — required for this project
- **Android SDK** at `/usr/local/android-sdk/`
- **System proxy** at `http://192.168.56.1:7890` — Java/Gradle don't use `http_proxy` env vars automatically, pass JVM args explicitly

## Build

Gradle wrapper is configured for **Gradle 8.8**. The proxy is required for dependency downloads.

Locally, Gradle discovers JDK 17 via `JAVA_HOME` (set by `source set-jdk17.sh`). CI uses `setup-java@v4` to set `JAVA_HOME` instead. Do NOT hardcode `org.gradle.java.home` in `gradle.properties` — it breaks CI portability.

Alternatively, use `use-gradle.sh` which wraps JDK 17 + system Gradle 8.8 + Android SDK.

```bash
# Full build with proxy
source set-jdk17.sh
export GRADLE_OPTS="-Dhttps.proxyHost=192.168.56.1 -Dhttps.proxyPort=7890 -Dhttp.proxyHost=192.168.56.1 -Dhttp.proxyPort=7890"
./gradlew assembleDebug --no-daemon

# Release APK (one-step script — generates keystore if needed, outputs GitForAndroid-release.apk)
./build-release.sh
```

### Running tests

Tests use **JUnit 4** (not JUnit 5). Coroutine-aware tests use `kotlinx-coroutines-test` with `runTest`.

```bash
# All unit tests
./gradlew test --no-daemon

# Single test class
./gradlew test --tests "com.gitforandroid.domain.parser.GitCliParserTest" --no-daemon

# Single test method
./gradlew test --tests "com.gitforandroid.domain.parser.GitCliParserTest.parse git commit -m with message" --no-daemon
```

There are currently no Android instrumented tests; `androidTest/` exists in the source tree but is empty.

### Clearing corrupted Gradle cache

If Gradle transform cache gets corrupted (from interrupted builds), clear it:
```bash
rm -rf /root/.gradle/caches/8.8/transforms /root/git-for-android/app/build /root/git-for-android/.gradle
```

## Architecture

**GitForAndroid** — dual-mode Android Git client with both GUI (Jetpack Compose + Material 3) and CLI terminal interfaces. Uses JGit as the Git engine (no native git binary required).

### Tech stack versions

| Dependency | Version |
|------------|---------|
| Kotlin | 1.9.24 |
| AGP | 8.4.1 |
| Gradle | 8.8 |
| Compose BOM | 2024.06.00 |
| Compose compiler ext | 1.5.14 |
| Hilt | 2.51.1 (KSP for annotation processing) |
| JGit | 6.10.0 |
| Room | 2.6.1 |
| compileSdk / targetSdk | 34 |
| minSdk | 26 (Android 8.0) |

### Layer stack (top-down)

```
ui/              Compose screens + ViewModels (Hilt-injected)
domain/          Use cases + CLI parser (no Android dependencies)
data/repository  Single AppRepository mediating DB and GitService
data/git/        GitService interface → GitServiceImpl (JGit wrapper)
data/local/      Room database (gitforandroid.db): RepoDao + SettingDao
```

### Key architectural patterns

- **Single Activity**: `MainActivity` hosts all Compose UI via `AppNavHost`
- **Dependency injection**: Hilt (`@HiltAndroidApp` on `GitApp`, `@AndroidEntryPoint` on `MainActivity`, `@HiltViewModel` for all ViewModels). Module: `di/AppModule.kt`
- **Navigation**: Bottom nav with 3 tabs (Repos, Terminal, Settings). Repo-specific screens (Status, Commit, Log, Branches, PushPull) are pushed onto the nav stack with `repoId` as a nav argument
- **Dual mode**: GUI screens under `ui/gui/`, CLI terminal under `ui/cli/`. Both use the same `AppRepository` → `GitService` backend
- **CLI parsing**: `domain/parser/GitCliParser` tokenizes git command strings into `GitCommand` sealed-class AST; `ExecuteCliCommandUseCase` dispatches each variant to the appropriate repository method
- **CLI command dispatch order** (in `TerminalViewModel.executeCommand()`): built-ins (`help`/`clear`/`pwd`) → `repo <sub>` → `git config` (no repo needed) → `git init`/`git clone` (handled directly via `AppRepository`, no pre-selected repoId required, auto-selects created repo) → other `git <cmd>` (requires repoId; auto-selects if only 1 repo exists; shows guided error if 0 or >1)
- **Error handling**: All `GitService` and `AppRepository` methods return `kotlin.Result<T>`; ViewModels map results to UI state via `MutableStateFlow.update {}`. `ExecuteCliCommandUseCase` preserves the failure channel (no longer wraps everything in `Result.success`) so `.onFailure {}` fires in ViewModel.

### Key classes

| Class | Role |
|-------|------|
| `GitService` (interface) | JGit abstraction: init, clone, status, add, commit, push, pull, fetch, log, branches, checkout, merge, diff, stash, executeRaw, getCurrentBranch |
| `GitServiceImpl` | JGit 6.10 implementation — all ops on `Dispatchers.IO`, extension functions map JGit types to domain models |
| `AppRepository` | Single data-layer entry point; maps `repoId` → `localPath` via Room DAO, delegates to `GitService`, manages `allRepos: Flow<List<RepoEntity>>` |
| `GitCliParser` | Tokenizer + parser: `"git commit -m 'msg'"` → `GitCommand.Commit(message="msg")`. Handles quoting, short flags (`-am`), aliases (`git co` → checkout) |
| `CommandAST` (`GitCommand` sealed class) | AST variants: Init, Clone, Status, Add, Commit, Push, Pull, Fetch, Log, Branch, Checkout, Merge, Diff, Stash, Remote, Config, Unknown |
| `ExecuteCliCommandUseCase` | Takes parsed command + repoId, dispatches to `AppRepository`, returns `Result<CliOutput>`. Errors propagate via `Result.failure()` — the ViewModel's `.onFailure {}` handler displays them. `Config` and repo-creation commands (`Init`/`Clone` without pre-selected repoId) are handled directly in `TerminalViewModel`, not here. |
| `TerminalViewModel` | Manages CLI REPL: input state, 500-entry command history with draft preservation, `help`/`clear`/`pwd` built-ins, `repo list|use|current`, `git config user.name|user.email` for author settings, `git init`/`git clone` handled directly (no repoId needed) with auto-select on success, single-repo auto-select for other git commands |
| `AppNavHost` | Central navigation graph: defined in `navigation/AppNavHost.kt`, all routes in `navigation/Screen.kt`, bottom bar visibility logic |
| `AppModule` (DI) | Hilt `@Module`: provides singletons for database, DAOs, GitService, GitCliParser, AppRepository |
| `Author` | Data class: `name`, `email` — passed through commit/push/pull flows from Settings to JGit's `PersonIdent` |
| `Credentials` | Data class: `username`, `password`, `sshKeyPath`, `sshPassphrase` — used by clone/push/pull/fetch for auth |
| `CheckoutResult` | Return type for checkout: `previousBranch`, `newBranch`, `message` |
| `SshKeyManager` | Utility: manages SSH keypair storage under app files dir, stub for key generation |
| `FileUtils` | Utility: directory creation, file listing, relative path resolution, human-readable sizes |
| `StoragePermissionManager` | Utility: storage permission checks (API-aware), SAF URI → file path conversion, default clone directory selection (public vs app-private) |
| `FileLogger` | Singleton file logger; writes timestamped entries to `files/logs/gitforandroid.log`, 5 MB rotation, controlled by `log_enabled` setting (default on) |

### Screen navigation flow

```
Home (repo list) ──→ Clone (modal)
  └──→ Status/{repoId} ──→ Commit/{repoId}
                      ├──→ Log/{repoId} ──→ CommitDetail/{repoId}/{hash}
                      ├──→ Branches/{repoId}
                      └──→ PushPull/{repoId}
Terminal (standalone, repo selected via "git init"/"git clone" auto-select, "repo use <id>", or auto-select when only 1 repo exists)
Settings (standalone)
```

### Round-tripping between modes

GUI-managed repos appear in the CLI's `repo list` via `repository.allRepos: Flow` collected in `TerminalViewModel.init`. A repo cloned in GUI → immediately visible in CLI, and vice versa.

## JGit compatibility notes

- `DirCache.writeTree()` takes `ObjectInserter`, not `ObjectReader` — pass `repository.newObjectInserter()`
- `RevCommit` has `commitTime` (int epoch seconds), no `committedAt` property
- `ProgressMonitor` interface includes `showDuration(Boolean)` — implement even as a no-op
- All JGit operations must run on `Dispatchers.IO` (blocking I/O)
- **Remote branch checkout**: `checkout()` auto-detects remote-tracking branches (`refs/remotes/origin/*`) and creates a local tracking branch via `setCreateBranch(true).setUpstreamMode(TRACK)` when no local branch exists — equivalent to `git checkout -b <name> --track origin/<name>`
- **Clone progress**: progress callback passes `String` (git-style messages like `"Receiving objects: 45% (123/456)"`), not `Float`. The `ProgressMonitorAdapter` formats JGit's `beginTask`/`update` into human-readable text.

## Signing (release builds)

Release APK is signed with a self-signed keystore at the project root:
- **Keystore**: `gitforandroid.keystore` (alias: `gitforandroid`, passwords: `android123`)
- **Config**: `app/build.gradle.kts` → `signingConfigs { create("release") { ... } }`
- Keystore is in `.gitignore` — never commit it. CI decodes it from `KEYSTORE_BASE64` secret.
- **Key compatibility**: the keystore MUST be generated with JDK 17's `keytool`. JDK 21+ produces PKCS#12 v2 (tags > 30) which JDK 17 cannot read, causing `Tag number over 30 is not supported`.
- Locally: generate a keystore with JDK 17 before running `assembleRelease`:
  ```bash
  keytool -genkeypair -v -keystore gitforandroid.keystore -storetype PKCS12 \
    -alias gitforandroid -keyalg RSA -keysize 2048 -validity 10000 \
    -storepass android123 -keypass android123 \
    -dname "CN=GitForAndroid, OU=Dev, O=GitForAndroid, L=Unknown, ST=Unknown, C=CN"
  ```
- Release APK output uses default name `app-release.apk` (CI picks it up from `app/build/outputs/apk/release/`)

## CI / Release (GitHub Actions)

Workflows in `.github/workflows/`:

| Workflow | Trigger | Actions |
|----------|---------|---------|
| `ci.yml` | push/PR to `main` | build + test, uploads debug APK artifact |
| `release.yml` | tag push (`v*`) | test → decode keystore from secret → `assembleRelease` → create GitHub Release with APK |

**Tag-version sync**: Git tag (e.g. `v1.0.0`) should match `versionName` in `app/build.gradle.kts`. Bump both when releasing. APK uses the default AGP filename (`app-release.apk`).

**Required secret** for releases: `KEYSTORE_BASE64` — base64 of `gitforandroid.keystore` generated with JDK 17 (not JDK 21+):
```bash
# Generate keystore with JDK 17, then encode:
keytool -genkeypair -v -keystore gitforandroid.keystore -storetype PKCS12 \
  -alias gitforandroid -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass android123 -keypass android123 \
  -dname "CN=GitForAndroid, OU=Dev, O=GitForAndroid, L=Unknown, ST=Unknown, C=CN"
base64 -w0 gitforandroid.keystore
```

```bash
# Release process
# 1. Bump versionName in app/build.gradle.kts
# 2. Commit, push
# 3. Tag and push
git tag v1.0.0 && git push origin v1.0.0
# GitHub Actions builds and publishes the signed APK to Releases
```

## Icons

App uses Android adaptive icon (API 26+), defined entirely in XML vectors:
- `drawable/ic_launcher_background.xml` — Git orange (#F14E32) solid background
- `drawable/ic_launcher_foreground.xml` — white branching diagram (3 commit nodes + Y-shaped connector)
- `mipmap-anydpi-v26/ic_launcher.xml` + `ic_launcher_round.xml` — adaptive icon definitions
- No raster PNGs needed; the vector scales to all densities.

## Test patterns

- JUnit 4 (`@Test`, `@Before`, `@After`), **not** JUnit 5
- `kotlinx-coroutines-test` provides `runTest` which wraps test bodies to call `suspend` functions
- `GitServiceImplTest` creates temp directories under `java.io.tmpdir`, cleans up in `@After`
- `GitCliParserTest` is pure unit tests (no Android dependencies, no coroutines)

## Room database

Single database `gitforandroid.db` with two DAOs:
- `RepoDao` — CRUD for `RepoEntity` (id, name, localPath, remoteUrl, currentBranch, lastOpened)
- `SettingDao` — key-value store for settings. Key settings: `author_name`, `author_email`, `ssh_key_path`, `log_enabled`

## Storage & permissions

- `targetSdk = 34` enforces scoped storage on Android 11+. `Environment.getExternalStorageDirectory()` cannot be written to without `MANAGE_EXTERNAL_STORAGE`.
- **Default clone path**: when permission is granted → `/storage/emulated/0/GitRepos` (public); otherwise → `Android/data/com.gitforandroid/files/repos` (app-private, always writable).
- `CloneScreen` provides a SAF directory picker (`ActivityResultContracts.OpenDocumentTree`) for choosing custom paths without any permission.
- `StoragePermissionManager.extractPathFromTreeUri()` converts SAF content URIs to filesystem paths for JGit.
- `CloneViewModel.refreshPermissionState()` listens for `ON_RESUME` to auto-upgrade the default path when the user grants permission in system settings.
