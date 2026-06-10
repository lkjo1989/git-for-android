package com.gitforandroid.ui.gui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Settings") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Author Info
            Text("Default Author", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = uiState.authorName,
                onValueChange = { viewModel.updateAuthorName(it) },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = uiState.authorEmail,
                onValueChange = { viewModel.updateAuthorEmail(it) },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Button(
                onClick = { viewModel.saveAuthor() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Author Info")
            }

            Divider()

            // SSH Keys
            Text("SSH Keys", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = uiState.sshKeyPath,
                onValueChange = { viewModel.updateSshKeyPath(it) },
                label = { Text("SSH Private Key Path") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { viewModel.generateSshKey() },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Generate")
                }
                Button(
                    onClick = { viewModel.saveSshKeyPath() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save Path")
                }
            }

            if (uiState.sshResult != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Text(uiState.sshResult!!, modifier = Modifier.padding(12.dp))
                }
            }

            Divider()

            // Logging
            Text("Diagnostics", style = MaterialTheme.typography.titleMedium)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("File Logging", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Write operation logs to file for debugging",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = uiState.logEnabled,
                    onCheckedChange = { viewModel.toggleLogging(it) }
                )
            }

            if (uiState.logPath.isNotBlank()) {
                Text(
                    text = "Log path: ${uiState.logPath}",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Divider()

            // About
            Text("About", style = MaterialTheme.typography.titleMedium)
            Text("Git For Android v${uiState.versionName}", style = MaterialTheme.typography.bodyMedium)
            Text("Built with JGit, Jetpack Compose, and Kotlin",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
