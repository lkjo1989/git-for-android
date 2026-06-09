package com.gitforandroid.ui.gui.commits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gitforandroid.data.git.model.DiffFile
import com.gitforandroid.data.git.model.GitCommit
import com.gitforandroid.data.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CommitDetailUiState(
    val commit: GitCommit? = null,
    val diffFiles: List<DiffFile> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class CommitDetailViewModel @Inject constructor(
    private val repository: AppRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CommitDetailUiState())
    val uiState: StateFlow<CommitDetailUiState> = _uiState.asStateFlow()

    fun load(repoId: Long, commitHash: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val logResult = repository.log(repoId, 200)
            val commit = logResult.getOrNull()?.find { it.commit.hash == commitHash }?.commit

            if (commit != null) {
                val diffResult = repository.diff(repoId)
                _uiState.update {
                    it.copy(
                        commit = commit,
                        diffFiles = diffResult.getOrNull()?.files ?: emptyList(),
                        isLoading = false
                    )
                }
            } else {
                _uiState.update { it.copy(error = "Commit not found", isLoading = false) }
            }
        }
    }
}
