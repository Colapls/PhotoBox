package com.photobox.core.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 用户已浏览过的照片。同 mediaId 多次浏览只保留首次时间。
 */
@Entity(tableName = "viewed")
data class ViewedEntity(
    @PrimaryKey val mediaId: Long,
    val viewedAt: Long,
)
