package com.photobox.core.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.photobox.core.data.db.entity.FavoriteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: FavoriteEntity)

    @Query("DELETE FROM favorite WHERE mediaId = :mediaId")
    suspend fun delete(mediaId: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM favorite WHERE mediaId = :mediaId)")
    fun observeIsFavorite(mediaId: Long): Flow<Boolean>

    @Query("SELECT mediaId FROM favorite")
    fun observeAllIds(): Flow<List<Long>>

    @Query("SELECT mediaId FROM favorite")
    suspend fun readAllIds(): List<Long>
}