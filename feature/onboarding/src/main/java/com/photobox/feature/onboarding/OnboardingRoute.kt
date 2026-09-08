package com.photobox.feature.onboarding

import androidx.compose.runtime.Composable
import com.photobox.core.ui.theme.PhotoBoxTheme

/**
 * Feature 模块对外暴露的入口。封装主题 + Screen，便于 app 模块 Navigation Compose 调用。
 */
@Composable
fun OnboardingRoute(
    onCompleted: () -> Unit,
) {
    PhotoBoxTheme {
        OnboardingScreen(onCompleted = onCompleted)
    }
}
