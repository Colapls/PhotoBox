package com.photobox.feature.day

import com.photobox.core.data.mediastore.MediaItem

data class DayAlbumUiState(
    val items: List<MediaItem> = emptyList(),
    val dateMs: Long = System.currentTimeMillis(),
    val likedIds: Set<Long> = emptySet(),
    val favoriteIds: Set<Long> = emptySet(),
    val isLoading: Boolean = true,
)