package com.photobox.feature.stream

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.photobox.core.data.mediastore.MediaItem
import com.photobox.core.media.MediaMetadataOverlay
import com.photobox.core.media.VideoPlayer
import com.photobox.core.media.ZoomablePhotoViewer

/**
 * 单张照片的全屏渲染：
 * - 图片用 [ZoomablePhotoViewer]（双指缩放 5x）。
 * - 视频用 [VideoPlayer]，传入 isActive 避免相邻页预加载发声。
 * - **双击点赞** 在此层用自定义 [awaitEachGesture] 实现，不消费 DOWN，
 *   所以 VerticalPager 仍能正常拿走 DOWN 处理垂直翻页 —— 不冲突。
 *   （用 [detectTapGestures] 会强制 consume DOWN → 卡死翻页。）
 * - **双击动效**：当 burstTick 变化时，在屏幕中央放大弹出一个红色爱心，
 *   0.6s 内 scale 0.4 → 1.2、alpha 1 → 0。
 */
@Composable
fun PhotoPage(
    item: MediaItem,
    isActive: Boolean,
    onDoubleTap: () -> Unit,
    modifier: Modifier = Modifier,
    burstTick: Int = 0,
) {
    val imageUri = item.uri
    val scale = remember { Animatable(0f) }
    val alpha = remember { Animatable(0f) }

    // burstTick 是单调递增计数器；每次 +1 重置动画到初始态再播放一次。
    LaunchedEffect(burstTick) {
        if (burstTick == 0) return@LaunchedEffect
        scale.snapTo(0.4f)
        alpha.snapTo(1f)
        scale.animateTo(1.2f, animationSpec = tween(durationMillis = 250))
        scale.animateTo(1.0f, animationSpec = tween(durationMillis = 100))
        alpha.animateTo(0f, animationSpec = tween(durationMillis = 350))
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(item.mediaId) {
                // 被动双击检测：不 consume DOWN / MOVE / UP。
                // 通过两次 UP 之间的时间差判断是否是 double-tap：
                //   1) 一次 DOWN-UP 序列（tap）：记录最后一次 UP 时间。
                //   2) 第二次 DOWN-UP 在 300ms 内出现 → 触发 onDoubleTap，清零防止三连击。
                // 任何事件都不消费 → VerticalPager 拿到的还是 unconsumed 事件，翻页不受影响。
                var lastUpUptimeMs = 0L
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val downUptimeMs = down.uptimeMillis
                    while (true) {
                        val event = awaitPointerEvent()
                        val allUp = event.changes.all { !it.pressed }
                        if (!allUp) continue
                        val upUptimeMs = event.changes.first().uptimeMillis
                        val tapDuration = upUptimeMs - downUptimeMs
                        val sinceLastUp = upUptimeMs - lastUpUptimeMs
                        if (tapDuration < 250L && sinceLastUp in 1L..300L) {
                            onDoubleTap()
                            lastUpUptimeMs = 0L  // 防三连击
                        } else if (tapDuration < 250L) {
                            lastUpUptimeMs = upUptimeMs
                        }
                        break
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        if (item.isVideo) {
            VideoPlayer(
                uri = imageUri,
                isActive = isActive,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            ZoomablePhotoViewer(
                uri = imageUri,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // 双击爆发的爱心：始终叠在屏幕上，scale/alpha 驱动显隐。
        if (burstTick > 0) {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = null,
                tint = Color(0xFFFF3344),
                modifier = Modifier
                    .size(120.dp)
                    .scale(scale.value)
                    .alpha(alpha.value),
            )
        }

        // 动图标识：mimeType == image/gif 时左上角叠一个 GIF 徽标
        if (item.mimeType.equals("image/gif", ignoreCase = true)) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 80.dp, start = 16.dp)
                    .background(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(4.dp),
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    text = "GIF",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }

        MediaMetadataOverlay(
            dateTakenMs = item.dateTakenMs,
            latitude = item.latitude,
            longitude = item.longitude,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 76.dp),
        )
    }
}
