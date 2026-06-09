package com.gitforandroid.ui.cli

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gitforandroid.ui.theme.*

/**
 * Renders a single terminal output line with ANSI color styling.
 */
@Composable
fun TerminalOutputLine(
    line: TerminalLine,
    modifier: Modifier = Modifier
) {
    val color = when (line.type) {
        TerminalLineType.INPUT -> TerminalGreen
        TerminalLineType.OUTPUT -> TerminalWhite
        TerminalLineType.ERROR -> TerminalRed
        TerminalLineType.SYSTEM -> TerminalCyan
    }

    Text(
        text = line.text,
        color = color,
        fontFamily = FontFamily.Monospace,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 1.dp)
    )
}
