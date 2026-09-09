package com.photobox.feature.profile

import com.photobox.core.data.repository.ProfileStats

data class ProfileUiState(
    val stats: ProfileStats? = null,
    val isLoading: Boolean = true,
)