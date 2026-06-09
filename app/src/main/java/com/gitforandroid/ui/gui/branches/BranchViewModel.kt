package com.gitforandroid.ui.gui.branches

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gitforandroid.data.git.model.GitBranch
import com.gitforandroid.data.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BranchUiState(
    val branches: List<GitBranch> = emptyList(),
    val localBranches: List<GitBranch> = emptyList(),
    val remoteBranches: List<GitBranch> = emptyList(),
    val isLoading: Boolean = true,
    val showCreateDialog: Boolean = false,
    val newBranchName: String = "",
    val operationMessage: String? = null,
    val error: String? = null
)

@HiltViewModel
class BranchViewModel @Inject constructor(
    private val repository: AppRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val repoId: Long = savedStateHandle.get<Long>("repoId") ?: 0

    private val _uiState = MutableStateFlow(BranchUiState())
    val uiState: StateFlow<BranchUiState> = _uiState.asStateFlow()

    init {
        loadBranches()
    }

    fun loadBranches() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.branches(repoId)
                .onSuccess { branches ->
                    _uiState.update {
                        it.copy(
                            branches = branches,
                            localBranches = branches.filter { !it.isRemote },
                            remoteBranches = branches.filter { it.isRemote },
                            isLoading = false
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message, isLoading = false) }
                }
        }
    }

    fun checkout(branchName: String) {
        viewModelScope.launch {
            repository.checkout(repoId, branchName)
                .onSuccess { result ->
                    _uiState.update { it.copy(operationMessage = result.message) }
                    loadBranches()
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
        }
    }

    fun deleteBranch(branchName: String) {
        viewModelScope.launch {
            repository.deleteBranch(repoId, branchName)
                .onSuccess { loadBranches() }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
        }
    }

    fun createBranch() {
        viewModelScope.launch {
            val name = _uiState.value.newBranchName
            if (name.isBlank()) return@launch
            repository.createBranch(repoId, name)
                .onSuccess {
                    _uiState.update { it.copy(showCreateDialog = false, newBranchName = "", operationMessage = "Created branch '$name'") }
                    loadBranches()
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
        }
    }

    fun showCreateDialog() = _uiState.update { it.copy(showCreateDialog = true) }
    fun hideCreateDialog() = _uiState.update { it.copy(showCreateDialog = false, newBranchName = "") }
    fun updateNewBranchName(name: String) = _uiState.update { it.copy(newBranchName = name) }
    fun clearMessage() = _uiState.update { it.copy(operationMessage = null) }
    fun clearError() = _uiState.update { it.copy(error = null) }
}
