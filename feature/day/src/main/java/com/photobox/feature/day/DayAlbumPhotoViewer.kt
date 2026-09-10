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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.photobox.core.media.VideoPlayer

/**
 * 当天相册的全屏查看器：复用 DayAlbumViewModel 的 state（保证数据与 grid 一致）。
 * 点击返回或 swipe back 退出。
 */
@Composable
fun DayAlbumPhotoViewer(
    initialMediaId: Long,
    onBack: () -> Unit,
    viewModel: DayAlbumViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val initialIndex = state.items.indexOfFirst { it.mediaId == initialMediaId }.coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = initialIndex) { state.items.size }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (state.items.isNotEmpty()) {
            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                key = { state.items[it].mediaId },
            ) { pageIndex ->
                val item = state.items[pageIndex]
                if (item.isVideo) {
                    VideoPlayer(
                        uri = item.uri,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    AsyncImage(
                        model = item.uri,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    )
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
