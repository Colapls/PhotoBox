package com.photobox.core.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource

/**
 * PhotoBox 品牌 Logo：来自 docs/logo.svg 的矢量图（弹出的照片卡 + 盒子 + 星光）。
 * 自带渐变背景，叠在任意颜色上都没问题。
 */
@Composable
fun BrandLogo(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(id = R.drawable.ic_brand_logo),
            contentDescription = "PhotoBox Logo",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
        )
    }
}
