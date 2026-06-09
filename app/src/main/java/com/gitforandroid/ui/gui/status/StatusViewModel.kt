package com.gitforandroid.ui.gui.status

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gitforandroid.data.git.model.GitStatus
import com.gitforandroid.data.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StatusUiState(
    val repoId: Long = 0,
    val repoName: String = "",
    val repoPath: String = "",
    val status: GitStatus? = null,
    val isLoading: Boolean = true,
    val selectedFiles: Set<String> = emptySet(),
    val operationResult: String? = null,
    val error: String? = null
)

@HiltViewModel
class StatusViewModel @Inject constructor(
    private val repository: AppRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val repoId: Long = savedStateHandle.get<Long>("repoId") ?: 0

    private val _uiState = MutableStateFlow(StatusUiState(repoId = repoId))
    val uiState: StateFlow<StatusUiState> = _uiState.asStateFlow()

    init {
        loadStatus()
        viewModelScope.launch {
            repository.getRepo(repoId)?.let { repo ->
                _uiState.update {
                    it.copy(
                        repoName = repo.name,
                        repoPath = repo.localPath
                    )
                }
            }
        }
    }

    fun loadStatus() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.status(repoId)
                .onSuccess { status ->
                    _uiState.update { it.copy(status = status, isLoading = false) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message, isLoading = false) }
                }
        }
    }

    fun toggleFileSelection(path: String) {
        _uiState.update { state ->
            val newSelected = if (path in state.selectedFiles) {
                state.selectedFiles - path
            } else {
                state.selectedFiles + path
            }
            state.copy(selectedFiles = newSelected)
        }
    }

    fun stageFiles() {
        val files = _uiState.value.selectedFiles.toList()
        if (files.isEmpty()) return
        viewModelScope.launch {
            repository.add(repoId, files)
                .onSuccess { loadStatus(); _uiState.update { it.copy(selectedFiles = emptySet()) } }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun unstageFiles() {
        val files = _uiState.value.selectedFiles.toList()
        if (files.isEmpty()) return
        viewModelScope.launch {
            repository.unstage(repoId, files)
                .onSuccess { loadStatus(); _uiState.update { it.copy(selectedFiles = emptySet()) } }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}
