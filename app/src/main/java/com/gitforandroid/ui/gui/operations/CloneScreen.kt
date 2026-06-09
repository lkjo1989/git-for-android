package com.gitforandroid.ui.gui.operations

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.gitforandroid.util.StoragePermissionManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloneScreen(
    onCloneComplete: () -> Unit,
    onBack: () -> Unit,
    viewModel: CloneViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // SAF directory picker launcher
    val directoryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            viewModel.onDirectoryPicked(uri)
        }
    }

    // Refresh permission state when returning from system settings
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissionState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Launch MANAGE_EXTERNAL_STORAGE intent
    val permIntent = remember { StoragePermissionManager.getManageStorageIntent() }

    LaunchedEffect(uiState.isComplete) {
        if (uiState.isComplete) onCloneComplete()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Clone Repository") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // --- URL ---
            OutlinedTextField(
                value = uiState.url,
                onValueChange = { viewModel.updateUrl(it) },
                label = { Text("Repository URL") },
                placeholder = { Text("https://github.com/user/repo.git") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // --- Local directory name ---
            OutlinedTextField(
                value = uiState.localName,
                onValueChange = { viewModel.updateLocalName(it) },
                label = { Text("Local directory name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // --- Base path selection ---
            Text(
                text = "Clone location",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = uiState.basePath,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace
                        )
                        if (uiState.localName.isNotBlank()) {
                            Text(
                                text = "→ ${uiState.basePath}/${uiState.localName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // SAF directory picker button
                OutlinedButton(
                    onClick = { directoryPickerLauncher.launch(null) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Choose Folder")
                }

                // Reset to default
                TextButton(
                    onClick = { viewModel.resetToDefaultPath() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Use Default", style = MaterialTheme.typography.labelMedium)
                }
            }

            // --- Permission status ---
            if (!uiState.hasStoragePermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "⚠ Storage access is limited",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "The default app directory always works. To use custom paths, grant \"All files access\" below.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(Modifier.height(8.dp))
                        TextButton(
                            onClick = { context.startActivity(permIntent) }
                        ) {
                            Text("Open Permission Settings")
                        }
                    }
                }
            }

            // --- Credentials (optional) ---
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

            // --- Progress ---
            if (uiState.isCloning) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
                if (uiState.progressText.isNotBlank()) {
                    Text(
                        text = uiState.progressText,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // --- Clone button ---
            Button(
                onClick = { viewModel.startClone() },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.url.isNotBlank() && uiState.localName.isNotBlank() && !uiState.isCloning
            ) {
                Text("Clone")
            }

            // --- Error ---
            if (uiState.error != null) {
                Spacer(Modifier.height(4.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = uiState.error!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("Dismiss")
                        }
                    }
                }
            }
        }
    }
}
