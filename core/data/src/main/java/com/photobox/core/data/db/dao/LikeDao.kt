package com.photobox.core.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.photobox.core.data.db.entity.LikeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LikeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: LikeEntity)

    @Query("DELETE FROM like WHERE mediaId = :mediaId")
    suspend fun delete(mediaId: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM like WHERE mediaId = :mediaId)")
    fun observeIsLiked(mediaId: Long): Flow<Boolean>

    @Query("SELECT mediaId FROM like")
    fun observeAllIds(): Flow<List<Long>>
}