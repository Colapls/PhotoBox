package com.photobox.core.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.photobox.core.data.db.entity.ViewedEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ViewedDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun upsert(entity: ViewedEntity)

    @Query("SELECT mediaId FROM viewed ORDER BY viewedAt DESC")
    fun observeAllIds(): Flow<List<Long>>

    @Query("SELECT COUNT(*) FROM viewed")
    fun observeCount(): Flow<Int>
}
