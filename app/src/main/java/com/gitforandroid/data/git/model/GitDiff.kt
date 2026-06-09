package com.gitforandroid.data.git.model

data class GitDiff(
    val files: List<DiffFile>,
    val rawDiff: String
)

data class DiffFile(
    val oldPath: String,
    val newPath: String,
    val changeType: ChangeType,
    val hunks: List<DiffHunk>
)

data class DiffHunk(
    val oldStart: Int,
    val oldCount: Int,
    val newStart: Int,
    val newCount: Int,
    val header: String,
    val lines: List<DiffLine>
)

data class DiffLine(
    val content: String,
    val lineType: DiffLineType
)

enum class DiffLineType {
    ADDED, REMOVED, CONTEXT, HEADER
}
