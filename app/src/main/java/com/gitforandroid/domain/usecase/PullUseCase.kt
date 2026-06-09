package com.gitforandroid.domain.usecase

import com.gitforandroid.data.git.Credentials
import com.gitforandroid.data.repository.AppRepository
import javax.inject.Inject

class PullUseCase @Inject constructor(
    private val repository: AppRepository
) {
    suspend operator fun invoke(
        repoId: Long,
        remote: String = "origin",
        branch: String? = null,
        credentials: Credentials? = null
    ): Result<String> = repository.pull(repoId, remote, branch, credentials)
}
