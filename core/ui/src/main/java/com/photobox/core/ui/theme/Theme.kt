package com.photobox.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = AccentOrange,
    onPrimary = OnAccent,
    secondary = AccentRed,
    onSecondary = OnAccent,
    background = Background,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextSub,
    outline = LineStrong,
    error = Danger,
    onError = OnAccent,
)

private val LightColors = lightColorScheme(
    primary = AccentOrange,
    onPrimary = OnAccent,
    secondary = AccentRed,
    onSecondary = OnAccent,
    background = Background,           // 即使 light 也保持暗色，符合产品视觉
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextSub,
    outline = LineStrong,
    error = Danger,
    onError = OnAccent,
)

@Composable
fun PhotoBoxTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // 设计稿统一暗色，暂不跟随系统主题
    val colorScheme = DarkColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = PhotoBoxTypography,
        shapes = PhotoBoxShapes,
        content = content,
    )
}

// 保留 LightColors 引用以避免 lint 警告（未来支持明亮模式时使用）
@Suppress("unused")
private val _unused = LightColors