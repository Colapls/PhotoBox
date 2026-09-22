package com.photobox.feature.profile

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.photobox.core.ui.theme.PhotoBoxTheme

@Composable
fun LikesListRoute(
    onBack: () -> Unit,
    onPhotoClick: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    PhotoBoxTheme {
        LikesListScreen(
            onBack = onBack,
            onPhotoClick = onPhotoClick,
            modifier = modifier,
        )
    }
}
