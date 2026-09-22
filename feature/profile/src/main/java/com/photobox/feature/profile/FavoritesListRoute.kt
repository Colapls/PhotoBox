package com.photobox.feature.profile

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.photobox.core.ui.theme.PhotoBoxTheme

@Composable
fun FavoritesListRoute(
    onBack: () -> Unit,
    onPhotoClick: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    PhotoBoxTheme {
        FavoritesListScreen(
            onBack = onBack,
            onPhotoClick = onPhotoClick,
            modifier = modifier,
        )
    }
}
