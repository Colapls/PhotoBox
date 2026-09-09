package com.photobox.feature.settings

import com.photobox.core.data.datastore.UserPreferences

data class SettingsUiState(
    val prefs: UserPreferences = com.photobox.core.data.datastore.UserPreferences(),
    val availableAlbums: List<String> = emptyList(),
    val isLoading: Boolean = true,
    val cacheSizeBytes: Long = 0L,
    val toast: String? = null,
)