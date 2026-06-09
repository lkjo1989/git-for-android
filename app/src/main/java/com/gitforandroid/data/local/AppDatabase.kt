package com.gitforandroid.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.gitforandroid.data.local.dao.RepoDao
import com.gitforandroid.data.local.dao.SettingDao
import com.gitforandroid.data.local.entity.RepoEntity
import com.gitforandroid.data.local.entity.SettingEntity

@Database(
    entities = [RepoEntity::class, SettingEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun repoDao(): RepoDao
    abstract fun settingDao(): SettingDao
}
