package com.gitforandroid.ui.gui.operations

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gitforandroid.data.git.Credentials
import com.gitforandroid.domain.usecase.CloneRepoUseCase
import com.gitforandroid.util.StoragePermissionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CloneUiState(
    val url: String = "",
    val localName: String = "",
    val username: String = "",
    val password: String = "",
    val basePath: String = "",
    val isCloning: Boolean = false,
    val progressText: String = "",
    val isComplete: Boolean = false,
    val error: String? = null,
    val showPermissionRequest: Boolean = false,
    val hasStoragePermission: Boolean = false
)

@HiltViewModel
class CloneViewModel @Inject constructor(
    private val cloneRepoUseCase: CloneRepoUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(CloneUiState())
    val uiState: StateFlow<CloneUiState> = _uiState.asStateFlow()

    init {
        val defaultDir = StoragePermissionManager.getDefaultCloneBaseDir(context)
        val hasPerm = StoragePermissionManager.hasStoragePermission(context)
        _uiState.update {
            it.copy(
                basePath = defaultDir,
                hasStoragePermission = hasPerm
            )
        }
    }

    fun updateUrl(url: String) = _uiState.update { it.copy(url = url) }

    fun updateLocalName(name: String) = _uiState.update { it.copy(localName = name) }

    fun updateUsername(username: String) = _uiState.update { it.copy(username = username) }

    fun updatePassword(password: String) = _uiState.update { it.copy(password = password) }

    /**
     * Called when the user picks a directory via SAF document tree picker.
     * Extracts the filesystem path from the URI so JGit can use it.
     */
    fun onDirectoryPicked(uri: Uri) {
        val path = StoragePermissionManager.extractPathFromTreeUri(uri)
        if (path != null) {
            _uiState.update { it.copy(basePath = path, error = null) }
        } else {
            _uiState.update {
                it.copy(
                    basePath = uri.toString(),
                    error = "Could not resolve directory path. Clone may fail."
                )
            }
        }
    }

    /**
     * Reset the clone base path to the best available default directory
     * (public when permission is granted, app-private otherwise).
     */
    fun resetToDefaultPath() {
        val defaultDir = StoragePermissionManager.getDefaultCloneBaseDir(context)
        _uiState.update { it.copy(basePath = defaultDir, error = null) }
    }

    /**
     * Check storage permission and refresh the state.
     * Call this after returning from system settings (onResume).
     * If permission was just granted and the user hasn't manually picked
     * a custom directory, upgrade from app-private to public default.
     */
    fun refreshPermissionState() {
        val hadPerm = _uiState.value.hasStoragePermission
        val hasPerm = StoragePermissionManager.hasStoragePermission(context)

        // Permission just granted: upgrade from app-private to public default
        val appPrivateDir = StoragePermissionManager.getAppPrivateCloneBaseDir(context)
        val currentPath = _uiState.value.basePath
        val isUsingAppPrivate = currentPath.startsWith(appPrivateDir)

        val newPath = if (!hadPerm && hasPerm && isUsingAppPrivate) {
            StoragePermissionManager.getPublicCloneBaseDir()
        } else {
            currentPath
        }

        _uiState.update {
            it.copy(
                basePath = newPath,
                hasStoragePermission = hasPerm,
                showPermissionRequest = false
            )
        }
    }

    /**
     * Request MANAGE_EXTERNAL_STORAGE permission.
     * The caller should launch the intent returned by [StoragePermissionManager.getManageStorageIntent].
     */
    fun requestStoragePermission() {
        _uiState.update { it.copy(showPermissionRequest = true) }
    }

    fun dismissPermissionRequest() {
        _uiState.update { it.copy(showPermissionRequest = false) }
    }

    fun startClone() {
        val state = _uiState.value

        // Validate inputs
        if (state.url.isBlank() || state.localName.isBlank()) {
            _uiState.update { it.copy(error = "URL and directory name are required") }
            return
        }

        // Check permissions for non-app-specific paths
        val appExternalDir = context.getExternalFilesDir(null)?.absolutePath ?: ""
        val appInternalDir = context.filesDir.absolutePath
        val isAppPrivatePath = state.basePath.startsWith(appExternalDir) ||
            state.basePath.startsWith(appInternalDir)

        if (!isAppPrivatePath && !state.hasStoragePermission) {
            _uiState.update { it.copy(showPermissionRequest = true) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isCloning = true, error = null, progressText = "") }
            val localPath = "${state.basePath}/${state.localName}"
            val credentials = if (state.username.isNotEmpty()) {
                Credentials(state.username, state.password)
            } else null

            cloneRepoUseCase(
                url = state.url,
                localPath = localPath,
                name = state.localName,
                credentials = credentials,
                progress = { text -> _uiState.update { it.copy(progressText = text) } }
            ).onSuccess {
                _uiState.update { it.copy(isCloning = false, isComplete = true) }
            }.onFailure { e ->
                val message = when {
                    e.message?.contains("Permission denied") == true ||
                    e.message?.contains("Access denied") == true ||
                    e.message?.contains("Read-only") == true ->
                        "Permission denied. Try using the default app directory or pick a different folder."
                    else -> e.message ?: "Clone failed"
                }
                _uiState.update { it.copy(isCloning = false, error = message) }
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}
