package com.gitforandroid.domain.usecase

import com.gitforandroid.data.git.model.Author
import com.gitforandroid.data.repository.AppRepository
import javax.inject.Inject

class CommitUseCase @Inject constructor(
    private val repository: AppRepository
) {
    suspend operator fun invoke(
        repoId: Long,
        files: List<String>,
        message: String,
        author: Author
    ): Result<String> {
        val addResult = repository.add(repoId, files)
        if (addResult.isFailure) return addResult
        return repository.commit(repoId, message, author)
    }
}
