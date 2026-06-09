package com.gitforandroid.data.local.dao

import androidx.room.*
import com.gitforandroid.data.local.entity.RepoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RepoDao {
    @Query("SELECT * FROM repos ORDER BY lastOpenedAt DESC")
    fun getAll(): Flow<List<RepoEntity>>

    @Query("SELECT * FROM repos WHERE id = :id")
    suspend fun getById(id: Long): RepoEntity?

    @Query("SELECT * FROM repos WHERE localPath = :path LIMIT 1")
    suspend fun getByPath(path: String): RepoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(repo: RepoEntity): Long

    @Update
    suspend fun update(repo: RepoEntity)

    @Delete
    suspend fun delete(repo: RepoEntity)

    @Query("DELETE FROM repos WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE repos SET lastOpenedAt = :timestamp WHERE id = :id")
    suspend fun updateLastOpened(id: Long, timestamp: Long = System.currentTimeMillis())
}
