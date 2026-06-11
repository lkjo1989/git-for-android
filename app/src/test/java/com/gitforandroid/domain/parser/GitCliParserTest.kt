package com.gitforandroid.domain.parser

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class GitCliParserTest {

    private lateinit var parser: GitCliParser

    @Before
    fun setUp() {
        parser = GitCliParser()
    }

    // --- Empty / Non-git input ---

    @Test
    fun `parse empty input returns Unknown`() {
        val result = parser.parse("")
        assertTrue(result is GitCommand.Unknown)
        assertEquals("", (result as GitCommand.Unknown).raw)
    }

    @Test
    fun `parse non-git command returns Unknown`() {
        val result = parser.parse("ls -la")
        assertTrue(result is GitCommand.Unknown)
        assertEquals("ls -la", (result as GitCommand.Unknown).raw)
    }

    @Test
    fun `parse git with no subcommand returns Unknown`() {
        val result = parser.parse("git")
        assertTrue(result is GitCommand.Unknown)
    }

    // --- Init ---

    @Test
    fun `parse git init`() {
        val result = parser.parse("git init")
        assertTrue(result is GitCommand.Init)
        assertNull((result as GitCommand.Init).path)
    }

    @Test
    fun `parse git init with path`() {
        val result = parser.parse("git init /tmp/repo")
        assertTrue(result is GitCommand.Init)
        assertEquals("/tmp/repo", (result as GitCommand.Init).path)
    }

    // --- Clone ---

    @Test
    fun `parse git clone with url`() {
        val result = parser.parse("git clone https://github.com/user/repo.git")
        assertTrue(result is GitCommand.Clone)
        val cmd = result as GitCommand.Clone
        assertEquals("https://github.com/user/repo.git", cmd.url)
        assertNull(cmd.path)
    }

    @Test
    fun `parse git clone with url and path`() {
        val result = parser.parse("git clone https://github.com/user/repo.git my-repo")
        assertTrue(result is GitCommand.Clone)
        val cmd = result as GitCommand.Clone
        assertEquals("https://github.com/user/repo.git", cmd.url)
        assertEquals("my-repo", cmd.path)
    }

    @Test
    fun `parse git clone without url returns Unknown`() {
        val result = parser.parse("git clone")
        assertTrue(result is GitCommand.Unknown)
    }

    // --- Status ---

    @Test
    fun `parse git status`() {
        val result = parser.parse("git status")
        assertTrue(result is GitCommand.Status)
        assertFalse((result as GitCommand.Status).porcelain)
    }

    @Test
    fun `parse git status --porcelain`() {
        val result = parser.parse("git status --porcelain")
        assertTrue(result is GitCommand.Status)
        assertTrue((result as GitCommand.Status).porcelain)
    }

    @Test
    fun `parse git status -s`() {
        val result = parser.parse("git status -s")
        assertTrue(result is GitCommand.Status)
        assertTrue((result as GitCommand.Status).porcelain)
    }

    // --- Add ---

    @Test
    fun `parse git add single file`() {
        val result = parser.parse("git add file.txt")
        assertTrue(result is GitCommand.Add)
        assertEquals(listOf("file.txt"), (result as GitCommand.Add).files)
    }

    @Test
    fun `parse git add multiple files`() {
        val result = parser.parse("git add file1.txt file2.txt file3.txt")
        assertTrue(result is GitCommand.Add)
        assertEquals(listOf("file1.txt", "file2.txt", "file3.txt"), (result as GitCommand.Add).files)
    }

    @Test
    fun `parse git add with no files defaults to dot`() {
        val result = parser.parse("git add")
        assertTrue(result is GitCommand.Add)
        assertEquals(listOf("."), (result as GitCommand.Add).files)
    }

    @Test
    fun `parse git add ignores flags`() {
        val result = parser.parse("git add -A file.txt")
        assertTrue(result is GitCommand.Add)
        assertEquals(listOf("file.txt"), (result as GitCommand.Add).files)
    }

    // --- Commit ---

    @Test
    fun `parse git commit -m with message`() {
        val result = parser.parse("git commit -m 'fix bug'")
        assertTrue(result is GitCommand.Commit)
        val cmd = result as GitCommand.Commit
        assertEquals("fix bug", cmd.message)
        assertFalse(cmd.all)
    }

    @Test
    fun `parse git commit -m without space`() {
        val result = parser.parse("git commit -m'fix bug'")
        assertTrue(result is GitCommand.Commit)
        assertEquals("fix bug", (result as GitCommand.Commit).message)
    }

    @Test
    fun `parse git commit -am`() {
        val result = parser.parse("git commit -am 'fix all'")
        assertTrue(result is GitCommand.Commit)
        val cmd = result as GitCommand.Commit
        assertEquals("fix all", cmd.message)
        assertTrue(cmd.all)
    }

    @Test
    fun `parse git commit --all`() {
        val result = parser.parse("git commit --all -m 'commit all'")
        assertTrue(result is GitCommand.Commit)
        val cmd = result as GitCommand.Commit
        assertTrue(cmd.all)
        assertEquals("commit all", cmd.message)
    }

    // --- Push ---

    @Test
    fun `parse git push with no args`() {
        val result = parser.parse("git push")
        assertTrue(result is GitCommand.Push)
        val cmd = result as GitCommand.Push
        assertNull(cmd.remote)
        assertNull(cmd.branch)
    }

    @Test
    fun `parse git push origin main`() {
        val result = parser.parse("git push origin main")
        assertTrue(result is GitCommand.Push)
        val cmd = result as GitCommand.Push
        assertEquals("origin", cmd.remote)
        assertEquals("main", cmd.branch)
    }

    @Test
    fun `parse git push with force flag ignores flag`() {
        val result = parser.parse("git push -f origin main")
        assertTrue(result is GitCommand.Push)
        val cmd = result as GitCommand.Push
        assertEquals("origin", cmd.remote)
        assertEquals("main", cmd.branch)
    }

    // --- Pull ---

    @Test
    fun `parse git pull`() {
        val result = parser.parse("git pull")
        assertTrue(result is GitCommand.Pull)
        assertNull((result as GitCommand.Pull).remote)
    }

    @Test
    fun `parse git pull origin main`() {
        val result = parser.parse("git pull origin main")
        assertTrue(result is GitCommand.Pull)
        val cmd = result as GitCommand.Pull
        assertEquals("origin", cmd.remote)
        assertEquals("main", cmd.branch)
    }

    // --- Fetch ---

    @Test
    fun `parse git fetch`() {
        val result = parser.parse("git fetch")
        assertTrue(result is GitCommand.Fetch)
        assertNull((result as GitCommand.Fetch).remote)
    }

    @Test
    fun `parse git fetch upstream`() {
        val result = parser.parse("git fetch upstream")
        assertTrue(result is GitCommand.Fetch)
        assertEquals("upstream", (result as GitCommand.Fetch).remote)
    }

    // --- Log ---

    @Test
    fun `parse git log with defaults`() {
        val result = parser.parse("git log")
        assertTrue(result is GitCommand.Log)
        val cmd = result as GitCommand.Log
        assertEquals(50, cmd.maxCount)
        assertFalse(cmd.oneline)
        assertFalse(cmd.graph)
    }

    @Test
    fun `parse git log --oneline`() {
        val result = parser.parse("git log --oneline")
        assertTrue(result is GitCommand.Log)
        assertTrue((result as GitCommand.Log).oneline)
    }

    @Test
    fun `parse git log -n 10`() {
        val result = parser.parse("git log -n 10")
        assertTrue(result is GitCommand.Log)
        assertEquals(10, (result as GitCommand.Log).maxCount)
    }

    @Test
    fun `parse git log -n5 compact`() {
        val result = parser.parse("git log -n5")
        assertTrue(result is GitCommand.Log)
        assertEquals(5, (result as GitCommand.Log).maxCount)
    }

    @Test
    fun `parse git log --graph`() {
        val result = parser.parse("git log --graph")
        assertTrue(result is GitCommand.Log)
        assertTrue((result as GitCommand.Log).graph)
    }

    // --- Branch ---

    @Test
    fun `parse git branch`() {
        val result = parser.parse("git branch")
        assertTrue(result is GitCommand.Branch)
        val cmd = result as GitCommand.Branch
        assertFalse(cmd.listAll)
        assertNull(cmd.delete)
        assertNull(cmd.create)
    }

    @Test
    fun `parse git branch -a`() {
        val result = parser.parse("git branch -a")
        assertTrue(result is GitCommand.Branch)
        assertTrue((result as GitCommand.Branch).listAll)
    }

    @Test
    fun `parse git branch -d name`() {
        val result = parser.parse("git branch -d old-branch")
        assertTrue(result is GitCommand.Branch)
        assertEquals("old-branch", (result as GitCommand.Branch).delete)
    }

    @Test
    fun `parse git branch new-branch`() {
        val result = parser.parse("git branch new-feature")
        assertTrue(result is GitCommand.Branch)
        assertEquals("new-feature", (result as GitCommand.Branch).create)
    }

    // --- Checkout ---

    @Test
    fun `parse git checkout branch`() {
        val result = parser.parse("git checkout main")
        assertTrue(result is GitCommand.Checkout)
        val cmd = result as GitCommand.Checkout
        assertEquals("main", cmd.branch)
        assertFalse(cmd.createNew)
    }

    @Test
    fun `parse git checkout -b new-branch`() {
        val result = parser.parse("git checkout -b feature-x")
        assertTrue(result is GitCommand.Checkout)
        val cmd = result as GitCommand.Checkout
        assertEquals("feature-x", cmd.branch)
        assertTrue(cmd.createNew)
    }

    @Test
    fun `parse git co alias`() {
        val result = parser.parse("git co main")
        assertTrue(result is GitCommand.Checkout)
        assertEquals("main", (result as GitCommand.Checkout).branch)
    }

    @Test
    fun `parse git checkout without branch returns Unknown`() {
        val result = parser.parse("git checkout")
        assertTrue(result is GitCommand.Unknown)
    }

    // --- Merge ---

    @Test
    fun `parse git merge branch`() {
        val result = parser.parse("git merge feature")
        assertTrue(result is GitCommand.Merge)
        assertEquals("feature", (result as GitCommand.Merge).branch)
    }

    @Test
    fun `parse git merge without branch returns Unknown`() {
        val result = parser.parse("git merge")
        assertTrue(result is GitCommand.Unknown)
    }

    // --- Diff ---

    @Test
    fun `parse git diff`() {
        val result = parser.parse("git diff")
        assertTrue(result is GitCommand.Diff)
        val cmd = result as GitCommand.Diff
        assertFalse(cmd.staged)
        assertNull(cmd.file)
    }

    @Test
    fun `parse git diff --staged`() {
        val result = parser.parse("git diff --staged")
        assertTrue(result is GitCommand.Diff)
        assertTrue((result as GitCommand.Diff).staged)
    }

    @Test
    fun `parse git diff --cached with file path`() {
        val result = parser.parse("git diff --cached file.txt")
        assertTrue(result is GitCommand.Diff)
        val cmd = result as GitCommand.Diff
        assertTrue(cmd.staged)
        assertEquals("file.txt", cmd.file)
    }

    // --- Stash ---

    @Test
    fun `parse git stash`() {
        val result = parser.parse("git stash")
        assertTrue(result is GitCommand.Stash)
        assertNull((result as GitCommand.Stash).subCommand)
    }

    @Test
    fun `parse git stash pop`() {
        val result = parser.parse("git stash pop")
        assertTrue(result is GitCommand.Stash)
        assertEquals("pop", (result as GitCommand.Stash).subCommand)
    }

    @Test
    fun `parse git stash list`() {
        val result = parser.parse("git stash list")
        assertTrue(result is GitCommand.Stash)
        assertEquals("list", (result as GitCommand.Stash).subCommand)
    }

    // --- Config ---

    @Test
    fun `parse git config user dot name with value`() {
        val result = parser.parse("git config user.name John")
        assertTrue(result is GitCommand.Config)
        val cmd = result as GitCommand.Config
        assertEquals("user.name", cmd.key)
        assertEquals("John", cmd.value)
    }

    @Test
    fun `parse git config user dot email with quoted value`() {
        val result = parser.parse("git config user.email 'john@example.com'")
        assertTrue(result is GitCommand.Config)
        val cmd = result as GitCommand.Config
        assertEquals("user.email", cmd.key)
        assertEquals("john@example.com", cmd.value)
    }

    @Test
    fun `parse git config without value`() {
        val result = parser.parse("git config user.name")
        assertTrue(result is GitCommand.Config)
        val cmd = result as GitCommand.Config
        assertEquals("user.name", cmd.key)
        assertNull(cmd.value)
    }

    @Test
    fun `parse git config with no args returns Unknown`() {
        val result = parser.parse("git config")
        assertTrue(result is GitCommand.Unknown)
    }

    // --- Remote ---

    @Test
    fun `parse git remote`() {
        val result = parser.parse("git remote")
        assertTrue(result is GitCommand.Remote)
    }

    // --- Quoting ---

    @Test
    fun `parse handles double-quoted message`() {
        val result = parser.parse("git commit -m \"fix: update login flow\"")
        assertTrue(result is GitCommand.Commit)
        assertEquals("fix: update login flow", (result as GitCommand.Commit).message)
    }

    @Test
    fun `parse handles single-quoted message`() {
        val result = parser.parse("git commit -m 'feat: add dark mode'")
        assertTrue(result is GitCommand.Commit)
        assertEquals("feat: add dark mode", (result as GitCommand.Commit).message)
    }

    @Test
    fun `parse handles quoted argument with spaces`() {
        val result = parser.parse("git add 'file with spaces.txt'")
        assertTrue(result is GitCommand.Add)
        assertEquals(listOf("file with spaces.txt"), (result as GitCommand.Add).files)
    }

    // --- Edge cases ---

    @Test
    fun `parse trims leading and trailing whitespace`() {
        val result = parser.parse("  git status  ")
        assertTrue(result is GitCommand.Status)
    }

    @Test
    fun `parse unknown git subcommand returns Unknown`() {
        val result = parser.parse("git rebase main")
        assertTrue(result is GitCommand.Unknown)
        assertEquals("git rebase main", (result as GitCommand.Unknown).raw)
    }

    @Test
    fun `parse git push with only remote`() {
        val result = parser.parse("git push origin")
        assertTrue(result is GitCommand.Push)
        val cmd = result as GitCommand.Push
        assertEquals("origin", cmd.remote)
        assertNull(cmd.branch)
    }

    @Test
    fun `parse git clone trims trailing slash from url`() {
        val result = parser.parse("git clone https://github.com/user/repo/")
        assertTrue(result is GitCommand.Clone)
        assertEquals("https://github.com/user/repo", (result as GitCommand.Clone).url)
    }
}
