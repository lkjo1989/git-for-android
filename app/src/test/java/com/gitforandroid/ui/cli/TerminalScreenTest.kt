package com.gitforandroid.ui.cli

import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gitforandroid.TestApplication
import com.gitforandroid.data.git.GitServiceImpl
import com.gitforandroid.data.local.AppDatabase
import com.gitforandroid.data.repository.AppRepository
import com.gitforandroid.domain.parser.GitCliParser
import com.gitforandroid.domain.usecase.ExecuteCliCommandUseCase
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlinx.coroutines.runBlocking
import org.robolectric.Shadows
import org.robolectric.annotation.Config

/**
 * ViewModel-level tests for the Terminal.
 *
 * Uses Robolectric for the Android framework (Context, Room in-memory DB),
 * but tests ViewModel state directly instead of Compose UI nodes.
 * This bypasses the Robolectric + Compose ActivityScenario limitation.
 *
 * For full Compose UI tests, instrumented tests on emulator/device are recommended.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33], application = TestApplication::class)
class TerminalScreenTest {

    private lateinit var viewModel: TerminalViewModel
    private lateinit var repository: AppRepository

    @Before
    fun setUp() {
        val appContext = ApplicationProvider.getApplicationContext<android.content.Context>()

        val db = Room.inMemoryDatabaseBuilder(appContext, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        val gitService = GitServiceImpl(appContext.applicationContext)
        repository = AppRepository(db.repoDao(), db.settingDao(), gitService, appContext)
        val parser = GitCliParser()
        val useCase = ExecuteCliCommandUseCase(repository, parser)

        viewModel = TerminalViewModel(useCase, repository, parser, appContext.applicationContext)
    }

    // ── Initial state ────────────────────────────────────────

    @Test
    fun `uiState initializes with expected defaults`() {
        val state = viewModel.uiState.value
        assert(state.currentRepoId == null) { "Expected no repo selected initially" }
        assert(state.repoList.isEmpty()) { "Expected empty repo list" }
        assert(state.lines.size == 3) { "Expected 3 initial welcome lines" }
        assert(state.currentInput.isEmpty()) { "Expected empty input" }
        assert(state.historyIndex == -1) { "Expected history index -1" }
        assert(state.currentDraft.isEmpty()) { "Expected empty draft" }
    }

    @Test
    fun `welcome messages are SYSTEM type`() {
        val state = viewModel.uiState.value
        val welcome = state.lines[0]
        assert(welcome.text == "Git For Android — Terminal Mode")
        assert(welcome.type == TerminalLineType.SYSTEM)
    }

    // ── Input management ─────────────────────────────────────

    @Test
    fun `updateInput changes currentInput`() {
        viewModel.updateInput("git status")
        assert(viewModel.uiState.value.currentInput == "git status")
    }

    @Test
    fun `updateInput resets history index`() {
        // Set up some history
        viewModel.updateInput("cmd1")
        viewModel.executeCommand()
        viewModel.updateInput("cmd2")
        viewModel.executeCommand()

        // Navigate into history
        viewModel.navigateHistory(1)
        assert(viewModel.uiState.value.historyIndex >= 0)

        // Typing should reset
        viewModel.updateInput("new input")
        assert(viewModel.uiState.value.historyIndex == -1)
    }

    // ── Command history ──────────────────────────────────────

    @Test
    fun `executeCommand adds input to history`() {
        viewModel.updateInput("git status")
        viewModel.executeCommand()

        val history = viewModel.uiState.value.commandHistory
        assert(history.contains("git status"))
    }

    @Test
    fun `history preserves draft input when navigating`() {
        viewModel.updateInput("cmd1")
        viewModel.executeCommand()
        viewModel.updateInput("cmd2")
        viewModel.executeCommand()

        // Type a draft
        viewModel.updateInput("my draft text")

        // Navigate up (into history)
        viewModel.navigateHistory(1)
        assert(viewModel.uiState.value.historyIndex >= 0)

        // Navigate back down past end — should restore draft
        viewModel.navigateHistory(-1)
        assert(viewModel.uiState.value.currentInput == "my draft text")
    }

    @Test
    fun `history capped at 500 entries`() {
        for (i in 1..600) {
            viewModel.updateInput("command $i")
            viewModel.executeCommand()
        }
        val history = viewModel.uiState.value.commandHistory
        assert(history.size <= 500) { "History should be capped at 500" }
        assert(!history.contains("command 1")) { "Oldest entries should be dropped" }
        assert(history.contains("command 600")) { "Newest entries should be kept" }
    }

    // ── Command execution ────────────────────────────────────

    @Test
    fun `empty input is a no-op`() {
        val countBefore = viewModel.uiState.value.lines.size
        viewModel.executeCommand()
        assert(viewModel.uiState.value.lines.size == countBefore)
    }

    @Test
    fun `clear command removes all lines`() {
        viewModel.updateInput("clear")
        viewModel.executeCommand()
        assert(viewModel.uiState.value.lines.isEmpty())
    }

    @Test
    fun `cls alias also clears`() {
        viewModel.updateInput("cls")
        viewModel.executeCommand()
        assert(viewModel.uiState.value.lines.isEmpty())
    }

    @Test
    fun `help command adds output lines`() {
        val countBefore = viewModel.uiState.value.lines.size
        viewModel.updateInput("help")
        viewModel.executeCommand()
        // help runs on Dispatchers.Main.immediate — already executed synchronously
        // on the main thread. But appendOutput is called synchronously too.
        val linesAfter = viewModel.uiState.value.lines
        assert(linesAfter.size > countBefore) { "Help should add output lines. Before: $countBefore, After: ${linesAfter.size}" }
        assert(linesAfter.any { it.text.contains("Built-in commands") })
    }

    @Test
    fun `unknown command shows error`() {
        viewModel.updateInput("foobar")
        viewModel.executeCommand()
        val lines = viewModel.uiState.value.lines
        assert(lines.any { it.type == TerminalLineType.ERROR })
    }

    @Test
    fun `pwd shows message when no repo selected`() {
        viewModel.updateInput("pwd")
        viewModel.executeCommand()
        val lines = viewModel.uiState.value.lines
        assert(lines.any { it.text == "No repository selected." })
    }

    // ── Repo management ──────────────────────────────────────

    @Test
    fun `repo list shows empty message when no repos`() {
        viewModel.updateInput("repo list")
        viewModel.executeCommand()
        val lines = viewModel.uiState.value.lines
        assert(lines.any {
            it.text.contains("No repositories") || it.text.contains("get started")
        })
    }

    @Test
    fun `repo use with invalid id shows error`() {
        viewModel.updateInput("repo use 999")
        viewModel.executeCommand()
        val lines = viewModel.uiState.value.lines
        assert(lines.any { it.type == TerminalLineType.ERROR })
    }

    @Test
    fun `repo current shows no repo message when none selected`() {
        viewModel.updateInput("repo current")
        viewModel.executeCommand()
        val lines = viewModel.uiState.value.lines
        assert(lines.any {
            it.text.contains("No repository selected")
        })
    }

    @Test
    fun `selectRepo with nonexistent id does not change state`() {
        val stateBefore = viewModel.uiState.value
        viewModel.selectRepo(999L)
        val stateAfter = viewModel.uiState.value
        assert(stateAfter.currentRepoId == stateBefore.currentRepoId)
    }

    // ── Git commands without repo ────────────────────────────

    @Test
    fun `git status without repo shows error`() {
        viewModel.updateInput("git status")
        viewModel.executeCommand()
        val lines = viewModel.uiState.value.lines
        assert(lines.any {
            it.type == TerminalLineType.ERROR &&
            it.text.contains("No repositories found")
        })
    }

    // ── Git init ─────────────────────────────────────────────

    @Test
    fun `git init creates repo and auto-selects it`() {
        viewModel.updateInput("git init testrepo")
        viewModel.executeCommand()
        // git init runs repository.initRepo() on Dispatchers.IO, then posts
        // back to Main. Wait for the IO to complete and Main to process.
        waitForState { viewModel.uiState.value.currentRepoId != null }

        val state = viewModel.uiState.value
        assert(state.currentRepoId != null) { "Newly created repo should be auto-selected, got null" }
        assert(state.currentRepoPath.isNotEmpty()) { "Path should be set after init" }
    }

    @Test
    fun `git init without name defaults to my-repo`() {
        viewModel.updateInput("git init")
        viewModel.executeCommand()
        waitForState { viewModel.uiState.value.currentRepoId != null }

        val state = viewModel.uiState.value
        assert(state.currentRepoId != null) { "Repo should be auto-selected" }
    }

    /** Poll for a state condition, draining the main Looper between polls. */
    private fun waitForState(
        timeoutMs: Long = 5000,
        condition: () -> Boolean
    ) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            if (condition()) return
            Thread.sleep(10)
        }
    }

    // ── Git config ───────────────────────────────────────────

    @Test
    fun `git config user dot name sets author name`() = runBlocking {
        viewModel.updateInput("git config user.name TestUser")
        viewModel.executeCommand()
        // handleGitConfig runs in viewModelScope.launch; config writes via
        // Room suspend DAO. Drain looper + poll for async completion.
        waitForState { runBlocking { repository.getSetting("author_name") } == "TestUser" }

        val name = repository.getSetting("author_name")
        assert(name == "TestUser") { "Author name should be persisted, got: $name" }
    }

    @Test
    fun `git config user dot email sets author email`() = runBlocking {
        viewModel.updateInput("git config user.email test@example.com")
        viewModel.executeCommand()
        waitForState { runBlocking { repository.getSetting("author_email") } == "test@example.com" }

        val email = repository.getSetting("author_email")
        assert(email == "test@example.com") { "Author email should be persisted, got: $email" }
    }

    @Test
    fun `git config without value reads current setting`() {
        runBlocking { repository.setSetting("author_name", "ExistingName") }

        viewModel.updateInput("git config user.name")
        viewModel.executeCommand()
        // Drain both the main Looper and allow background IO to complete
        Thread.sleep(50)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        val lines = viewModel.uiState.value.lines
        assert(lines.any { it.text.contains("ExistingName") }) {
            "Expected 'ExistingName' in output, got lines:\n${lines.joinToString("\n") { "[${it.type}] ${it.text}" }}"
        }
    }
}
