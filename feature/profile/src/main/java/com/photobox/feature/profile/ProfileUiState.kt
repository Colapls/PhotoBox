package com.photobox.feature.profile

import com.photobox.core.data.repository.ProfileStats

data class ProfileUiState(
    val stats: ProfileStats? = null,
    val isLoading: Boolean = true,
    /** 往年今天有多少张照片；>0 时才显示「那年今日」入口。 */
    val onThisDayCount: Int = 0,
)