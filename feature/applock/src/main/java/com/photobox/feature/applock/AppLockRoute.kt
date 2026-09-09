package com.photobox.feature.applock

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.photobox.core.ui.theme.PhotoBoxTheme

@Composable
fun AppLockRoute(modifier: Modifier = Modifier) {
    PhotoBoxTheme { AppLockScreen(modifier = modifier) }
}
