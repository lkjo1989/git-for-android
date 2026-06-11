package com.gitforandroid.ui.gui.status

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gitforandroid.data.git.model.ChangeType
import com.gitforandroid.data.git.model.StatusFile
import com.gitforandroid.ui.common.ErrorDialog
import com.gitforandroid.ui.common.LoadingIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusScreen(
    repoId: Long,
    onCommitClick: () -> Unit,
    onLogClick: () -> Unit,
    onBranchesClick: () -> Unit,
    onPushPullClick: () -> Unit,
    onOpenInTerminal: () -> Unit,
    onBack: () -> Unit,
    viewModel: StatusViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.error != null) {
        ErrorDialog(message = uiState.error!!, onDismiss = { viewModel.clearError() })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.repoName.ifEmpty { "Status" }) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onCommitClick) {
                        Icon(Icons.Filled.Save, contentDescription = "Commit")
                    }
                    IconButton(onClick = onLogClick) {
                        Icon(Icons.Filled.History, contentDescription = "Log")
                    }
                    IconButton(onClick = onBranchesClick) {
                        Icon(Icons.Filled.AccountTree, contentDescription = "Branches")
                    }
                    IconButton(onClick = onPushPullClick) {
                        Icon(Icons.Filled.CloudUpload, contentDescription = "Push/Pull")
                    }
                    IconButton(onClick = onOpenInTerminal) {
                        Icon(Icons.Filled.Terminal, contentDescription = "Open in Terminal")
                    }
                }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> LoadingIndicator(Modifier.padding(padding))
            uiState.status == null -> {
                Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Text("No status info available")
                }
            }
            else -> {
                val status = uiState.status!!
                LazyColumn(modifier = Modifier.padding(padding)) {
                    // Repo info card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Branch: ${status.branch}",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = uiState.repoPath,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                                if (status.isClean) {
                                    Spacer(Modifier.height(4.dp))
                                    Text("Working tree clean")
                                }
                            }
                        }
                    }

                    // Staged files
                    if (status.added.isNotEmpty() || status.changed.isNotEmpty() || status.removed.isNotEmpty()) {
                        item {
                            Text(
                                "Staged",
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                        items(status.added) { file -> FileRow(file, uiState.selectedFiles, viewModel::toggleFileSelection) }
                        items(status.changed) { file -> FileRow(file, uiState.selectedFiles, viewModel::toggleFileSelection) }
                        items(status.removed) { file -> FileRow(file, uiState.selectedFiles, viewModel::toggleFileSelection) }
                    }

                    // Unstaged files
                    val unstaged = status.modified + status.untracked + status.missing
                    if (unstaged.isNotEmpty()) {
                        item {
                            Text(
                                "Unstaged",
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        items(unstaged) { file -> FileRow(file, uiState.selectedFiles, viewModel::toggleFileSelection) }
                    }

                    // Conflict files
                    if (status.conflicting.isNotEmpty()) {
                        item {
                            Text(
                                "Conflicts",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        items(status.conflicting) { file -> FileRow(file, uiState.selectedFiles, viewModel::toggleFileSelection) }
                    }

                    // Actions
                    if (uiState.selectedFiles.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(onClick = { viewModel.stageFiles() }, modifier = Modifier.weight(1f)) {
                                    Text("Stage")
                                }
                                OutlinedButton(onClick = { viewModel.unstageFiles() }, modifier = Modifier.weight(1f)) {
                                    Text("Unstage")
                                }
                            }
                        }
                    }

                    item { Spacer(Modifier.height(80.dp)) } // bottom padding for FAB
                }
            }
        }
    }
}

@Composable
private fun FileRow(
    file: StatusFile,
    selectedFiles: Set<String>,
    onToggle: (String) -> Unit
) {
    val isSelected = file.path in selectedFiles
    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = { onToggle(file.path) }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Checkbox(checked = isSelected, onCheckedChange = { onToggle(file.path) })
            Icon(
                imageVector = when (file.changeType) {
                    ChangeType.ADDED -> Icons.Filled.AddCircleOutline
                    ChangeType.MODIFIED -> Icons.Filled.Edit
                    ChangeType.DELETED -> Icons.Filled.RemoveCircleOutline
                    ChangeType.UNTRACKED -> Icons.Filled.NoteAdd
                    ChangeType.CONFLICTING -> Icons.Filled.Warning
                    else -> Icons.Filled.FilePresent
                },
                contentDescription = null,
                tint = when (file.changeType) {
                    ChangeType.ADDED -> MaterialTheme.colorScheme.tertiary
                    ChangeType.DELETED -> MaterialTheme.colorScheme.error
                    ChangeType.CONFLICTING -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
            Column {
                Text(
                    file.path,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    file.changeType.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
