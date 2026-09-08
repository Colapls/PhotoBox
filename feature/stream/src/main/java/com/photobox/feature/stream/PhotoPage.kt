package com.photobox.feature.stream

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage

/**
 * 单页：黑底，Coil AsyncImage 加载，双击触发点赞动画。
 * 不在本页处理 like/favorite 状态切换（由 VM 通过 likeTrigger 回调）。
 */
@Composable
fun PhotoPage(
    imageUri: String,
    onDoubleTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var likeTrigger by remember { mutableIntStateOf(0) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(imageUri) {
                detectTapGestures(
                    onDoubleTap = {
                        likeTrigger += 1
                        onDoubleTap()
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = imageUri,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
        LikeAnimation(
            triggerKey = likeTrigger,
            modifier = Modifier.fillMaxSize(),
        )
    }
}