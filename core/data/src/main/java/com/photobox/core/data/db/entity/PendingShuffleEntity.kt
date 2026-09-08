package com.photobox.core.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_shuffle")
data class PendingShuffleEntity(
    @PrimaryKey val mediaId: Long,
    val discoveredAt: Long,
)