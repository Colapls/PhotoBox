package com.photobox.feature.day

import com.photobox.core.data.mediastore.MediaItem

data class DayAlbumUiState(
    val items: List<MediaItem> = emptyList(),
    val dateMs: Long = System.currentTimeMillis(),
    val likedIds: Set<Long> = emptySet(),
    val favoriteIds: Set<Long> = emptySet(),
    val isLoading: Boolean = true,
    /** 长按进入选择模式。true 时点缩略图 = 选中/取消选中；false 时 = 打开大图。 */
    val isSelectionMode: Boolean = false,
    val selectedIds: Set<Long> = emptySet(),
)