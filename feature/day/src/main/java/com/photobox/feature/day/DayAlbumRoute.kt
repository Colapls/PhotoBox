package com.photobox.feature.day

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.photobox.core.ui.theme.PhotoBoxTheme

@Composable
fun DayAlbumRoute(
    dateMs: Long,
    onBack: () -> Unit,
    onPhotoClick: (mediaId: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    PhotoBoxTheme {
        DayAlbumScreen(
            dateMs = dateMs,
            onBack = onBack,
            onPhotoClick = onPhotoClick,
            modifier = modifier,
        )
    }
}