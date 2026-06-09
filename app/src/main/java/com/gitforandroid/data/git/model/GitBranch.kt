package com.gitforandroid.data.git.model

data class GitBranch(
    val name: String,
    val fullName: String, // e.g. "refs/heads/main"
    val isRemote: Boolean,
    val isCurrentBranch: Boolean,
    val objectId: String
)

data class CheckoutResult(
    val previousBranch: String,
    val newBranch: String,
    val message: String
)
