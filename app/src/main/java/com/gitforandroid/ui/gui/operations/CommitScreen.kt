package com.gitforandroid.ui.gui.operations

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gitforandroid.ui.common.ErrorDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommitScreen(
    repoId: Long,
    onCommitComplete: () -> Unit,
    onBack: () -> Unit,
    viewModel: CommitViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.error != null) {
        ErrorDialog(message = uiState.error!!, onDismiss = { viewModel.clearError() })
    }

    LaunchedEffect(uiState.isComplete) {
        if (uiState.isComplete) onCommitComplete()
    }

    LaunchedEffect(repoId) { viewModel.setRepoId(repoId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Commit") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = uiState.authorName,
                onValueChange = { viewModel.updateAuthorName(it) },
                label = { Text("Author Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = uiState.authorEmail,
                onValueChange = { viewModel.updateAuthorEmail(it) },
                label = { Text("Author Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = uiState.message,
                onValueChange = { viewModel.updateMessage(it) },
                label = { Text("Commit message") },
                modifier = Modifier.fillMaxWidth().weight(1f),
                maxLines = 10
            )

            if (uiState.isCommitting) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            Button(
                onClick = { viewModel.commit() },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.message.isNotBlank() && !uiState.isCommitting
            ) {
                Text("Commit")
            }
        }
    }
}
