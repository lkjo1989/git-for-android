package com.gitforandroid.data.git.model

data class GitCommit(
    val hash: String,
    val shortHash: String,
    val author: Author,
    val committer: Author,
    val message: String,
    val fullMessage: String,
    val timestamp: Long,
    val parentHashes: List<String>,
    val parentCount: Int
)

data class Author(
    val name: String,
    val email: String
)
