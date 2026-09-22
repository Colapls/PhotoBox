package com.photobox.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.photobox.core.data.db.converter.LongListConverter
import com.photobox.core.data.db.dao.FavoriteDao
import com.photobox.core.data.db.dao.LikeDao
import com.photobox.core.data.db.dao.PendingShuffleDao
import com.photobox.core.data.db.dao.ShuffleDao
import com.photobox.core.data.db.dao.ViewedDao
import com.photobox.core.data.db.entity.FavoriteEntity
import com.photobox.core.data.db.entity.LikeEntity
import com.photobox.core.data.db.entity.PendingShuffleEntity
import com.photobox.core.data.db.entity.ShuffleStateEntity
import com.photobox.core.data.db.entity.ViewedEntity

@Database(
    entities = [
        FavoriteEntity::class,
        LikeEntity::class,
        ShuffleStateEntity::class,
        PendingShuffleEntity::class,
        ViewedEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
@TypeConverters(LongListConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun likeDao(): LikeDao
    abstract fun shuffleDao(): ShuffleDao
    abstract fun pendingShuffleDao(): PendingShuffleDao
    abstract fun viewedDao(): ViewedDao
}
