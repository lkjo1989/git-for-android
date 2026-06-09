package com.gitforandroid.data.git

import android.content.Context
import com.gitforandroid.data.git.model.*
import com.gitforandroid.util.FileLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.CreateBranchCommand
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ListBranchCommand
import org.eclipse.jgit.api.ResetCommand
import org.eclipse.jgit.api.Status
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import org.eclipse.jgit.treewalk.EmptyTreeIterator
import org.eclipse.jgit.util.io.DisabledOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
class GitServiceImpl @Inject constructor(
    @ApplicationContext private val appContext: Context? = null
) : GitService {

    override suspend fun init(repoPath: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(repoPath)
            dir.mkdirs()
            Git.init().setDirectory(dir).call().use { git ->
                "Initialized empty Git repository in ${git.repository.directory.canonicalPath}"
            }
        }
    }

    override suspend fun clone(
        url: String,
        path: String,
        credentials: Credentials?,
        progress: (String) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(path)
            val cmd = Git.cloneRepository()
                .setURI(url)
                .setDirectory(dir)
                .setCloneAllBranches(true)

            if (credentials?.username != null && credentials.password != null) {
                cmd.setCredentialsProvider(
                    UsernamePasswordCredentialsProvider(credentials.username, credentials.password)
                )
            }

            cmd.setProgressMonitor(ProgressMonitorAdapter(progress))
            cmd.call().use { git ->
                logInfo("Git", "clone OK: $url → ${git.repository.directory.parentFile.canonicalPath}")
                "Cloned into ${git.repository.directory.parentFile.canonicalPath}"
            }
        }.onFailure { e ->
            logError("Git", "clone failed: url=$url path=$path", e)
        }
    }

    override suspend fun status(repoPath: String): Result<GitStatus> = withContext(Dispatchers.IO) {
        runCatching {
            Git.open(File(repoPath)).use { git ->
                val status = git.status().call()
                GitStatus(
                    branch = git.repository.branch,
                    added = status.added.map { StatusFile(it, changeType = ChangeType.ADDED) },
                    changed = status.changed.map { StatusFile(it, changeType = ChangeType.MODIFIED) },
                    removed = status.removed.map { StatusFile(it, changeType = ChangeType.DELETED) },
                    modified = status.modified.map { StatusFile(it, changeType = ChangeType.MODIFIED) },
                    missing = status.missing.map { StatusFile(it, changeType = ChangeType.DELETED) },
                    untracked = status.untracked.map { StatusFile(it, changeType = ChangeType.UNTRACKED) },
                    conflicting = status.conflicting.map { StatusFile(it, changeType = ChangeType.CONFLICTING) }
                )
            }
        }
    }

    override suspend fun add(repoPath: String, files: List<String>): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            Git.open(File(repoPath)).use { git ->
                val addCmd = git.add()
                files.forEach { addCmd.addFilepattern(it) }
                addCmd.call()
                "Staged ${files.size} file(s)"
            }
        }
    }

    override suspend fun unstage(repoPath: String, files: List<String>): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            Git.open(File(repoPath)).use { git ->
                val resetCmd = git.reset()
                files.forEach { resetCmd.addPath(it) }
                resetCmd.call()
                "Unstaged ${files.size} file(s)"
            }
        }
    }

    override suspend fun commit(
        repoPath: String,
        message: String,
        author: Author
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            Git.open(File(repoPath)).use { git ->
                val person = PersonIdent(author.name, author.email)
                val commit = git.commit()
                    .setMessage(message)
                    .setAuthor(person)
                    .setCommitter(person)
                    .call()
                "Committed: ${commit.abbreviate(7).name()} - ${commit.shortMessage}"
            }
        }
    }

    override suspend fun push(
        repoPath: String,
        remote: String,
        branch: String?,
        credentials: Credentials?
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            Git.open(File(repoPath)).use { git ->
                val cmd = git.push().setRemote(remote)
                if (branch != null) {
                    cmd.add("refs/heads/$branch:refs/heads/$branch")
                }
                if (credentials?.username != null && credentials.password != null) {
                    cmd.setCredentialsProvider(
                        UsernamePasswordCredentialsProvider(credentials.username, credentials.password)
                    )
                }
                val results = cmd.call()
                val sb = StringBuilder()
                for (result in results) {
                    sb.appendLine(result.messages)
                }
                sb.toString().ifBlank { "Push successful" }
            }
        }.onFailure { e ->
            logError("Git", "push failed: repo=$repoPath remote=$remote branch=$branch", e)
        }
    }

    override suspend fun pull(
        repoPath: String,
        remote: String,
        branch: String?,
        credentials: Credentials?
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            Git.open(File(repoPath)).use { git ->
                val cmd = git.pull().setRemote(remote)
                if (branch != null) {
                    cmd.setRemoteBranchName(branch)
                }
                if (credentials?.username != null && credentials.password != null) {
                    cmd.setCredentialsProvider(
                        UsernamePasswordCredentialsProvider(credentials.username, credentials.password)
                    )
                }
                val result = cmd.call()
                if (result.isSuccessful) {
                    result.mergeResult?.mergeStatus?.name ?: "Pull successful"
                } else {
                    throw Exception("Pull failed: ${result.mergeResult?.mergeStatus?.name ?: "unknown"}")
                }
            }
        }.onFailure { e ->
            logError("Git", "pull failed: repo=$repoPath remote=$remote branch=$branch", e)
        }
    }

    override suspend fun fetch(
        repoPath: String,
        remote: String,
        credentials: Credentials?
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            Git.open(File(repoPath)).use { git ->
                val cmd = git.fetch().setRemote(remote)
                if (credentials?.username != null && credentials.password != null) {
                    cmd.setCredentialsProvider(
                        UsernamePasswordCredentialsProvider(credentials.username, credentials.password)
                    )
                }
                val result = cmd.call()
                "Fetched from $remote"
            }
        }
    }

    override suspend fun log(
        repoPath: String,
        maxCount: Int
    ): Result<List<GitLogEntry>> = withContext(Dispatchers.IO) {
        runCatching {
            Git.open(File(repoPath)).use { git ->
                val refs = git.repository.refDatabase.refs
                    .filter { it.name.startsWith("refs/heads/") || it.name.startsWith("refs/tags/") }
                    .groupBy { it.objectId?.name }

                git.log().setMaxCount(maxCount).call().map { commit ->
                    val commitRefs = refs[commit.name]?.map { it.name } ?: emptyList()
                    GitLogEntry(commit.toModel(), commitRefs)
                }
            }
        }
    }

    override suspend fun branches(repoPath: String): Result<List<GitBranch>> = withContext(Dispatchers.IO) {
        runCatching {
            Git.open(File(repoPath)).use { git ->
                val currentBranch = git.repository.branch
                val localBranches = git.branchList().call().map { it.toModel(currentBranch) }
                val remoteBranches = git.branchList()
                    .setListMode(ListBranchCommand.ListMode.REMOTE)
                    .call()
                    .map { it.toModel(currentBranch) }
                localBranches + remoteBranches
            }
        }
    }

    override suspend fun createBranch(repoPath: String, name: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            Git.open(File(repoPath)).use { git ->
                git.branchCreate()
                    .setName(name)
                    .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
                    .call()
                "Created branch '$name'"
            }
        }
    }

    override suspend fun deleteBranch(
        repoPath: String,
        name: String,
        force: Boolean
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            Git.open(File(repoPath)).use { git ->
                git.branchDelete()
                    .setBranchNames(name)
                    .setForce(force)
                    .call()
                "Deleted branch '$name'"
            }
        }
    }

    override suspend fun checkout(repoPath: String, name: String): Result<CheckoutResult> = withContext(Dispatchers.IO) {
        runCatching {
            Git.open(File(repoPath)).use { git ->
                val prevBranch = git.repository.branch
                val localRef = git.repository.findRef("refs/heads/$name")
                val remoteRef = git.repository.findRef("refs/remotes/origin/$name")

                logInfo("Git", "checkout: name=$name repo=$repoPath localRef=${localRef != null} remoteRef=${remoteRef != null}")

                if (localRef != null) {
                    git.checkout().setName(name).call()
                    CheckoutResult(prevBranch, name, "Switched to branch '$name'")
                } else if (remoteRef != null) {
                    git.checkout()
                        .setCreateBranch(true)
                        .setName(name)
                        .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
                        .setStartPoint("origin/$name")
                        .call()
                    logInfo("Git", "checkout: created local tracking branch '$name' → origin/$name")
                    CheckoutResult(prevBranch, name, "Switched to new branch '$name' (tracking origin/$name)")
                } else {
                    git.checkout().setName(name).call()
                    CheckoutResult(prevBranch, name, "Switched to branch '$name'")
                }
            }
        }.onFailure { e ->
            logError("Git", "checkout failed: name=$name repo=$repoPath", e)
        }
    }

    override suspend fun merge(repoPath: String, branch: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            Git.open(File(repoPath)).use { git ->
                val result = git.merge()
                    .include(git.repository.findRef(branch))
                    .call()
                result.mergeStatus.name
            }
        }
    }

    override suspend fun diff(
        repoPath: String,
        staged: Boolean,
        path: String?
    ): Result<GitDiff> = withContext(Dispatchers.IO) {
        runCatching {
            Git.open(File(repoPath)).use { git ->
                val reader = git.repository.newObjectReader()
                val inserter = git.repository.newObjectInserter()
                val formatter = DiffFormatter(DisabledOutputStream.INSTANCE)
                formatter.setRepository(git.repository)

                try {
                    val diffs: List<DiffEntry> = if (staged) {
                        val headId = git.repository.resolve("HEAD^{tree}")
                        if (headId != null) {
                            val oldTreeIter = CanonicalTreeParser()
                            oldTreeIter.reset(reader, headId)
                            val newTreeIter = CanonicalTreeParser()
                            val index = git.repository.readDirCache()
                            val indexTreeId = index.writeTree(inserter)
                            inserter.flush()
                            newTreeIter.reset(reader, indexTreeId)
                            formatter.scan(oldTreeIter, newTreeIter)
                        } else {
                            // No HEAD yet – compare against empty tree
                            val newTreeIter = CanonicalTreeParser()
                            val index = git.repository.readDirCache()
                            val indexTreeId = index.writeTree(inserter)
                            inserter.flush()
                            newTreeIter.reset(reader, indexTreeId)
                            formatter.scan(EmptyTreeIterator(), newTreeIter)
                        }
                    } else {
                        // Unstaged diff: compare HEAD tree vs working directory
                        val headId = git.repository.resolve("HEAD^{tree}")
                        val oldTreeIter = if (headId != null) {
                            CanonicalTreeParser().also { it.reset(reader, headId) }
                        } else {
                            EmptyTreeIterator()
                        }
                        formatter.scan(
                            oldTreeIter,
                            org.eclipse.jgit.treewalk.FileTreeIterator(git.repository)
                        )
                    }

                    val filtered = if (path != null) {
                        diffs.filter { it.newPath == path || it.oldPath == path }
                    } else diffs

                    val diffFiles = filtered.map { entry ->
                        val bos = ByteArrayOutputStream()
                        DiffFormatter(bos).use { df ->
                            df.setRepository(git.repository)
                            df.format(entry)
                        }
                        DiffFile(
                            oldPath = entry.oldPath,
                            newPath = entry.newPath,
                            changeType = entry.changeType.toModel(),
                            hunks = parseHunks(bos.toString())
                        )
                    }

                    val rawBos = ByteArrayOutputStream()
                    DiffFormatter(rawBos).use { df ->
                        df.setRepository(git.repository)
                        filtered.forEach { df.format(it) }
                    }

                    GitDiff(files = diffFiles, rawDiff = rawBos.toString())
                } finally {
                    reader.close()
                }
            }
        }
    }

    override suspend fun stash(repoPath: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            Git.open(File(repoPath)).use { git ->
                val result = git.stashCreate().call()
                result?.name ?: "Stashed changes"
            }
        }
    }

    override suspend fun stashPop(repoPath: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            Git.open(File(repoPath)).use { git ->
                git.stashApply().setStashRef("stash@{0}").call()
                git.stashDrop().setStashRef(0).call()
                "Popped stash"
            }
        }
    }

    override suspend fun stashList(repoPath: String): Result<List<String>> = withContext(Dispatchers.IO) {
        runCatching {
            Git.open(File(repoPath)).use { git ->
                git.stashList().call().map { stash ->
                    java.util.Date(stash.commitTime.toLong() * 1000).toString() + ": " + stash.shortMessage
                }
            }
        }
    }

    override suspend fun executeRaw(
        command: List<String>,
        repoPath: String
    ): Result<CliOutput> = withContext(Dispatchers.IO) {
        // Will be handled by GitCliParser in CLI mode; default fallback
        runCatching {
            CliOutput(
                stdout = "Unknown command: ${command.joinToString(" ")}",
                stderr = "",
                exitCode = 1
            )
        }
    }

    override suspend fun getCurrentBranch(repoPath: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            Git.open(File(repoPath)).use { git ->
                git.repository.branch
            }
        }
    }

    // --- Logging helpers (no-op when context is null, e.g. in unit tests) ---

    private suspend fun logInfo(tag: String, message: String) {
        appContext?.let { FileLogger.info(it, tag, message) }
    }

    private suspend fun logError(tag: String, message: String, throwable: Throwable? = null) {
        appContext?.let { FileLogger.error(it, tag, message, throwable) }
    }

    // --- Extension helpers ---

    private fun RevCommit.toModel(): GitCommit = GitCommit(
        hash = name,
        shortHash = abbreviate(7).name(),
        author = Author(authorIdent.name, authorIdent.emailAddress),
        committer = Author(committerIdent.name, committerIdent.emailAddress),
        message = shortMessage,
        fullMessage = fullMessage,
        timestamp = commitTime.toLong() * 1000,
        parentHashes = parents.map { it.name },
        parentCount = parentCount
    )

    private fun Ref.toModel(currentBranch: String): GitBranch = GitBranch(
        name = org.eclipse.jgit.lib.Repository.shortenRefName(name),
        fullName = name,
        isRemote = name.startsWith("refs/remotes/"),
        isCurrentBranch = org.eclipse.jgit.lib.Repository.shortenRefName(name) == currentBranch,
        objectId = objectId?.name ?: ""
    )

    private fun DiffEntry.ChangeType.toModel(): ChangeType = when (this) {
        DiffEntry.ChangeType.ADD -> ChangeType.ADDED
        DiffEntry.ChangeType.MODIFY -> ChangeType.MODIFIED
        DiffEntry.ChangeType.DELETE -> ChangeType.DELETED
        DiffEntry.ChangeType.RENAME -> ChangeType.RENAMED
        DiffEntry.ChangeType.COPY -> ChangeType.ADDED
    }

    private fun parseHunks(rawDiff: String): List<DiffHunk> {
        val hunks = mutableListOf<DiffHunk>()
        var currentHunk: MutableList<DiffLine>? = null
        var hunkHeader = ""
        var oldStart = 0; var oldCount = 0; var newStart = 0; var newCount = 0

        for (line in rawDiff.lines()) {
            when {
                line.startsWith("@@") -> {
                    currentHunk?.let { hunkLines ->
                        hunks.add(DiffHunk(oldStart, oldCount, newStart, newCount, hunkHeader, hunkLines))
                    }
                    currentHunk = mutableListOf()
                    hunkHeader = line
                    // Parse @@ -oldStart,oldCount +newStart,newCount @@
                    val regex = Regex("@@ -(\\d+)(?:,(\\d+))? \\+(\\d+)(?:,(\\d+))? @@")
                    val match = regex.find(line)
                    if (match != null) {
                        oldStart = match.groupValues[1].toInt()
                        oldCount = match.groupValues[2].ifEmpty { "1" }.toInt()
                        newStart = match.groupValues[3].toInt()
                        newCount = match.groupValues[4].ifEmpty { "1" }.toInt()
                    }
                }
                line.startsWith("diff --git") || line.startsWith("index ") ||
                        line.startsWith("---") || line.startsWith("+++") ||
                        line.startsWith("new file") || line.startsWith("deleted file") -> {
                    currentHunk?.add(DiffLine(line, DiffLineType.HEADER))
                }
                line.startsWith("+") -> currentHunk?.add(DiffLine(line, DiffLineType.ADDED))
                line.startsWith("-") -> currentHunk?.add(DiffLine(line, DiffLineType.REMOVED))
                else -> currentHunk?.add(DiffLine(line, DiffLineType.CONTEXT))
            }
        }
        currentHunk?.let { hunkLines ->
            hunks.add(DiffHunk(oldStart, oldCount, newStart, newCount, hunkHeader, hunkLines))
        }
        return hunks
    }
}

private class ProgressMonitorAdapter(
    private val callback: (String) -> Unit
) : org.eclipse.jgit.lib.ProgressMonitor {
    private var taskTitle: String = ""
    private var taskTotalWork = 0
    private var taskWorkDone = 0

    override fun start(totalTasks: Int) { }

    override fun beginTask(title: String?, totalWork: Int) {
        taskTitle = title ?: ""
        taskTotalWork = totalWork
        taskWorkDone = 0
    }

    override fun update(completed: Int) {
        taskWorkDone = completed
        val pct = if (taskTotalWork > 0) {
            (taskWorkDone * 100) / taskTotalWork
        } else {
            0
        }
        val line = if (taskTotalWork > 0) {
            "$taskTitle: $pct% ($taskWorkDone/$taskTotalWork)"
        } else {
            taskTitle
        }
        if (line.isNotBlank()) {
            callback(line)
        }
    }

    override fun endTask() { }

    override fun isCancelled(): Boolean = false

    override fun showDuration(enabled: Boolean) { }
}
