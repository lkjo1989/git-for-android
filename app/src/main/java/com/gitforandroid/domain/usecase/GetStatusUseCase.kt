package com.gitforandroid.domain.usecase

import com.gitforandroid.data.git.model.GitStatus
import com.gitforandroid.data.repository.AppRepository
import javax.inject.Inject

class GetStatusUseCase @Inject constructor(
    private val repository: AppRepository
) {
    suspend operator fun invoke(repoId: Long): Result<GitStatus> = repository.status(repoId)
}
