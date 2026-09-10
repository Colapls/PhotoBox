package com.photobox.core.media

import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

/**
 * 包装 Media3 ExoPlayer 为 Compose Composable。
 *
 * - `remember(uri)` 在 uri 变化时重建 ExoPlayer（避免同一 Surface 复用旧 media）。
 * - `DisposableEffect(uri)` 负责 prepare / release 生命周期。
 * - `Player.Listener` 捕获 PlaybackException → 渲染 "无法播放" overlay。
 *
 * 调用方负责把 URI 作为 `String` 传入；uri 一般来自 MediaStore 的 content URI
 * (e.g. `content://media/external/video/media/12345`)。
 */
@Composable
fun VideoPlayer(uri: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val exoPlayer = remember(uri) { ExoPlayer.Builder(context).build() }
    var error by remember { mutableStateOf<PlaybackException?>(null) }

    DisposableEffect(uri) {
        exoPlayer.setMediaItem(MediaItem.fromUri(uri))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
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

    Box(modifier = modifier) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                }
            },
            modifier = Modifier.matchParentSize(),
        )
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