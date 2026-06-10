package com.gitforandroid.ui.cli

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gitforandroid.data.git.CliOutput
import com.gitforandroid.data.git.model.Author
import com.gitforandroid.data.repository.AppRepository
import com.gitforandroid.domain.usecase.ExecuteCliCommandUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
    val historyIndex: Int = -1
)

@HiltViewModel
class TerminalViewModel @Inject constructor(
    private val executeCliCommandUseCase: ExecuteCliCommandUseCase,
    private val repository: AppRepository
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
        _uiState.update { it.copy(currentInput = input, historyIndex = -1) }
    }

    fun navigateHistory(direction: Int) {
        val history = _uiState.value.commandHistory
        if (history.isEmpty()) return

        val newIndex = (_uiState.value.historyIndex + direction).coerceIn(-1, history.lastIndex)
        val input = if (newIndex >= 0) history[history.size - 1 - newIndex] else ""

        _uiState.update {
            it.copy(currentInput = input, historyIndex = newIndex)
        }
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
                historyIndex = -1
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
                input.startsWith("repo ") -> {
                    handleRepoCommand(input.removePrefix("repo "))
                }
                input.startsWith("git ") -> {
                    val repoId = _uiState.value.currentRepoId
                    if (repoId == null) {
                        appendError("No repository selected. Use 'repo list' and 'repo use <id>' first.")
                    } else {
                        // Show immediate feedback for slow operations
                        if (input.matches(Regex("git (checkout|co) .+"))) {
                            val branch = input.removePrefix("git ").removePrefix("checkout ").removePrefix("co ").trim()
                            appendSystem("Checking out branch '$branch'...")
                        }

                        // Load author from settings
                        val name = repository.getSetting("author_name") ?: "Unknown"
                        val email = repository.getSetting("author_email") ?: "unknown@example.com"
                        val author = Author(name, email)

                        executeCliCommandUseCase(repoId, input, author)
                            .onSuccess { output ->
                                appendOutput(output.stdout)
                                if (output.stderr.isNotEmpty()) appendError(output.stderr)
                            }
                            .onFailure { e ->
                                appendError(e.message ?: "Command failed")
                            }
                    }
                }
                else -> {
                    appendError("Unknown command: $input\nType 'help' for available commands, or prefix git commands with 'git '.")
                }
            }
        }
    }

    private fun handleRepoCommand(args: String) {
        val parts = args.split("\\s+".toRegex())
        when (parts.firstOrNull()) {
            "list" -> {
                val repos = _uiState.value.repoList
                if (repos.isEmpty()) {
                    appendOutput("No repositories. Clone one from the Repos tab first.")
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
            else -> appendError("Unknown repo command: $args\nAvailable: repo list, repo use <id>")
        }
    }

    private fun appendOutput(text: String) {
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
            |  clear                Clear the terminal
            |  repo list            List all repositories
            |  repo use <id>        Select a repository
            |
            |Git commands (must have a repo selected):
            |  git init
            |  git clone <url> [path]
            |  git status
            |  git add <files...>
            |  git commit -m <message>
            |  git push [remote] [branch]
            |  git pull [remote] [branch]
            |  git fetch [remote]
            |  git log [-n <count>] [--oneline]
            |  git branch [-a] [-d <name>]
            |  git checkout <branch> [-b]
            |  git merge <branch>
            |  git diff [--staged] [path]
            |  git stash [pop|list]
            |
            |Tip: Use the Repos tab to clone repos with GUI.
        """.trimMargin()
    }
}
