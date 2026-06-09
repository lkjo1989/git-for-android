package com.gitforandroid.data.git.model

data class GitLogEntry(
    val commit: GitCommit,
    val refs: List<String> // branch/tag refs pointing to this commit
)
