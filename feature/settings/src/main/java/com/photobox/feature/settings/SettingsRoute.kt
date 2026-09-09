package com.photobox.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.photobox.core.ui.theme.PhotoBoxTheme

@Composable
fun SettingsRoute(onBack: () -> Unit, modifier: Modifier = Modifier) {
    PhotoBoxTheme { SettingsScreen(onBack = onBack, modifier = modifier) }
}
