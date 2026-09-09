package com.photobox.feature.stream

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.photobox.core.ui.theme.PhotoBoxTheme

@Composable
fun StreamRoute(
    modifier: Modifier = Modifier,
    onSwipeToDayAlbum: (dateMs: Long) -> Unit = {},
) {
    PhotoBoxTheme {
        StreamScreen(modifier = modifier, onSwipeToDayAlbum = onSwipeToDayAlbum)
    }
}
