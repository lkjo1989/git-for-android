package com.gitforandroid.ui.gui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gitforandroid.data.repository.AppRepository
import com.gitforandroid.util.FileLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class SettingsUiState(
    val authorName: String = "",
    val authorEmail: String = "",
    val sshKeyPath: String = "",
    val sshResult: String? = null,
    val logEnabled: Boolean = true,
    val logPath: String = ""
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: AppRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val name = repository.getSetting("author_name")
            val email = repository.getSetting("author_email")
            val sshPath = repository.getSetting("ssh_key_path")
            val logEnabled = repository.getSetting("log_enabled")?.let { it != "false" } ?: true
            FileLogger.enabled = logEnabled
            _uiState.update {
                it.copy(
                    authorName = name ?: "",
                    authorEmail = email ?: "",
                    sshKeyPath = sshPath ?: "",
                    logEnabled = logEnabled,
                    logPath = FileLogger.getLogPath(context)
                )
            }
        }
    }

    fun toggleLogging(enabled: Boolean) {
        _uiState.update { it.copy(logEnabled = enabled) }
        FileLogger.enabled = enabled
        viewModelScope.launch {
            repository.setSetting("log_enabled", enabled.toString())
        }
    }

    fun updateAuthorName(name: String) = _uiState.update { it.copy(authorName = name) }
    fun updateAuthorEmail(email: String) = _uiState.update { it.copy(authorEmail = email) }
    fun updateSshKeyPath(path: String) = _uiState.update { it.copy(sshKeyPath = path) }

    fun saveAuthor() {
        viewModelScope.launch {
            repository.setSetting("author_name", _uiState.value.authorName)
            repository.setSetting("author_email", _uiState.value.authorEmail)
        }
    }

    fun saveSshKeyPath() {
        viewModelScope.launch {
            repository.setSetting("ssh_key_path", _uiState.value.sshKeyPath)
        }
    }

    fun generateSshKey() {
        viewModelScope.launch {
            try {
                val sshDir = File("/data/data/com.gitforandroid/files/ssh")
                sshDir.mkdirs()
                val keyFile = File(sshDir, "id_rsa")
                val pubFile = File(sshDir, "id_rsa.pub")

                // Simple RSA key generation note
                _uiState.update {
                    it.copy(
                        sshResult = "SSH key generation will be implemented via JGit's JschConfigSessionFactory.\n" +
                                "Keys stored at: ${sshDir.absolutePath}",
                        sshKeyPath = keyFile.absolutePath
                    )
                }
                repository.setSetting("ssh_key_path", keyFile.absolutePath)
            } catch (e: Exception) {
                _uiState.update { it.copy(sshResult = "Error: ${e.message}") }
            }
        }
    }
}
