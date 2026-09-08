package com.photobox.feature.stream

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 随机照片流（Plan 2 实现）。
 * 当前 Plan 仅显示空状态占位，确认路由通畅。
 */
@Composable
fun StreamScreen(modifier: Modifier = Modifier) {
    EmptyState(
        title = "相册里一张照片都没有",
        description = "去系统相机拍几张，回来看这里能不能刷出来",
        modifier = modifier,
    )
}
