package com.photobox.feature.applock

data class AppLockUiState(
    val isLocked: Boolean = true,
    val isBiometricAvailable: Boolean = false,
    val errorMessage: String? = null,
)
