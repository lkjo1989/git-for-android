package com.gitforandroid.ui.gui.status

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gitforandroid.data.git.model.DiffFile
import com.gitforandroid.data.git.model.DiffLine
import com.gitforandroid.data.git.model.DiffLineType

@Composable
fun DiffView(
    diffFile: DiffFile,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "${diffFile.oldPath} → ${diffFile.newPath}",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )

        diffFile.hunks.forEach { hunk ->
            Text(
                text = hunk.header,
                color = MaterialTheme.colorScheme.tertiary,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )

            hunk.lines.forEach { line ->
                val bgColor = when (line.lineType) {
                    DiffLineType.ADDED -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                    DiffLineType.REMOVED -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                    DiffLineType.HEADER -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                    else -> androidx.compose.ui.graphics.Color.Transparent
                }
                val fgColor = when (line.lineType) {
                    DiffLineType.ADDED -> MaterialTheme.colorScheme.tertiary
                    DiffLineType.REMOVED -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurface
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(bgColor)
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = line.content,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = fgColor
                    )
                }
            }
        }
    }
}
