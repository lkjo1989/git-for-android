package com.gitforandroid.ui.cli

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gitforandroid.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun TerminalScreen(
    viewModel: TerminalViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Auto-scroll to bottom when new lines appear
    LaunchedEffect(uiState.lines.size) {
        if (uiState.lines.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(uiState.lines.size - 1)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TerminalBackground)
            .imePadding()
    ) {
        // Terminal output area
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            items(uiState.lines) { line ->
                Text(
                    text = line.text,
                    color = when (line.type) {
                        TerminalLineType.INPUT -> TerminalGreen
                        TerminalLineType.OUTPUT -> TerminalWhite
                        TerminalLineType.ERROR -> TerminalRed
                        TerminalLineType.SYSTEM -> TerminalCyan
                    },
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(vertical = 1.dp)
                )
            }
        }

        // Divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(TerminalGreen.copy(alpha = 0.3f))
        )

        // Input area
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(TerminalBackground)
                .padding(8.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            val repoIndicator = if (uiState.currentRepoId != null) {
                val repo = uiState.repoList.find { it.first == uiState.currentRepoId }
                repo?.second?.take(20) ?: "repo${uiState.currentRepoId}"
            } else "no-repo"

            Text(
                text = "git:$repoIndicator> ",
                color = TerminalGreen,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp
            )

            var textFieldValue by remember { mutableStateOf(TextFieldValue("")) }

            BasicTextField(
                value = textFieldValue,
                onValueChange = { newValue ->
                    textFieldValue = newValue
                    viewModel.updateInput(newValue.text)
                },
                modifier = Modifier
                    .weight(1f)
                    .onKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown) {
                            when (event.key) {
                                Key.Enter -> {
                                    viewModel.executeCommand()
                                    textFieldValue = TextFieldValue("")
                                    true
                                }
                                Key.DirectionUp -> {
                                    val newInput = viewModel.navigateHistory(1)
                                    textFieldValue = TextFieldValue(newInput)
                                    true
                                }
                                Key.DirectionDown -> {
                                    val newInput = viewModel.navigateHistory(-1)
                                    textFieldValue = TextFieldValue(newInput)
                                    true
                                }
                                else -> false
                            }
                        } else false
                    },
                textStyle = TextStyle(
                    color = TerminalWhite,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                ),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(TerminalGreen),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        viewModel.executeCommand()
                        textFieldValue = TextFieldValue("")
                    }
                )
            )
        }
    }
}
