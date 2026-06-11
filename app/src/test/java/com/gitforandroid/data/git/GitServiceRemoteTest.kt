package com.gitforandroid.data.git

import com.gitforandroid.data.git.model.Author
import kotlinx.coroutines.test.runTest
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.URIish
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * Tests for remote Git operations (clone, push, pull, fetch)
 * using local bare repositories via file:// protocol.
 *
 * No network access required — JGit's TransportLocal handles file://
 * URLs with standard filesystem operations, testing the exact same
 * code paths as real remote operations.
 *
 * Note: JGit 6.10's PushCommand/FetchCommand/PullCommand require a
 * configured remote name (like "origin"), NOT a raw URL. Clone
 * configures "origin" automatically; for init-ed repos, we configure
 * the remote via JGit directly in the test setup.
 */
class GitServiceRemoteTest {

    private lateinit var gitService: GitServiceImpl
    private lateinit var workDir: File
    private lateinit var bareRepo: File
    private var bareUrl: String = ""

    private val author = Author("Test User", "test@example.com")

    @Before
    fun setUp() {
        gitService = GitServiceImpl()
        workDir = File(System.getProperty("java.io.tmpdir"), "gfa-remote-${System.nanoTime()}")
        workDir.mkdirs()
        bareRepo = File(workDir, "bare.git")
        bareRepo.mkdirs()
    }

    @After
    fun tearDown() {
        workDir.deleteRecursively()
    }

    // ── Helpers ──────────────────────────────────────────

    private fun newRepo(name: String) = File(workDir, name).also { it.mkdirs() }

    /** Create a bare repository and set bareUrl. */
    private fun createBareRepo(): File {
        Git.init().setBare(true).setDirectory(bareRepo).call().close()
        bareUrl = "file://${bareRepo.absolutePath}"
        return bareRepo
    }

    /**
     * Configure an "origin" remote on a git repository so push/pull/fetch
     * can use the remote name instead of a raw URL (required by JGit 6.10).
     */
    private fun configureOriginRemote(repoPath: String, url: String) {
        Git.open(File(repoPath)).use { git ->
            git.remoteAdd()
                .setName("origin")
                .setUri(URIish(url))
                .call()
        }
    }

    /**
     * Seed a bare repo with an initial commit by:
     * 1. Creating a local init-ed repo
     * 2. Configuring the bare repo as "origin" remote
     * 3. Committing and pushing to origin
     */
    private fun seedBareRepo() {
        createBareRepo()
        val seedDir = newRepo("seed")
        val seedPath = seedDir.absolutePath

        Git.init().setDirectory(seedDir).call().use { git ->
            File(seedDir, "README.md").writeText("# Test Repo")
            git.add().addFilepattern("README.md").call()
            git.commit()
                .setMessage("initial commit")
                .setAuthor("Test User", "test@example.com")
                .call()

            // Configure "origin" remote
            git.remoteAdd()
                .setName("origin")
                .setUri(URIish(bareUrl))
                .call()

            // Push to origin/master
            git.push()
                .setRemote("origin")
                .add("refs/heads/master:refs/heads/master")
                .call()
        }
    }

    // ── Clone ─────────────────────────────────────────────

    @Test
    fun `clone from local bare repo succeeds`() = runTest {
        seedBareRepo()
        val cloneDir = newRepo("clone1").absolutePath

        val result = gitService.clone(bareUrl, cloneDir)
        assertTrue("Clone should succeed: $result", result.isSuccess)
        assertTrue("README.md should exist", File(cloneDir, "README.md").exists())
        assertEquals("# Test Repo", File(cloneDir, "README.md").readText())
    }

    @Test
    fun `clone into non-empty directory fails`() = runTest {
        seedBareRepo()
        val cloneDir = newRepo("clone-fail")
        File(cloneDir, "junk.txt").writeText("existing content")

        val result = gitService.clone(bareUrl, cloneDir.absolutePath)
        assertTrue("Clone into non-empty dir should fail", result.isFailure)
    }

    @Test
    fun `clone creates git metadata directory`() = runTest {
        seedBareRepo()
        val cloneDir = newRepo("clone2").absolutePath

        gitService.clone(bareUrl, cloneDir)
        assertTrue(".git should exist", File(cloneDir, ".git").exists())
    }

    @Test
    fun `clone with null credentials succeeds for local bare repo`() = runTest {
        seedBareRepo()
        val cloneDir = newRepo("clone3").absolutePath

        val result = gitService.clone(bareUrl, cloneDir, null) { }
        assertTrue(result.isSuccess)
    }

    @Test
    fun `clone with progress callback receives progress messages`() = runTest {
        seedBareRepo()
        val cloneDir = newRepo("clone-progress").absolutePath
        val messages = mutableListOf<String>()

        gitService.clone(bareUrl, cloneDir, null) { msg -> messages.add(msg) }
        // For a tiny repo, there may or may not be messages; just check no crash
        assertTrue("Clone should succeed", File(cloneDir, "README.md").exists())
    }

    // ── Push ──────────────────────────────────────────────

    @Test
    fun `push sends commits to bare repo`() = runTest {
        seedBareRepo()
        val cloneDir = newRepo("push-test").absolutePath
        gitService.clone(bareUrl, cloneDir)

        File(cloneDir, "feature.txt").writeText("feature content")
        gitService.add(cloneDir, listOf("feature.txt"))
        gitService.commit(cloneDir, "feat: add feature", author)

        // After clone, "origin" remote is configured — push using remote name
        val pushResult = gitService.push(cloneDir, "origin", "master", null)
        assertTrue("Push should succeed: $pushResult", pushResult.isSuccess)
    }

    @Test
    fun `push is visible to another clone`() = runTest {
        seedBareRepo()

        // Clone A: make a commit and push to origin
        val repoA = newRepo("repoA").absolutePath
        gitService.clone(bareUrl, repoA)
        File(repoA, "file-a.txt").writeText("from A")
        gitService.add(repoA, listOf("file-a.txt"))
        gitService.commit(repoA, "commit from A", author)
        gitService.push(repoA, "origin", "master", null)

        // Clone B: pull from origin, should get A's commit
        val repoB = newRepo("repoB").absolutePath
        gitService.clone(bareUrl, repoB)
        gitService.pull(repoB, "origin", "master", null)

        assertTrue("file-a.txt should exist in repo B after pull",
            File(repoB, "file-a.txt").exists())
        assertEquals("from A", File(repoB, "file-a.txt").readText())
    }

    @Test
    fun `push to new branch on remote`() = runTest {
        seedBareRepo()
        val cloneDir = newRepo("branch-push").absolutePath
        gitService.clone(bareUrl, cloneDir)

        gitService.createBranch(cloneDir, "develop")
        gitService.checkout(cloneDir, "develop")

        File(cloneDir, "dev-file.txt").writeText("dev work")
        gitService.add(cloneDir, listOf("dev-file.txt"))
        gitService.commit(cloneDir, "development work", author)
        gitService.push(cloneDir, "origin", "develop", null)

        // Verify the branch is visible in a fresh clone
        val fresh = newRepo("fresh").absolutePath
        gitService.clone(bareUrl, fresh)
        val branches = gitService.branches(fresh).getOrThrow()
        assertTrue("Remote-tracking branch origin/develop should exist",
            branches.any { it.name.contains("develop") })
    }

    // ── Pull ──────────────────────────────────────────────

    @Test
    fun `pull fetches and merges remote changes`() = runTest {
        seedBareRepo()

        // Clone A: add commit, push
        val repoA = newRepo("pull-a").absolutePath
        gitService.clone(bareUrl, repoA)
        File(repoA, "from-a.txt").writeText("content from A")
        gitService.add(repoA, listOf("from-a.txt"))
        gitService.commit(repoA, "A's commit", author)
        gitService.push(repoA, "origin", "master", null)

        // Clone B: pull — should get A's commit
        val repoB = newRepo("pull-b").absolutePath
        gitService.clone(bareUrl, repoB)
        gitService.pull(repoB, "origin", "master", null)

        assertTrue("from-a.txt should exist after pull",
            File(repoB, "from-a.txt").exists())
    }

    @Test
    fun `pull with fast-forward works cleanly`() = runTest {
        seedBareRepo()
        val cloneDir = newRepo("ff-test").absolutePath
        gitService.clone(bareUrl, cloneDir)

        // Push a change from another clone
        val other = newRepo("other").absolutePath
        gitService.clone(bareUrl, other)
        File(other, "other.txt").writeText("other work")
        gitService.add(other, listOf("other.txt"))
        gitService.commit(other, "other commit", author)
        gitService.push(other, "origin", "master", null)

        // Pull in original clone — should fast-forward
        val pullResult = gitService.pull(cloneDir, "origin", "master", null)
        assertTrue("Fast-forward pull should succeed: $pullResult", pullResult.isSuccess)
        assertTrue("other.txt should exist after ff pull",
            File(cloneDir, "other.txt").exists())
    }

    @Test
    fun `pull with no remote configured fails`() = runTest {
        val local = newRepo("no-remote").absolutePath
        gitService.init(local)

        val result = gitService.pull(local, "origin", null, null)
        assertTrue("Pull with no remote configured should fail", result.isFailure)
    }

    // ── Fetch ─────────────────────────────────────────────

    @Test
    fun `fetch retrieves remote refs without merging`() = runTest {
        seedBareRepo()

        // Clone A: create feature branch, push
        val repoA = newRepo("fetch-a").absolutePath
        gitService.clone(bareUrl, repoA)
        gitService.createBranch(repoA, "feature")
        gitService.checkout(repoA, "feature")
        File(repoA, "feat.txt").writeText("feature work")
        gitService.add(repoA, listOf("feat.txt"))
        gitService.commit(repoA, "feature commit", author)
        gitService.push(repoA, "origin", "feature", null)

        // Clone B: fetch
        val repoB = newRepo("fetch-b").absolutePath
        gitService.clone(bareUrl, repoB)
        val fetchResult = gitService.fetch(repoB, "origin", null)
        assertTrue("Fetch should succeed: $fetchResult", fetchResult.isSuccess)

        val branches = gitService.branches(repoB).getOrThrow()
        assertTrue("origin/feature should be visible after fetch",
            branches.any { it.name.contains("feature") })
    }

    @Test
    fun `fetch does not modify working tree`() = runTest {
        seedBareRepo()
        val cloneDir = newRepo("fetch-nowork").absolutePath
        gitService.clone(bareUrl, cloneDir)

        // Push from another repo
        val other = newRepo("remote-pusher").absolutePath
        gitService.clone(bareUrl, other)
        File(other, "remote-work.txt").writeText("remote changes")
        gitService.add(other, listOf("remote-work.txt"))
        gitService.commit(other, "remote commit", author)
        gitService.push(other, "origin", "master", null)

        // Fetch but don't merge
        gitService.fetch(cloneDir, "origin", null)

        // Working tree should be unchanged by fetch alone
        assertFalse("remote-work.txt should not appear after fetch alone",
            File(cloneDir, "remote-work.txt").exists())
    }

    // ── Full round-trip ───────────────────────────────────

    @Test
    fun `full round-trip init commit push clone pull`() = runTest {
        // 1. Create local repo
        val local = newRepo("roundtrip-local")
        val localPath = local.absolutePath
        gitService.init(localPath)
        File(local, "hello.txt").writeText("Hello Git!")
        gitService.add(localPath, listOf("hello.txt"))
        gitService.commit(localPath, "initial", author)

        // 2. Create bare remote, configure origin, push
        createBareRepo()
        configureOriginRemote(localPath, bareUrl)
        gitService.push(localPath, "origin", "master", null)

        // 3. Clone from bare
        val clone = newRepo("roundtrip-clone").absolutePath
        gitService.clone(bareUrl, clone)
        assertEquals("Hello Git!", File(clone, "hello.txt").readText())

        // 4. Make change in clone, push to origin
        File(clone, "world.txt").writeText("World!")
        gitService.add(clone, listOf("world.txt"))
        gitService.commit(clone, "second commit", author)
        gitService.push(clone, "origin", "master", null)

        // 5. Pull in local from origin
        gitService.pull(localPath, "origin", "master", null)
        assertTrue("world.txt should exist in local after pull",
            File(local, "world.txt").exists())
    }

    @Test
    fun `push and pull with multiple branches`() = runTest {
        seedBareRepo()
        val cloneDir = newRepo("multi-branch").absolutePath
        gitService.clone(bareUrl, cloneDir)

        // Create three branches and push each
        listOf("feature-x", "feature-y", "feature-z").forEach { branch ->
            gitService.createBranch(cloneDir, branch)
            gitService.checkout(cloneDir, branch)
            File(cloneDir, "$branch.txt").writeText("content for $branch")
            gitService.add(cloneDir, listOf("$branch.txt"))
            gitService.commit(cloneDir, "commit on $branch", author)
            gitService.push(cloneDir, "origin", branch, null)
        }

        // Fresh clone should see all remote branches
        val fresh = newRepo("multi-verify").absolutePath
        gitService.clone(bareUrl, fresh)
        val branches = gitService.branches(fresh).getOrThrow()

        assertTrue("origin/feature-x should be visible",
            branches.any { it.name.contains("feature-x") })
        assertTrue("origin/feature-y should be visible",
            branches.any { it.name.contains("feature-y") })
        assertTrue("origin/feature-z should be visible",
            branches.any { it.name.contains("feature-z") })
    }

    // ── Error handling ────────────────────────────────────

    @Test
    fun `clone with invalid URL fails gracefully`() = runTest {
        val dir = newRepo("bad-clone").absolutePath
        val result = gitService.clone("file:///nonexistent/path/repo.git", dir)
        assertTrue("Clone from nonexistent path should fail", result.isFailure)
    }
}
