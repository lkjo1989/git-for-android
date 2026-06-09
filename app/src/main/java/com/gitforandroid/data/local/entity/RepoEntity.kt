package com.gitforandroid.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "repos")
data class RepoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val localPath: String,
    val remoteUrl: String? = null,
    val currentBranch: String? = null,
    val lastOpenedAt: Long = System.currentTimeMillis()
)
