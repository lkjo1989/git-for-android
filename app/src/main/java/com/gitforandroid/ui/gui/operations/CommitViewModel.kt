package com.gitforandroid.ui.gui.operations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gitforandroid.data.git.model.Author
import com.gitforandroid.data.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CommitUiState(
    val repoId: Long = 0,
    val authorName: String = "",
    val authorEmail: String = "",
    val message: String = "",
    val isCommitting: Boolean = false,
    val isComplete: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class CommitViewModel @Inject constructor(
    private val repository: AppRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CommitUiState())
    val uiState: StateFlow<CommitUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val name = repository.getSetting("author_name")
            val email = repository.getSetting("author_email")
            _uiState.update {
                it.copy(
                    authorName = name ?: "",
                    authorEmail = email ?: ""
                )
            }
        }
    }

    fun setRepoId(repoId: Long) = _uiState.update { it.copy(repoId = repoId) }

    fun updateAuthorName(name: String) = _uiState.update { it.copy(authorName = name) }

    fun updateAuthorEmail(email: String) = _uiState.update { it.copy(authorEmail = email) }

    fun updateMessage(message: String) = _uiState.update { it.copy(message = message) }

    fun commit() {
        viewModelScope.launch {
            val state = _uiState.value
            _uiState.update { it.copy(isCommitting = true, error = null) }

            // Save author info for next time
            repository.setSetting("author_name", state.authorName)
            repository.setSetting("author_email", state.authorEmail)

            val author = Author(state.authorName.ifBlank { "Unknown" }, state.authorEmail.ifBlank { "unknown@example.com" })

            // Stage all first, then commit
            val addResult = repository.add(state.repoId, listOf("."))
            if (addResult.isFailure) {
                _uiState.update { it.copy(isCommitting = false, error = addResult.exceptionOrNull()?.message) }
                return@launch
            }

            repository.commit(state.repoId, state.message, author)
                .onSuccess {
                    _uiState.update { it.copy(isCommitting = false, isComplete = true) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isCommitting = false, error = e.message) }
                }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}
