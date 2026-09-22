package com.photobox.feature.day

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.photobox.core.media.VideoPlayer
import com.photobox.core.media.ZoomablePhotoViewer

/**
 * 当天相册的全屏查看器：
 * - 这是一个独立路由，Hilt 会给我们一个新的 [DayAlbumViewModel] 实例；
 *   必须主动 `load(dateMs)`，否则 items 为空 → 黑屏。
 * - 每页用 ZoomablePhotoViewer / VideoPlayer；视频页传 isActive 保证翻页停止声音。
 * - 点击返回或 swipe back 退出。
 */
@Composable
fun DayAlbumPhotoViewer(
    dateMs: Long,
    initialMediaId: Long,
    onBack: () -> Unit,
    viewModel: DayAlbumViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(dateMs) {
        viewModel.load(dateMs)
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (state.items.isNotEmpty()) {
            // 用 items.size + initialMediaId 作为 key：首次组合时 items 为空不会进入这里；
            // load() 完成后 items 变化 → key block 重建 → 用正确的 initialIndex 创建 pager。
            // 否则直接 rememberPagerState(initialPage = -1.coerceAtLeast(0) = 0) 会把用户
            // 点的那张照片错位到第一张。
            key(state.items.size, initialMediaId) {
                val initialIndex = state.items
                    .indexOfFirst { it.mediaId == initialMediaId }
                    .coerceAtLeast(0)
                val pagerState = rememberPagerState(initialPage = initialIndex) { state.items.size }
                VerticalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    key = { state.items[it].mediaId },
                ) { pageIndex ->
                    val item = state.items[pageIndex]
                    if (item.isVideo) {
                        VideoPlayer(
                            uri = item.uri,
                            isActive = pageIndex == pagerState.currentPage,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        ZoomablePhotoViewer(
                            uri = item.uri,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
        IconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "返回",
                tint = Color.White,
            )
        }
    }
}