package com.gitforandroid.data.git

import com.gitforandroid.data.git.model.*

data class Credentials(
    val username: String? = null,
    val password: String? = null,
    val sshKeyPath: String? = null,
    val sshPassphrase: String? = null
)

data class CliOutput(
    val stdout: String,
    val stderr: String,
    val exitCode: Int,
    val coloredLines: List<AnsiLine> = emptyList()
)

data class AnsiLine(
    val text: String,
    val fgColor: Int? = null,
    val bgColor: Int? = null,
    val bold: Boolean = false,
    val italic: Boolean = false
)

interface GitService {
    suspend fun init(repoPath: String): Result<String>
    suspend fun clone(
        url: String,
        path: String,
        credentials: Credentials? = null,
        progress: (String) -> Unit = {}
    ): Result<String>
    suspend fun status(repoPath: String): Result<GitStatus>
    suspend fun add(repoPath: String, files: List<String>): Result<String>
    suspend fun unstage(repoPath: String, files: List<String>): Result<String>
    suspend fun commit(repoPath: String, message: String, author: Author): Result<String>
    suspend fun push(
        repoPath: String,
        remote: String = "origin",
        branch: String? = null,
        credentials: Credentials? = null
    ): Result<String>
    suspend fun pull(
        repoPath: String,
        remote: String = "origin",
        branch: String? = null,
        credentials: Credentials? = null
    ): Result<String>
    suspend fun fetch(
        repoPath: String,
        remote: String = "origin",
        credentials: Credentials? = null
    ): Result<String>
    suspend fun log(repoPath: String, maxCount: Int = 50): Result<List<GitLogEntry>>
    suspend fun branches(repoPath: String): Result<List<GitBranch>>
    suspend fun createBranch(repoPath: String, name: String): Result<String>
    suspend fun deleteBranch(repoPath: String, name: String, force: Boolean = false): Result<String>
    suspend fun checkout(repoPath: String, name: String): Result<CheckoutResult>
    suspend fun merge(repoPath: String, branch: String): Result<String>
    suspend fun diff(repoPath: String, staged: Boolean = false, path: String? = null): Result<GitDiff>
    suspend fun stash(repoPath: String): Result<String>
    suspend fun stashPop(repoPath: String): Result<String>
    suspend fun stashList(repoPath: String): Result<List<String>>
    suspend fun executeRaw(command: List<String>, repoPath: String): Result<CliOutput>
    suspend fun getCurrentBranch(repoPath: String): Result<String>
}
