package com.photobox.feature.profile

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.photobox.core.ui.theme.PhotoBoxTheme

@Composable
fun ProfileRoute(
    onBack: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenLikes: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PhotoBoxTheme {
        ProfileScreen(
            onBack = onBack,
            onOpenFavorites = onOpenFavorites,
            onOpenLikes = onOpenLikes,
            modifier = modifier,
        )
    }
}
