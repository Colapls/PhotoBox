package com.photobox.core.media

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem as Media3MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage

/**
 * 包装 Media3 ExoPlayer 为 Compose Composable。
 *
 * 关键设计：
 * - **不自动播放**：必须传 `isActive = true` 才 prepare + play。
 *   父层 VerticalPager 在 `beyondViewportPageCount = 1` 下会预加载相邻页的视频，
 *   之前默认 playWhenReady = true 导致"还没划到视频就有声音" + "划走还响"。
 *   现在仅当 pagerState.currentPage == 此页时 isActive 才为 true，
 *   划走立刻 pause，划回再 resume（player 仍 alive）。
 * - **首帧占位**：isActive=false 时显示 [AsyncImage]（由 Coil + VideoFrameDecoder 抽首帧），
 *   加上 ▶ 图标，避免黑屏。
 * - `DisposableEffect(uri)` 全权负责 ExoPlayer 的 prepare / release 生命周期。
 * - `Player.Listener` 捕获 PlaybackException → 渲染 "无法播放" overlay。
 */
@Composable
fun VideoPlayer(
    uri: String,
    isActive: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val exoPlayer = remember(uri) { ExoPlayer.Builder(context).build() }
    var error by remember { mutableStateOf<PlaybackException?>(null) }
    var mediaAttached by remember(uri) { mutableStateOf(false) }

    DisposableEffect(uri) {
        exoPlayer.setMediaItem(Media3MediaItem.fromUri(uri))
        mediaAttached = true
        val listener = object : Player.Listener {
            override fun onPlayerError(e: PlaybackException) {
                error = e
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    LaunchedEffect(isActive, mediaAttached) {
        if (!mediaAttached) return@LaunchedEffect
        if (isActive) {
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        } else {
            exoPlayer.playWhenReady = false
        }
    }

    Box(modifier = modifier.background(Color.Black)) {
        if (isActive) {
            // 当前页：ExoPlayer surface 接管显示
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        setOnTouchListener { _, _ -> false }
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                    }
                },
                modifier = Modifier.matchParentSize(),
            )
        } else {
            // 非当前页：Coil 抽首帧 + ▶ 占位
            AsyncImage(
                model = uri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "视频",
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.align(Alignment.Center),
            )
        }
        if (error != null) {
            Text(
                text = "无法播放",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}