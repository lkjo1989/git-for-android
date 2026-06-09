package com.gitforandroid.di

import android.content.Context
import androidx.room.Room
import com.gitforandroid.data.git.GitService
import com.gitforandroid.data.git.GitServiceImpl
import com.gitforandroid.data.local.AppDatabase
import com.gitforandroid.data.local.dao.RepoDao
import com.gitforandroid.data.local.dao.SettingDao
import com.gitforandroid.data.repository.AppRepository
import com.gitforandroid.domain.parser.GitCliParser
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "gitforandroid.db"
        ).build()
    }

    @Provides
    @Singleton
    fun provideRepoDao(database: AppDatabase): RepoDao = database.repoDao()

    @Provides
    @Singleton
    fun provideSettingDao(database: AppDatabase): SettingDao = database.settingDao()

    @Provides
    @Singleton
    fun provideGitService(@ApplicationContext context: Context): GitService = GitServiceImpl(context)

    @Provides
    @Singleton
    fun provideGitCliParser(): GitCliParser = GitCliParser()

    @Provides
    @Singleton
    fun provideAppRepository(
        repoDao: RepoDao,
        settingDao: SettingDao,
        gitService: GitService,
        @ApplicationContext context: Context
    ): AppRepository = AppRepository(repoDao, settingDao, gitService, context)
}
