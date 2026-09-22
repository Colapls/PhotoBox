package com.photobox.feature.stream

import com.photobox.core.data.mediastore.MediaItem

/**
 * 流屏幕的 UI 状态。
 * - items: 当前队列中所有 media 的元信息（按洗牌后顺序）。
 * - currentIndex: 用户当前停留的位置。
 * - liked / favorite ids: 用于头像状态展示（独立 Flow 合并到此处，VM 内部 use combine）。
 * - roundFinished: 是否已看完整轮（用于显示"再来一轮"按钮）。
 * - isLoading: 启动后是否还在第一次扫描 MediaStore（避免一打开就闪"空相册"）。
 *   与 isEmpty 互斥：isLoading=true 时不显示"空相册"。
 * - isEmpty: 当前筛选条件下相册里没有匹配项。**设置按钮依然可见**，会弹「不存在这样的照片」
 *   提示框；点确认后自动 fallback 到不筛选模式（MIXED + ALL）重洗。
 * - filterEmptyPromptVisible: 控制空筛选提示 AlertDialog 的可见性。
 * - onThisDayAvailable: 往年今天是否有照片，决定是否在设置按钮下方挂「那年今日」入口。
 */
data class StreamUiState(
    val items: List<MediaItem> = emptyList(),
    val currentIndex: Int = 0,
    val likedIds: Set<Long> = emptySet(),
    val favoriteIds: Set<Long> = emptySet(),
    val roundFinished: Boolean = false,
    val isLoading: Boolean = true,
    val isEmpty: Boolean = false,
    val filterEmptyPromptVisible: Boolean = false,
    val viewedCount: Int = 0,
    val lastDeleteFailed: Boolean = false,
    val onThisDayAvailable: Boolean = false,
) {
    val currentItem: MediaItem?
        get() = items.getOrNull(currentIndex)
}