package com.photobox.feature.stream

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color

/**
 * 心形点赞动画：0 → 1.1 → 1.0，800ms，自动消失。
 * 触发条件：父组件 double-tap → 设置 triggerKey 变化。
 */
@Composable
fun LikeAnimation(
    triggerKey: Int,
    modifier: Modifier = Modifier,
) {
    val scale = remember { Animatable(0f) }
    LaunchedEffect(triggerKey) {
        if (triggerKey == 0) return@LaunchedEffect
        scale.animateTo(1.1f, animationSpec = tween(200))
        scale.animateTo(1.0f, animationSpec = tween(200))
        kotlinx.coroutines.delay(400)
        scale.animateTo(0f, animationSpec = tween(200))
    }
    if (scale.value > 0f) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = Color(0xFFFF3D5E),
                modifier = Modifier.scale(scale.value),
            )
        }
    }
}