package com.photobox.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import com.photobox.core.data.db.AppDatabase
import com.photobox.core.data.db.dao.FavoriteDao
import com.photobox.core.data.db.dao.LikeDao
import com.photobox.core.data.db.dao.PendingShuffleDao
import com.photobox.core.data.db.dao.ShuffleDao
import com.photobox.core.data.mediastore.MediaStoreDataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun providePreferencesDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
        produceFile = { context.preferencesDataStoreFile("photobox_prefs") },
    )

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "photobox.db")
            .fallbackToDestructiveMigration() // 仅 dev；上线前替换为正式 migrations
            .build()

    @Provides fun provideFavoriteDao(db: AppDatabase): FavoriteDao = db.favoriteDao()
    @Provides fun provideLikeDao(db: AppDatabase): LikeDao = db.likeDao()
    @Provides fun provideShuffleDao(db: AppDatabase): ShuffleDao = db.shuffleDao()
    @Provides fun providePendingShuffleDao(db: AppDatabase): PendingShuffleDao = db.pendingShuffleDao()

    @Provides
    @Singleton
    fun provideMediaStoreDataSource(
        @ApplicationContext context: Context,
        pendingShuffleDao: PendingShuffleDao,
    ): MediaStoreDataSource = MediaStoreDataSource(context, pendingShuffleDao)
}

private fun Context.preferencesDataStoreFile(name: String) =
    java.io.File(applicationContext.filesDir, "datastore/$name.preferences_pb")
