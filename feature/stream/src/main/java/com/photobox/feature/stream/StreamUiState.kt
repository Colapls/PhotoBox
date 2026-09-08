package com.photobox.feature.stream

import com.photobox.core.data.mediastore.MediaItem

/**
 * 流屏幕的 UI 状态。
 * - items: 当前队列中所有 media 的元信息（按洗牌后顺序）。
 * - currentIndex: 用户当前停留的位置。
 * - liked / favorite ids: 用于头像状态展示（独立 Flow 合并到此处，VM 内部 use combine）。
 * - roundFinished: 是否已看完整轮（用于显示"再来一轮"按钮）。
 * - isEmpty: 相册本身为空（区别于 roundFinished）。
 */
data class StreamUiState(
    val items: List<MediaItem> = emptyList(),
    val currentIndex: Int = 0,
    val likedIds: Set<Long> = emptySet(),
    val favoriteIds: Set<Long> = emptySet(),
    val roundFinished: Boolean = false,
    val isEmpty: Boolean = false,
) {
    val currentItem: MediaItem?
        get() = items.getOrNull(currentIndex)
}
