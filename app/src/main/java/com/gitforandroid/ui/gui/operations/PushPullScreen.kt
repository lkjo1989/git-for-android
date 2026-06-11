package com.gitforandroid.ui.gui.operations

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gitforandroid.ui.common.ErrorDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PushPullScreen(
    repoId: Long,
    onOpenInTerminal: () -> Unit,
    onBack: () -> Unit,
    viewModel: PushPullViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(repoId) { viewModel.setRepoId(repoId) }

    if (uiState.error != null) {
        ErrorDialog(message = uiState.error!!, onDismiss = { viewModel.clearError() })
    }

    if (uiState.result != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearResult() },
            title = { Text("Result") },
            text = { Text(uiState.result!!) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearResult() }) { Text("OK") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Push & Pull") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenInTerminal) {
                        Icon(Icons.Filled.Terminal, "Open in Terminal")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = uiState.remote,
                onValueChange = { viewModel.updateRemote(it) },
                label = { Text("Remote") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = uiState.branch,
                onValueChange = { viewModel.updateBranch(it) },
                label = { Text("Branch (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            var showCredentials by remember { mutableStateOf(false) }
            if (showCredentials) {
                OutlinedTextField(
                    value = uiState.username,
                    onValueChange = { viewModel.updateUsername(it) },
                    label = { Text("Username") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = uiState.password,
                    onValueChange = { viewModel.updatePassword(it) },
                    label = { Text("Password / Token") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
            TextButton(onClick = { showCredentials = !showCredentials }) {
                Text(if (showCredentials) "Hide credentials" else "Add credentials")
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { viewModel.push() },
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isLoading
                ) { Text("Push") }

                Button(
                    onClick = { viewModel.pull() },
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isLoading
                ) { Text("Pull") }
            }

            OutlinedButton(
                onClick = { viewModel.fetch() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            ) { Text("Fetch") }

            if (uiState.isLoading) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        }
    }
}
