package com.photobox.feature.stream

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.size.Size
import com.photobox.core.data.mediastore.MediaItem
import com.photobox.core.media.MediaMetadataOverlay
import com.photobox.core.media.VideoPlayer

private const val MAX_ZOOM = 5f

@Composable
fun PhotoPage(
    item: MediaItem,
    onDoubleTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val imageUri = item.uri
    var likeTrigger by remember { mutableIntStateOf(0) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(imageUri) {
                detectTapGestures(onDoubleTap = {
                    likeTrigger += 1
                    onDoubleTap()
                })
            },
        contentAlignment = Alignment.Center,
    ) {
        if (item.isVideo) {
            VideoPlayer(
                uri = imageUri,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            ZoomableImage(
                imageUri = imageUri,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // 动图标识：mimeType == image/gif 时左下角叠一个 GIF 徽标
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
        LikeAnimation(
            triggerKey = likeTrigger,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/**
 * 可缩放/平移的图片。
 *
 * 缩放 1x–5x：基于 [detectTransformGestures] 统一处理 pinch-zoom + pan，
 * 缩放以 centroid 为锚点；放大后单指拖动即平移。
 *
 * 清晰度：传给 Coil 的 [Size.ORIGINAL]，请求原图分辨率解码 bitmap，
 * 5x 缩放后仍然有像素细节。
 *
 * 回到 1x 时自动归位 offset。
 */
@Composable
private fun ZoomableImage(
    imageUri: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var scale by remember(imageUri) { mutableFloatStateOf(1f) }
    var offset by remember(imageUri) { mutableStateOf(Offset.Zero) }

    val request = remember(imageUri) {
        ImageRequest.Builder(context)
            .data(imageUri)
            .size(Size.ORIGINAL)
            .build()
    }

    AsyncImage(
        model = request,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            }
            .pointerInput(imageUri) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(1f, MAX_ZOOM)
                    val ratio = newScale / scale
                    // 缩放以 centroid 为锚点 + 加上手指 pan
                    offset = centroid + (offset - centroid) * ratio + pan
                    scale = newScale
                    if (scale <= 1f) {
                        scale = 1f
                        offset = Offset.Zero
                    }
                }
            },
    )
}