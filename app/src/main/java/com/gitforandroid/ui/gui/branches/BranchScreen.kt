package com.gitforandroid.ui.gui.branches

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gitforandroid.data.git.model.GitBranch
import com.gitforandroid.ui.common.ConfirmDialog
import com.gitforandroid.ui.common.ErrorDialog
import com.gitforandroid.ui.common.LoadingIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BranchScreen(
    repoId: Long,
    onBack: () -> Unit,
    viewModel: BranchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.error != null) {
        ErrorDialog(message = uiState.error!!, onDismiss = { viewModel.clearError() })
    }

    if (uiState.showCreateDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideCreateDialog() },
            title = { Text("Create Branch") },
            text = {
                OutlinedTextField(
                    value = uiState.newBranchName,
                    onValueChange = { viewModel.updateNewBranchName(it) },
                    label = { Text("Branch name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.createBranch() }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideCreateDialog() }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Branches") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.showCreateDialog() }) {
                        Icon(Icons.Filled.Add, "Create Branch")
                    }
                }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> LoadingIndicator(Modifier.padding(padding))
            else -> {
                LazyColumn(Modifier.padding(padding)) {
                    item {
                        Text(
                            "Local Branches",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(16.dp, 12.dp, 16.dp, 4.dp)
                        )
                    }
                    items(uiState.localBranches) { branch ->
                        BranchRow(branch, onCheckout = { viewModel.checkout(branch.name) },
                            onDelete = { viewModel.deleteBranch(branch.name) })
                    }

                    if (uiState.remoteBranches.isNotEmpty()) {
                        item {
                            Text(
                                "Remote Branches",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(16.dp, 12.dp, 16.dp, 4.dp)
                            )
                        }
                        items(uiState.remoteBranches) { branch ->
                            BranchRow(branch, onCheckout = { viewModel.checkout(branch.name.removePrefix("origin/")) },
                                onDelete = null)
                        }
                    }

                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun BranchRow(
    branch: GitBranch,
    onCheckout: () -> Unit,
    onDelete: (() -> Unit)?
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onCheckout
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (branch.isCurrentBranch) {
                    Icon(Icons.Filled.Check, contentDescription = "Current",
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
                Text(branch.name, style = MaterialTheme.typography.bodyMedium)
            }
            if (onDelete != null && !branch.isCurrentBranch) {
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Delete, "Delete", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
