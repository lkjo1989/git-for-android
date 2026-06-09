package com.gitforandroid.data.git.model

data class GitStatus(
    val branch: String,
    val added: List<StatusFile>,
    val changed: List<StatusFile>,
    val removed: List<StatusFile>,
    val modified: List<StatusFile>,
    val missing: List<StatusFile>,
    val untracked: List<StatusFile>,
    val conflicting: List<StatusFile>
) {
    val isClean: Boolean
        get() = added.isEmpty() && changed.isEmpty() && removed.isEmpty() &&
                modified.isEmpty() && missing.isEmpty() && untracked.isEmpty() &&
                conflicting.isEmpty()

    val totalChanges: Int
        get() = added.size + changed.size + removed.size + modified.size +
                missing.size + untracked.size + conflicting.size
}

data class StatusFile(
    val path: String,
    val oldPath: String? = null, // for renamed files
    val changeType: ChangeType
)

enum class ChangeType {
    ADDED, MODIFIED, DELETED, RENAMED, UNTRACKED, CONFLICTING
}
