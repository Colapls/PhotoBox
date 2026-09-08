package com.photobox.core.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 单行（id 恒为 1）；保存当前洗牌序列和进度。
 * lastShuffledMediaIds 记录本轮已浏览的 mediaId（去重用），逗号分隔。
 */
@Entity(tableName = "shuffle_state")
data class ShuffleStateEntity(
    @PrimaryKey val id: Int = 1,
    val queueMediaIds: List<Long>,
    val currentIndex: Int,
    val roundStartedAt: Long,
    val lastShuffledMediaIds: List<Long>,
)