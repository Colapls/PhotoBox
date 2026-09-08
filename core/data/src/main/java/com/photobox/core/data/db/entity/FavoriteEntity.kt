package com.photobox.core.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite")
data class FavoriteEntity(
    @PrimaryKey val mediaId: Long,
    val addedAt: Long,
    val mediaUri: String,
    val dateTaken: Long,
)