package com.gitforandroid.ui.gui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gitforandroid.data.local.entity.RepoEntity
import com.gitforandroid.data.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val repos: List<RepoEntity> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: AppRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.allRepos.collect { repos ->
                _uiState.update { it.copy(repos = repos, isLoading = false, error = null) }
            }
        }
    }

    fun deleteRepo(id: Long) {
        viewModelScope.launch {
            repository.deleteRepo(id)
        }
    }

    fun refresh() {
        _uiState.update { it.copy(isLoading = true) }
    }
}
