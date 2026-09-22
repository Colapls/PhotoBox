package com.photobox.feature.profile

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.photobox.core.ui.theme.PhotoBoxTheme

@Composable
fun ViewedListRoute(
    onBack: () -> Unit,
    onPhotoClick: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    PhotoBoxTheme {
        ViewedListScreen(
            onBack = onBack,
            onPhotoClick = onPhotoClick,
            modifier = modifier,
        )
    }
}
