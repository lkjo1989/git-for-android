package com.gitforandroid.ui.gui.operations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gitforandroid.data.git.Credentials
import com.gitforandroid.data.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PushPullUiState(
    val repoId: Long = 0,
    val remote: String = "origin",
    val branch: String = "",
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val result: String? = null,
    val error: String? = null
)

@HiltViewModel
class PushPullViewModel @Inject constructor(
    private val repository: AppRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PushPullUiState())
    val uiState: StateFlow<PushPullUiState> = _uiState.asStateFlow()

    fun setRepoId(repoId: Long) = _uiState.update { it.copy(repoId = repoId) }
    fun updateRemote(remote: String) = _uiState.update { it.copy(remote = remote) }
    fun updateBranch(branch: String) = _uiState.update { it.copy(branch = branch) }
    fun updateUsername(u: String) = _uiState.update { it.copy(username = u) }
    fun updatePassword(p: String) = _uiState.update { it.copy(password = p) }
    fun clearResult() = _uiState.update { it.copy(result = null) }
    fun clearError() = _uiState.update { it.copy(error = null) }

    private fun credentials(): Credentials? {
        val s = _uiState.value
        return if (s.username.isNotEmpty()) Credentials(s.username, s.password) else null
    }

    fun push() {
        val s = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.push(s.repoId, s.remote, s.branch.ifBlank { null }, credentials())
                .onSuccess { msg -> _uiState.update { it.copy(isLoading = false, result = msg) } }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
        }
    }

    fun pull() {
        val s = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.pull(s.repoId, s.remote, s.branch.ifBlank { null }, credentials())
                .onSuccess { msg -> _uiState.update { it.copy(isLoading = false, result = msg) } }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
        }
    }

    fun fetch() {
        val s = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.fetch(s.repoId, s.remote, credentials())
                .onSuccess { msg -> _uiState.update { it.copy(isLoading = false, result = msg) } }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
        }
    }
}
