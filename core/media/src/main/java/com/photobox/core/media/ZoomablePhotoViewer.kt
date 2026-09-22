package com.photobox.core.media

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.size.Size

/**
 * 可缩放 + 单指拖动的图片查看器。
 *
 * 设计要点：
 * - 始终请求原图分辨率（Size.ORIGINAL），Coil 解码到 ARGB_8888，5x 缩放后仍有像素细节。
 * - graphicsLayer 应用 scale + translation，无中间位图重建 → 缩放过程 GPU 直接合成，流畅。
 * - **双指 pinch**：以**图片中心**为锚点缩放（用户要求"始终以照片中心为中心来放大"），
 *   同时记录两指间距离变化计算 zoomDelta；缩放过程中的两指 centroid 平移合并到 offset。
 * - **单指拖动**：仅当 scale > 1f 时才接管并消费事件 → 平移查看细节；
 *   scale == 1f 时不消费 → 事件放行给父 VerticalPager 处理垂直翻页。
 *   （detectTransformGestures 会无差别消费单指拖动，会把翻页卡死 —— 这是 custom 手势的原因。）
 */
@Composable
fun ZoomablePhotoViewer(
    uri: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var scale by remember(uri) { mutableFloatStateOf(1f) }
    var offset by remember(uri) { mutableStateOf(Offset.Zero) }
    var viewport by remember(uri) { mutableStateOf(IntSize.Zero) }

    val request = remember(uri) {
        ImageRequest.Builder(context)
            .data(uri)
            .size(Size.ORIGINAL)
            .build()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .onSizeChanged { viewport = it },
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = request,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                }
                .pointerInput(uri) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)

                        var prevDistance: Float? = null
                        // 单指拖动用：上一帧 pointer 位置
                        var prevPointerPos: Offset? = null

                        while (true) {
                            val event = awaitPointerEvent()
                            val pressed = event.changes.filter { it.pressed }

                            if (pressed.isEmpty()) break

                            if (pressed.size >= 2) {
                                // 双指 pinch：以图片中心为锚点。
                                val p0 = pressed[0].position
                                val p1 = pressed[1].position
                                val distance = (p0 - p1).getDistance()
                                val centroid = (p0 + p1) / 2f

                                if (prevDistance != null && prevDistance > 0f) {
                                    val zoomDelta = distance / prevDistance
                                    applyZoomCentered(
                                        zoomDelta = zoomDelta,
                                        centroid = centroid,
                                        get = { scale to offset },
                                        set = { s, o -> scale = s; offset = o },
                                        viewport = viewport,
                                    )
                                }
                                prevDistance = distance
                                prevPointerPos = null

                                // 消费避免 VerticalPager 误触翻页
                                pressed.forEach { it.consume() }
                            } else {
                                // 单指：只在已放大时接管 → 单指拖动查看细节
                                prevDistance = null
                                val pointer = pressed.first()
                                val current = pointer.position
                                if (scale > 1f && prevPointerPos != null) {
                                    val delta = current - prevPointerPos!!
                                    val newOffset = clampOffset(
                                        offset + delta,
                                        scale,
                                        viewport,
                                    )
                                    offset = newOffset
                                    pointer.consume()
                                }
                                prevPointerPos = current
                            }
                        }
                    }
                },
        )
    }
}

/**
 * 以图片中心为锚点缩放：
 * - 缩放前 offset' = 0 时，scale 改变后图像仍以 viewport 中心为基点放大。
 * - 缩放前已有 offset 时，按 viewport center 重设基准：
 *   把缩放中心（centroid）相对 viewport center 的位置，按新/旧 scale 比值反向补偿，
 *   让 centroid 处的图像点保持在 centroid 处（避免图像"飞走"）。
 */
private fun applyZoomCentered(
    zoomDelta: Float,
    centroid: Offset,
    get: () -> Pair<Float, Offset>,
    set: (Float, Offset) -> Unit,
    viewport: IntSize,
) {
    val (oldScale, oldOffset) = get()
    val newScale = (oldScale * zoomDelta).coerceIn(1f, MAX_ZOOM)
    if (newScale == oldScale) return

    val center = Offset(viewport.width / 2f, viewport.height / 2f)
    // 当前 centroid 在 viewport 里的位置是 oldOffset + centroid；
    // 我们希望缩放后图像仍让 centroid 处显示原像素，即：
    // newOffset + centroid = center + (oldOffset + centroid - center) * (newScale / oldScale)
    val ratio = if (oldScale == 0f) 1f else newScale / oldScale
    var newOffset = center + (oldOffset + centroid - center) * ratio - centroid

    var finalScale = newScale
    if (finalScale <= 1f) {
        finalScale = 1f
        newOffset = Offset.Zero
    } else {
        newOffset = clampOffset(newOffset, finalScale, viewport)
    }
    set(finalScale, newOffset)
}

/** 防止放大后图片被拖得太靠边看不到。保留 200dp 余量。 */
private fun clampOffset(offset: Offset, scale: Float, viewport: IntSize): Offset {
    val slop = 200f
    val maxX = viewport.width * (scale - 1f) / 2f + slop
    val maxY = viewport.height * (scale - 1f) / 2f + slop
    return Offset(
        x = offset.x.coerceIn(-maxX, maxX),
        y = offset.y.coerceIn(-maxY, maxY),
    )
}

private const val MAX_ZOOM = 5f