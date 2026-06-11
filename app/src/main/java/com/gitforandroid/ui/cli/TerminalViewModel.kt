package com.gitforandroid.ui.cli

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gitforandroid.data.git.CliOutput
import com.gitforandroid.data.git.Credentials
import com.gitforandroid.data.git.model.Author
import com.gitforandroid.data.repository.AppRepository
import com.gitforandroid.domain.parser.GitCliParser
import com.gitforandroid.domain.parser.GitCommand
import com.gitforandroid.domain.usecase.ExecuteCliCommandUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class TerminalLine(
    val text: String,
    val type: TerminalLineType
)

enum class TerminalLineType {
    INPUT, OUTPUT, ERROR, SYSTEM
}

data class TerminalUiState(
    val lines: List<TerminalLine> = listOf(
        TerminalLine("Git For Android — Terminal Mode", TerminalLineType.SYSTEM),
        TerminalLine("Type 'git <command>' to execute. Type 'help' for available commands.", TerminalLineType.SYSTEM),
        TerminalLine("", TerminalLineType.OUTPUT)
    ),
    val currentInput: String = "",
    val currentRepoId: Long? = null,
    val currentRepoPath: String = "",
    val repoList: List<Pair<Long, String>> = emptyList(), // id -> name
    val commandHistory: List<String> = emptyList(),
    val historyIndex: Int = -1,
    val currentDraft: String = "" // preserves typed text when navigating history
)

@HiltViewModel
class TerminalViewModel @Inject constructor(
    private val executeCliCommandUseCase: ExecuteCliCommandUseCase,
    private val repository: AppRepository,
    private val parser: GitCliParser,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(TerminalUiState())
    val uiState: StateFlow<TerminalUiState> = _uiState.asStateFlow()

    private val maxHistorySize = 500

    init {
        viewModelScope.launch {
            repository.allRepos.collect { repos ->
                _uiState.update {
                    it.copy(repoList = repos.map { r -> r.id to r.name })
                }
            }
        }
    }

    fun updateInput(input: String) {
        _uiState.update {
            it.copy(currentInput = input, historyIndex = -1, currentDraft = "")
        }
    }

    fun selectRepo(repoId: Long) {
        val repo = _uiState.value.repoList.find { it.first == repoId }
        if (repo != null) {
            _uiState.update { it.copy(currentRepoId = repoId, currentRepoPath = repo.second) }
            appendSystem("Switched to repo: ${repo.second} [id=$repoId]")
        }
    }

    fun navigateHistory(direction: Int): String {
        val state = _uiState.value
        val history = state.commandHistory
        if (history.isEmpty()) return state.currentInput

        val newIndex = (state.historyIndex + direction).coerceIn(-1, history.lastIndex)
        val input = when {
            newIndex >= 0 -> history[history.size - 1 - newIndex]
            else -> state.currentDraft // restore draft when exiting history
        }

        _uiState.update {
            it.copy(
                currentInput = input,
                historyIndex = newIndex,
                // Save current input as draft when first entering history navigation
                currentDraft = if (it.historyIndex == -1 && newIndex >= 0) it.currentInput else it.currentDraft
            )
        }
        return input
    }

    fun executeCommand() {
        val input = _uiState.value.currentInput.trim()
        if (input.isEmpty()) return

        // Update history
        val newHistory = (_uiState.value.commandHistory + input).takeLast(maxHistorySize)

        _uiState.update {
            it.copy(
                lines = it.lines + TerminalLine("$ ${it.currentInput}", TerminalLineType.INPUT),
                currentInput = "",
                commandHistory = newHistory,
                historyIndex = -1,
                currentDraft = ""
            )
        }

        viewModelScope.launch {
            when {
                input == "help" -> {
                    appendOutput(helpText)
                }
                input == "clear" || input == "cls" -> {
                    _uiState.update { it.copy(lines = emptyList()) }
                }
                input == "pwd" -> {
                    val path = _uiState.value.currentRepoPath
                    if (path.isNotEmpty()) {
                        appendOutput(path)
                    } else {
                        appendOutput("No repository selected.")
                    }
                }
                input.startsWith("repo ") -> {
                    handleRepoCommand(input.removePrefix("repo "))
                }
                // git config — doesn't need a repo (handled before repo check)
                input == "git config" || input.startsWith("git config ") -> {
                    handleGitConfig(input)
                }
                input == "git init" || input.startsWith("git init ") -> {
                    handleRepoCreationCommand(input)
                }
                input == "git clone" || input.startsWith("git clone ") -> {
                    handleRepoCreationCommand(input)
                }
                input.startsWith("git ") -> {
                    var repoId = _uiState.value.currentRepoId

                    if (repoId == null) {
                        val repos = _uiState.value.repoList
                        when {
                            repos.size == 1 -> {
                                // Auto-select the only available repo
                                val singleRepo = repos[0]
                                repoId = singleRepo.first
                                _uiState.update {
                                    it.copy(currentRepoId = repoId, currentRepoPath = singleRepo.second)
                                }
                                appendSystem("Auto-selected repo '${singleRepo.second}' [id=$repoId]")
                            }
                            repos.isEmpty() -> {
                                appendError("No repositories found. Use 'git init <name>' or 'git clone <url>' to get started.")
                                return@launch
                            }
                            else -> {
                                appendError("No repository selected. Use 'repo list' and 'repo use <id>' to select one.")
                                return@launch
                            }
                        }
                    }

                    // Show immediate feedback for slow operations
                    if (input.matches(Regex("git (checkout|co) .+"))) {
                        val branch = input.removePrefix("git ").removePrefix("checkout ").removePrefix("co ").trim()
                        appendSystem("Checking out branch '$branch'...")
                    }

                    // Load author from settings
                    val name = repository.getSetting("author_name") ?: "Unknown"
                    val email = repository.getSetting("author_email") ?: "unknown@example.com"
                    val author = Author(name, email)

                    executeCliCommandUseCase(repoId!!, input, author)
                        .onSuccess { output ->
                            appendOutput(output.stdout)
                            if (output.stderr.isNotEmpty()) appendError(output.stderr)
                        }
                        .onFailure { e ->
                            appendError(e.message ?: "Command failed")
                        }
                }
                else -> {
                    appendError("Unknown command: $input\nType 'help' for available commands, or prefix git commands with 'git '.")
                }
            }
        }
    }

    // --- Repo creation (git init / git clone) ---

    private suspend fun handleRepoCreationCommand(input: String) {
        val trimmed = input.trim()
        when {
            trimmed == "git init" || trimmed.startsWith("git init ") -> {
                val arg = trimmed.removePrefix("git init").trim()
                val repoName = arg.ifBlank { "my-repo" }
                val reposDir = File(context.filesDir, "repos").also { it.mkdirs() }
                val targetPath = File(reposDir, repoName).absolutePath

                appendSystem("Initializing empty Git repository in $targetPath...")

                repository.initRepo(targetPath, repoName)
                    .onSuccess { newRepoId ->
                        _uiState.update {
                            it.copy(currentRepoId = newRepoId, currentRepoPath = targetPath)
                        }
                        appendOutput("Initialized empty Git repository in $targetPath")
                        appendSystem("Auto-selected repo '$repoName' [id=$newRepoId]")
                    }
                    .onFailure { e ->
                        appendError("Failed to init repo: ${e.message}")
                    }
            }
            trimmed.startsWith("git clone ") -> {
                val parsed = parser.parse(trimmed)
                if (parsed is GitCommand.Clone) {
                    val reposDir = File(context.filesDir, "repos").also { it.mkdirs() }
                    val repoName = parsed.path
                        ?: parsed.url.substringAfterLast("/").removeSuffix(".git").ifBlank { "repo" }
                    val targetPath = File(reposDir, repoName).absolutePath

                    appendSystem("Cloning ${parsed.url} into $targetPath...")

                    repository.cloneRepo(
                        url = parsed.url,
                        localPath = targetPath,
                        name = repoName,
                        progress = { message ->
                            appendSystem(message)
                        }
                    )
                        .onSuccess { result ->
                            _uiState.update {
                                it.copy(currentRepoId = result.repoId, currentRepoPath = targetPath)
                            }
                            appendOutput("Cloned into $targetPath")
                            appendSystem("Auto-selected repo '${result.repoName}' [id=${result.repoId}]")
                        }
                        .onFailure { e ->
                            appendError("Clone failed: ${e.message}")
                        }
                } else {
                    appendError("Failed to parse clone command. Usage: git clone <url> [path]")
                }
            }
        }
    }

    // --- Git config handling ---

    private suspend fun handleGitConfig(input: String) {
        val parsed = parser.parse(input.trim())
        if (parsed !is GitCommand.Config) {
            appendError("Failed to parse config command. Usage: git config <key> [value]")
            return
        }

        when {
            parsed.key == "user.name" && parsed.value != null -> {
                repository.setSetting("author_name", parsed.value)
                appendOutput("Set user.name = ${parsed.value}")
            }
            parsed.key == "user.email" && parsed.value != null -> {
                repository.setSetting("author_email", parsed.value)
                appendOutput("Set user.email = ${parsed.value}")
            }
            parsed.key == "user.name" && parsed.value == null -> {
                val current = repository.getSetting("author_name") ?: "not set"
                appendOutput("user.name = $current")
            }
            parsed.key == "user.email" && parsed.value == null -> {
                val current = repository.getSetting("author_email") ?: "not set"
                appendOutput("user.email = $current")
            }
            parsed.value != null -> {
                appendError("Unsupported config key: ${parsed.key}. Supported keys: user.name, user.email")
            }
            else -> {
                appendError("Usage: git config <key> [value]. Supported keys: user.name, user.email")
            }
        }
    }

    // --- Repo management ---

    private fun handleRepoCommand(args: String) {
        val parts = args.split("\\s+".toRegex())
        when (parts.firstOrNull()) {
            "list" -> {
                val repos = _uiState.value.repoList
                if (repos.isEmpty()) {
                    appendOutput("No repositories. Use 'git init <name>' or 'git clone <url>' to get started.")
                } else {
                    appendOutput(repos.joinToString("\n") { (id, name) -> "  [$id] $name" })
                }
            }
            "use" -> {
                val idStr = parts.getOrNull(1)
                val id = idStr?.toLongOrNull()
                if (id != null) {
                    val repo = _uiState.value.repoList.find { it.first == id }
                    if (repo != null) {
                        _uiState.update { it.copy(currentRepoId = id, currentRepoPath = repo.second) }
                        appendOutput("Switched to repo: ${repo.second} [id=$id]")
                    } else {
                        appendError("Repo with id=$id not found.")
                    }
                } else {
                    appendError("Usage: repo use <id>")
                }
            }
            "current" -> {
                val state = _uiState.value
                if (state.currentRepoId != null) {
                    val repo = state.repoList.find { it.first == state.currentRepoId }
                    if (repo != null) {
                        appendOutput("[${repo.first}] ${repo.second}")
                    } else {
                        appendOutput("[${state.currentRepoId}] (name not in list)")
                    }
                    if (state.currentRepoPath.isNotEmpty()) {
                        appendOutput("  Path: ${state.currentRepoPath}")
                    }
                } else {
                    appendOutput("No repository selected. Use 'repo list' and 'repo use <id>' to select one.")
                }
            }
            else -> appendError("Unknown repo command: $args\nAvailable: repo list, repo use <id>, repo current")
        }
    }

    // --- Output helpers ---

    private fun appendOutput(text: String) {
        if (text.isBlank()) {
            _uiState.update {
                it.copy(lines = it.lines + TerminalLine("", TerminalLineType.OUTPUT))
            }
            return
        }
        _uiState.update {
            it.copy(lines = it.lines + text.lines().map { line ->
                TerminalLine(line, TerminalLineType.OUTPUT)
            })
        }
    }

    private fun appendError(text: String) {
        _uiState.update {
            it.copy(lines = it.lines + TerminalLine(text, TerminalLineType.ERROR))
        }
    }

    private fun appendSystem(text: String) {
        _uiState.update {
            it.copy(lines = it.lines + TerminalLine(text, TerminalLineType.SYSTEM))
        }
    }

    companion object {
        private val helpText = """
            |Git For Android — Terminal Commands
            |
            |Built-in commands:
            |  help                 Show this help
            |  clear / cls          Clear the terminal
            |  pwd                  Show current repository path
            |  repo list            List all repositories
            |  repo use <id>        Select a repository
            |  repo current         Show the currently selected repository
            |
            |Git commands (those marked * work without selecting a repo first):
            |  git init [name]      * Initialize a new repository
            |  git clone <url> [path] * Clone a repository
            |  git config <key> [val] Set/view author (user.name, user.email)
            |  git status           Show working tree status
            |  git add <files...>   Stage changes
            |  git commit -m <msg>  Commit staged changes
            |  git push [remote] [branch]
            |  git pull [remote] [branch]
            |  git fetch [remote]   Fetch from remote
            |  git log [-n <count>] [--oneline]
            |  git branch [-a] [-d <name>]
            |  git checkout <branch> [-b]
            |  git merge <branch>   Merge branch into current
            |  git diff [--staged] [path]
            |  git stash [pop|list]
            |
            |Tip: 'git init' and 'git clone' work without selecting a repo first.
            |     If you have only one repo, 'git status' auto-selects it.
        """.trimMargin()
    }
}
