package com.photobox.core.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "like")
data class LikeEntity(
    @PrimaryKey val mediaId: Long,
    val likedAt: Long,
)