package com.gitforandroid.ui.gui.commits

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gitforandroid.data.git.model.GitLogEntry
import com.gitforandroid.data.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LogUiState(
    val repoId: Long = 0,
    val repoName: String = "",
    val entries: List<GitLogEntry> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class LogViewModel @Inject constructor(
    private val repository: AppRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val repoId: Long = savedStateHandle.get<Long>("repoId") ?: 0

    private val _uiState = MutableStateFlow(LogUiState(repoId = repoId))
    val uiState: StateFlow<LogUiState> = _uiState.asStateFlow()

    init {
        loadLog()
        viewModelScope.launch {
            repository.getRepo(repoId)?.let { repo ->
                _uiState.update { it.copy(repoName = repo.name) }
            }
        }
    }

    fun loadLog() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.log(repoId)
                .onSuccess { entries ->
                    _uiState.update { it.copy(entries = entries, isLoading = false) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message, isLoading = false) }
                }
        }
    }
}
