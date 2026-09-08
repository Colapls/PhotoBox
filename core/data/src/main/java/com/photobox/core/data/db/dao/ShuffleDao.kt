package com.photobox.core.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.photobox.core.data.db.entity.ShuffleStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShuffleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ShuffleStateEntity)

    @Query("SELECT * FROM shuffle_state WHERE id = 1")
    fun observeState(): Flow<ShuffleStateEntity?>

    @Query("SELECT * FROM shuffle_state WHERE id = 1")
    suspend fun readState(): ShuffleStateEntity?

    @Query("DELETE FROM shuffle_state")
    suspend fun clear()
}