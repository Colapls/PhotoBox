package com.photobox.core.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.photobox.core.data.db.entity.PendingShuffleEntity

@Dao
interface PendingShuffleDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(entity: PendingShuffleEntity)

    @Query("SELECT mediaId FROM pending_shuffle")
    suspend fun readAllIds(): List<Long>

    @Query("DELETE FROM pending_shuffle")
    suspend fun clear()
}