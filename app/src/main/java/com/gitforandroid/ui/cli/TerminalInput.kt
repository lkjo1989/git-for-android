package com.gitforandroid.ui.cli

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gitforandroid.ui.theme.TerminalGreen
import com.gitforandroid.ui.theme.TerminalWhite

/**
 * Terminal input composable with colored prompt.
 */
@Composable
fun TerminalInputBar(
    prompt: String,
    inputValue: String,
    onInputChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth().padding(8.dp)) {
        androidx.compose.material3.Text(
            text = prompt,
            color = TerminalGreen,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp
        )

        BasicTextField(
            value = inputValue,
            onValueChange = onInputChange,
            modifier = Modifier.weight(1f),
            textStyle = TextStyle(
                color = TerminalWhite,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp
            ),
            cursorBrush = SolidColor(TerminalGreen),
            singleLine = true
        )
    }
}
