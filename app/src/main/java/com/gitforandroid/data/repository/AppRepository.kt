package com.gitforandroid.data.repository

import android.content.Context
import com.gitforandroid.data.git.Credentials
import com.gitforandroid.data.git.GitService
import com.gitforandroid.data.git.model.*
import com.gitforandroid.data.local.dao.RepoDao
import com.gitforandroid.data.local.dao.SettingDao
import com.gitforandroid.data.local.entity.RepoEntity
import com.gitforandroid.data.local.entity.SettingEntity
import kotlinx.coroutines.flow.Flow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRepository @Inject constructor(
    private val repoDao: RepoDao,
    private val settingDao: SettingDao,
    private val gitService: GitService,
    private val context: Context
) {
    // --- Repo management ---

    val allRepos: Flow<List<RepoEntity>> = repoDao.getAll()

    suspend fun getRepo(id: Long): RepoEntity? = repoDao.getById(id)

    suspend fun addRepo(name: String, localPath: String, remoteUrl: String?): Long {
        val existing = repoDao.getByPath(localPath)
        if (existing != null) {
            repoDao.updateLastOpened(existing.id)
            return existing.id
        }
        return repoDao.insert(
            RepoEntity(name = name, localPath = localPath, remoteUrl = remoteUrl)
        )
    }

    suspend fun deleteRepo(id: Long) {
        repoDao.deleteById(id)
    }

    suspend fun updateLastOpened(id: Long) {
        repoDao.updateLastOpened(id)
    }

    suspend fun getCurrentBranch(repoId: Long): Result<String> {
        val repo = repoDao.getById(repoId) ?: return Result.failure(Exception("Repo not found"))
        return gitService.getCurrentBranch(repo.localPath).also { result ->
            result.onSuccess { branch ->
                repoDao.update(repo.copy(currentBranch = branch))
            }
        }
    }

    // --- Settings ---

    suspend fun getSetting(key: String): String? = settingDao.getValue(key)

    suspend fun setSetting(key: String, value: String) {
        settingDao.set(SettingEntity(key, value))
    }

    // --- Git operations ---

    data class CloneResult(val repoName: String, val repoId: Long)

    suspend fun cloneRepo(
        url: String,
        localPath: String,
        name: String? = null,
        credentials: Credentials? = null,
        progress: (String) -> Unit = {}
    ): Result<CloneResult> {
        return gitService.clone(url, localPath, credentials, progress).map { message ->
            val repoName = name ?: extractRepoName(url)
            val repoId = addRepo(repoName, localPath, url)
            CloneResult(repoName, repoId)
        }
    }

    suspend fun initRepo(localPath: String, name: String): Result<Long> {
        return gitService.init(localPath).map { message ->
            addRepo(name, localPath, null)
        }
    }

    suspend fun status(repoId: Long): Result<GitStatus> = withRepo(repoId) { gitService.status(it) }

    suspend fun add(repoId: Long, files: List<String>): Result<String> = withRepo(repoId) { gitService.add(it, files) }

    suspend fun unstage(repoId: Long, files: List<String>): Result<String> = withRepo(repoId) { gitService.unstage(it, files) }

    suspend fun commit(repoId: Long, message: String, author: Author): Result<String> =
        withRepo(repoId) { gitService.commit(it, message, author) }

    suspend fun push(repoId: Long, remote: String = "origin", branch: String? = null, credentials: Credentials? = null): Result<String> =
        withRepo(repoId) { gitService.push(it, remote, branch, credentials) }

    suspend fun pull(repoId: Long, remote: String = "origin", branch: String? = null, credentials: Credentials? = null): Result<String> =
        withRepo(repoId) { gitService.pull(it, remote, branch, credentials) }

    suspend fun fetch(repoId: Long, remote: String = "origin", credentials: Credentials? = null): Result<String> =
        withRepo(repoId) { gitService.fetch(it, remote, credentials) }

    suspend fun log(repoId: Long, maxCount: Int = 50): Result<List<GitLogEntry>> =
        withRepo(repoId) { gitService.log(it, maxCount) }

    suspend fun branches(repoId: Long): Result<List<GitBranch>> =
        withRepo(repoId) { gitService.branches(it) }

    suspend fun createBranch(repoId: Long, name: String): Result<String> =
        withRepo(repoId) { gitService.createBranch(it, name) }

    suspend fun deleteBranch(repoId: Long, name: String, force: Boolean = false): Result<String> =
        withRepo(repoId) { gitService.deleteBranch(it, name, force) }

    suspend fun checkout(repoId: Long, name: String): Result<CheckoutResult> =
        withRepo(repoId) { gitService.checkout(it, name) }.also { result ->
            // Refresh currentBranch in DB so GUI picks up the branch change
            result.onSuccess { getCurrentBranch(repoId) }
        }

    suspend fun merge(repoId: Long, branch: String): Result<String> =
        withRepo(repoId) { gitService.merge(it, branch) }

    suspend fun diff(repoId: Long, staged: Boolean = false, path: String? = null): Result<GitDiff> =
        withRepo(repoId) { gitService.diff(it, staged, path) }

    suspend fun stash(repoId: Long): Result<String> = withRepo(repoId) { gitService.stash(it) }

    suspend fun stashPop(repoId: Long): Result<String> = withRepo(repoId) { gitService.stashPop(it) }

    suspend fun stashList(repoId: Long): Result<List<String>> = withRepo(repoId) { gitService.stashList(it) }

    suspend fun executeCliCommand(repoId: Long, command: List<String>): Result<com.gitforandroid.data.git.CliOutput> =
        withRepo(repoId) { gitService.executeRaw(command, it) }

    // --- Helper ---

    private suspend fun <T> withRepo(repoId: Long, block: suspend (String) -> Result<T>): Result<T> {
        val repo = repoDao.getById(repoId) ?: return Result.failure(Exception("Repo not found"))
        return block(repo.localPath)
    }

    private fun extractRepoName(url: String): String {
        val name = url.substringAfterLast("/").removeSuffix(".git")
        return name.ifBlank { url }
    }
}
