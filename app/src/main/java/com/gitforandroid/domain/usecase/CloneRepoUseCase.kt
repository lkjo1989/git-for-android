package com.gitforandroid.domain.usecase

import com.gitforandroid.data.git.Credentials
import com.gitforandroid.data.repository.AppRepository
import javax.inject.Inject

class CloneRepoUseCase @Inject constructor(
    private val repository: AppRepository
) {
    suspend operator fun invoke(
        url: String,
        localPath: String,
        name: String? = null,
        credentials: Credentials? = null,
        progress: (String) -> Unit = {}
    ): Result<AppRepository.CloneResult> {
        return repository.cloneRepo(url, localPath, name, credentials, progress)
    }
}
