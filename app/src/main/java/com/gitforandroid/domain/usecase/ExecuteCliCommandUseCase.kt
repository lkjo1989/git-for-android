package com.gitforandroid.domain.usecase

import com.gitforandroid.data.git.CliOutput
import com.gitforandroid.data.git.Credentials
import com.gitforandroid.data.git.model.Author
import com.gitforandroid.data.repository.AppRepository
import com.gitforandroid.domain.parser.GitCliParser
import com.gitforandroid.domain.parser.GitCommand
import javax.inject.Inject

class ExecuteCliCommandUseCase @Inject constructor(
    private val repository: AppRepository,
    private val parser: GitCliParser
) {
    suspend operator fun invoke(
        repoId: Long,
        input: String,
        author: Author? = null,
        credentials: Credentials? = null
    ): Result<CliOutput> {
        val command = parser.parse(input)
        return execute(repoId, command, author, credentials)
    }

    private suspend fun execute(
        repoId: Long,
        command: GitCommand,
        author: Author?,
        credentials: Credentials?
    ): Result<CliOutput> {
        val result = when (command) {
            is GitCommand.Status -> repository.status(repoId).map { status ->
                buildString {
                    appendLine("On branch ${status.branch}")
                    if (status.isClean) {
                        appendLine("nothing to commit, working tree clean")
                    } else {
                        appendLine("Changes:")
                        status.added.forEach { appendLine("  new file:   ${it.path}") }
                        status.changed.forEach { appendLine("  modified:   ${it.path}") }
                        status.removed.forEach { appendLine("  deleted:    ${it.path}") }
                        status.untracked.forEach { appendLine("  untracked:  ${it.path}") }
                        status.conflicting.forEach { appendLine("  conflict:   ${it.path}") }
                    }
                }
            }
            is GitCommand.Add -> repository.add(repoId, command.files)
            is GitCommand.Commit -> {
                val aut = author ?: Author("Unknown", "unknown@example.com")
                repository.commit(repoId, command.message, aut)
            }
            is GitCommand.Push -> repository.push(repoId, command.remote ?: "origin", command.branch, credentials)
            is GitCommand.Pull -> repository.pull(repoId, command.remote ?: "origin", command.branch, credentials)
            is GitCommand.Fetch -> repository.fetch(repoId, command.remote ?: "origin", credentials)
            is GitCommand.Log -> repository.log(repoId, command.maxCount).map { entries ->
                entries.joinToString("\n") { entry ->
                    val commit = entry.commit
                    if (command.oneline) {
                        "${commit.shortHash} ${commit.message}"
                    } else {
                        buildString {
                            appendLine("commit ${commit.hash}")
                            appendLine("Author: ${commit.author.name} <${commit.author.email}>")
                            appendLine("Date:   ${java.text.SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy Z").format(java.util.Date(commit.timestamp))}")
                            appendLine()
                            appendLine("    ${commit.message}")
                        }
                    }
                }
            }
            is GitCommand.Branch -> {
                if (command.delete != null) {
                    repository.deleteBranch(repoId, command.delete)
                } else if (command.create != null) {
                    repository.createBranch(repoId, command.create)
                } else {
                    repository.branches(repoId).map { branches ->
                        branches.joinToString("\n") { branch ->
                            val prefix = if (branch.isCurrentBranch) "* " else "  "
                            prefix + branch.name
                        }
                    }
                }
            }
            is GitCommand.Checkout -> repository.checkout(repoId, command.branch).map { it.message }
            is GitCommand.Merge -> repository.merge(repoId, command.branch)
            is GitCommand.Diff -> repository.diff(repoId, command.staged, command.file).map { it.rawDiff }
            is GitCommand.Stash -> {
                when (command.subCommand) {
                    "pop" -> repository.stashPop(repoId)
                    "list" -> repository.stashList(repoId).map { it.joinToString("\n") }
                    else -> repository.stash(repoId)
                }
            }
            is GitCommand.Init -> repository.initRepo(
                command.path ?: (repository.getRepo(repoId)?.localPath ?: "."),
                "init"
            ).map { "Initialized repository" }
            is GitCommand.Clone -> repository.cloneRepo(command.url, command.path ?: ".")
            is GitCommand.Remote -> Result.success("origin")
            is GitCommand.Unknown -> Result.failure(Exception("Unknown command: ${command.raw}"))
        }

        return result.map { stdout ->
            CliOutput(stdout.toString(), "", 0)
        }.getOrElse { e ->
            CliOutput("", e.message ?: "Error", 1)
        }.let { Result.success(it) }
    }
}
