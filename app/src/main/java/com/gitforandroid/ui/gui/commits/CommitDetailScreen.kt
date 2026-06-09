package com.gitforandroid.ui.gui.commits

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gitforandroid.data.git.model.DiffFile
import com.gitforandroid.data.git.model.DiffLineType
import com.gitforandroid.ui.common.LoadingIndicator
import com.gitforandroid.ui.gui.status.DiffView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommitDetailScreen(
    repoId: Long,
    commitHash: String,
    onBack: () -> Unit,
    viewModel: CommitDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(repoId, commitHash) {
        viewModel.load(repoId, commitHash)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Commit Detail") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> LoadingIndicator(Modifier.padding(padding))
            uiState.commit != null -> {
                val commit = uiState.commit!!
                LazyColumn(Modifier.padding(padding).padding(16.dp)) {
                    item {
                        Text(commit.hash, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(8.dp))
                        Text(commit.fullMessage.ifEmpty { commit.message }, style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(4.dp))
                        Text("Author: ${commit.author.name} <${commit.author.email}>", style = MaterialTheme.typography.bodySmall)
                        Text("Date: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date(commit.timestamp))}",
                            style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(16.dp))
                    }

                    if (uiState.diffFiles.isNotEmpty()) {
                        item {
                            Text("Changes:", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                        }
                        items(uiState.diffFiles) { diffFile ->
                            DiffView(diffFile = diffFile, modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                }
            }
            uiState.error != null -> {
                Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Text(uiState.error!!)
                }
            }
        }
    }
}
